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

/**
 * Checked exception when a robot cannot be reached via HTTP.
 *
 */
public class RobotConnectionException extends Exception {

  private final int statusCode;

  /**
   * Constructor with a detail message.
   *
   * @param message detail message for this exception.
   */
  public RobotConnectionException(String message) {
    super(message);
    this.statusCode = 0;
  }

  /**
   * Constructor with a detail message and cause.
   *
   * @param message detail message for this exception.
   * @param cause the exception that caused this.
   */
  public RobotConnectionException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = 0;
  }

  /**
   * Constructor with a detail message and status code.
   *
   * @param message detail message for this exception.
   * @param statusCode the status code returned by the robot.
   */
  public RobotConnectionException(String message, int statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  /** @return the status code returned by the robot. */
  public int getStatusCode() {
    return statusCode;
  }
}
