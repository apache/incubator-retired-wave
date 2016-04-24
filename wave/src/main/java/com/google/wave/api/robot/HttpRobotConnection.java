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

package com.google.wave.api.robot;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * A {@link RobotConnection} that uses Apache's {@code HTTP Client} for
 * communicating with the robot.
 *
 */
public class HttpRobotConnection implements RobotConnection {

  /** A user agent client that can execute HTTP methods. */
  private final HttpClient httpClient;

  /** An executor to submit tasks asynchronously. */
  private final ExecutorService executor;

  /**
   * Constructor.
   *
   * @param client the client for executing HTTP methods.
   * @param executor the executor for submitting tasks asynchronously.
   */
  public HttpRobotConnection(HttpClient client, ExecutorService executor) {
    this.httpClient = client;
    this.executor = executor;
  }

  @Override
  public String get(String url) throws RobotConnectionException {
    GetMethod method = new GetMethod(url);
    return fetch(url, method);
  }

  @Override
  public ListenableFuture<String> asyncGet(final String url) {
    return JdkFutureAdapters.listenInPoolThread(executor.submit(new Callable<String>() {
      @Override
      public String call() throws RobotConnectionException {
        return get(url);
      }
    }));
  }

  @Override
  public String postJson(String url, String body) throws RobotConnectionException {
    PostMethod method = new PostMethod(url);
    try {
      method.setRequestEntity(new StringRequestEntity(body, RobotConnection.JSON_CONTENT_TYPE,
          Charsets.UTF_8.name()));
      return fetch(url, method);
    } catch (IOException e) {
      String msg = "Robot fetch http failure: " + url + ": " + e;
      throw new RobotConnectionException(msg, e);
    }
  }

  @Override
  public ListenableFuture<String> asyncPostJson(final String url, final String body) {
    return JdkFutureAdapters.listenInPoolThread(executor.submit(new Callable<String>() {
      @Override
      public String call() throws RobotConnectionException {
        return postJson(url, body);
      }
    }));
  }

  /**
   * Fetches the given URL, given a method ({@code GET} or {@code POST}).
   *
   * @param url the URL to be fetched.
   * @param method the method to fetch the URL, can be {@code GET} or
   *     {@code POST}.
   * @return the content of the URL.
   *
   * @throws RobotConnectionException if there is a problem fetching the URL,
   *     for example, if the response code is not HTTP OK (200).
   */
  private String fetch(String url, HttpMethod method) throws RobotConnectionException {
    try {
      int statusCode = httpClient.executeMethod(method);
      return RobotConnectionUtil.validateAndReadResponse(url, statusCode,
          method.getResponseBodyAsStream());
    } catch (IOException e) {
      String msg = "Robot fetch http failure: " + url + ".";
      throw new RobotConnectionException(msg, e);
    } finally {
      method.releaseConnection();
    }
  }
}
