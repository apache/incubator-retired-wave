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

package org.waveprotocol.wave.concurrencycontrol.common;

public enum ResponseCode {
  /** Unknown response code. */
  UNKNOWN(-1),

  /** The delta was successfully transformed and applied. */
  OK(0),

  /** The WaveletSubmitRequest was ill-formed. */
  BAD_REQUEST(1),

  /** An internal error occurred. */
  INTERNAL_ERROR(2),

  /** The verifiedUserId or delta.userId were rejected by access control. */
  NOT_AUTHORIZED(3),

  /** The delta.version and delta.preSignature didn't match a point in the wavelet history. */
  VERSION_ERROR(4),

  /** An operation in the delta was invalid (before, during, or after transformation). */
  INVALID_OPERATION(5),

  /** An operation in the delta didn't preserve the document schema. */
  SCHEMA_VIOLATION(6),

  /** The delta is too big or the resulting document count or size is too big. */
  SIZE_LIMIT_EXCEEDED(7),

  /** An operation was rejected by the namespace policer. */
  POLICY_VIOLATION(8),

  /** The object is unavailable because it has been quarantined. */
  QUARANTINED(9),

  /**
   * The version the delta was being submitted against is too old.
   * The client should reconnect, transform, and retry.
   */
  TOO_OLD(10);

  private final int value;

  private ResponseCode(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  /**
   * @return The response code whose getValue() is the specified integer.
   * @throws IndexOutOfBoundsException if there is no such response code.
   */
  public static ResponseCode of(int value) {
    // NOTE: This implementation assumes that the response codes have consecutive
    // integer values beginning with -1.
    return values()[value + 1];
  }
}
