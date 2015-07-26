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

import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;

/**
 * A wrapper for Indexability that provides default values for absent
 * assignments. Setting an indexability to the default value clears the
 * assignment.
 *
 */
public class DefaultingIndexability implements MutableIndexability {
  private final MutableIndexability target;
  private final IndexDecision defaultDecision;

  public DefaultingIndexability(
      final MutableIndexability indexability, IndexDecision defaultDecision) {
    Preconditions.checkNotNull(indexability, "indexability can't be null");
    this.target = indexability;
    this.defaultDecision = defaultDecision;
  }

  @Override
  public Set<ParticipantId> getIndexDecisions() {
    return target.getIndexDecisions();
  }

  @Override
  public IndexDecision getIndexability(ParticipantId participant) {
    IndexDecision result = target.getIndexability(participant);
    if (result == null) {
      return defaultDecision;
    }
    return result;
  }

  @Override
  public void setIndexability(ParticipantId participant, IndexDecision indexability) {
    if (indexability == defaultDecision) {
      target.setIndexability(participant, null);
    } else {
      target.setIndexability(participant, indexability);
    }
  }

}
