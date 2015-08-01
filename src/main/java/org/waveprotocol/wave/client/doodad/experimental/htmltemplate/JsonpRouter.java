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

package org.waveprotocol.wave.client.doodad.experimental.htmltemplate;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ScriptElement;

/**
 * Performs requests via JSONP, and works with services that disallow '.' in
 * the JSONP callback name (GWT's JsonpRequestBuilder requires that the
 * callback name allow '.' characters).
 *
 * Each request is handled by a unique window function.
 *
 * @param <R> response type
 */
final class JsonpRouter<R extends JavaScriptObject> {

  private final static String JSONP_HANDLER_PREFIX = "__jsonp";
  private final static String JSONP_CALLBACK_PARAM = "callback";

  private static int jsonpCounter;

  static <R extends JavaScriptObject> JsonpRouter<R> create() {
    return new JsonpRouter<R>();
  }

  /**
   * Registers a function on the root window object.
   *
   * @param id  function name
   * @param callback  1-arg function
   */
  private static native void registerCallback(String id, Callback<JavaScriptObject> callback) /*-{
    $wnd[id] = $entry(function(obj) {
      callback.@org.waveprotocol.wave.client.doodad.experimental.htmltemplate.Callback::onSuccess(Ljava/lang/Object;)
            (obj);
    });
  }-*/;

  /**
   * Deletes a function previously registered with
   * {@link #registerCallback(String, Callback)}.
   *
   * @param id function name
   */
  private static native void deregisterCallback(String id) /*-{
    delete $wnd["id"];
  }-*/;

  /**
   * Fires a request to a JSONP-supporting URL.
   */
  public void request(String url, final Callback<? super R> request) {
    final String id = JSONP_HANDLER_PREFIX + jsonpCounter++;
    final ScriptElement requestMaker = Document.get().createScriptElement();
    registerCallback(id, new Callback<JavaScriptObject>() {
      @Override
      public void onSuccess(JavaScriptObject cajoled) {
        requestMaker.removeFromParent();
        deregisterCallback(id);
        request.onSuccess(cajoled.<R>cast());
      }

      @Override
      public void onError(String message) {
        // Ignore
      }
    });

    requestMaker.setSrc(url + "&" + JSONP_CALLBACK_PARAM + "=" + id);
    Document.get().getBody().appendChild(requestMaker);
  }
}
