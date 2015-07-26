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
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.operation.TransformException;

import java.util.HashSet;
import java.util.Set;

/**
 * A utility class for checking the tameness of a structure-preserving operation
 * with respect to a deletion operation.
 *
 * A structure-preserving operation is considered to be tamed by a deletion
 * operation if it contains no attribute modifications and any regions with
 * annotations changes are deleted by the deletion operation.
 *
 * Note that the deletion operation may apply to either the document to which
 * the structure-preserving operation applies, or the document which results
 * from applying the structure-preserving operation.
 *
 * This class is used by {@link ReferenceTransformer} to aid in determining
 * whether a pair of structure-preserving operations is tamed by a pair of
 * deletion operations.
 *
 * If the composition of p1 and d1 is being transformed against the composition
 * of p2 and d2, where p1 and p2 are structure-preserving operations, and d1 and
 * d2 are deletion operations, then if each of p1 and p2 are tamed by each of d1
 * and d2, the result of the transform will be equivalent to the transform of d1
 * and d2.
 *
 * @author Alexandre Mah
 */
final class AnnotationTamenessChecker {

  /**
   * The relative position of one cursor relative to a second cursor.
   */
  private interface RelativePosition {

    /**
     * Increase the relative position of the cursor.
     *
     * @param amount The amount by which to increase the relative position.
     */
    void increase(int amount);

    /**
     * @return The relative position.
     */
    int get();

  }

  /**
   * A tracker that tracks the positions of two cursors relative to each other.
   */
  private static final class PositionTracker {

    int position = 0;

    /**
     * @return A RelativePosition representing the position in the preservation operation relative
     *         to the position in the deletion operation.
     */
    RelativePosition getPreservationPosition() {
      return new RelativePosition() {

        public void increase(int amount) {
          position += amount;
        }

        public int get() {
          return position;
        }

      };
    }

    /**
     * @return A RelativePosition representing the position in the deletion operation relative to
     *         the position in the preservation operation.
     */
    RelativePosition getDeletionPosition() {
      return new RelativePosition() {

        public void increase(int amount) {
          position -= amount;
        }

        public int get() {
          return -position;
        }

      };
    }

  }

  private abstract class Target implements DocOpCursor {

    /**
     * The position of the processing cursor associated with this target
     * relative to the position of the processing cursor associated to the
     * opposing target. All positional calculations are based on cursor
     * positions in the original document on which the two original operations
     * apply.
     */
    final RelativePosition relativePosition;

    Target(RelativePosition relativePosition) {
      this.relativePosition = relativePosition;
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

  }

  private final class PreservationTarget extends Target {

    PreservationTarget(RelativePosition relativePosition) {
      super(relativePosition);
    }

    @Override
    public void retain(int itemCount) {
      if (relativePosition.get() < 0) {
        checkAnnotations();
      }
      relativePosition.increase(itemCount);
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
      annotationsAreTame = false;
      relativePosition.increase(1);
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      annotationsAreTame = false;
      relativePosition.increase(1);
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
      for (int i = 0; i < map.endSize(); ++i) {
        activeAnnotations.remove(map.getEndKey(i));
      }
      for (int i = 0; i < map.changeSize(); ++i) {
        activeAnnotations.add(map.getChangeKey(i));
      }
    }

  }

  private final class DeletionTarget extends Target {

    DeletionTarget(RelativePosition relativePosition) {
      super(relativePosition);
    }

    @Override
    public void retain(int itemCount) {
      process(itemCount, false);
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
      process(chars.length(), true);
    }

    @Override
    public void deleteElementStart(String tag, Attributes attrs) {
      process(1, true);
    }

    @Override
    public void deleteElementEnd() {
      process(1, true);
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

    void process(int itemCount, boolean newDeletionState) {
      deletionState = newDeletionState;
      checkAnnotations();
      relativePosition.increase(itemCount);
    }

  }

  private boolean annotationsAreTame = true;
  private boolean deletionState = false;
  private final Set<String> activeAnnotations = new HashSet<String>();

  private AnnotationTamenessChecker() {}

  private void checkAnnotations() {
    if (deletionState == false && !activeAnnotations.isEmpty()) {
      annotationsAreTame = false;
    }
  }

  /**
   * Determines whether annotations are tame.
   *
   * @param preservationOp the structure-preserving operation
   * @param deletionOp the deletion operation
   * @return whether the annotations are tame
   */
  private boolean annotationsAreTame(
      DocOp preservationOp, DocOp deletionOp) throws TransformException {
    PositionTracker positionTracker = new PositionTracker();

    RelativePosition preservationPosition = positionTracker.getPreservationPosition();
    RelativePosition deletionPosition = positionTracker.getDeletionPosition();

    // The target responsible for processing components of the preservation operation.
    PreservationTarget preservationTarget = new PreservationTarget(preservationPosition);

    // The target responsible for processing components of the deletion operation.
    DeletionTarget deletionTarget = new DeletionTarget(deletionPosition);

    // Incrementally apply the two operations in a linearly-ordered interleaving
    // fashion.
    int preservationIndex = 0;
    int deletionIndex = 0;
    while (preservationIndex < preservationOp.size()) {
      preservationOp.applyComponent(preservationIndex++, preservationTarget);
      while (preservationPosition.get() > 0) {
        if (deletionIndex >= deletionOp.size()) {
          throw new TransformException("Ran out of " + deletionOp.size()
              + " deletion op components after " + preservationIndex + " of "
              + preservationOp.size() + " preservation op components, with "
              + preservationPosition.get() + " spare positions");
        }
        deletionOp.applyComponent(deletionIndex++, deletionTarget);
      }
    }
    while (deletionIndex < deletionOp.size()) {
      deletionOp.applyComponent(deletionIndex++, deletionTarget);
    }
    return annotationsAreTame;
  }

  /**
   * Determines whether a structure-preserving operation is tame relative to a
   * deletion operation.
   *
   * @param preservationOp the structure-preserving operation
   * @param deletionOp the deletion operation
   * @return whether the structure-preserving operation is tame
   * @throws TransformException if a problem is encountered
   */
  private static boolean checkTameness(
      DocOp preservationOp, DocOp deletionOp) throws TransformException {
    return new AnnotationTamenessChecker().annotationsAreTame(preservationOp, deletionOp);
  }

  /**
   * Determines whether a pair of structure-preserving operations are tame
   * relative to each of two deletion operations.
   *
   * @param p1 the first structure-preserving operation
   * @param p2 the second structure-preserving operation
   * @param d1 the first deletion operation
   * @param d2 the second deletion operation
   * @return whether the structure-preserving operations are tame
   * @throws TransformException if a problem is encountered
   */
  static boolean checkTameness(DocOp p1, DocOp p2,
      DocOp d1, DocOp d2) throws TransformException {
    return checkTameness(p1, d1)
        && checkTameness(p1, d2)
        && checkTameness(p2, d1)
        && checkTameness(p2, d2);
  }

}
