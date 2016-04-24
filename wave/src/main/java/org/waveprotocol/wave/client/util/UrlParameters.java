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


package org.waveprotocol.wave.client.util;

import com.google.gwt.http.client.URL;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * NOTE(user): Strictly speaking the initial '?' is not part of the query
 * string, but we treat it as part of the query string in this class for
 * convenience.
 *
 */
public class UrlParameters implements TypedSource {

  private static UrlParameters singleton;

  private final HashMap<String, String> map = new HashMap<String, String>();

  private static native String getQueryString() /*-{
    return $wnd.location.search;
  }-*/;

  UrlParameters(String query) {
    if (query.length() > 1) {
      String[] keyvalpairs = query.substring(1, query.length()).split("&");
      for (String pair : keyvalpairs) {
        String[] keyval = pair.split("=");
        // Some basic error handling for invalid query params.
        if (keyval.length == 2) {
          map.put(URL.decodeComponent(keyval[0]), URL.decodeComponent(keyval[1]));
        } else if (keyval.length == 1) {
          map.put(URL.decodeComponent(keyval[0]), "");
        }
      }
    }
  }

  public String getParameter(String name) {
    return map.get(name);
  }

  public static UrlParameters get() {
    if (singleton == null) {
      singleton = new UrlParameters(getQueryString());
    }
    return singleton;
  }

  /** {@inheritDoc} */
  public Boolean getBoolean(String key) {
    String value = getParameter(key);
    if (value == null) {
      return null;
    }
    return Boolean.valueOf(value);
  }

  /** {@inheritDoc} */
  public Double getDouble(String key) {
    String value = getParameter(key);
    if (value == null) {
      return null;
    }

    try {
      return Double.valueOf(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** {@inheritDoc} */
  public Integer getInteger(String key) {
    String value = getParameter(key);
    if (value == null) {
      return null;
    }

    try {
      return Integer.valueOf(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** {@inheritDoc} */
  public String getString(String key) {
    String value = getParameter(key);
    return value;
  }

  /**
   * Build a query string out of a map of key/value pairs.
   * @param queryEntries
   */
  public static String buildQueryString(Map<String, String> queryEntries) {
    StringBuffer sb = new StringBuffer();
    boolean firstIteration = true;
    for (Entry<String, String> e : queryEntries.entrySet()) {
      if (firstIteration) {
        sb.append('?');
      } else {
        sb.append('&');
      }
      String encodedName = URL.encodeComponent(e.getKey());
      sb.append(encodedName);

      sb.append('=');

      String encodedValue = URL.encodeComponent(e.getValue());
      sb.append(encodedValue);
      firstIteration = false;
    }
    return sb.toString();
  }
}
