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
package com.google.gwt.websockets.client;

import com.google.gwt.core.client.JavaScriptObject;

public class WebSocket {
  private final static class WebSocketImpl extends JavaScriptObject {
    public static native WebSocketImpl create(WebSocket client, String server)
    /*-{
    var ws = new WebSocket(server);
    ws.onopen = $entry(function() {
      client.@com.google.gwt.websockets.client.WebSocket::onOpen()();
    });
    ws.onmessage = $entry(function(response) {
      client.@com.google.gwt.websockets.client.WebSocket::onMessage(Ljava/lang/String;)(response.data);
    });
    ws.onclose = $entry(function() {
      client.@com.google.gwt.websockets.client.WebSocket::onClose()();
    });
    return ws;
    }-*/;

    public static native boolean isSupported() /*-{return !!window.WebSocket;}-*/;

    protected WebSocketImpl() {
    }

    public native void close() /*-{this.close();}-*/;

    public native void send(String data) /*-{this.send(data);}-*/;
  }

  private final WebSocketCallback callback;
  private WebSocketImpl webSocket;

  public WebSocket(WebSocketCallback callback) {
    this.callback = callback;
  }

  public void close() {
    if (webSocket == null) {
      throw new IllegalStateException("Not connected");
    }
    webSocket.close();
    webSocket = null;
  }

  public void connect(String server) {
    if (!WebSocketImpl.isSupported()) {
      throw new RuntimeException("No WebSocket support");
    }
    webSocket = WebSocketImpl.create(this, server);
  }

  public void send(String data) {
    if (webSocket == null) {
      throw new IllegalStateException("Not connected");
    }
    webSocket.send(data);
  }

  @SuppressWarnings("unused")
  private void onClose() {
    callback.onDisconnect();
  }

  @SuppressWarnings("unused")
  private void onMessage(String message) {
    callback.onMessage(message);
  }

  @SuppressWarnings("unused")
  private void onOpen() {
    callback.onConnect();
  }
}
