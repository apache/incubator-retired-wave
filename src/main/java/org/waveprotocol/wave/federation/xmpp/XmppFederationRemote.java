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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.typesafe.config.Config;
import org.apache.commons.codec.binary.Base64;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.FederationRemoteBridge;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.federation.xmpp.XmppUtil.UnknownSignerType;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.id.WaveletName;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Remote implementation. Receives submit and history requests from the local
 * wave server and sends them to a remote wave server Host, and also receives
 * update messages from a remote wave server Host and sends them to the local
 * wave server.
 */
public class XmppFederationRemote implements WaveletFederationProvider {
  private static final Logger LOG = Logger.getLogger(XmppFederationRemote.class.getCanonicalName());

  // Timeout for outstanding provider calls sent over XMPP.
  private static final int XMPP_PROVIDER_TIMEOUT = 30;

  private final WaveletFederationListener.Factory updatesListenerFactory;
  private final XmppDisco disco;
  private final String jid;

  private XmppManager manager = null;

  /**
   * Constructor. Note that {@link #setManager} must be called before this class
   * is ready to use.
   *
   * @param updatesListenerFactory used to communicate back to the local wave
   *        server when an update arrives.
   */
  @Inject
  public XmppFederationRemote(
      @FederationRemoteBridge WaveletFederationListener.Factory updatesListenerFactory,
      XmppDisco disco, Config config) {
    this.updatesListenerFactory = updatesListenerFactory;
    this.disco = disco;
    this.jid = config.getString("federation.xmpp_jid");
  }

  /**
   * Set the manager instance for this class. Must be invoked before any other
   * methods are used.
   */
  public void setManager(XmppManager manager) {
    this.manager = manager;
  }

  /**
   * Request submission of signed delta. This is part of the Federation Remote
   * interface - sends a submit request on behalf of the wave server. Part of
   * the WaveletFederationProvider interface.
   *
   * @param waveletName name of wavelet.
   * @param signedDelta delta signed by the submitting wave server.
   * @param listener callback for the result of the submit.
   */
  @Override
  public void submitRequest(final WaveletName waveletName,
                            final ProtocolSignedDelta signedDelta,
                            final SubmitResultListener listener) {

    final IQ submitIq = new IQ(IQ.Type.set);
    submitIq.setID(XmppUtil.generateUniqueId());

    LOG.info("Submitting delta to remote server, wavelet " + waveletName);
    submitIq.setFrom(jid);

    Element pubsub = submitIq.setChildElement("pubsub", XmppNamespace.NAMESPACE_PUBSUB);
    Element publish = pubsub.addElement("publish");
    publish.addAttribute("node", "wavelet");
    Element submitRequest = publish.addElement("item").addElement("submit-request",
        XmppNamespace.NAMESPACE_WAVE_SERVER);
    Element deltaElement = submitRequest.addElement("delta");

    deltaElement.addCDATA(Base64Util.encode(signedDelta.toByteArray()));
    try {
      deltaElement.addAttribute("wavelet-name",
          XmppUtil.waveletNameCodec.waveletNameToURI(waveletName));
    } catch (EncodingException e) {
      listener.onFailure(FederationErrors.badRequest(
          "Couldn't encode wavelet name " + waveletName));
      return;
    }

    final PacketCallback callback = new PacketCallback() {
      @Override
      public void error(FederationError error) {
        listener.onFailure(error);
      }

      @Override
      public void run(Packet packet) {
        processSubmitResponse(packet, listener);
      }
    };

    disco.discoverRemoteJid(waveletName.waveletId.getDomain(),
        new SuccessFailCallback<String, String>() {
          @Override
          public void onSuccess(String remoteJid) {
            Preconditions.checkNotNull(remoteJid);
            submitIq.setTo(remoteJid);
            manager.send(submitIq, callback, XMPP_PROVIDER_TIMEOUT);
          }

          @Override
          public void onFailure(String errorMessage) {
            // TODO(thorogood): Broken, Disco should return the error (and it
            // should be timeout/etc)
            listener.onFailure(FederationErrors.badRequest(
                "No such wave server " + waveletName.waveletId.getDomain() + ": " + errorMessage));
          }
        });
  }

