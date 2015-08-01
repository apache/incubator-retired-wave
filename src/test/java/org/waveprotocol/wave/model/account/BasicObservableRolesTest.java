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

/**
 * Tests for {@link BasicObservableRoles}.
 *
 */

public class BasicObservableRolesTest extends ObservableRolesTestBase {
  @Override
  protected ObservableRoles getRoles() {
    return new BasicObservableRoles();
  }

  public void testConstructFromExistingRoles() {
    ObservableRoles.Listener listener = mock(ObservableRoles.Listener.class);
    BasicObservableRoles permissions = new BasicObservableRoles();
    permissions.assign(p("public@a.gwave.com"), Role.READ_ONLY);
    permissions.assign(p("tirsen@google.com"), Role.READ_ONLY);

    BasicObservableRoles copy = new BasicObservableRoles(permissions.getAssignments());
    copy.assign(p("whitelaw@example.com"), Role.READ_ONLY);
    assertEquals(copy.getRole(p("tirsen@google.com")),
        permissions.getRole(p("tirsen@google.com")));
    assertEquals(copy.getRole(p("public@a.gwave.com")),
        permissions.getRole(p("public@a.gwave.com")));
    assertEquals(copy.getRole(p("whitelaw@example.com")), Role.READ_ONLY);
    assertEquals(permissions.getRole(p("whitelaw@example.com")), Role.FULL);
  }
}
