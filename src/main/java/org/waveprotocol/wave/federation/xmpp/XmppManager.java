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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.dom4j.Element;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides abstraction between Federation-specific code and the backing XMPP
 * transport, including support for reliable outgoing calls (i.e. calls that are
 * guaranteed to time out) and sending error responses.
 *
 * TODO(thorogood): Find a better name for this class. Suggestions include
 * PacketHandler, Switchbox, TransportConnector, ReliableRouter, ...
 *
 * @author thorogood@google.com (Sam Thorogood)
 */
public class XmppManager implements IncomingPacketHandler {
  private static final Logger LOG = Logger.getLogger(XmppManager.class.getCanonicalName());

  /**
   * Inner static class representing a single outgoing call.
   */
  private static class OutgoingCall {
    final Class<? extends Packet> responseType;
    PacketCallback callback;
    ScheduledFuture<?> timeout;

    OutgoingCall(Class<? extends Packet> responseType, PacketCallback callback) {
      this.responseType = responseType;
      this.callback = callback;
    }

    void start(ScheduledFuture<?> timeout) {
      Preconditions.checkState(this.timeout == null);
      this.timeout = timeout;
    }
  }

  /**
   * Inner non-static class representing a single incoming call. These are not
   * cancellable and do not time out; this is just a helper class so success and
   * failure responses may be more cleanly invoked.
   */
  private class IncomingCallback implements PacketCallback {
    private final Packet request;
    private boolean complete = false;

    IncomingCallback(Packet request) {
      this.request = request;
    }

    @Override
    public void error(FederationError error) {
      Preconditions.checkState(!complete,
          "Must not callback multiple times for incoming packet: %s", request);
      complete = true;
      sendErrorResponse(request, error);
    }

    @Override
    public void run(Packet response) {
      Preconditions.checkState(!complete,
          "Must not callback multiple times for incoming packet: %s", request);
      // TODO(thorogood): Check outgoing response versus stored incoming request
      // to ensure that to/from are paired correctly?
      complete = true;
      transport.sendPacket(response);
    }
  }

  // Injected types that handle incoming XMPP packet types.
  private final XmppFederationHost host;
  private final XmppFederationRemote remote;
  private final XmppDisco disco;
  private final OutgoingPacketTransport transport;
  private final String jid;

  // Pending callbacks to outgoing requests.
  private final ConcurrentMap<String, OutgoingCall> callbacks = new MapMaker().makeMap();
  private final ScheduledExecutorService timeoutExecutor =
      Executors.newSingleThreadScheduledExecutor();

  @Inject
  public XmppManager(XmppFederationHost host, XmppFederationRemote remote, XmppDisco disco,
      OutgoingPacketTransport transport, Config config) {
    this.host = host;
    this.remote = remote;
    this.disco = disco;
    this.transport = transport;
    this.jid = config.getString("federation.xmpp_jid");

    // Configure all related objects with this manager. Eventually, this should
    // be replaced by better Guice interface bindings.
    host.setManager(this);
    remote.setManager(this);
    disco.setManager(this);
  }

  @Override
  public void receivePacket(final Packet packet) {
    if (LOG.isLoggable(Level.FINE)) {
      LOG.fine("Received incoming XMPP packet:\n" + packet);
    }

    if (packet instanceof IQ) {
      IQ iq = (IQ) packet;
      if (iq.getType().equals(IQ.Type.result) || iq.getType().equals(IQ.Type.error)) {
        // Result type, hand off to callback handler.
        response(packet);
      } else {
        processIqGetSet(iq);
      }
    } else if (packet instanceof Message) {
      Message message = (Message) packet;
      if (message.getType().equals(Message.Type.error)
          || message.getChildElement("received", XmppNamespace.NAMESPACE_XMPP_RECEIPTS) != null) {
        // Response type, hand off to callback handler.
        response(packet);
      } else {
        processMessage(message);
      }
    } else {
      sendErrorResponse(packet, FederationError.Code.BAD_REQUEST, "Unhandled packet type: "
          + packet.getElement().getQName().getName());
    }
  }

  /**
   * Populate the given request subclass of Packet and return it.
   */
  private <V extends Packet> V createRequest(V packet, String toJid) {
    packet.setTo(toJid);
    packet.setID(XmppUtil.generateUniqueId());
    packet.setFrom(jid);
    return packet;
  }

  /**
   * Create a request IQ stanza with the given toJid.
   *
   * @param toJid target JID
   * @return new IQ stanza
   */
  public IQ createRequestIQ(String toJid) {
    return createRequest(new IQ(), toJid);
  }

  /**
   * Create a request Message stanza with the given toJid.
   *
   * @param toJid target JID
   * @return new Message stanza
   */
  public Message createRequestMessage(String toJid) {
    return createRequest(new Message(), toJid);
  }

