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
package org.waveprotocol.box.server.util;

import com.google.common.collect.Maps;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

/**
 * Processes URL parameters.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class UrlParameters {
  private UrlParameters() {}

  public static String addParameter(String url, String name, String value) {
    int qpos = url.indexOf('?');
    int hpos = url.indexOf('#');
    char sep = qpos == -1 ? '?' : '&';
    String seg = sep + encodeUrl(name) + '=' + encodeUrl(value);
    return hpos == -1 ? url + seg : url.substring(0, hpos) + seg
        + url.substring(hpos);
  }

  public static Map<String, String> getParameters(String query) {
    Map<String, String> map = Maps.newHashMap();
    if (query != null) {
      String[] params = query.split("&");
      for (String param : params) {
        String[] parts = param.split("=");
        if (parts.length == 2) {
          map.put(parts[0], parts[1]);
        }
      }
    }
    return map;
  }

  private static String encodeUrl(String url) {
    try {
      return URLEncoder.encode(url, "UTF-8");
    } catch (UnsupportedEncodingException uee) {
      throw new IllegalArgumentException(uee);
    }
  }
}
