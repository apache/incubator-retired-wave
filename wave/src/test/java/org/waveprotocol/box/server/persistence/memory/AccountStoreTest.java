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

package org.waveprotocol.box.server.persistence.memory;

import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.AccountStoreTestBase;

/**
 * Testcases for the {@link MemoryStore} implementation of the
 * {@link AccountStore}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class AccountStoreTest extends AccountStoreTestBase {

  @Override
  protected AccountStore newAccountStore() {
    return new MemoryStore();
  }
}
