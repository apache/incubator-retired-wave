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

package org.waveprotocol.wave.model.account;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Plain Java implementation of Indexability.
 *
 */
public class BasicObservableIndexability implements ObservableMutableIndexability {
  private final Map<ParticipantId, IndexDecision> map = CollectionUtils.newHashMap();
  private final CopyOnWriteSet<ObservableIndexability.Listener> listeners = CopyOnWriteSet.create();

  @Override
  public void setIndexability(ParticipantId participant, IndexDecision indexability) {
    Preconditions.checkNotNull(participant, "Participant can't be null");
    IndexDecision current = getIndexability(participant);
    if (indexability == null) {
      map.remove(participant);
    } else {
      map.put(participant, indexability);
    }
    if (current != indexability) {
      for (ObservableIndexability.Listener l : listeners) {
        l.onChanged(participant, indexability);
      }
    }
  }

  @Override
  public IndexDecision getIndexability(ParticipantId participant) {
    return map.get(participant);
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  @Override
  public Set<ParticipantId> getIndexDecisions() {
    return Collections.unmodifiableSet(map.keySet());
  }

}
