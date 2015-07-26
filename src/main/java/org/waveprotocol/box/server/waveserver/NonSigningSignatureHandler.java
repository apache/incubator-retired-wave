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

package org.waveprotocol.box.server.waveserver;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.waveprotocol.box.server.CoreSettingsNames;
import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;

import java.util.Collections;

/**
 * Signature handler that doesn't sign deltas.
 */
public class NonSigningSignatureHandler implements SignatureHandler {
  /**
   * Guice {@link Provider} for the instance of {@link NonSigningSignatureHandler}
   */
  @Singleton
  public static class NonSigningSignatureHandlerProvider implements Provider<SignatureHandler> {
    private final String waveDomain;
    private NonSigningSignatureHandler signer = null;

    @Inject
    public NonSigningSignatureHandlerProvider(
        @Named(CoreSettingsNames.WAVE_SERVER_DOMAIN) String waveDomain) {
      this.waveDomain = waveDomain;
    }
    
    @Override
    public NonSigningSignatureHandler get() {
      synchronized (this) {
        if (signer == null) {
          signer = new NonSigningSignatureHandler(waveDomain);
        }
      }
      return signer;
    }
  }
  
  
  private final String domain;

  public NonSigningSignatureHandler(String domain) {
    this.domain = domain;
  }

  @Override
  public String getDomain() {
    return domain;
  }

  @Override
  public SignerInfo getSignerInfo() {
    return null;
  }

  @Override
  public Iterable<ProtocolSignature> sign(ByteStringMessage<ProtocolWaveletDelta> delta) {
    return Collections.emptyList();
  }

}
