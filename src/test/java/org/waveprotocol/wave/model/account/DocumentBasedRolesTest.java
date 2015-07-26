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

import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.testing.BasicFactories;


/**
 * Tests for {@link DocumentBasedRoles}.
 *
 */

public class DocumentBasedRolesTest extends ObservableRolesTestBase {

  public void testOnlyPersistsChangedRole() {
    ObservableDocument doc = BasicFactories.observableDocumentProvider().parse("");
    DocumentBasedRoles permissions = DocumentBasedRoles.create(doc);
    permissions.assign(p("public@a.gwave.com"), Role.READ_ONLY);
    assertEquals("<assign address=\"public@a.gwave.com\" role=\"READ_ONLY\"/>", doc.toXmlString());
  }

  public void testPersistNothingWhenSettingDefaultRole() {
    ObservableDocument doc = BasicFactories.observableDocumentProvider().parse("");
    DocumentBasedRoles permissions = DocumentBasedRoles.create(doc);
    permissions.assign(p("joe@example.com"), Role.FULL);
    assertEquals("", doc.toXmlString());
  }

  @Override
  protected ObservableRoles getRoles() {
    ObservableDocument doc = BasicFactories.observableDocumentProvider().parse("");
    DocumentBasedRoles permissions = DocumentBasedRoles.create(doc);
    return permissions;
  }
}
