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

package org.waveprotocol.wave.model.operation;


/**
 * Operation exception boxed into a runtime exception.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
@SuppressWarnings("serial")
public class OperationRuntimeException extends RuntimeException {

  private final OperationException opException;
  private final String message;

  /**
   * @param message Description of unexpected failure
   * @param opException The boxed exception
   */
  public OperationRuntimeException(String message, OperationException opException) {
    super(message, opException);
    this.message = message;
    this.opException = opException;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "BoxedOpException: " + message + ": " + opException;
  }

  /**
   * @return The boxed exception
   */
  public OperationException get() {
    return opException;
  }
}
