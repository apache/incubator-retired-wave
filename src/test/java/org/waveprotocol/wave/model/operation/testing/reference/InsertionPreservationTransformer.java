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
import org.waveprotocol.wave.model.document.operation.algorithm.RangeNormalizer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.testing.reference.PositionTracker.RelativePosition;

/**
 * A utility class for transforming an insertion operation with a
 * structure-preserving operation.
 *
 * @author Alexandre Mah
 */
final class InsertionPreservationTransformer {

  /**
   * A cache for the effect of a component of a document mutation that affects a
   * range of the document.
   */
  private static abstract class RangeCache {

    abstract void resolve(int retain);

  }

  /**
   * A target of a document mutation which can be used to transform document
   * mutations by making use primarily of information from one mutation with the
   * help of auxiliary information from a second mutation. These targets should
   * be used in pairs.
   */
  private static abstract class Target implements EvaluatingDocOpCursor<DocOp> {

    /**
     * The target to which to write the transformed mutation.
     */
    final EvaluatingDocOpCursor<DocOp> targetDocument;

    /**
     * The position of the processing cursor associated with this target
     * relative to the position of the processing cursor associated to the
     * opposing target. All positional calculations are based on cursor
     * positions in the original document on which the two original operations
     * apply.
     */
    final RelativePosition relativePosition;

    Target(EvaluatingDocOpCursor<DocOp> targetDocument, RelativePosition relativePosition) {
      this.targetDocument = targetDocument;
      this.relativePosition = relativePosition;
    }

    @Override
    public DocOp finish() {
      return targetDocument.finish();
    }

  }

  private static final class InsertionTarget extends Target {

    NoninsertionTarget otherTarget;

    InsertionTarget(EvaluatingDocOpCursor<DocOp> targetDocument,
        RelativePosition relativePosition) {
      super(targetDocument, relativePosition);
    }

    public void setOtherTarget(NoninsertionTarget otherTarget) {
      this.otherTarget = otherTarget;
    }

    @Override
    public void retain(int itemCount) {
      int oldPosition = relativePosition.get();
      relativePosition.increase(itemCount);
      if (relativePosition.get() < 0) {
        otherTarget.rangeCache.resolve(itemCount);
      } else if (oldPosition < 0) {
        otherTarget.rangeCache.resolve(-oldPosition);
      }
    }

    @Override
    public void characters(String chars) {
      targetDocument.characters(chars);
      otherTarget.targetDocument.retain(chars.length());
    }

    @Override
    public void elementStart(String tag, Attributes attrs) {
      targetDocument.elementStart(tag, attrs);
      otherTarget.targetDocument.retain(1);
    }

    @Override
    public void elementEnd() {
      targetDocument.elementEnd();
      otherTarget.targetDocument.retain(1);
    }

    @Override
    public void deleteCharacters(String chars) {
      throw new UnsupportedOperationException("This method should never be called.");
    }

    @Override
    public void deleteElementStart(String tag, Attributes attrs) {
      throw new UnsupportedOperationException("This method should never be called.");
    }

    @Override
    public void deleteElementEnd() {
      throw new UnsupportedOperationException("This method should never be called.");
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      throw new UnsupportedOperationException("This method should never be called.");
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      throw new UnsupportedOperationException("This method should never be called.");
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
      throw new UnsupportedOperationException("This method should never be called.");
    }

  }

  private static final class NoninsertionTarget extends Target {

    private final class ReplaceAttributesCache extends RangeCache {

      private final Attributes oldAttributes;
      private final Attributes newAttributes;

      ReplaceAttributesCache(Attributes oldAttributes, Attributes newAttributes) {
        this.oldAttributes = oldAttributes;
        this.newAttributes = newAttributes;
      }

      @Override
      void resolve(int itemCount) {
        targetDocument.replaceAttributes(oldAttributes, newAttributes);
        otherTarget.targetDocument.retain(1);
      }

    }

    private final class UpdateAttributesCache extends RangeCache {

      private final AttributesUpdate update;

      UpdateAttributesCache(AttributesUpdate update) {
        this.update = update;
      }

      @Override
      void resolve(int itemCount) {
        targetDocument.updateAttributes(update);
        otherTarget.targetDocument.retain(1);
      }

    }

    private final RangeCache retainCache = new RangeCache() {

      @Override
      void resolve(int itemCount) {
        targetDocument.retain(itemCount);
        otherTarget.targetDocument.retain(itemCount);
      }

    };

