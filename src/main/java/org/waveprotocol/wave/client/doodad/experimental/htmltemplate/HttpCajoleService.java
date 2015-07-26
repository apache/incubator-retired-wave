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

/**
 * Exposes a public http cajoling service as a {@link CajoleService}.
 */
public final class HttpCajoleService implements CajoleService {

  /** Public cajoling service. */
  private static final String SERVICE_URL = "http://caja.appspot.com/cajole";

  /** String to prepend to page URLs to form the cajoling request. */
  private static final String URL_PREFIX = SERVICE_URL //
      + "?input-mime-type=text/html" //
      + "&output-mime-type=application/javascript" //
      + "&alt=json-in-script" //
      + "&url=";

  /**
   * Exposes the JSO response as a {@link CajoleService.CajolerResponse}.
   */
  private static final class CajolerResponseJsoImpl extends JavaScriptObject
      implements CajolerResponse {

    // GWT requires JSO constructor visibility to be exactly protected
    @SuppressWarnings("unused")
    protected CajolerResponseJsoImpl() {
    }

    @Override
    public native String getHtml() /*-{
      return this.html;
    }-*/;

    @Override
    public native String getJs() /*-{
      return this.js;
    }-*/;
  }

  /** RPC processo for cajoler request/responses. */
  private final JsonpRouter<CajolerResponseJsoImpl> router = JsonpRouter.create();

  @Override
  public void cajole(String scriptUrl, Callback<? super CajolerResponse> callback) {
    router.request(URL_PREFIX + scriptUrl, callback);
  }
}