  /**
   * Retrieve delta history for the given wavelet. <p/> Part of the
   * WaveletFederationProvider interface.
   *
   * @param waveletName  name of wavelet.
   * @param domain       the remote Federation Host
   * @param startVersion beginning of range (inclusive), minimum 0.
   * @param endVersion   end of range (exclusive).
   * @param lengthLimit  estimated size, in bytes, as an upper limit on the
   *                     amount of data returned.
   * @param listener     callback for the result.
   */
  public void requestHistory(final WaveletName waveletName,
                             final String domain,
                             ProtocolHashedVersion startVersion,
                             ProtocolHashedVersion endVersion,
                             long lengthLimit,
                             final WaveletFederationProvider.HistoryResponseListener listener) {
    final IQ submitIq = new IQ(IQ.Type.get);
    submitIq.setID(XmppUtil.generateUniqueId());

    LOG.info("Getting history from remote server, wavelet " + waveletName
                + " version " + startVersion + " (inc) through " + endVersion
                + " (ex)");
    submitIq.setFrom(jid);

    Element pubsub =
        submitIq.setChildElement("pubsub",
                                 XmppNamespace.NAMESPACE_PUBSUB);
    Element items = pubsub.addElement("items");
    items.addAttribute("node", "wavelet");
    Element historyDelta =
        items.addElement("delta-history",
                         XmppNamespace.NAMESPACE_WAVE_SERVER);

    historyDelta.addAttribute("start-version", Long.toString(startVersion
        .getVersion()));
    historyDelta.addAttribute("start-version-hash", Base64Util
        .encode(startVersion.getHistoryHash()));
    historyDelta.addAttribute("end-version", Long.toString(endVersion
        .getVersion()));
    historyDelta.addAttribute("end-version-hash", Base64Util.encode(endVersion
        .getHistoryHash()));
    if (lengthLimit > 0) {
      historyDelta.addAttribute("response-length-limit", Long
          .toString(lengthLimit));
    }
    try {
      historyDelta.addAttribute("wavelet-name",
          XmppUtil.waveletNameCodec.waveletNameToURI(waveletName));
    } catch (EncodingException e) {
      listener.onFailure(
          FederationErrors.badRequest("Couldn't encode wavelet name " + waveletName));
      return;
    }

    final PacketCallback callback = new PacketCallback() {
      public void error(FederationError error) {
        listener.onFailure(error);
      }

      @Override
      public void run(Packet packet) {
        processHistoryResponse(packet, listener);
      }
    };

    disco.discoverRemoteJid(domain, new SuccessFailCallback<String, String>() {
      @Override
      public void onSuccess(String remoteJid) {
        Preconditions.checkNotNull(remoteJid);
        submitIq.setTo(remoteJid);
        manager.send(submitIq, callback, XMPP_PROVIDER_TIMEOUT);
      }

      @Override
      public void onFailure(String errorMessage) {
        listener.onFailure(FederationErrors.badRequest(
            "No such wave server " + domain + ": " + errorMessage));
      }
    });
  }

  @Override
  public void getDeltaSignerInfo(ByteString signerId, WaveletName waveletName,
                                 ProtocolHashedVersion deltaEndVersion,
                                 final DeltaSignerInfoResponseListener listener) {
    final IQ getSignerIq = new IQ(IQ.Type.get);
    getSignerIq.setID(XmppUtil.generateUniqueId());

    getSignerIq.setFrom(jid);
    // Extract domain from waveletId
    final String remoteDomain = waveletName.waveletId.getDomain();
    Element pubsub =
        getSignerIq.setChildElement("pubsub",
                                    XmppNamespace.NAMESPACE_PUBSUB);
    Element items = pubsub.addElement("items");
    items.addAttribute("node", "signer");
    // TODO: should allow multiple requests in the same packet
    Element signerRequest =
        items.addElement("signer-request",
                         XmppNamespace.NAMESPACE_WAVE_SERVER);
    signerRequest.addAttribute("signer-id", Base64Util.encode(signerId));
    signerRequest.addAttribute("history-hash", Base64Util
        .encode(deltaEndVersion.getHistoryHash()));
    signerRequest.addAttribute("version", String.valueOf(deltaEndVersion
        .getVersion()));
    try {
      signerRequest.addAttribute("wavelet-name",
          XmppUtil.waveletNameCodec.waveletNameToURI(waveletName));
    } catch (EncodingException e) {
      listener.onFailure(FederationErrors.badRequest(
          "Couldn't encode wavelet name " + waveletName));
      return;
    }

    final PacketCallback callback = new PacketCallback() {
      @Override
      public void error(FederationError error) {
        listener.onFailure(error);
      }

      @Override
      public void run(Packet packet) {
        processGetSignerResponse(packet, listener);
      }
    };

    disco.discoverRemoteJid(
        remoteDomain, new SuccessFailCallback<String, String>() {
          @Override
          public void onSuccess(String remoteJid) {
            Preconditions.checkNotNull(remoteJid);
            getSignerIq.setTo(remoteJid);
            manager.send(getSignerIq, callback, XMPP_PROVIDER_TIMEOUT);
          }

          @Override
          public void onFailure(String errorMessage) {
            listener.onFailure(FederationErrors.badRequest(
                "No such wave server " + remoteDomain + ": " + errorMessage));
          }
        });
  }

