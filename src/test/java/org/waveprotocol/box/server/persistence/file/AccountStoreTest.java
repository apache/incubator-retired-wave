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

import com.typesafe.config.ConfigFactory;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.AccountStoreTestBase;

import java.io.File;

/**
 * Unittest for the {@link AccountStore} implementation in {@link FileAccountStore}.
 *
 * @author tad.glines@gmail.com (Tad Glines)
 */
public class AccountStoreTest extends AccountStoreTestBase {
  private File path;

  @Override
  protected void setUp() throws Exception {
    path = FileUtils.createTemporaryDirectory();
    super.setUp();
  }

  @Override
  protected AccountStore newAccountStore() {
    return new FileAccountStore(
      ConfigFactory.parseString("core.account_store_directory : " + path.getAbsolutePath()));
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();

    org.apache.commons.io.FileUtils.deleteDirectory(path);
  }
}
