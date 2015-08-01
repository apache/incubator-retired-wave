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

package org.waveprotocol.wave.client.common.util;

/**
 * We use this exception class for debugging purposes. Mainly, this class is
 * used to wrap NPEs that are caught with a try/catch around code that is
 * suspected of generating them. NOTE: Once we'll have stack traces for these
 * NPEs in all browsers the use of this exception should cease.
 *
 */
public class ClientDebugException extends RuntimeException {

  /**
   * @param message Some useful message about the exception
   */
  public ClientDebugException(String message) {
    super(message);
  }

  /**
   * @param message Some useful message about the exception
   * @param cause The original exception to be wrapped.
   */
  public ClientDebugException(String message, Throwable cause) {
    super(message, cause);
  }

}
