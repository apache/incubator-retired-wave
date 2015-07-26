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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.crypto.WaveSigner;
import org.waveprotocol.wave.crypto.WaveSignerFactory;
import org.waveprotocol.wave.federation.Proto.ProtocolSignature;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * A signature handler that delegates to a wave signer to sign deltas.
 */
public class SigningSignatureHandler implements SignatureHandler {
  /**
   * Guice {@link Provider} for the instance of {@link SigningSignatureHandler}
   */
  @Singleton
  public static class SigningSignatureHandlerProvider implements Provider<SignatureHandler> {
    private static final FileOpener FILE_OPENER = new FileOpener();

    private String privateKey;
    private List<String> certs;
    private String certDomain;
    private final WaveSignerFactory waveSignerFactory;
    private SigningSignatureHandler signer = null;

    /**
     * @param factory A {@link WaveSignerFactory}.
     * @param config the configuration.
     */
    @Inject
    public SigningSignatureHandlerProvider(WaveSignerFactory factory, Config config) {
      this.privateKey = config.getString("federation.certificate_private_key");
      this.certs = config.getStringList("federation.certificate_files");
      this.certDomain = config.getString("federation.certificate_domain");
      this.waveSignerFactory = factory;
    }

    @Override
    public SigningSignatureHandler get() {
      synchronized (this) {
        if (signer == null) {
          FileInputStream privateKeyStream;
          try {
            privateKeyStream = new FileInputStream(privateKey);
          } catch (FileNotFoundException e) {
            throw new ProvisionException("could not read private key", e);
          }

          Iterable<FileInputStream> certStreams =
              Iterables.transform(certs, FILE_OPENER);

          try {
            WaveSigner inner = waveSignerFactory.getSigner(privateKeyStream, certStreams, certDomain);
            signer = new SigningSignatureHandler(inner);
          } catch (SignatureException e) {
            throw new ProvisionException("could not make wave signer", e);
          }
        }
      }
      return signer;
    }

    // Function that turns file names into FileInputStreams
    private static class FileOpener implements Function<String, FileInputStream> {

      @Override
      public FileInputStream apply(String filename) {
        try {
          return new FileInputStream(filename);
        } catch (FileNotFoundException e) {
          throw new ProvisionException("could not read certificates", e);
        }
      }
    }
  }

  private final WaveSigner signer;

  public SigningSignatureHandler(WaveSigner signer) {
    this.signer = signer;
  }

  @Override
  public String getDomain() {
    return signer.getSignerInfo().getDomain();
  }

  public SignerInfo getSignerInfo() {
    return signer.getSignerInfo();
  }

  @Override
  public Iterable<ProtocolSignature> sign(ByteStringMessage<ProtocolWaveletDelta> delta) {
    return ImmutableList.of(signer.sign(delta.getByteString().toByteArray()));
  }

}
