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

package org.waveprotocol.wave.model.document.operation.impl;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.automaton.AutomatonDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ValidationResult;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Validates an operation against a document.
 *
 *
 */
public final class DocOpValidator {

  private DocOpValidator() {}

  /**
   * Returns whether op is a well-formed document initialization and satisfies
   * the given schema constraints.
   */
  public static ValidationResult validate(ViolationCollector v,
      DocumentSchema schema, DocInitialization op) {
    Preconditions.checkNotNull(schema, "Schema constraints required, if not, " +
        "use DocumentSchema.NO_SCHEMA_CONSTRAINTS");
    return validate(v, schema, DocOpAutomaton.EMPTY_DOCUMENT, op);
  }

  /**
   * Returns whether op is well-formed.
   *
   * Any violations recorded in the output v that are not well-formedness
   * violations are meaningless.
   */
  public static boolean isWellFormed(ViolationCollector v, DocOp op) {
    if (op instanceof BufferedDocOpImpl) {
      return isWellFormed(v, (BufferedDocOpImpl) op);
    } else {
      return isWellFormedRaw(v, op);
    }
  }

  /**
   * Same as {@link #isWellFormed(ViolationCollector, DocOp)}, but with
   * a fast path for already-validated instances of BufferedDocOpImpl
   */
  public static boolean isWellFormed(ViolationCollector v, BufferedDocOpImpl buffered) {
    if (buffered.isKnownToBeWellFormed()) {
      // fast path
      return true;
    } else {
      if (isWellFormedRaw(v, buffered)) {
        buffered.markWellFormed();
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * Same as {@link #isWellFormed(ViolationCollector, DocOp)}, but without
   * the fast path for BufferedDocOpImpl
   */
  public static boolean isWellFormedRaw(ViolationCollector v, DocOp op) {
    // We validate the operation against the empty document.  It will likely
    // be invalid; however, we ignore the validity aspect anyway since we
    // only care about well-formedness.
    return !validate(v, DocumentSchema.NO_SCHEMA_CONSTRAINTS,
        DocOpAutomaton.EMPTY_DOCUMENT, op)
        .isIllFormed();
  }

  private static final class IllFormed extends RuntimeException {
    IllFormed(String message) {
      super(message);
    }
    @Override
    public Throwable fillInStackTrace() {
      return this;  // don't fill in stack trace, for efficiency
    }
  }

  private static final IllFormed ILL_FORMED = new IllFormed(
      "Preallocated exception with a meaningless stack trace");

  /**
   * Returns whether op is well-formed, applies to doc, and preserves the given
   * schema constraints.  Will not modify doc.
   */
  public static <N, E extends N, T extends N> ValidationResult validate(
      final ViolationCollector v, DocumentSchema schema,
      AutomatonDocument doc, DocOp op) {

    if (schema == null) {
      schema = DocumentSchema.NO_SCHEMA_CONSTRAINTS;
    }
    final DocOpAutomaton a = new DocOpAutomaton(doc, schema);
    final ValidationResult[] accu = new ValidationResult[] { ValidationResult.VALID };
    try {
      op.apply(new DocOpCursor() {
        void abortIfIllFormed() {
          if (accu[0].isIllFormed()) {
            throw ILL_FORMED;
          }
        }

        @Override
        public void characters(String s) {
          accu[0] = accu[0].mergeWith(a.checkCharacters(s, v));
          abortIfIllFormed();
          a.doCharacters(s);
        }

        @Override
        public void deleteCharacters(String chars) {
          accu[0] = accu[0].mergeWith(a.checkDeleteCharacters(chars, v));
          abortIfIllFormed();
          a.doDeleteCharacters(chars);
        }

        @Override
        public void deleteElementEnd() {
          accu[0] = accu[0].mergeWith(a.checkDeleteElementEnd(v));
          abortIfIllFormed();
          a.doDeleteElementEnd();
        }

        @Override
        public void deleteElementStart(String type, Attributes attrs) {
          accu[0] = accu[0].mergeWith(a.checkDeleteElementStart(type, attrs, v));
          abortIfIllFormed();
          a.doDeleteElementStart(type, attrs);
        }

        @Override
        public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
          accu[0] = accu[0].mergeWith(a.checkReplaceAttributes(oldAttrs, newAttrs, v));
          abortIfIllFormed();
          a.doReplaceAttributes(oldAttrs, newAttrs);
        }

        @Override
        public void retain(int itemCount) {
          accu[0] = accu[0].mergeWith(a.checkRetain(itemCount, v));
          abortIfIllFormed();
          a.doRetain(itemCount);
        }

        @Override
        public void updateAttributes(AttributesUpdate u) {
          accu[0] = accu[0].mergeWith(a.checkUpdateAttributes(u, v));
          abortIfIllFormed();
          a.doUpdateAttributes(u);
        }

        @Override
        public void annotationBoundary(AnnotationBoundaryMap map) {
          accu[0] = accu[0].mergeWith(a.checkAnnotationBoundary(map, v));
          abortIfIllFormed();
          a.doAnnotationBoundary(map);
        }

        @Override
        public void elementEnd() {
          accu[0] = accu[0].mergeWith(a.checkElementEnd(v));
          abortIfIllFormed();
          a.doElementEnd();
        }

        @Override
        public void elementStart(String type, Attributes attrs) {
          accu[0] = accu[0].mergeWith(a.checkElementStart(type, attrs, v));
          abortIfIllFormed();
          a.doElementStart(type, attrs);
        }
      });
    } catch (IllFormed e) {
      return ValidationResult.ILL_FORMED;
    }
    accu[0] = accu[0].mergeWith(a.checkFinish(v));
    return accu[0];
  }

}