  @Override
  public void postSignerInfo(
      final String remoteDomain,
      ProtocolSignerInfo signerInfo,
      final WaveletFederationProvider.PostSignerInfoResponseListener listener) {
    final IQ request = new IQ(IQ.Type.set);
    request.setID(XmppUtil.generateUniqueId());

    request.setFrom(jid);
    Element pubsub = request.setChildElement("pubsub", XmppNamespace.NAMESPACE_PUBSUB);
    Element publish = pubsub.addElement("publish");
    publish.addAttribute("node", "signer");
    XmppUtil.protocolSignerInfoToXml(signerInfo, publish.addElement("item"));

    final PacketCallback callback = new PacketCallback() {
      @Override
      public void error(FederationError error) {
        listener.onFailure(error);
      }

      @Override
      public void run(Packet packet) {
        processPostSignerResponse(packet, listener);
      }
    };

    disco.discoverRemoteJid(
        remoteDomain, new SuccessFailCallback<String, String>() {
          @Override
          public void onSuccess(String remoteJid) {
            Preconditions.checkNotNull(remoteJid);
            request.setTo(remoteJid);
            manager.send(request, callback, XMPP_PROVIDER_TIMEOUT);
          }

          @Override
          public void onFailure(String errorMessage) {
            listener.onFailure(FederationErrors.badRequest(
                "No such wave server " + remoteDomain + ": " + errorMessage));
          }
        });
  }

