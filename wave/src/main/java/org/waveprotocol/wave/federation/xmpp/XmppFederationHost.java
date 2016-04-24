/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.wave.federation.xmpp;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.typesafe.config.Config;
import org.dom4j.Element;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.FederationHostBridge;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.federation.xmpp.XmppUtil.UnknownSignerType;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.id.WaveletName;
import org.xmpp.packet.IQ;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * This class encapsulates the incoming packet processing portion of the
 * Federation Host. Messages arrive on this class from a foreign Federation
 * Remote for wavelets hosted by the local wave server.
 */
public class XmppFederationHost implements WaveletFederationListener.Factory {
  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(XmppFederationHost.class.getCanonicalName());

  private final WaveletFederationProvider waveletProvider;

  private XmppManager manager = null;

  // A map of update listeners. There is one per remote domain we are sending updates to.
  // The name 'listener' refers to them listening for updates from the waveserver to send to the
  // network.
  private final LoadingCache<String, WaveletFederationListener> listeners;

  /**
   * Constructor. Note that {@link #setManager} must be called before this class
   * is ready to use.
   *
   * @param waveletProvider used for communicating back to the Host part of the
   *        wavelet server.
   * @param disco used for discovery
   */
  @Inject
  public XmppFederationHost(@FederationHostBridge WaveletFederationProvider waveletProvider,
      final XmppDisco disco, final Config config) {
    this.waveletProvider = waveletProvider;
    listeners = CacheBuilder.newBuilder().build(new CacheLoader<String, WaveletFederationListener>() {
      @Override
      public WaveletFederationListener load(@SuppressWarnings("NullableProblems") String domain) {
        return new XmppFederationHostForDomain(domain, manager, disco, config);
      }
    });
  }

  /**
   * Set the manager instance for this class. Must be invoked before any other
   * methods are used.
   * @param manager the XmppManager object, used to send packets.
   */
  public void setManager(XmppManager manager) {
    this.manager = manager;
  }

  /**
   * Parse to a ProtocolHashedVersion from a given string version/base64-hash combination.
   *
   * @param startVersion the starting version
   * @param base64Hash   the base64 hash
   * @throws IllegalArgumentException on bad data
   * @return a parsed protobuf object
   */
  private static ProtocolHashedVersion parseFromUnsafe(String startVersion, String base64Hash)
      throws IllegalArgumentException {
    return ProtocolHashedVersion.newBuilder()
        .setVersion(Long.parseLong(startVersion))
        .setHistoryHash(Base64Util.decode(base64Hash)).build();
  }

  /**
   * Reads a history request off the wire, sends it to the WS with a new
   * callback for returning the response.
   * @param request          the history request
   * @param responseCallback the callback to send the response back
   */
  void processHistoryRequest(final IQ request, final PacketCallback responseCallback) {
    Element items = null, historyDelta = null;
    Element pubsubRequest = request.getElement().element("pubsub");
    if (pubsubRequest != null) {
      items = pubsubRequest.element("items");
      if (items != null) {
        historyDelta = items.element("delta-history");
      }
    }
    if (items == null || historyDelta == null
            || historyDelta.attribute("start-version") == null
            || historyDelta.attribute("start-version-hash") == null
            || historyDelta.attribute("end-version") == null
            || historyDelta.attribute("end-version-hash") == null
            || historyDelta.attribute("wavelet-name") == null) {
      responseCallback.error(FederationErrors.badRequest("Malformed history request"));
      return;
    }

    final ProtocolHashedVersion startVersion;
    try {
      startVersion = parseFromUnsafe(historyDelta.attributeValue("start-version"),
          historyDelta.attributeValue("start-version-hash"));
    } catch (IllegalArgumentException e) {
      responseCallback.error(FederationErrors.badRequest("Invalid format of start version"));
      return;
    }

    final ProtocolHashedVersion endVersion;
    try {
      endVersion = parseFromUnsafe(historyDelta.attributeValue("end-version"),
          historyDelta.attributeValue("end-version-hash"));
    } catch (IllegalArgumentException e) {
      responseCallback.error(FederationErrors.badRequest("Invalid format of end version"));
      return;
    }

    final long responseLengthLimit;
    if (historyDelta.attribute("response-length-limit") != null) {
      try {
        responseLengthLimit = Long.parseLong(historyDelta.attributeValue("response-length-limit"));
      } catch (NumberFormatException e) {
        responseCallback.error(FederationErrors.badRequest("Invalid response length limit"));
        return;
      }
    } else {
      responseLengthLimit = 0;
    }

    final WaveletName waveletName;
    try {
      waveletName =
          XmppUtil.waveletNameCodec.uriToWaveletName(historyDelta.attributeValue("wavelet-name"));
    } catch (EncodingException e) {
      responseCallback.error(FederationErrors.badRequest(
          "Malformed wavelet name: " + historyDelta.attributeValue("wavelet-name")));
      return;
    }

    // Construct a new response listener inline.
    WaveletFederationProvider.HistoryResponseListener listener =
        new WaveletFederationProvider.HistoryResponseListener() {
          @Override
          public void onFailure(FederationError error) {
            responseCallback.error(error);
          }

          @Override
          public void onSuccess(List<ByteString> appliedDeltaSet,
              ProtocolHashedVersion lastCommittedVersion, long versionTruncatedAt) {
            IQ response = IQ.createResultIQ(request);

            Element pubsub = response.setChildElement("pubsub", XmppNamespace.NAMESPACE_PUBSUB);
            Element items = pubsub.addElement("items");

            // Add each delta to the outgoing response.
            for (ByteString appliedDelta : appliedDeltaSet) {
              items.addElement("item").addElement("applied-delta",
                  XmppNamespace.NAMESPACE_WAVE_SERVER).addCDATA(
                  Base64Util.encode(appliedDelta.toByteArray()));
            }

            // Set the LCV history-hash, if provided.
            // TODO(thorogood): We don't set the hashed version, which is wrong,
            // but it's not part of the current spec (Feb 2010).
            if (lastCommittedVersion != null && lastCommittedVersion.hasVersion()) {
              String version = String.valueOf(lastCommittedVersion.getVersion());
              items.addElement("item").addElement("commit-notice",
                  XmppNamespace.NAMESPACE_WAVE_SERVER).addAttribute("version", version);
            }

            // Set the version truncated at, if provided.
            if (versionTruncatedAt > 0) {
              String version = String.valueOf(versionTruncatedAt);
              items.addElement("item").addElement("history-truncated",
                  XmppNamespace.NAMESPACE_WAVE_SERVER).addAttribute("version", version);
            }

            // Send the message to the source.
            responseCallback.run(response);
          }
    };

    // Hand off a history request to the waveletProvider.
    // TODO(thorogood,arb): Note that the following remote domain is going to be
    // the Wave component JID (e.g. wave.foo.com), and *not* the actual remote domain.
    String remoteDomain = request.getFrom().getDomain();
    waveletProvider.requestHistory(waveletName, remoteDomain, startVersion,
        endVersion, responseLengthLimit, listener);
  }

