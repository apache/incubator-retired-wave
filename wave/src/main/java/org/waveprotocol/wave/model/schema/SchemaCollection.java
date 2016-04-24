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

package org.waveprotocol.wave.model.schema;

import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collection of schema providers.  When asked for a schema the collection will
 * ask all providers to give a schema and if one returns a schema, that will
 * be the result.
 *
 */
public class SchemaCollection implements SchemaProvider {

  /**
   * Returns a new empty, unmodifiable schema collection.
   */
  public static SchemaCollection empty() {
    List<SchemaProvider> emptyList = Collections.emptyList();
    return new SchemaCollection(Collections.unmodifiableList(emptyList));
  }

  private final List<SchemaProvider> providers;

  public SchemaCollection() {
    this(new ArrayList<SchemaProvider>());
  }

  private SchemaCollection(List<SchemaProvider> providers) {
    this.providers = providers;
  }

  public void add(SchemaProvider provider) {
    if (!providers.contains(provider))
      providers.add(provider);
  }

  /**
   * Asks all providers to give a schema and if one returns a schema, that will
   * be the result.  If none return a value null will be returned; if more than
   * one does an {@link IllegalStateException} will be thrown.
   */
  @Override
  public DocumentSchema getSchemaForId(WaveletId waveletId, String documentId) {
    DocumentSchema result = null;
    for (SchemaProvider provider : providers) {
      // TODO(user): Change the way providers (or individual schemas) are
      //   registered to catch ambiguities earlier.
      DocumentSchema value = provider.getSchemaForId(waveletId, documentId);
      assert value != null;
      if (value != DocumentSchema.NO_SCHEMA_CONSTRAINTS) {
        // Check that only one schema matches.  This ensures that the ordering of
        // resolvers doesn't influence which schema is used which could lead to
        // some pretty horrible bugs.
        Preconditions.checkState(result == null, "Several different schemas apply to document");
        result = value;
      }
    }
    return (result == null) ? DocumentSchema.NO_SCHEMA_CONSTRAINTS : result;
  }

}
