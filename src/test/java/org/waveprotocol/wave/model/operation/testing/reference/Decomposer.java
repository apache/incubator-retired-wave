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

package org.waveprotocol.wave.model.operation.testing.reference;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.algorithm.OperationNormalizer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;

/**
 * A utility class for decomposing operations into an insertion operation, a
 * structure-preserving operation, and a deletion operation.
 *
 * @author Alexandre Mah
 */
final class Decomposer {

  /**
   * A decomposition consisting of an insertion operation, a
   * structure-preserving operation, and a deletion operation.
   */
  static final class Decomposition {

    final DocOp insertion;
    final DocOp preservation;
    final DocOp deletion;

    Decomposition(DocOp insertion, DocOp preservation, DocOp deletion) {
      this.insertion = insertion;
      this.preservation = preservation;
      this.deletion = deletion;
    }

  }

  private static final class Target
      implements EvaluatingDocOpCursor<Decomposition> {

    private final EvaluatingDocOpCursor<DocOp> insertionOp =
        OperationNormalizer.createNormalizer(new DocOpBuffer());
    private final EvaluatingDocOpCursor<DocOp> preservationOp =
        OperationNormalizer.createNormalizer(new DocOpBuffer());
    private final EvaluatingDocOpCursor<DocOp> deletionOp =
        OperationNormalizer.createNormalizer(new DocOpBuffer());

    @Override
    public Decomposition finish() {
      return new Decomposition(insertionOp.finish(), preservationOp.finish(), deletionOp.finish());
    }

    @Override
    public void retain(int itemCount) {
      insertionOp.retain(itemCount);
      preservationOp.retain(itemCount);
      deletionOp.retain(itemCount);
    }

    @Override
    public void characters(String chars) {
      insertionOp.characters(chars);
      preservationOp.retain(chars.length());
      deletionOp.retain(chars.length());
    }

    @Override
    public void elementStart(String type, Attributes attrs) {
      insertionOp.elementStart(type, attrs);
      preservationOp.retain(1);
      deletionOp.retain(1);
    }

    @Override
    public void elementEnd() {
      insertionOp.elementEnd();
      preservationOp.retain(1);
      deletionOp.retain(1);
    }

    @Override
    public void deleteCharacters(String chars) {
      insertionOp.retain(chars.length());
      preservationOp.retain(chars.length());
      deletionOp.deleteCharacters(chars);
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      insertionOp.retain(1);
      preservationOp.retain(1);
      deletionOp.deleteElementStart(type, attrs);
    }

    @Override
    public void deleteElementEnd() {
      insertionOp.retain(1);
      preservationOp.retain(1);
      deletionOp.deleteElementEnd();
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      insertionOp.retain(1);
      preservationOp.replaceAttributes(oldAttrs, newAttrs);
      deletionOp.retain(1);
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      insertionOp.retain(1);
      preservationOp.updateAttributes(attrUpdate);
      deletionOp.retain(1);
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
      preservationOp.annotationBoundary(map);
    }

  }

  /**
   * Decomposes an operation into an insertion operation, a structure-preserving
   * operation, and a deletion operation.
   *
   * @param op the operation to decompose
   * @return the decomposition of the given operation
   */
 static Decomposition decompose(DocOp op) {
    Target target = new Target();
    op.apply(target);
    return target.finish();
  }

}
