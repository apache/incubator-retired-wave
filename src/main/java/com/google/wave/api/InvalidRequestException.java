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

package com.google.wave.api;


/**
 * Checked exception for signaling invalid input supplied by a robot for an
 * operation request.
 *
 * @author davidbyttow@google.com (David Byttow)
 */
public class InvalidRequestException extends Exception {

  private final OperationRequest invalidOperation;

  /**
   * Constructor with a detail message.
   *
   * @param message detail message for this exception.
   */
  public InvalidRequestException(String message, OperationRequest operation) {
    super(message);
    this.invalidOperation = operation;
  }

  public InvalidRequestException(String message, OperationRequest operation, Exception e) {
    super(message, e);
    this.invalidOperation = operation;
  }

  public InvalidRequestException(String message) {
    super(message);
    this.invalidOperation = null;
  }

  public OperationRequest getInvalidOperation() {
    return invalidOperation;
  }
}
