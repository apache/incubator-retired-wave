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

package org.waveprotocol.box.server.persistence.mongodb;

import com.mongodb.DB;

import org.waveprotocol.box.server.persistence.CertPathStoreTestBase;
import org.waveprotocol.wave.crypto.CertPathStore;

/**
 * Testcase for the {@link CertPathStore} implementation in
 * {@link MongoDbStore}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 *
 */
public class CertPathStoreTest extends CertPathStoreTestBase {

  private static final String TEST_DATABASE = "CertPathTest";

  private final MongoDbStore certPathStore;
  private final DB database;

  /**
   * Initializes the MongoDB version of a {@link CertPathStoreTestBase}.
   */
  public CertPathStoreTest() throws Exception {
    MongoDbProvider mongoDbProvider = new MongoDbProvider("127.0.0.1", "27017", "wiab_test");
    this.database = mongoDbProvider.getDatabaseForName(TEST_DATABASE);
    certPathStore = new MongoDbStore(database);
  }

  @Override
  protected CertPathStore newCertPathStore() {
    database.dropDatabase();
    return certPathStore;
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    database.dropDatabase();
  }
}