  /**
   * Handles a submit request from a foreign wave remote. Sends it to the wave
   * server, sets up a callback to send the response.
   * @param request          the submit request
   * @param responseCallback the callback to send the response back
   */
  void processSubmitRequest(final IQ request, final PacketCallback responseCallback) {
    Element item = null, submitRequest = null, deltaElement = null;
    Element pubsubRequest = request.getElement().element("pubsub");
    // TODO: check for correct elements.
    Element publish = pubsubRequest.element("publish");
    if (publish != null) {
      item = publish.element("item");
      if (item != null) {
        submitRequest = item.element("submit-request");
        if (submitRequest != null) {
          deltaElement = submitRequest.element("delta");
        }
      }
    }
    if (publish == null || item == null || submitRequest == null
            || deltaElement == null
            || deltaElement.attribute("wavelet-name") == null
            || deltaElement.getText() == null) {
      responseCallback.error(FederationErrors.badRequest("Malformed submit request"));
      return;
    }

    final WaveletName waveletName;
    try {
      waveletName =
          XmppUtil.waveletNameCodec.uriToWaveletName(deltaElement.attributeValue("wavelet-name"));
    } catch (EncodingException e) {
      responseCallback.error(FederationErrors.badRequest(
          "Malformed wavelet name: " + deltaElement.attributeValue("wavelet-name")));
      return;
    }

    final ProtocolSignedDelta delta;
    try {
      delta = ProtocolSignedDelta.parseFrom(Base64Util.decode(deltaElement.getText()));
    } catch (InvalidProtocolBufferException e) {
      responseCallback.error(FederationErrors.badRequest(
          "Malformed delta, not a valid protocol buffer"));
      return;
    }

    // Construct a submit result listener inline.
    WaveletFederationProvider.SubmitResultListener listener =
        new WaveletFederationProvider.SubmitResultListener() {
          @Override
          public void onFailure(FederationError error) {
            responseCallback.error(error);
          }

          @Override
          public void onSuccess(int operations, ProtocolHashedVersion version, long timestamp) {
            IQ response = IQ.createResultIQ(request);

            Element pubsub = response.setChildElement("pubsub", XmppNamespace.NAMESPACE_PUBSUB);
            Element submitResponse = pubsub.addElement("publish").addElement("item")
                .addElement("submit-response", XmppNamespace.NAMESPACE_WAVE_SERVER);

            submitResponse.addAttribute("application-timestamp", String.valueOf(timestamp));
            submitResponse.addAttribute("operations-applied", String.valueOf(operations));

            Element hashedVersion = submitResponse.addElement("hashed-version");
            hashedVersion.addAttribute("history-hash", Base64Util.encode(version.getHistoryHash()));
            hashedVersion.addAttribute("version", String.valueOf(version.getVersion()));

            responseCallback.run(response);
          }
    };

    // Hand off the submit request to the wavelet provider.
    waveletProvider.submitRequest(waveletName, delta, listener);
  }

