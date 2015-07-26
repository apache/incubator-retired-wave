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

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.typesafe.config.Config;
import org.dom4j.Element;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.id.WaveletName;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An instance of this class is created on demand for outgoing
 * messages to another wave Federation Remote. The wave server asks
 * the XmppFederationHost to create these.
 */
class XmppFederationHostForDomain implements WaveletFederationListener {

  private static final Logger LOG =
    Logger.getLogger(XmppFederationHostForDomain.class.getCanonicalName());

  // Timeout for outstanding listener updates sent over XMPP.
  private static final int XMPP_LISTENER_TIMEOUT = 30;

  private final String remoteDomain;
  private final XmppManager manager;
  private final String jid;
  private final XmppDisco disco;

  @Inject
  public XmppFederationHostForDomain(final String domain, XmppManager manager,
      XmppDisco disco, Config config) {
    this.remoteDomain = domain;
    this.manager = manager;
    this.jid = config.getString("federation.xmpp_jid");
    this.disco = disco;
  }

  @Override
  public void waveletCommitUpdate(WaveletName waveletName, ProtocolHashedVersion committedVersion,
      WaveletUpdateCallback callback) {
    waveletUpdate(waveletName, null, committedVersion, callback);
  }

  @Override
  public void waveletDeltaUpdate(WaveletName waveletName, List<ByteString> appliedDeltas,
      WaveletUpdateCallback callback) {
    waveletUpdate(waveletName, appliedDeltas, null, callback);
  }

  /**
   * Sends a wavelet update message on behalf of the wave server. This
   * method just triggers a disco lookup (which may be cached) and
   * sets up a callback to call the real method that does the work.
   * This method may contain applied deltas, a commit notice, or both.
   *
   * @param waveletName the wavelet name
   * @param deltaList the deltas to include in the message, or null
   * @param committedVersion last committed version to include, or null
   * @param callback callback to invoke on delivery success/failure
   */
  public void waveletUpdate(final WaveletName waveletName, final List<ByteString> deltaList,
      final ProtocolHashedVersion committedVersion, final WaveletUpdateCallback callback) {
    if ((deltaList == null || deltaList.isEmpty()) && committedVersion == null) {
      throw new IllegalArgumentException("Must send at least one delta, or a last committed " +
          "version notice, for the target wavelet: " + waveletName);
    }

    disco.discoverRemoteJid(remoteDomain, new SuccessFailCallback<String, String>() {
      @Override
      public void onSuccess(String remoteJid) {
        internalWaveletUpdate(waveletName, deltaList, committedVersion, callback, remoteJid);
      }

      @Override
      public void onFailure(String errorMessage) {
        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("Disco failed for remote domain " + remoteDomain + ", update not sent");
        }
        callback.onFailure(FederationErrors.newFederationError(
            FederationError.Code.RESOURCE_CONSTRAINT, errorMessage));
      }
    });
  }

  /**
   * Sends a wavelet update message on behalf of the wave server once disco is
   * complete. This method may contain applied deltas, a commit notice, or both.
   *
   * @param waveletName      the wavelet name
   * @param deltaList        the deltas to include in the message, or null
   * @param committedVersion last committed version to include, or null
   * @param callback         callback to invoke on delivery success/failure
   * @param remoteJid        the remote JID to send the update to
   */
  private void internalWaveletUpdate(final WaveletName waveletName,
      final List<ByteString> deltaList, final ProtocolHashedVersion committedVersion,
      final WaveletUpdateCallback callback, String remoteJid) {
    Message message = new Message();
    message.setType(Message.Type.normal);
    message.setFrom(jid);
    message.setTo(remoteJid);
    message.setID(XmppUtil.generateUniqueId());
    message.addChildElement("request", XmppNamespace.NAMESPACE_XMPP_RECEIPTS);

    final String encodedWaveletName;
    try {
      encodedWaveletName = XmppUtil.waveletNameCodec.waveletNameToURI(waveletName);
    } catch (EncodingException e) {
      callback.onFailure(FederationErrors.badRequest("Bad wavelet name " + waveletName));
      return;
    }

    Element itemElement = message.addChildElement("event", XmppNamespace.NAMESPACE_PUBSUB_EVENT)
        .addElement("items").addElement("item");
    if (deltaList != null) {
      for (ByteString delta : deltaList) {
        Element waveletUpdate =
            itemElement.addElement("wavelet-update", XmppNamespace.NAMESPACE_WAVE_SERVER)
                .addAttribute("wavelet-name", encodedWaveletName);
        waveletUpdate.addElement("applied-delta").addCDATA(Base64Util.encode(delta.toByteArray()));
      }
    }
    if (committedVersion != null) {
      Element waveletUpdate =
          itemElement.addElement("wavelet-update", XmppNamespace.NAMESPACE_WAVE_SERVER)
              .addAttribute("wavelet-name", encodedWaveletName);
      waveletUpdate.addElement("commit-notice").addAttribute("version",
          Long.toString(committedVersion.getVersion())).addAttribute("history-hash",
          Base64Util.encode(committedVersion.getHistoryHash()));
    }

    // Send the generated message through to the foreign XMPP server.
    manager.send(message, new PacketCallback() {
      @Override
      public void error(FederationError error) {
        callback.onFailure(error);
      }

      @Override
      public void run(Packet packet) {
        callback.onSuccess();
      }
    }, XMPP_LISTENER_TIMEOUT);
  }
}
