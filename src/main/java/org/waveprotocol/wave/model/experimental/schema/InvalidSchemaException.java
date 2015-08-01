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

package org.waveprotocol.wave.model.experimental.schema;

/**
 * An exception thrown to indicate an invalid schema.
 *
 */
public final class InvalidSchemaException extends Exception {

  /**
   * Constructs a new exception with null as its detail message.
   */
  public InvalidSchemaException() {
    super();
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause
   */
  public InvalidSchemaException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public InvalidSchemaException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified cause and a detail message of
   * <tt>(cause==null ? null : cause.toString())</tt>.
   *
   * @param cause the cause
   */
  public InvalidSchemaException(Throwable cause) {
    super(cause);
  }

}
