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

package org.waveprotocol.box.server.persistence;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import org.waveprotocol.box.server.persistence.file.FileAccountStore;
import org.waveprotocol.box.server.persistence.file.FileAttachmentStore;
import org.waveprotocol.box.server.persistence.file.FileDeltaStore;
import org.waveprotocol.box.server.persistence.file.FileSignerInfoStore;
import org.waveprotocol.box.server.persistence.memory.MemoryDeltaStore;
import org.waveprotocol.box.server.persistence.memory.MemoryStore;
import org.waveprotocol.box.server.persistence.mongodb.MongoDbProvider;
import org.waveprotocol.box.server.waveserver.DeltaStore;
import org.waveprotocol.wave.crypto.CertPathStore;

/**
 * Module for setting up the different persistence stores.
 *
 *<p>
 * The valid names for the cert store are 'memory', 'file' and 'mongodb'
 *
 *<p>
 *The valid names for the attachment store are 'disk' and 'mongodb'
 *
 *<p>
 *The valid names for the account store are 'memory', 'file' and 'mongodb'.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class PersistenceModule extends AbstractModule {

  private final String signerInfoStoreType;

  private final String attachmentStoreType;

  private final String accountStoreType;

  private final String deltaStoreType;

  private MongoDbProvider mongoDbProvider;

  private final String mongoDBHost;

  private final String mongoDBPort;

  private final String mongoDBdatabase;


  @Inject
  public PersistenceModule(Config config) {
    this.signerInfoStoreType = config.getString("core.signer_info_store_type");
    this.attachmentStoreType = config.getString("core.attachment_store_type");
    this.accountStoreType = config.getString("core.account_store_type");
    this.deltaStoreType = config.getString("core.delta_store_type");
    this.mongoDBHost = config.getString("core.mongodb_host");
    this.mongoDBPort = config.getString("core.mongodb_port");
    this.mongoDBdatabase = config.getString("core.mongodb_database");
  }

  /**
   * Returns a {@link MongoDbProvider} instance.
   */
  public MongoDbProvider getMongoDbProvider() {
    if (mongoDbProvider == null) {
      mongoDbProvider = new MongoDbProvider(mongoDBHost, mongoDBPort, mongoDBdatabase);
    }
    return mongoDbProvider;
  }

  @Override
  protected void configure() {
    bindCertPathStore();
    bindAttachmentStore();
    bindAccountStore();
    bindDeltaStore();
  }

  /**
   * Binds the CertPathStore implementation to the store specified in the
   * properties.
   */
  private void bindCertPathStore() {
    if (signerInfoStoreType.equalsIgnoreCase("memory")) {
      bind(CertPathStore.class).to(MemoryStore.class).in(Singleton.class);
    } else if (signerInfoStoreType.equalsIgnoreCase("file")) {
      bind(CertPathStore.class).to(FileSignerInfoStore.class).in(Singleton.class);
    } else if (signerInfoStoreType.equalsIgnoreCase("mongodb")) {
      MongoDbProvider mongoDbProvider = getMongoDbProvider();
      bind(CertPathStore.class).toInstance(mongoDbProvider.provideMongoDbStore());
    } else {
      throw new RuntimeException(
          "Invalid certificate path store type: '" + signerInfoStoreType + "'");
    }
  }

  private void bindAttachmentStore() {
    if (attachmentStoreType.equalsIgnoreCase("disk")) {
      bind(AttachmentStore.class).to(FileAttachmentStore.class).in(Singleton.class);
    } else if (attachmentStoreType.equalsIgnoreCase("mongodb")) {
      MongoDbProvider mongoDbProvider = getMongoDbProvider();
      bind(AttachmentStore.class).toInstance(mongoDbProvider.provideMongoDbStore());
    } else {
      throw new RuntimeException("Invalid attachment store type: '" + attachmentStoreType + "'");
    }
  }

  private void bindAccountStore() {
    if (accountStoreType.equalsIgnoreCase("memory")) {
      bind(AccountStore.class).to(MemoryStore.class).in(Singleton.class);
    } else if (accountStoreType.equalsIgnoreCase("file")) {
      bind(AccountStore.class).to(FileAccountStore.class).in(Singleton.class);
    } else if (accountStoreType.equalsIgnoreCase("fake")) {
      bind(AccountStore.class).to(FakePermissiveAccountStore.class).in(Singleton.class);
    } else if (accountStoreType.equalsIgnoreCase("mongodb")) {
      MongoDbProvider mongoDbProvider = getMongoDbProvider();
      bind(AccountStore.class).toInstance(mongoDbProvider.provideMongoDbStore());
    } else {
      throw new RuntimeException("Invalid account store type: '" + accountStoreType + "'");
    }
  }

  private void bindDeltaStore() {
    if (deltaStoreType.equalsIgnoreCase("memory")) {
      bind(DeltaStore.class).to(MemoryDeltaStore.class).in(Singleton.class);
    } else if (deltaStoreType.equalsIgnoreCase("file")) {
      bind(DeltaStore.class).to(FileDeltaStore.class).in(Singleton.class);
    } else if (deltaStoreType.equalsIgnoreCase("mongodb")) {
      MongoDbProvider mongoDbProvider = getMongoDbProvider();
      bind(DeltaStore.class).toInstance(mongoDbProvider.provideMongoDbDeltaStore());
    } else {
      throw new RuntimeException("Invalid delta store type: '" + deltaStoreType + "'");
    }
  }
}
