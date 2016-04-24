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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;


/**
 * A small implementation of HttpClient to serve the needs of the OAuth library
 * rather than requiring org.apache.http.client as a dependency.
 *
 * @author api.dwh@google.com (Dan Holevoet)
 * @author davidbyttow@google.com (David Byttow)
 */
public class OpenSocialHttpClient implements net.oauth.http.HttpClient {
  
  /** Size of data stream buffer. */
  private static final int BUF_SIZE = 0x1000; // 4K
  
  /**
   * Executes the request, sending the request body if applicable.
   * 
   * @param request
   * @return Response message
   * @throws IOException
   */
  @Override
  public OpenSocialHttpResponseMessage execute(HttpMessage request, 
      Map<String,Object> parameters) throws IOException {
    
    // If a POST message, translates the body into a string.
    String body = null;
    if (request.getBody() != null) {
      body = new String(toByteArray(request.getBody()));
    }
    
    OpenSocialHttpMessage openSocialRequest = new OpenSocialHttpMessage(
        request.method, new OpenSocialUrl(request.url.toExternalForm()), body);
    return execute(openSocialRequest);
  }

  /**
   * Executes the request, sending the request body if applicable.
   * 
   * @param request
   * @return Response message
   * @throws IOException
   */
  private OpenSocialHttpResponseMessage execute(OpenSocialHttpMessage request)
      throws IOException {
    final String method = request.method;
    final boolean isPut = PUT.equalsIgnoreCase(method);
    final boolean isPost = POST.equalsIgnoreCase(method);
    final boolean isDelete = DELETE.equalsIgnoreCase(method);

    final String bodyString = request.getBodyString();
    final String contentType = request.getHeader(HttpMessage.CONTENT_TYPE);
    final OpenSocialUrl url = request.getUrl();

    OpenSocialHttpResponseMessage response = null;
    if (isPut) {
      response = send("PUT", url, contentType, bodyString);
    } else if (isPost) {
      response = send("POST", url, contentType, bodyString);
    } else if (isDelete) {
      response = send("DELETE", url, contentType);
    } else {
      response = send("GET", url, contentType);
    }

    return response;
  }

  /**
   * Executes a request without writing any data in the request's body.
   *
   * @param method
   * @param url
   * @return Response message
   */
  private OpenSocialHttpResponseMessage send(String method, OpenSocialUrl url,
      String contentType) throws IOException {
    return send(method, url, contentType, null);
  }

  /**
   * Executes a request and writes all data in the request's body to the
   * output stream.
   *
   * @param method
   * @param url
   * @param body
   * @return Response message
   */
  private OpenSocialHttpResponseMessage send(String method, OpenSocialUrl url,
      String contentType, String body) throws IOException {
    HttpURLConnection connection = null;
    try {
      connection = getConnection(method, url, contentType);
      if (body != null) {
        OutputStreamWriter out =
          new OutputStreamWriter(connection.getOutputStream());
        out.write(body);
        out.flush();
        out.close();
      }

      return new OpenSocialHttpResponseMessage(method, url,
          connection.getInputStream(), connection.getResponseCode());
    } catch (IOException e) {
      throw new IOException("Container returned status " +
          connection.getResponseCode() + " \"" + e.getMessage() + "\"");
    }
  }

  /**
   * Opens a new HTTP connection for the URL associated with this object.
   *
   * @param method
   * @param url
   * @return Opened connection
   * @throws IOException if URL is invalid, or unsupported
   */
  private HttpURLConnection getConnection(String method, OpenSocialUrl url,
      String contentType) throws IOException {
    HttpURLConnection connection =
      (HttpURLConnection) new URL(url.toString()).openConnection();
    if (contentType != null && !contentType.equals("")) {
      connection.setRequestProperty(HttpMessage.CONTENT_TYPE, contentType);
    }
    connection.setRequestMethod(method);
    connection.setDoOutput(true);
    connection.connect();

    return connection;
  }
  
  private static long copy(InputStream from, OutputStream to) throws IOException {
    byte[] buf = new byte[BUF_SIZE];
    long total = 0;
    while (true) {
      int r = from.read(buf);
      if (r == -1) {
        break;
      }
      to.write(buf, 0, r);
      total += r;
    }
    return total;
  }
  
  private static byte[] toByteArray(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    copy(in, out);
    return out.toByteArray();
  }
}
