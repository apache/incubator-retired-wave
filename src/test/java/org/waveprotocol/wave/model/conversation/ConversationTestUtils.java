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

import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.testing.FakeWaveView;

/**
 * Static utilities for conversation model internal tests.
 *
 * @author anorth@google.com (Alex North)
 */
final class ConversationTestUtils {

  private static final SchemaProvider SCHEMA_PROVIDER = new ConversationSchemas();
  private static final IdGenerator ID_GENERATOR = FakeIdGenerator.create();

  static FakeWaveView createWaveView() {
    return createWaveView(ID_GENERATOR);
  }

  static FakeWaveView createWaveView(IdGenerator idGenerator) {
    return FakeWaveView.builder(SCHEMA_PROVIDER).with(idGenerator).build();
  }

  static ObservableDocument createBlipDocument() {
    return BasicFactories.createDocument(ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS);
  }

  static ObservableDocument createManifestDocument() {
    return BasicFactories.createDocument(ConversationSchemas.MANIFEST_SCHEMA_CONSTRAINTS);
  }
}