  /**
   * Reads a get signer request off the wire, sends it to the WS with a new
   * callback for returning the response.
   * @param request          the get signer request
   * @param responseCallback the callback to send the response back
   */
  void processGetSignerRequest(final IQ request, final PacketCallback responseCallback) {
    Element items = request.getChildElement().element("items");
    Element signerRequest = items != null ? items.element("signer-request") : null;

    if (items == null || signerRequest == null
            || signerRequest.attributeValue("wavelet-name") == null
            || signerRequest.attributeValue("signer-id") == null
            || signerRequest.attributeValue("version") == null
            || signerRequest.attributeValue("history-hash") == null) {
      manager.sendErrorResponse(request, FederationErrors.badRequest("Malformed signer request"));
      return;
    }

    final ByteString signerId;
    try {
      signerId = Base64Util.decode(signerRequest.attributeValue("signer-id"));
    } catch (IllegalArgumentException e) {
      responseCallback.error(FederationErrors.badRequest("Malformed signer ID"));
      return;
    }

    final ProtocolHashedVersion deltaEndVersion;
    try {
      deltaEndVersion = parseFromUnsafe(signerRequest.attributeValue("version"),
          signerRequest.attributeValue("history-hash"));
    } catch (IllegalArgumentException e) {
      responseCallback.error(FederationErrors.badRequest("Invalid hashed version"));
      return;
    }

    final WaveletName waveletName;
    try {
      waveletName =
          XmppUtil.waveletNameCodec.uriToWaveletName(signerRequest.attributeValue("wavelet-name"));
    } catch (EncodingException e) {
      responseCallback.error(FederationErrors.badRequest("Malformed wavelet name"));
      return;
    }

    WaveletFederationProvider.DeltaSignerInfoResponseListener listener =
        new WaveletFederationProvider.DeltaSignerInfoResponseListener() {
          @Override
          public void onFailure(FederationError error) {
            responseCallback.error(error);
          }

          @Override
          public void onSuccess(ProtocolSignerInfo signerInfo) {
            IQ response = IQ.createResultIQ(request);

            Element pubsub = response.setChildElement("pubsub", XmppNamespace.NAMESPACE_PUBSUB);
            Element items = pubsub.addElement("items");
            XmppUtil.protocolSignerInfoToXml(signerInfo, items);

            responseCallback.run(response);
          }

        };

    waveletProvider.getDeltaSignerInfo(signerId, waveletName, deltaEndVersion, listener);
  }

  /**
   * Reads a post signer request off the wire, sends it to the WS with a new
   * callback for returning the response.
   * @param request          the post signer request
   * @param responseCallback the callback to send the response back
   */
  void processPostSignerRequest(final IQ request, final PacketCallback responseCallback) {
    Element item = null, signatureElement = null;
    Element pubsubRequest = request.getElement().element("pubsub");
    Element publish = pubsubRequest.element("publish");
    if (publish != null) {
      item = publish.element("item");
      if (item != null) {
        signatureElement = item.element("signature");
      }
    }

    if (publish == null || item == null || signatureElement == null
            || signatureElement.attribute("domain") == null
            || signatureElement.attribute("algorithm") == null
            || signatureElement.element("certificate") == null) {
      responseCallback.error(FederationErrors.badRequest("Malformed post signer request"));
      return;
    }

    ProtocolSignerInfo signer;
    try {
      signer = XmppUtil.xmlToProtocolSignerInfo(signatureElement);
    } catch (UnknownSignerType e) {
      responseCallback.error(FederationErrors.badRequest(
          "Could not understand signer algorithm: " + e));
      return;
    }

    WaveletFederationProvider.PostSignerInfoResponseListener listener =
        new WaveletFederationProvider.PostSignerInfoResponseListener() {
          @Override
          public void onFailure(FederationError error) {
            responseCallback.error(error);
          }

          @Override
          public void onSuccess() {
            IQ response = IQ.createResultIQ(request);

            Element pubsub = response.setChildElement("pubsub", XmppNamespace.NAMESPACE_PUBSUB);
            Element item = pubsub.addElement("publish").addElement("item");

            item.addAttribute("node", "signer");
            item.addElement("signature-response", XmppNamespace.NAMESPACE_WAVE_SERVER);

            responseCallback.run(response);
          }
    };

    // TODO(thorogood,arb): This field is a Bad Idea; it could be faked and not
    // be a provider we host on this instance. Instead, we should infer from the
    // "To:" JID.
    String targetDomain = signatureElement.attributeValue("domain");

    // The first argument is the domain we intend to send this information to.
    waveletProvider.postSignerInfo(targetDomain, signer, listener);
  }

  @Override
  public WaveletFederationListener listenerForDomain(String domain) {
    try {
      // TODO(thorogood): Kick off disco here instead of inside
      // XmppFederationHostForDomain.
      return listeners.get(domain);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
  }
}
