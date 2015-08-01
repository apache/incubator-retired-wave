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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.wave.ParticipantId;


/**
 * Tests for {@link BasicObservableRoles}.
 *
 */
public abstract class ObservableRolesTestBase extends TestCase {

  private ObservableRoles permissions;

  @Override
  public void setUp() {
    permissions = getRoles();
  }

  protected abstract ObservableRoles getRoles();

  public void testParticipantsWithoutRoleCanWrite() {
    permissions.assign(p("public@a.gwave.com"), Role.READ_ONLY);

    assertFalse(permissions.isPermitted(p("public@a.gwave.com"), Capability.WRITE));
    assertTrue(permissions.isPermitted(p("jon@example.com"), Capability.WRITE));
  }

  public void testReadOnlyParticipantCannotWrite() {
    permissions.assign(p("public@a.gwave.com"), Role.READ_ONLY);

    permissions.assign(p("jon@example.com"), Role.READ_ONLY);
    assertFalse(permissions.isPermitted(p("jon@example.com"), Capability.WRITE));
    assertTrue(permissions.isPermitted(p("jvn@google.com"), Capability.WRITE));
  }


  public void testAssigningTriggersListeners() {
    ObservableRoles.Listener listener = mock(ObservableRoles.Listener.class);
    permissions.assign(p("public@a.gwave.com"), Role.READ_ONLY);
    permissions.addListener(listener);

    // Modify the existing assignment.
    permissions.assign(p("public@a.gwave.com"), Role.FULL);
    verify(listener).onChanged();
    reset(listener);

    // Add an assignment.
    permissions.assign(p("jon@example.com"), Role.READ_ONLY);
    verify(listener).onChanged();
    reset(listener);

    // Change it.
    permissions.assign(p("jon@example.com"), Role.FULL);
    verify(listener).onChanged();
    reset(listener);

    // Re-set to the same value (listener will not be triggered);
    permissions.assign(p("jon@example.com"), Role.FULL);
    verify(listener, never()).onChanged();
    reset(listener);
  }

  protected ParticipantId p(String address) {
    return new ParticipantId(address);
  }
}
