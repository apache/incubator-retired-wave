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

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A role implies a set of capabilities defined by {@link Capability}.
 */
public enum Role {

  /**
   * Full member.
   */
  FULL(EnumSet.allOf(Capability.class)),

  /**
   * Can only view the wave.
   */
  READ_ONLY(Capability.READ, Capability.INDEX);
  
  private Set<Capability> capabilities;

  Role(Capability first, Capability...rest) {
    this(EnumSet.of(first, rest));
  }

  Role(Set<Capability> capabilities) {
    this.capabilities = Collections.unmodifiableSet(capabilities);
  }

  public boolean isPermitted(Capability capability) {
    return capabilities.contains(capability);
  }
}
