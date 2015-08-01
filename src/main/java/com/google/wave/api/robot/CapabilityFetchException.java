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
 * Exception thrown when retrieving robot capabilities.
 *
 */
public class CapabilityFetchException extends Exception {

  public final int httpStatus;
  /**
   * Constructs a new exception with the given detail message.
   */
  public CapabilityFetchException(String message) {
    super(message);
    httpStatus = 0;
  }

  /**
   * Constructs a new exception with the given cause.
   */
  public CapabilityFetchException(Throwable cause) {
    super(cause);
    httpStatus = 0;
  }

  /**
   * Constructs a new exception with the given detail message and cause.
   */
  public CapabilityFetchException(String message, Throwable cause) {
    super(message, cause);
    httpStatus = 0;
  }

  public CapabilityFetchException(String message, int status) {
    super(message);
    httpStatus = status;
  }
}
