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

package org.waveprotocol.box.webclient.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;

import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.common.logging.LoggerBundle;

/**
 * Manage locale settings on the server.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public final class RemoteLocaleService implements LocaleService {

  private static final LoggerBundle LOG = new DomLogger(RemoteLocaleService.class.getName());

  private static final String LOCALE_URL_BASE = "/locale";

  public RemoteLocaleService() {
  }

  @Override
  public void storeLocale(String locale) {
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST,
        LOCALE_URL_BASE + "/?locale=" + locale);

    LOG.trace().log("Store locale");

    requestBuilder.setCallback(new RequestCallback() {
      @Override
      public void onResponseReceived(Request request, Response response) {
        if (response.getStatusCode() != Response.SC_OK) {
          LOG.error().log("Got back status code " + response.getStatusCode());
        } else {
          LOG.error().log("Locale was stored");
        }
      }

      @Override
      public void onError(Request request, Throwable exception) {
        LOG.error().log("Storing locale error: ", exception);
      }
    });

    try {
      requestBuilder.send();
    } catch (RequestException e) {
      LOG.error().log(e.getMessage());
    }
  }
}
