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

package org.waveprotocol.box.server.rpc;

import org.waveprotocol.wave.util.logging.Log;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;


import java.io.IOException;

/**
 * A channel implementation for websocket.
 *
 * @author akaplanov@gmai.com (A. Kaplanov)
 */
@WebSocket
public class WebSocketChannelImpl extends WebSocketChannel {
  private static final Log LOG = Log.get(WebSocketChannelImpl.class);

  private Session session;

  public WebSocketChannelImpl(ProtoCallback callback) {
    super(callback);
  }

  @OnWebSocketConnect
  public void onOpen(Session session) {
    synchronized (this) {
      this.session = session;
    }
  }

  @OnWebSocketMessage
  public void onMessage(String data) {
    handleMessageString(data);
  }

  @OnWebSocketClose
  public void onClose(int closeCode, String closeReason) {
    LOG.fine("websocket disconnected (" + closeCode + " - " + closeReason + "): " + this);
    synchronized (this) {
      session = null;
    }
  }

  @Override
  protected void sendMessageString(String data) throws IOException {
    Session s;
    synchronized (this) {
      s = session;
    }
    if (s == null) {
      LOG.warning("Websocket is not connected");
    } else {
      s.getRemote().sendStringByFuture(data);
    }
  }
}
