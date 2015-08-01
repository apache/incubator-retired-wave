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

package org.waveprotocol.box.webclient.common.communication.interchange;

/**
 * Holds status codes used by the client and set by WFE server or the client to
 * communicate success or reasons for failure of various remote calls issued
 * from the client.
 *
 * @author majewski@google.com (Bo Majewski)
 */
//TODO(shanestephens, majewski): consider grouping errors in a manner similar
//                               to HTTP error codes
public enum StatusCode {
  /**
   * Indicates that the response was successful.
   */
  SUCCESS(0, "Success"),

  /**
   * Indicates that the request timed out before it was answered.
   */
  TIMEOUT(1, "Request timed out"),

  /**
   * Indicates that the request has been canceled.
   */
  CANCELLED(2, "Request canceled"),

  /**
   * Indicates that the client is not connected to the server.
   */
  DISCONNECTED(3, "Not connected"),

  /**
   * Indicates that one of the wave server is not responding.
   */
  WAVE_SERVER_ERROR(4, "Wave server not responding"),

  /**
   * Indicates that the JSON response does not parse correctly.  Errors with
   * this code are generated client side.
   */
  INVALID_JSON(5, "Invalid JSON response"),

  /**
   * Indicates that the request could not be completed due to some other error.
   */
  OTHER_ERROR(1000, "Unknown error");

  private final int value;
  private final String description;

  StatusCode(int value, String description) {
    this.value = value;
    this.description = description;
  }

  /**
   * Returns the numeric value assigned to this status code.
   *
   * @return Numeric value of this status code.
   */
  public int getValue() {
    return this.value;
  }

  /**
   * @return A description of this status code.
   */
  public String getDescription() {
    return this.description;
  }

  /**
   * Converts a given value to a status code. If no status code with a matching
   * value is found, this method returns OTHER_ERROR.
   *
   * @return The status code corresponding to the given value.
   */
  public static StatusCode fromValue(int value) {
    for (StatusCode type : StatusCode.values()) {
      if (type.value == value) {
        return type;
      }
    }
    return OTHER_ERROR;
  }

  @Override
  public String toString() {
    return value + " " + description;
  }
}
