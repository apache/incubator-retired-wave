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

package org.waveprotocol.wave.client.gadget.renderer;

import com.google.gwt.core.client.Duration;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;

import org.waveprotocol.wave.model.id.WaveletName;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Gadget data store implementation.
 *
 * Please see the initial metadata call spec at:
 * https://cwiki.apache.org/SHINDIG/shindigs-metadata-call.html
 *
 *<p>
 * <li>TODO(user): Add unit tests.</li>
 * <li>TODO(vadimg): Consider batching gadget requests to improve performance.
 * </li>
 * <li>TODO(vadimg): Work out how to gracefully renew expired security tokens.
 * </li>
 *
 */
public class GadgetDataStoreImpl implements GadgetDataStore {

  /**
   * Lifetime of a cache element in milliseconds. The cache elements can be
   * evicted after this time period. WFE also gets long-life security tokens (1
   * hour) to support this cache expiration time.
   */
  private static final double CACHE_EXPIRATION_TIME_MS = 3600000;

  /** Gadget Metadata path. */
  public static final String GADGET_METADATA_PATH = "/gadgets/metadata";

  /**
   * Cache element class that contains both cached gadget metadata and
   * expiration time.
   */
  private static class CacheElement {
    private final GadgetMetadata metadata;
    private final String securityToken;
    private final double expirationTime;

    CacheElement(GadgetMetadata metadata, String securityToken) {
      this.metadata = metadata;
      this.securityToken = securityToken;
      expirationTime = Duration.currentTimeMillis() + CACHE_EXPIRATION_TIME_MS;
    }

    GadgetMetadata getMetadata() {
      return metadata;
    }

    String getSecurityToken() {
      return securityToken;
    }

    boolean expired() {
      return Duration.currentTimeMillis() > expirationTime;
    }
  }

  /** Metadata cache. Maps the gadget instance key to cache element objects. */
  private final Map<String, CacheElement> metadataMap = new HashMap<String, CacheElement>();

  private static GadgetDataStoreImpl singleton = null;

  /** Private singleton constructor. */
  private GadgetDataStoreImpl() {
  }

  /**
   * Retrieves the class singleton.
   *
   * @return singleton instance of the class.
   */
  static GadgetDataStore getInstance() {
    if (singleton == null) {
      singleton = new GadgetDataStoreImpl();
    }
    return singleton;
  }

  private void cleanupExpiredCache() {
    Iterator<CacheElement> i = metadataMap.values().iterator();
    while (i.hasNext()) {
      if (i.next().expired()) {
        i.remove();
      }
    }
  }

  private boolean fetchDataByKey(String key, DataCallback receiveDataCommand) {
    if (metadataMap.containsKey(key)) {
      CacheElement cache = metadataMap.get(key);
      receiveDataCommand.onDataReady(cache.getMetadata(), cache.getSecurityToken());
      return true;
    }
    return false;
  }

  @Override
  public void getGadgetData(final String gadgetSpecUrl, WaveletName waveletName, int instanceId,
      final DataCallback receiveDataCommand) {
    cleanupExpiredCache();
    final String secureGadgetDataKey =
        waveletName.waveId + " " + waveletName.waveletId + " " + instanceId + " " + gadgetSpecUrl;
    if (fetchDataByKey(secureGadgetDataKey, receiveDataCommand)) {
      return;
    }
    final String nonSecureGadgetDataKey = gadgetSpecUrl;
    if (fetchDataByKey(nonSecureGadgetDataKey, receiveDataCommand)) {
      return;
    }

    JSONObject request = new JSONObject();
    JSONObject requestContext = new JSONObject();
    JSONArray gadgets = new JSONArray();
    JSONObject gadget = new JSONObject();
    try {
      gadget.put("url", new JSONString(gadgetSpecUrl));
      gadgets.set(0, gadget);
      requestContext.put("container", new JSONString("wave"));
      request.put("context", requestContext);
      request.put("gadgets", gadgets);
      RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, GADGET_METADATA_PATH);
      builder.sendRequest(request.toString(), new RequestCallback() {

        public void onError(Request request, Throwable exception) {
          receiveDataCommand.onError("Error retrieving metadata from the server.", exception);
        }

        public void onResponseReceived(Request request, Response response) {
          JSONObject gadgetMetadata = null;
          try {
            gadgetMetadata =
                JSONParser.parseLenient(response.getText()).isObject().get("gadgets").isArray().get(
                    0).isObject();
          } catch (NullPointerException exception) {
            receiveDataCommand.onError("Error in gadget metadata JSON.", exception);
          }
          if (gadgetMetadata != null) {
            GadgetMetadata metadata = new GadgetMetadata(gadgetMetadata);

            // TODO: Security token is unused therefore the gadget is stored
            // under the non secure key.
            String securityToken = null;
            metadataMap.put(nonSecureGadgetDataKey, new CacheElement(metadata, securityToken));

            receiveDataCommand.onDataReady(metadata, securityToken);
          } else {
            receiveDataCommand.onError("Error in gadget metadata JSON.", null);
          }
        }
      });
    } catch (RequestException e) {
      receiveDataCommand.onError("Unable to process gadget request.", e);
    }

  }
}
