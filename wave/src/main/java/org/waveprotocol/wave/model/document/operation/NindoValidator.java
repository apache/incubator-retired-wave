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

package org.waveprotocol.wave.model.document.operation;

import static org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema.NO_SCHEMA_CONSTRAINTS;

import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.Nindo.NindoCursor;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ValidationResult;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.EmptyDocument;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Map;

/**
 * Validates an operation against a document.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public final class NindoValidator {

  private NindoValidator() {}

  private static IndexedDocument<Node, Element, Text> getEmptyDocument() {
    return DocProviders.POJO.build(EmptyDocument.EMPTY_DOCUMENT, NO_SCHEMA_CONSTRAINTS);
  }

  /**
   * Returns whether m is a well-formed document initialization and satisfies
   * the given schema constraints.
   */
  public static ViolationCollector validate(Nindo m, DocumentSchema s) {
    return validate(getEmptyDocument(), m, s);
  }

  private static final class IllFormed extends RuntimeException {}

  /**
   * Returns whether m is well-formed, applies to doc, and preserves the given
   * schema constraints.  Will not modify doc.
   */
  public static <N, E extends N, T extends N> ViolationCollector validate(
      IndexedDocument<N, E, T> doc,
      Nindo m, DocumentSchema schema) {
    Preconditions.checkNotNull(schema, "Schema constraints required, if not, " +
        "use DocumentSchema.NO_SCHEMA_CONSTRAINTS");
    final NindoAutomaton<N, E, T> a = new NindoAutomaton<N, E, T>(schema, doc);
    final ViolationCollector v = new ViolationCollector();
    try {
      m.apply(new NindoCursor() {

        @Override
        public void begin() {
          // Not checking begin and finish for now since they should go away.
        }

        @Override
        public void characters(String s) {
          if (a.checkCharacters(s, v) == ValidationResult.ILL_FORMED) {
            throw new IllFormed();
          }
          a.doCharacters(s);
        }

        @Override
        public void deleteCharacters(int n) {
          if (a.checkDeleteCharacters(n, v) == ValidationResult.ILL_FORMED) {
            throw new IllFormed();
          }
          a.doDeleteCharacters(n);
        }

        @Override
        public void deleteElementEnd() {
          if (a.checkDeleteElementEnd(v) == ValidationResult.ILL_FORMED) {
            throw new IllFormed();
          }
          a.doDeleteElementEnd();
        }

        @Override
        public void deleteElementStart() {
          if (a.checkDeleteElementStart(v) == ValidationResult.ILL_FORMED) {
            throw new IllFormed();
          }
          a.doDeleteElementStart();
        }

        @Override
        public void elementEnd() {
          if (a.checkElementEnd(v) == ValidationResult.ILL_FORMED) {
            throw new IllFormed();
          }
          a.doElementEnd();
        }

        @Override
        public void elementStart(String tagName, Attributes attributes) {
          if (a.checkElementStart(tagName, attributes, v) == ValidationResult.ILL_FORMED) {
            throw new IllFormed();
          }
          a.doElementStart(tagName, attributes);
        }

        @Override
        public void endAnnotation(String key) {
          if (a.checkEndAnnotation(key, v) == ValidationResult.ILL_FORMED) {
            throw new IllFormed();
          }
          a.doEndAnnotation(key);
        }

        @Override
        public void finish() {
          // Not checking begin and finish for now since they should go away.
        }

        @Override
        public void replaceAttributes(Attributes attributes) {
          if (a.checkSetAttributes(attributes, v) == ValidationResult.ILL_FORMED) {
            throw new IllFormed();
          }
          a.doSetAttributes(attributes);
        }

        @Override
        public void skip(int n) {
          if (a.checkSkip(n, v) == ValidationResult.ILL_FORMED) {
            throw new IllFormed();
          }
          a.doSkip(n);
        }

        @Override
        public void startAnnotation(String key, String value) {
          if (a.checkStartAnnotation(key, value, v) == ValidationResult.ILL_FORMED) {
            throw new IllFormed();
          }
          a.doStartAnnotation(key, value);
        }

        @Override
        public void updateAttributes(Map<String, String> attributes) {
          if (a.checkUpdateAttributes(attributes, v) == ValidationResult.ILL_FORMED) {
            throw new IllFormed();
          }
          a.doUpdateAttributes(attributes);
        }
      });
    } catch (IllFormed e) {
      return v;
    }
    if (a.checkFinish(v) == ValidationResult.ILL_FORMED) {
      return v;
    }
    a.doFinish();
    return v;
  }

}
