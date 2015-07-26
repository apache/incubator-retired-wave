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

import net.oauth.http.HttpResponseMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A small implementation of an HttpResponseMessage that does not require
 * org.apache.http.client as a dependency.
 *
 * @author api.dwh@google.com (Dan Holevoet)
 * @author apijason@google.com (Jason Cooper)
 */
class OpenSocialHttpResponseMessage extends HttpResponseMessage {

  protected int status;

  protected OpenSocialHttpResponseMessage(String method, OpenSocialUrl url,
      InputStream responseStream, int status) throws IOException {
    super(method, url.toURL());

    this.body = responseStream;
    this.status = status;
  }

  /**
   * Returns the status code for the response.
   *
   * @return Status code
   */
  @Override
  public int getStatusCode() {
    return this.status;
  }

  /**
   * Transforms response output contained in the InputStream object returned by
   * the connection into a string representation which can later be parsed into
   * a more meaningful object, e.g. OpenSocialPerson.
   *
   * @return Response body as a String
   * @throws IOException if the InputStream is not retrievable or accessible
   */
  public String getBodyString() throws IOException {
    if (body != null) {
      StringBuilder sb = new StringBuilder();
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(body));

      String line = null;
      while ((line = reader.readLine()) != null) {
        sb.append(line);
      }

      body.close();

      return sb.toString();
    }

    return null;
  }
}
