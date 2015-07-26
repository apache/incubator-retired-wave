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

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Interface for the storage and retrieval of {@link AccountData}s.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public interface AccountStore {
  /**
   * Initialize the account store.
   * Implementations are expected to validate any configuration values, validate the state of the
   * store, and perform an start-up action needed (e.g. load list of accounts into memory,
   * establish connection to database, etc...).
   * 
   * @throws PersistenceException
   */
  void initializeAccountStore() throws PersistenceException;

  /**
   * Returns an {@link AccountData} for the given username or null if not
   * exists.
   *
   * @param id participant id of the requested account.
   */
  AccountData getAccount(ParticipantId id) throws PersistenceException;

  /**
   * Puts the given {@link AccountData} in the storage, overrides an existing
   * account if the username is already in use.
   *
   * @param account to store.
   */
  void putAccount(AccountData account) throws PersistenceException;

  /**
   * Removes an account from storage.
   *
   * @param id the participant id of the account to remove.
   */
  void removeAccount(ParticipantId id) throws PersistenceException;
}
