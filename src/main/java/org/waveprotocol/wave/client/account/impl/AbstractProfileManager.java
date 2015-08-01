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

package org.waveprotocol.wave.client.account.impl;

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileListener;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Serves as a base for concrete {@link ProfileManager} implementations.
 * 
 * @author yurize@apache.org (Yuri Zelikov)
 */
public abstract class AbstractProfileManager<P extends Profile> implements ProfileManager {
  
  protected final StringMap<ProfileImpl> profiles = CollectionUtils.createStringMap();
  protected final CopyOnWriteSet<ProfileListener> listeners = CopyOnWriteSet.create();
  
  @Override
  public boolean shouldIgnore(ParticipantId participant) {
    return false;
  }

  @Override
  public void addListener(ProfileListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(ProfileListener listener) {
    listeners.remove(listener);
  }
  
  protected void fireOnUpdated(P profile) {
    for (ProfileListener listener : listeners) {
      listener.onProfileUpdated(profile);
    }
  }
}