  /**
   * Sends the given XMPP packet over the backing transport. This accepts a
   * callback which is guaranteed to be invoked at a later point, either through
   * a normal response, error response, or timeout.
   *
   * @param packet packet to be sent
   * @param callback callback to be invoked on response or timeout
   * @param timeout timeout, in seconds, for this callback
   */
  public void send(Packet packet, final PacketCallback callback, int timeout) {
    final String key = packet.getID() + "#" + packet.getTo() + "#" + packet.getFrom();

    final OutgoingCall call = new OutgoingCall(packet.getClass(), callback);
    if (callbacks.putIfAbsent(key, call) == null) {
      // Timeout runnable to be invoked on packet expiry.
      Runnable timeoutTask = new Runnable() {
        @Override
        public void run() {
          if (callbacks.remove(key, call)) {
            callback.error(
                FederationErrors.newFederationError(FederationError.Code.REMOTE_SERVER_TIMEOUT));
          } else {
            // Likely race condition where success has actually occurred. Ignore.
          }
        }
      };
      call.start(timeoutExecutor.schedule(timeoutTask, timeout, TimeUnit.SECONDS));
      transport.sendPacket(packet);
    } else {
      String msg = "Could not send packet, ID already in-flight: " + key;
      LOG.warning(msg);

      // Invoke the callback with an internal error.
      callback.error(
          FederationErrors.newFederationError(FederationError.Code.UNDEFINED_CONDITION, msg));
    }
  }

  /**
   * Cause an immediate timeout for the given packet, which is presumed to have
   * already been sent via {@link #send}.
   */
  @VisibleForTesting
  void causeImmediateTimeout(Packet packet) {
    String key = packet.getID() + "#" + packet.getTo() + "#" + packet.getFrom();
    OutgoingCall call = callbacks.remove(key);
    if (call != null) {
      call.callback.error(FederationErrors.newFederationError(
          FederationError.Code.REMOTE_SERVER_TIMEOUT, "Forced immediate timeout"));
    }
  }

  /**
   * Invoke the callback for a packet already identified as a response. This may
   * either invoke the error or normal callback as necessary.
   */
  private void response(Packet packet) {
    String key = packet.getID() + "#" + packet.getFrom() + "#" + packet.getTo();
    OutgoingCall call = callbacks.remove(key);

    if (call == null) {
      LOG.warning("Received response packet without paired request: " + packet.getID());
    } else {
      // Cancel the outstanding timeout.
      call.timeout.cancel(false);

      // Look for error condition and invoke the relevant callback.
      Element element = packet.getElement().element("error");
      if (element != null) {
        LOG.fine("Invoking error callback for: " + packet.getID());
        call.callback.error(toFederationError(new PacketError(element)));
      } else {
        if (call.responseType.equals(packet.getClass())) {
          LOG.fine("Invoking normal callback for: " + packet.getID());
          call.callback.run(packet);
        } else {
          String msg =
              "Received mismatched response packet type: expected " + call.responseType
                  + ", given " + packet.getClass();
          LOG.warning(msg);
          call.callback.error(FederationErrors.newFederationError(
              FederationError.Code.UNDEFINED_CONDITION, msg));
        }
      }

      // Clear call's reference to callback, otherwise callback only
      // becomes eligible for GC once the timeout expires, because
      // timeoutExecutor holds on to the call object till then, even
      // though we cancelled the timeout.
      call.callback = null;
    }
  }

  /**
   * Process IQ request stanzas. This encompasses XMPP disco, submit and history
   * requests/responses, and get/post signer info requests/responses.
   */
  private void processIqGetSet(IQ iq) {
    Element body = iq.getChildElement();
    if (body == null) {
      sendErrorResponse(iq, FederationErrors.badRequest("Malformed request, no IQ child"));
      return;
    }

    final String namespace = body.getQName().getNamespace().getURI();
    final boolean isIQSet;
    if (iq.getType().equals(IQ.Type.get)) {
      isIQSet = false;
    } else if (iq.getType().equals(IQ.Type.set)) {
      isIQSet = true;
    } else {
      throw new IllegalArgumentException("Can only process an IQ get/set.");
    }
    PacketCallback responseCallback = new IncomingCallback(iq);

    if (namespace.equals(XmppNamespace.NAMESPACE_PUBSUB)) {
      final Element pubsub = iq.getChildElement();
      final Element element = pubsub.element(isIQSet ? "publish" : "items");

      if (element.attributeValue("node").equals("wavelet")) {
        if (isIQSet) {
          host.processSubmitRequest(iq, responseCallback);
        } else {
          host.processHistoryRequest(iq, responseCallback);
        }
      } else if (element.attributeValue("node").equals("signer")) {
        if (isIQSet) {
          host.processPostSignerRequest(iq, responseCallback);
        } else {
          host.processGetSignerRequest(iq, responseCallback);
        }
      } else {
        sendErrorResponse(iq, FederationError.Code.BAD_REQUEST, "Unhandled pubsub request");
      }
    } else if (!isIQSet) {
      switch (namespace) {
        case XmppNamespace.NAMESPACE_DISCO_INFO:
          disco.processDiscoInfoGet(iq, responseCallback);
          break;
        case XmppNamespace.NAMESPACE_DISCO_ITEMS:
          disco.processDiscoItemsGet(iq, responseCallback);
          break;
        default:
          sendErrorResponse(iq, FederationError.Code.BAD_REQUEST, "Unhandled IQ get");
          break;
      }
    } else {
      sendErrorResponse(iq, FederationError.Code.BAD_REQUEST, "Unhandled IQ set");
    }
  }