    /**
     * A cache for the effect of mutation components which affect ranges.
     */
    private RangeCache rangeCache = retainCache;

    private InsertionTarget otherTarget;

    NoninsertionTarget(EvaluatingDocOpCursor<DocOp> targetDocument,
        RelativePosition relativePosition) {
      super(targetDocument, relativePosition);
    }

    public void setOtherTarget(InsertionTarget otherTarget) {
      this.otherTarget = otherTarget;
    }

    @Override
    public void retain(int itemCount) {
      resolveRange(itemCount, retainCache);
      rangeCache = retainCache;
    }

    @Override
    public void characters(String chars) {
      throw new UnsupportedOperationException("This method should never be called.");
    }

    @Override
    public void elementStart(String tag, Attributes attrs) {
      throw new UnsupportedOperationException("This method should never be called.");
    }

    @Override
    public void elementEnd() {
      throw new UnsupportedOperationException("This method should never be called.");
    }

    @Override
    public void deleteCharacters(String chars) {
      throw new UnsupportedOperationException("This method should never be called.");
    }

    @Override
    public void deleteElementStart(String tag, Attributes attrs) {
      throw new UnsupportedOperationException("This method should never be called.");
    }

    @Override
    public void deleteElementEnd() {
      throw new UnsupportedOperationException("This method should never be called.");
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      RangeCache cache = new ReplaceAttributesCache(oldAttrs, newAttrs);
      if (resolveRange(1, cache) == 0) {
        rangeCache = cache;
      }
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      RangeCache cache = new UpdateAttributesCache(attrUpdate);
      if (resolveRange(1, cache) == 0) {
        rangeCache = new UpdateAttributesCache(attrUpdate);
      }
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
      targetDocument.annotationBoundary(map);
    }

    /**
     * Resolves the transformation of a range.
     *
     * @param size the requested size to resolve
     * @param cache the cache to use
     * @return the portion of the requested size that was resolved, or -1 to
     *         indicate that the entire range was resolved
     */
    private int resolveRange(int size, RangeCache cache) {
      int oldPosition = relativePosition.get();
      relativePosition.increase(size);
      if (relativePosition.get() > 0) {
        if (oldPosition < 0) {
          cache.resolve(-oldPosition);
        }
        return -oldPosition;
      } else {
        cache.resolve(size);
        return -1;
      }
    }

  }

  /**
   * Transforms an insertion operation with a structure-preserving operation.
   *
   * @param insertionOp the insertion operation
   * @param preservationOp the structure-preserving operation
   * @return the transformed pair of operations
   * @throws TransformException if a problem was encountered during the
   *         transformation process
   */
  OperationPair<DocOp> transformOperations(DocOp insertionOp,
      DocOp preservationOp) throws TransformException {
    PositionTracker positionTracker = new PositionTracker();

    RelativePosition insertionPosition = positionTracker.getPosition1();
    RelativePosition preservationPosition = positionTracker.getPosition2();

    // The target responsible for processing components of the insertion operation.
    InsertionTarget insertionTarget = new InsertionTarget(
        new RangeNormalizer<DocOp>(new DocOpBuffer()), insertionPosition);

    // The target responsible for processing components of the preservation operation.
    NoninsertionTarget preservationTarget =
        new NoninsertionTarget(
            OperationNormalizer.createNormalizer(new DocOpBuffer()), preservationPosition);

    insertionTarget.setOtherTarget(preservationTarget);
    preservationTarget.setOtherTarget(insertionTarget);

    // Incrementally apply the two operations in a linearly-ordered interleaving
    // fashion.
    int insertionIndex = 0;
    int preservationIndex = 0;
    while (insertionIndex < insertionOp.size()) {
      insertionOp.applyComponent(insertionIndex++, insertionTarget);
      while (insertionPosition.get() > 0) {
        if (preservationIndex >= preservationOp.size()) {
          throw new TransformException("Ran out of " + preservationOp.size()
              + " noninsertion op components after " + insertionIndex + " of " + insertionOp.size()
              + " insertion op components, with " + insertionPosition.get() + " spare positions");
        }
        preservationOp.applyComponent(preservationIndex++, preservationTarget);
      }
    }
    while (preservationIndex < preservationOp.size()) {
      preservationOp.applyComponent(preservationIndex++, preservationTarget);
    }
    insertionOp = insertionTarget.finish();
    preservationOp = preservationTarget.finish();
    return new OperationPair<DocOp>(insertionOp, preservationOp);
  }

}
