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

package org.waveprotocol.wave.model.document.operation.algorithm;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.util.Pair;

/**
 * A utility class for decomposing an operation into an insertion operation and
 * an operation that contains no insertions.
 *
 * @author Alexandre Mah
 */
final class Decomposer {

  private static final class Target
      implements EvaluatingDocOpCursor<Pair<DocOp, DocOp>> {

    private final EvaluatingDocOpCursor<DocOp> insertionOp =
        OperationNormalizer.createNormalizer(new DocOpBuffer());
    private final EvaluatingDocOpCursor<DocOp> noninsertionOp =
        OperationNormalizer.createNormalizer(new DocOpBuffer());

    @Override
    public Pair<DocOp, DocOp> finish() {
      return new Pair<DocOp, DocOp>(insertionOp.finish(), noninsertionOp.finish());
    }

    @Override
    public void retain(int itemCount) {
      insertionOp.retain(itemCount);
      noninsertionOp.retain(itemCount);
    }

    @Override
    public void characters(String chars) {
      insertionOp.characters(chars);
      noninsertionOp.retain(chars.length());
    }

    @Override
    public void elementStart(String type, Attributes attrs) {
      insertionOp.elementStart(type, attrs);
      noninsertionOp.retain(1);
    }

    @Override
    public void elementEnd() {
      insertionOp.elementEnd();
      noninsertionOp.retain(1);
    }

    @Override
    public void deleteCharacters(String chars) {
      insertionOp.retain(chars.length());
      noninsertionOp.deleteCharacters(chars);
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
      insertionOp.retain(1);
      noninsertionOp.deleteElementStart(type, attrs);
    }

    @Override
    public void deleteElementEnd() {
      insertionOp.retain(1);
      noninsertionOp.deleteElementEnd();
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      insertionOp.retain(1);
      noninsertionOp.replaceAttributes(oldAttrs, newAttrs);
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      insertionOp.retain(1);
      noninsertionOp.updateAttributes(attrUpdate);
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
      noninsertionOp.annotationBoundary(map);
    }

  }

  /**
   * Returns the decomposition of an operation into an insertion operation and
   * an operation that contains no insertions.
   *
   * @param op the operation to decompose
   * @return a pair of operations, where the first operation in an insertion
   *         operation and the second contains no insertions.
   */
  static Pair<DocOp, DocOp> decompose(DocOp op) {
    Target target = new Target();
    op.apply(target);
    return target.finish();
  }

}