  /**
   * Processes Message stanzas. This encompasses wavelet updates, update acks,
   * and ping messages.
   */
  private void processMessage(Message message) {
    if (message.getChildElement("event", XmppNamespace.NAMESPACE_PUBSUB_EVENT) != null) {
      remote.update(message, new IncomingCallback(message));
    } else if (message.getChildElement("ping", XmppNamespace.NAMESPACE_WAVE_SERVER) != null) {
      // Respond inline to the ping.
      LOG.info("Responding to ping from: " + message.getFrom());
      Message response = XmppUtil.createResponseMessage(message);
      response.addChildElement("received", XmppNamespace.NAMESPACE_XMPP_RECEIPTS);
      transport.sendPacket(response);
    } else {
      sendErrorResponse(message, FederationError.Code.BAD_REQUEST, "Unhandled message type");
    }
  }

  /**
   * Helper method to send generic error responses, backed onto
   * {@link #sendErrorResponse(Packet, FederationError)}.
   */
  void sendErrorResponse(Packet request, FederationError.Code code) {
    sendErrorResponse(request, FederationErrors.newFederationError(code));
  }

  /**
   * Helper method to send error responses, backed onto
   * {@link #sendErrorResponse(Packet, FederationError)}.
   */
  void sendErrorResponse(Packet request, FederationError.Code code, String text) {
    sendErrorResponse(request, FederationErrors.newFederationError(code, text));
  }

  /**
   * Send an error request to the passed incoming request.
   *
   * @param request packet request, target is derived from its to/from
   * @param error error to be contained in response
   */
  void sendErrorResponse(Packet request, FederationError error) {
    if (error.getErrorCode() == FederationError.Code.OK) {
      throw new IllegalArgumentException("Can't send an error of OK!");
    }
    sendErrorResponse(request, toPacketError(error));
  }

  /**
   * Send an error response to the passed incoming request. Throws
   * IllegalArgumentException if the original packet is also an error, or is of
   * the IQ result type.
   *
   * According to RFC 3920 (9.3.1), the error packet may contain the original
   * packet. However, this implementation does not include it.
   *
   * @param request packet request, to/from is inverted for response
   * @param error packet error describing error condition
   */
  void sendErrorResponse(Packet request, PacketError error) {
    if (request instanceof IQ) {
      IQ.Type type = ((IQ) request).getType();
      if (!(type.equals(IQ.Type.get) ||  type.equals(IQ.Type.set))) {
        throw new IllegalArgumentException("May only return an error to IQ get/set, not: " + type);
      }
    } else if (request instanceof Message) {
      Message message = (Message) request;
      if (message.getType().equals(Message.Type.error)) {
        throw new IllegalArgumentException("Can't return an error to another message error");
      }
    } else {
      throw new IllegalArgumentException("Unexpected Packet subclass, expected Message/IQ: "
          + request.getClass());
    }

    LOG.fine("Sending error condition in response to " + request.getID() + ": "
        + error.getCondition().name());

    // Note that this does not include the original packet; just the ID.
    final Packet response = XmppUtil.createResponsePacket(request);
    response.setError(error);

    transport.sendPacket(response);
  }

  /**
   * Convert a FederationError instance to a PacketError. This may return
   * <undefined-condition> if the incoming error can't be understood.
   *
   * @param error the incoming error
   * @return a generated PacketError instance
   * @throws IllegalArgumentException if the OK error code is given
   */
  private static PacketError toPacketError(FederationError error) {
    Preconditions.checkArgument(error.getErrorCode() != FederationError.Code.OK);

    String tag = error.getErrorCode().name().toLowerCase().replace('_', '-');
    PacketError.Condition condition;
    try {
      condition = PacketError.Condition.fromXMPP(tag);
    } catch (IllegalArgumentException e) {
      condition = PacketError.Condition.undefined_condition;
      LOG.warning("Did not understand error condition, defaulting to: " + condition.name());
    }
    PacketError result = new PacketError(condition);
    if (error.hasErrorMessage()) {
      // TODO(thorogood): Hide this behind a flag so we don't always broadcast error cases.
      result.setText(error.getErrorMessage(), "en");
    }
    return result;
  }

  /**
   * Convert a PacketError instance to an internal FederationError. This may
   * return an error code of UNDEFINED_CONDITION if the incoming error can't be
   * understood.
   *
   * @param error the incoming PacketError
   * @return the generated FederationError instance
   */
  private static FederationError toFederationError(PacketError error) {
    String tag = error.getCondition().name().toUpperCase().replace('-', '_');
    FederationError.Code code;
    try {
      code = FederationError.Code.valueOf(tag);
    } catch (IllegalArgumentException e) {
      code = FederationError.Code.UNDEFINED_CONDITION;
    }
    FederationError.Builder builder = FederationError.newBuilder().setErrorCode(code);
    if (error.getText() != null) {
      builder.setErrorMessage(error.getText());
    }
    return builder.build();
  }
}
