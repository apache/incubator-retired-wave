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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Future;

/**
 * Interface for sending messages to robots. This utility supports synchronous
 * and asynchronous {@code GET} and {@code POST} methods.
 *
 */
public interface RobotConnection {
  /** Constant for JSON content type. */
  static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

  /**
   * Fetches a robot URL using {@code HTTP GET} method.
   *
   * @param url the robot's URL.
   * @return the content of the URL.
   *
   * @throws RobotConnectionException if there is a problem fetching the URL,
   *     for example, if the response code is not HTTP OK (200).
   */
  String get(String url) throws RobotConnectionException;

  /**
   * Asynchronously fetches a robot URL using {@code HTTP GET} method.
   *
   * @param url the robot's URL.
   * @return a {@link Future} that represents the content of the URL.
   *
   * @throws RobotConnectionException if there is a problem fetching the URL,
   *     for example, if the response code is not HTTP OK (200).
   */
  ListenableFuture<String> asyncGet(String url) throws RobotConnectionException;

  /**
   * Fetches a robot URL using {@code HTTP POST} method.
   *
   * @param url the robot's URL.
   * @param jsonBody the POST body of the request, in JSON.
   * @return the content of the URL.
   *
   * @throws RobotConnectionException if there is a problem fetching the URL,
   *     for example, if the response code is not HTTP OK (200).
   */
  String postJson(String url, String jsonBody) throws RobotConnectionException;

  /**
   * Asynchronously fetches a robot URL using {@code HTTP POST} method.
   *
   * @param url the robot's URL.
   * @param jsonBody the POST body of the request, in JSON.
   * @return a {@link Future} that represents the content of the URL.
   *
   * @throws RobotConnectionException if there is a problem fetching the URL,
   *     for example, if the response code is not HTTP OK (200).
   */
  ListenableFuture<String> asyncPostJson(String url, String jsonBody)
      throws RobotConnectionException;
}
