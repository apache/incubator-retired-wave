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

package com.google.wave.api.oauth.impl;

import net.oauth.http.HttpMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * A simple extension of net.oauth.http.HttpMessage using an OpenSocialUrl
 * instead of java.net.URL and encoding the body as a String instead of an
 * InputStream.
 *
 * @author apijason@google.com (Jason Cooper)
 */
class OpenSocialHttpMessage extends HttpMessage {

  protected String body;
  protected OpenSocialUrl url;

  public OpenSocialHttpMessage(String method, OpenSocialUrl url) {
    this(method, url, null);
  }

  public OpenSocialHttpMessage(String method, OpenSocialUrl url, String body) {
    this.method = method;
    this.body = body;
    this.url = url;
  }

  public OpenSocialUrl getUrl() {
    return url;
  }

  public String getBodyString() {
    return body;
  }

  public void addHeader(String headerName, String value) {
    Map<String, String> messageHeaders = new HashMap<String, String>();
    messageHeaders.put(headerName, value);

    for (Map.Entry<String, String> entry : messageHeaders.entrySet()) {
      this.headers.add(entry);
    }
  }

  public void addHeaders(Map<String, String> messageHeaders) {
    for (Map.Entry<String, String> entry : messageHeaders.entrySet()) {
      this.headers.add(entry);
    }
  }
}
