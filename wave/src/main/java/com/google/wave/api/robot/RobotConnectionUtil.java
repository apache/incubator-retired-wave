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
import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletResponse;

/**
 * A helper class that contains utility method to be used by
 * {@link RobotConnection}'s subclasses.
 *
 */
public class RobotConnectionUtil {

  /**
   * Validates and read the response.
   *
   * @param url the URL where the response was received from.
   * @param statusCode the HTTP status code.
   * @param response the raw response of fetching the given URL.
   * @return the response, as {@link String}.
   *
   * @throws RobotConnectionException if the response code is not HTTP OK (200).
   */
  public static String validateAndReadResponse(String url, int statusCode, byte[] response)
      throws RobotConnectionException {
    if (statusCode != HttpServletResponse.SC_OK) {
      String msg = "Robot fetch http failure: " + url + ": " + statusCode;
      throw new RobotConnectionException(msg, statusCode);
    }

    // Read the response.
    return new String(response, Charsets.UTF_8);
  }

  /**
   * Validates and read the response.
   *
   * @param url the URL where the response was received from.
   * @param statusCode the HTTP status code.
   * @param response the raw response of fetching the given URL.
   * @return the response, as {@link String}.
   *
   * @throws RobotConnectionException if the response code is not HTTP OK (200).
   */
  public static String validateAndReadResponse(String url, int statusCode, InputStream response)
      throws RobotConnectionException {
    try {
      return validateAndReadResponse(url, statusCode, ByteStreams.toByteArray(response));
    } catch (IOException e) {
      String msg = "Robot fetch http failure: " + url + ".";
      throw new RobotConnectionException(msg, statusCode);
    }
  }
}
