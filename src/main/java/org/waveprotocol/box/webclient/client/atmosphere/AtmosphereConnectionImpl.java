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

package org.waveprotocol.box.webclient.client.atmosphere;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.ScriptInjector;

/**
 * The wrapper implementation of the atmosphere javascript client.
 *
 * https://github.com/Atmosphere/atmosphere/wiki/jQuery.atmosphere.js-atmosphere
 * .js-API
 *
 * More info about transports
 * https://github.com/Atmosphere/atmosphere/wiki/Supported
 * -WebServers-and-Browsers
 *
 * It tries to use Server-Sent Events first and fallback transport to
 * long-polling. We ignore Websockets by now because they are the default
 * transport on WiAB and atmosphere is the fallback.
 *
 * http://stackoverflow.com/questions/9397528/server-sent-events-vs-polling
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class AtmosphereConnectionImpl implements AtmosphereConnection {


      private static final class AtmosphereSocket extends JavaScriptObject {
        public static native AtmosphereSocket create(AtmosphereConnectionImpl impl, String urlBase) /*-{

      var client = $wnd.atmosphere;

                var atsocket = {
                    request: null,
                    socket: null };

                var connectionUrl = window.location.protocol + "//" +  $wnd.__websocket_address + "/";

                connectionUrl += 'atmosphere';

                //console.log("Connection URL is "+urlBase);
                atsocket.request = new client.AtmosphereRequest();
                atsocket.request.url = connectionUrl;
                atsocket.request.contenType = 'text/plain;charset=UTF-8';
                atsocket.request.transport = 'sse';
                atsocket.request.fallbackTransport = 'long-polling';

                atsocket.request.onOpen = $entry(function() {
                    impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onConnect()();
                });

                atsocket.request.onMessage =  $entry(function(response) {

                  var r = response.responseBody;

                  if (r.indexOf('|') == 0) {

                      while (r.indexOf('|') == 0 && r.length > 1) {

                        r = r.substring(1);
                        var marker = r.indexOf('}|');
                        impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onMessage(Ljava/lang/String;)(r.substring(0, marker+1));
                        r = r.substring(marker+1);

                     }

                  }
                  else {

                    impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onMessage(Ljava/lang/String;)(r);

                  }

                });

                atsocket.request.onClose = $entry(function(response) {

                   client.util.info("Connection closed");

                   impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onDisconnect(Ljava/lang/String;)(response);
                });



                atsocket.request.onTransportFailure = function(errorMsg, request) {

                      client.util.info(errorMsg);

                  };


                atsocket.request.onReconnect = function(request, response) {

                      client.util.info("Reconnected to the server");

                  };

                  atsocket.request.onError = function(response) {

                      client.util.info("Unexpected Error");

                  };


                return atsocket;


        }-*/;

        protected AtmosphereSocket() {
    }


    public native void close() /*-{
			this.socket.unsubscribe();
    }-*/;

    public native AtmosphereSocket connect() /*-{
			this.socket = $wnd.atmosphere.subscribe(this.request);

    }-*/;

    public native void send(String data) /*-{
			this.socket.push(data);
    }-*/;
    }


    private final AtmosphereConnectionListener listener;
    private String urlBase;
    private AtmosphereConnectionState state;
    private AtmosphereSocket socket = null;

    public AtmosphereConnectionImpl(AtmosphereConnectionListener listener,
               String urlBase) {
        this.listener = listener;
        this.urlBase = urlBase;

    }


    @Override
    public void connect() {
        if (socket == null) {

                ScriptInjector.fromUrl("/atmosphere/atmosphere.js").setCallback(
                        new Callback<Void, Exception>() {
                                public void onFailure(Exception reason) {
                                        throw new IllegalStateException("atmosphere.js load failed!");
                                }
                                public void onSuccess(Void result) {

                                        socket = AtmosphereSocket.create(AtmosphereConnectionImpl.this, urlBase);
                                        socket.connect();
                                }
                        }).setWindow(ScriptInjector.TOP_WINDOW).inject();
        } else {


          if (AtmosphereConnectionState.CLOSED.equals(this.state))
            socket.connect();

        }
    }

    @Override
    public void close() {
        if (!AtmosphereConnectionState.CLOSED.equals(this.state))
                socket.close();

    }


    @Override
    public void sendMessage(String message) {
    this.state = AtmosphereConnectionState.MESSAGE_PUBLISHED;
        socket.send(message);
    }



    @SuppressWarnings("unused")
    private void onConnect() {
    this.state = AtmosphereConnectionState.OPENED;
        listener.onConnect();
    }

  /**
   * This method is called when an Atmosphere onClose event happens:
   *
   * when an error occurs. when the server or a proxy closes the connection.
   * when an expected exception occurs. when the specified transport is not
   * supported or fail to connect.
   *
   * @param response
   *
   */
    @SuppressWarnings("unused")
    private void onDisconnect(String response) {
      this.state = AtmosphereConnectionState.CLOSED;
      listener.onDisconnect();
    }

    @SuppressWarnings("unused")
    private void onMessage(String message) {
    this.state = AtmosphereConnectionState.MESSAGE_RECEIVED;
      listener.onMessage(message);
    }


}