  /**
   * Handles a wavelet update message from a foreign Federation Host. Passes the
   * message to the local waveserver (synchronously) and replies.
   *
   * @param updateMessage the incoming XMPP message.
   * @param responseCallback response callback for acks and errors
   */
  public void update(final Message updateMessage, final PacketCallback responseCallback) {
    final Element receiptRequested =
        updateMessage.getChildElement("request", XmppNamespace.NAMESPACE_XMPP_RECEIPTS);

    // Check existence of <event>
    Element event = updateMessage.getChildElement("event", XmppNamespace.NAMESPACE_PUBSUB_EVENT);
    if (event == null) {
      responseCallback.error(FederationErrors.badRequest("Event element missing from message"));
      return;
    }

    // Check existence of <items> within <event>
    Element items = event.element("items");
    if (items == null) {
      responseCallback.error(FederationErrors.badRequest(
          "Items element missing from update message"));
      return;
    }

    // Complain if no items have been included.
    List<Element> elements = XmppUtil.toSafeElementList(items.elements("item"));
    if (elements.isEmpty()) {
      responseCallback.error(FederationErrors.badRequest("No items included"));
      return;
    }

    // Create a callback latch counter and corresponding countDown runnable.
    // When the latch reaches zero, send receipt (if it was requested).
    final AtomicInteger callbackCount = new AtomicInteger(1);
    final Runnable countDown = new Runnable() {
      @Override
      public void run() {
        if (callbackCount.decrementAndGet() == 0 && receiptRequested != null) {
          Message response = XmppUtil.createResponseMessage(updateMessage);
          response.addChildElement("received", XmppNamespace.NAMESPACE_XMPP_RECEIPTS);
          responseCallback.run(response);
        }
      }
    };

    WaveletFederationListener.WaveletUpdateCallback callback =
        new WaveletFederationListener.WaveletUpdateCallback() {
          @Override
          public void onSuccess() {
            countDown.run();
          }

          @Override
          public void onFailure(FederationError error) {
            // Note that we don't propogate the error, we just ack the stanza
            // and continue.
            // TODO(thorogood): We may want to rate-limit misbehaving servers
            // that are sending us invalid/malicious data.
            LOG.warning("Incoming XMPP waveletUpdate failure: " + error);
            countDown.run();
          }
        };

    // We must call callback once on every iteration to ensure that we send
    // response if receiptRequested != null.
    for (Element item : elements) {
      Element waveletUpdate = item.element("wavelet-update");

      if (waveletUpdate == null) {
        callback.onFailure(FederationErrors.badRequest(
            "wavelet-update element missing from message: " + updateMessage));
        continue;
      }

      final WaveletName waveletName;
      try {
        waveletName = XmppUtil.waveletNameCodec.uriToWaveletName(
            waveletUpdate.attributeValue("wavelet-name"));
      } catch (EncodingException e) {
        callback.onFailure(FederationErrors.badRequest(
            "Couldn't decode wavelet name: " + waveletUpdate.attributeValue("wavelet-name")));
        continue;
      }

      WaveletFederationListener listener =
          updatesListenerFactory.listenerForDomain(waveletName.waveletId.getDomain());

      // Submit all applied deltas to the domain-focused listener.
      ImmutableList.Builder<ByteString> builder = ImmutableList.builder();
      for (Element appliedDeltaElement :
          XmppUtil.toSafeElementList(waveletUpdate.elements("applied-delta"))) {
        builder.add(Base64Util.decode(appliedDeltaElement.getText()));
      }
      ImmutableList<ByteString> deltas = builder.build();
      if (!deltas.isEmpty()) {
        callbackCount.incrementAndGet(); // Increment required callbacks.
        listener.waveletDeltaUpdate(waveletName, deltas, callback);
      }

      // Optionally submit any received last committed notice.
      Element commitNoticeElement = waveletUpdate.element("commit-notice");
      if (commitNoticeElement != null) {
        ProtocolHashedVersion version = ProtocolHashedVersion.newBuilder()
            .setHistoryHash(Base64Util.decode(commitNoticeElement.attributeValue("history-hash")))
            .setVersion(Long.parseLong(commitNoticeElement.attributeValue("version"))).build();
        callbackCount.incrementAndGet(); // Increment required callbacks.
        listener.waveletCommitUpdate(waveletName, version, callback);
      }
    }

    // Release sentinel so that 'expected' callbacks from the WS don't invoke
    // sending a receipt.
    countDown.run();
  }

  /**
   * Parses the response to a submitRequest and passes the result to the correct
   * wave server.
   *
   * @param result   the XMPP Packet
   * @param listener the listener to invoke with the response.
   */
  private void processSubmitResponse(Packet result, SubmitResultListener listener) {
    Element publish = null;
    Element item = null;
    Element submitResponse = null;
    Element hashedVersionElement = null;
    Element pubsub = ((IQ) result).getChildElement();
    if (pubsub != null) {
      publish = pubsub.element("publish");
      if (publish != null) {
        item = publish.element("item");
        if (item != null) {
          submitResponse = item.element("submit-response");
          if (submitResponse != null) {
            hashedVersionElement = submitResponse.element("hashed-version");
          }
        }
      }
    }

    if (pubsub == null || publish == null || item == null
        || submitResponse == null || hashedVersionElement == null
        || hashedVersionElement.attribute("history-hash") == null
        || hashedVersionElement.attribute("version") == null
        || submitResponse.attribute("application-timestamp") == null
        || submitResponse.attribute("operations-applied") == null) {
      LOG.severe("Unexpected submitResponse to submit request: " + result);
      listener.onFailure(FederationErrors.badRequest("Invalid submitResponse: " + result));
      return;
    }

    ProtocolHashedVersion.Builder hashedVersion = ProtocolHashedVersion.newBuilder();
    hashedVersion.setHistoryHash(
        Base64Util.decode(hashedVersionElement.attributeValue("history-hash")));
    hashedVersion.setVersion(Long.parseLong(hashedVersionElement.attributeValue("version")));
    long applicationTimestamp =
        Long.parseLong(submitResponse.attributeValue("application-timestamp"));
    int operationsApplied = Integer.parseInt(submitResponse.attributeValue("operations-applied"));
    listener.onSuccess(operationsApplied, hashedVersion.build(), applicationTimestamp);
  }

