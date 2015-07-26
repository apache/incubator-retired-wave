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

package org.waveprotocol.box.server.persistence.file;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.apache.commons.codec.binary.Hex;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.SignerInfoStore;
import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.crypto.DefaultCertPathStore;
import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.util.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A flat file based implementation of {@link SignerInfoStore}
 *
 * @author tad.glines@gmail.com (Tad Glines)
 */
public class FileSignerInfoStore implements SignerInfoStore {
  private static final String SIGNER_FILE_EXTENSION = ".signer";
  private final String signerInfoStoreBasePath;
  private final CertPathStore certPathStore = new DefaultCertPathStore();

  private static final Log LOG = Log.get(FileSignerInfoStore.class);

  @Inject
  public FileSignerInfoStore(Config config) {
    this.signerInfoStoreBasePath = config.getString("core.signer_info_store_directory");
  }

  private String signerIdToFileName(byte[] id) {
    return signerInfoStoreBasePath + File.separator + new String(Hex.encodeHex(id))
        + SIGNER_FILE_EXTENSION;
  }

  @Override
  public void initializeSignerInfoStore() throws PersistenceException {
    FileUtils.performDirectoryChecks(signerInfoStoreBasePath, SIGNER_FILE_EXTENSION,
        "signer info store", LOG);
  }

  @Override
  public SignerInfo getSignerInfo(byte[] signerId) throws SignatureException {
    synchronized(certPathStore) {
      SignerInfo signerInfo = certPathStore.getSignerInfo(signerId);
      File signerFile = new File(signerIdToFileName(signerId));
      if (signerInfo == null) {
        if (signerFile.exists()) {
          FileInputStream file = null;
          try {
            file = new FileInputStream(signerFile);
            ProtocolSignerInfo data = ProtocolSignerInfo.newBuilder().mergeFrom(file).build();
            signerInfo = new SignerInfo(data);
          } catch (SignatureException | IOException e) {
            throw new SignatureException("Failed to parse signer info from file: "
                + signerFile.getAbsolutePath(), e);
          } finally {
            FileUtils.closeAndIgnoreException(file, signerFile, LOG);
          }
        }
      }
      return signerInfo;
    }
  }

  @Override
  public void putSignerInfo(ProtocolSignerInfo protoSignerInfo) throws SignatureException {
    synchronized(certPathStore) {
      SignerInfo signerInfo = new SignerInfo(protoSignerInfo);
      File signerFile = new File(signerIdToFileName(signerInfo.getSignerId()));
      FileOutputStream file = null;
      try {
        file = new FileOutputStream(signerFile);
        file.write(protoSignerInfo.toByteArray());
        file.flush();
        certPathStore.putSignerInfo(protoSignerInfo);
      } catch (IOException e) {
        throw new SignatureException("Failed to write signer info to file: "
            + signerFile.getAbsolutePath(), e);
      } finally {
        FileUtils.closeAndIgnoreException(file, signerFile, LOG);
      }
    }
  }
}
