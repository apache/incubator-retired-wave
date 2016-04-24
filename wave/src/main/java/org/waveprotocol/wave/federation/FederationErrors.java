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

package org.waveprotocol.wave.federation;

import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;

/**
 * Convenience methods for creating {@code FederationError}s.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public class FederationErrors {

  private FederationErrors() {
  }

  public static FederationError newFederationError(FederationError.Code errorCode) {
    return FederationError.newBuilder().setErrorCode(errorCode).build();
  }

  public static FederationError newFederationError(FederationError.Code errorCode,
      String errorMessage) {
    return FederationError.newBuilder()
        .setErrorCode(errorCode)
        .setErrorMessage(errorMessage).build();
  }

  public static FederationError badRequest(String errorMessage) {
    return newFederationError(FederationError.Code.BAD_REQUEST, errorMessage);
  }

  public static FederationError internalServerError(String errorMessage) {
    return newFederationError(FederationError.Code.INTERNAL_SERVER_ERROR, errorMessage);
  }
}
