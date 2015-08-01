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

package org.waveprotocol.wave.federation.noop;

import com.google.protobuf.ByteString;

import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.model.id.WaveletName;

import java.util.List;

/**
 * An of {@link WaveletFederationListener.Factory} that returns instances of
 * {@link WaveletFederationListener} that always call the onFailure() method
 * of the callback.
 * 
 * @author tad.glines@gmail.com (Tad Glines)
 */
public class NoOpFederationHost implements WaveletFederationListener.Factory {

  @Override
  public WaveletFederationListener listenerForDomain(String domain) {
    return new WaveletFederationListener() {
      @Override
      public void waveletDeltaUpdate(WaveletName waveletName, List<ByteString> deltas,
          WaveletUpdateCallback callback) {
        callback.onFailure(FederationErrors.badRequest("Federation is not enabled!"));
      }

      @Override
      public void waveletCommitUpdate(WaveletName waveletName,
          ProtocolHashedVersion committedVersion, WaveletUpdateCallback callback) {
        callback.onFailure(FederationErrors.badRequest("Federation is not enabled!"));
      }
    };
  }

}
