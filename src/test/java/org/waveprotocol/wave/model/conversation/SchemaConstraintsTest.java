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

package org.waveprotocol.wave.model.conversation;

import static org.waveprotocol.wave.model.schema.conversation.ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema.PermittedCharacters;

import java.util.Collections;

/**
 * Mainly a test for the AbstractXmlSchemaConstraints code
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class SchemaConstraintsTest extends TestCase {

  /** Tests the abstract helper class */
  public void testAbstractHelperClass() {
    assertFalse(
        BLIP_SCHEMA_CONSTRAINTS.permitsAttribute("body", "x"));
    assertEquals(Collections.singletonList("line"),
        BLIP_SCHEMA_CONSTRAINTS.getRequiredInitialChildren("body"));
    assertEquals(Collections.singletonList("line"),
        BLIP_SCHEMA_CONSTRAINTS.getRequiredInitialChildren("textarea"));
    assertEquals(Collections.emptyList(),
        BLIP_SCHEMA_CONSTRAINTS.getRequiredInitialChildren("line"));
    assertTrue(
        BLIP_SCHEMA_CONSTRAINTS.permitsAttribute("reply", "id", "..."));
    assertTrue(
        BLIP_SCHEMA_CONSTRAINTS.permitsAttribute("reply", "id", "b+sdf"));
    assertTrue(
        BLIP_SCHEMA_CONSTRAINTS.permitsAttribute("line", "t", "h3"));
    assertFalse(
        BLIP_SCHEMA_CONSTRAINTS.permitsAttribute("line", "t", "z"));
    assertTrue(
        BLIP_SCHEMA_CONSTRAINTS.permitsAttribute("image", "attachment"));
    assertTrue(
        BLIP_SCHEMA_CONSTRAINTS.permitsAttribute("image", "attachment", "blahblah"));
    assertFalse(
        BLIP_SCHEMA_CONSTRAINTS.permitsAttribute("image", "something"));
    assertFalse(
        BLIP_SCHEMA_CONSTRAINTS.permitsAttribute("image", "something", "stuff"));
    assertFalse(
        BLIP_SCHEMA_CONSTRAINTS.permitsAttribute("caption", "something"));
    assertFalse(
        BLIP_SCHEMA_CONSTRAINTS.permitsAttribute("caption", "something", "stuff"));
    assertTrue(
        BLIP_SCHEMA_CONSTRAINTS.permitsChild("body", "line"));
    assertTrue(
        BLIP_SCHEMA_CONSTRAINTS.permitsChild("textarea", "line"));
    assertTrue(
        BLIP_SCHEMA_CONSTRAINTS.permitsChild(null, "body"));
    assertFalse(
        BLIP_SCHEMA_CONSTRAINTS.permitsChild(null, "blah"));
    assertEquals(PermittedCharacters.BLIP_TEXT,
        BLIP_SCHEMA_CONSTRAINTS.permittedCharacters("body"));
    assertEquals(PermittedCharacters.NONE,
        BLIP_SCHEMA_CONSTRAINTS.permittedCharacters("line"));
    assertEquals(PermittedCharacters.NONE,
        BLIP_SCHEMA_CONSTRAINTS.permittedCharacters(null));
  }
}