  /**
   * Parses a response to a history request and passes the result to the wave
   * server.
   *
   * @param historyResponse the XMPP packet
   * @param listener        interface to the wave server
   */
  @SuppressWarnings("unchecked")
  private void processHistoryResponse(Packet historyResponse,
      WaveletFederationProvider.HistoryResponseListener listener) {
    Element pubsubResponse = historyResponse.getElement().element("pubsub");
    Element items = pubsubResponse.element("items");
    long versionTruncatedAt = -1;
    long lastCommittedVersion = -1;
    List<ByteString> deltaList = Lists.newArrayList();

    if (items != null) {
      for (Element itemElement : (List<Element>) items.elements()) {
        for (Element element : (List<Element>) itemElement.elements()) {
          String elementName = element.getQName().getName();
          switch (elementName) {
            case "applied-delta":
              String deltaBody = element.getText();
              deltaList.add(ByteString.copyFrom(Base64.decodeBase64(deltaBody.getBytes())));
              break;
            case "commit-notice":
              Attribute commitVersion = element.attribute("version");
              if (commitVersion != null) {
                try {
                  lastCommittedVersion = Long.parseLong(commitVersion.getValue());
                } catch (NumberFormatException e) {
                  lastCommittedVersion = -1;
                }
              }
              break;
            case "history-truncated":
              Attribute truncVersion = element.attribute("version");
              if (truncVersion != null) {
                try {
                  versionTruncatedAt = Long.parseLong(truncVersion.getValue());
                } catch (NumberFormatException e) {
                  versionTruncatedAt = -1;
                }
              }
              break;
            default:
              listener.onFailure(FederationErrors.badRequest(
                      "Bad response packet: " + historyResponse));
              break;
          }
        }
      }
    } else {
      listener.onFailure(FederationErrors.badRequest("Bad response packet: " + historyResponse));
    }

    final ProtocolHashedVersion lastCommitted;
    if (lastCommittedVersion > -1) {
      // TODO(thorogood): fedone doesn't send a history hash, and it's arguable
      // that it's even sane to include it.
      // Can't set it to null - NPE
      lastCommitted =
          ProtocolHashedVersion.newBuilder()
              .setVersion(lastCommittedVersion).setHistoryHash(ByteString.EMPTY)
              .build();
    } else {
      lastCommitted = null;
    }
    listener.onSuccess(deltaList, lastCommitted, versionTruncatedAt);
  }

  /**
   * Parses a GetSigner response, passes result to the waveserver.
   *
   * @param packet   the response packet
   * @param listener the interface to the wave server
   */
  private void processGetSignerResponse(Packet packet, DeltaSignerInfoResponseListener listener) {
    IQ response = (IQ) packet;
    Element items = response.getChildElement().element("items");
    Element signature = items.element("signature");
    if (signature == null) {
      LOG.severe("Empty getDeltaSignerRequest response: " + response);
      listener.onFailure(FederationErrors.badRequest("Bad getDeltaSignatureRequest response"));
      return;
    }
    String domain = signature.attributeValue("domain");
    String hashName = signature.attributeValue("algorithm");
    if (domain == null || hashName == null || signature.element("certificate") == null) {
      LOG.severe("Bad getDeltaSignerRequest response: " + response);
      listener.onFailure(FederationErrors.badRequest("Bad getDeltaSignatureRequest response"));
      return;
    }
    ProtocolSignerInfo signer;
    try {
      signer = XmppUtil.xmlToProtocolSignerInfo(signature);
    } catch (UnknownSignerType e) {
      listener.onFailure(FederationErrors.badRequest(e.toString()));
      return;
    }
    listener.onSuccess(signer);
  }

  /**
   * Parses a response to a PostSigner request, passes result to wave server.
   *
   * @param packet   the response XMPP packet
   * @param listener the listener to invoke
   */
  private void processPostSignerResponse(
      Packet packet,
      WaveletFederationProvider.PostSignerInfoResponseListener listener) {
    IQ response = (IQ) packet;
    Element pubsub = response.getChildElement();
    Element item = pubsub.element("publish").element("item");
    if (item.element("signature-response") != null) {
      listener.onSuccess();
    } else {
      listener.onFailure(FederationErrors.badRequest("No valid response"));
    }
  }
}
