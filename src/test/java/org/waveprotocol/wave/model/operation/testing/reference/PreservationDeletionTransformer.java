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
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.testing.reference.PositionTracker.RelativePosition;
import org.waveprotocol.wave.model.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A utility class for transforming a structure-preserving operation with a
 * deletion operation.
 *
 * @author Alexandre Mah
 */
final class PreservationDeletionTransformer {

  /**
   * For internal error propagation. It is a RuntimeException to facilitate
   * error propagation through document operation interfaces from outside
   * this package.
   */
  private static class InternalTransformException extends RuntimeException {

    InternalTransformException(String message) {
      super(message);
    }

  }

  /**
   * A cache for the effect of a component of a document mutation that affects a
   * range of the document.
   */
  private static abstract class RangeCache {

    abstract void resolveRetain(int retain);

    void resolveDeleteCharacters(String characters) {
      throw new InternalTransformException("Incompatible operations in transformation");
    }

    void resolveDeleteElementStart(String type, Attributes attributes) {
      throw new InternalTransformException("Incompatible operations in transformation");
    }

    void resolveDeleteElementEnd() {
      throw new InternalTransformException("Incompatible operations in transformation");
    }

    void resolveReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
      throw new InternalTransformException("Incompatible operations in transformation");
    }

    void resolveUpdateAttributes(AttributesUpdate update) {
      throw new InternalTransformException("Incompatible operations in transformation");
    }

  }

  /**
   * A resolver for mutation components which affects ranges.
   */
  private interface RangeResolver {

    /**
     * Resolves a mutation component with a cached mutation component from a
     * different document mutation.
     *
     * @param size The size of the range affected by the range modifications to
     *        resolve.
     * @param cache The cached range.
     */
    void resolve(int size, RangeCache cache);

  }

  /**
   * A target of a document mutation which can be used to transform document
   * mutations by making use primarily of information from one mutation with the
   * help of auxiliary information from a second mutation. These targets should
   * be used in pairs.
   */
  private abstract class Target<T> implements EvaluatingDocOpCursor<T> {

    /**
     * The position of the processing cursor associated with this target
     * relative to the position of the processing cursor associated to the
     * opposing target. All positional calculations are based on cursor
     * positions in the original document on which the two original operations
     * apply.
     */
    private final RelativePosition relativePosition;

    /**
     * The target that is used opposite this target in the transformation.
     */
    private Target<?> otherTarget;

    /**
     * A cache for the effect of mutation components which affect ranges.
     */
    private RangeCache rangeCache = retainCache;

    Target(RelativePosition relativePosition) {
      this.relativePosition = relativePosition;
    }

    // TODO: See if we can remove this explicit method and find a
    // better way to do this using a constructor or factory.
    public void setOtherTarget(Target<?> otherTarget) {
      this.otherTarget = otherTarget;
    }

    @Override
    public void retain(int itemCount) {
      resolveRange(itemCount, RETAIN_RESOLVER);
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

    /**
     * Resolves the transformation of a range.
     *
     * @param size the requested size to resolve
     * @param resolver the resolver to use
     * @return the portion of the requested size that was resolved, or -1 to
     *         indicate that the entire range was resolved
     */
    int resolveRange(int size, RangeResolver resolver) {
      int oldPosition = relativePosition.get();
      relativePosition.increase(size);
      if (relativePosition.get() > 0) {
        if (oldPosition < 0) {
          resolver.resolve(-oldPosition, otherTarget.rangeCache);
        }
        return -oldPosition;
      } else {
        resolver.resolve(size, otherTarget.rangeCache);
        return -1;
      }
    }

    void setRangeCache(RangeCache rangeCache) {
      this.rangeCache = rangeCache;
    }

  }

  /**
   * A target of a document mutation which can be used to transform document
   * mutations by making use primarily of information from one mutation with the
   * help of auxiliary information from a second mutation. These targets should
   * be used in pairs.
   */
  private final class PreservationTarget extends Target<DocOp> {

    private final class ReplaceAttributesCache extends RangeCache {

      private final Attributes oldAttributes;
      private final Attributes newAttributes;

      ReplaceAttributesCache(Attributes oldAttributes, Attributes newAttributes) {
        this.oldAttributes = oldAttributes;
        this.newAttributes = newAttributes;
      }

      @Override
      void resolveRetain(int itemCount) {
        processReplaceAttributes(oldAttributes, newAttributes);
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {
        processDeleteElementStart(type, newAttributes);
      }

    }

    private final class UpdateAttributesCache extends RangeCache {

      private final AttributesUpdate update;

      UpdateAttributesCache(AttributesUpdate update) {
        this.update = update;
      }

      @Override
      void resolveRetain(int itemCount) {
        processUpdateAttributes(update);
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {
        processDeleteElementStart(type, attributes.updateWith(update));
      }

    }

    PreservationTarget(RelativePosition relativePosition) {
      super(relativePosition);
    }

    @Override
    public DocOp finish() {
      return preservationOperation.finish();
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
      if (resolveRange(1, new ReplaceAttributesResolver(oldAttrs, newAttrs)) == 0) {
        setRangeCache(new ReplaceAttributesCache(oldAttrs, newAttrs));
      }
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      if (resolveRange(1, new UpdateAttributesResolver(attrUpdate)) == 0) {
        setRangeCache(new UpdateAttributesCache(attrUpdate));
      }
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
      preservationOperation.annotationBoundary(map);
      for (int i = 0; i < map.endSize(); ++i) {
        String key = map.getEndKey(i);
        if (!propagatingAnnotations.containsKey(key)) {
          propagatingAnnotations.put(key, activeAnnotations.get(key));
        }
        activeAnnotations.remove(key);
      }
      for (int i = 0; i < map.changeSize(); ++i) {
        String key = map.getChangeKey(i);
        if (!propagatingAnnotations.containsKey(key)) {
          propagatingAnnotations.put(key, activeAnnotations.get(key));
        }
        activeAnnotations.put(key,
            new ValueUpdate(map.getOldValue(i), map.getNewValue(i)));
      }
    }

  }

  /**
   * A target of a document mutation which can be used to transform document
   * mutations by making use primarily of information from one mutation with the
   * help of auxiliary information from a second mutation. These targets should
   * be used in pairs.
   */
  private final class DeletionTarget extends Target<Pair<DocOp, DocOp>> {

    private final class DeleteCharactersCache extends RangeCache {

      private String characters;

      DeleteCharactersCache(String characters) {
        this.characters = characters;
      }

      @Override
      void resolveRetain(int itemCount) {
        processDeleteCharacters(characters.substring(0, itemCount));
        characters = characters.substring(itemCount);
      }

      @Override
      void resolveDeleteCharacters(String characters) {
        this.characters = this.characters.substring(characters.length());
      }

    }

    private final class DeleteElementStartCache extends RangeCache {

      private final String type;
      private final Attributes attributes;

      DeleteElementStartCache(String type, Attributes attributes) {
        this.type = type;
        this.attributes = attributes;
      }

      @Override
      void resolveRetain(int itemCount) {
        processDeleteElementStart(type, attributes);
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {}

      @Override
      void resolveReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
        // This point should be unreachable.
        assert false;
        processDeleteElementStart(type, newAttributes);
      }

      @Override
      void resolveUpdateAttributes(AttributesUpdate update) {
        // This point should be unreachable.
        assert false;
        processDeleteElementStart(type, attributes.updateWith(update));
      }

    }

    private final class DeleteElementEndCache extends RangeCache {

      @Override
      void resolveRetain(int itemCount) {
        processDeleteElementEnd();
      }

      @Override
      void resolveDeleteElementEnd() {}

    }

    DeletionTarget(RelativePosition relativePosition) {
      super(relativePosition);
    }

    @Override
    public Pair<DocOp, DocOp> finish() {
      return new Pair<DocOp, DocOp>(
          annotationResidue.finish(), deletionOperation.finish());
    }

    @Override
    public void retain(int itemCount) {
      resolveRange(itemCount, RETAIN_RESOLVER);
      setRangeCache(retainCache);
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
      int resolutionSize = resolveRange(chars.length(), new DeleteCharactersResolver(chars));
      if (resolutionSize >= 0) {
        setRangeCache(new DeleteCharactersCache(chars.substring(resolutionSize)));
      }
    }

    @Override
    public void deleteElementStart(String tag, Attributes attrs) {
      if (resolveRange(1, new DeleteElementStartResolver(tag, attrs)) == 0) {
        setRangeCache(new DeleteElementStartCache(tag, attrs));
      }
    }

    @Override
    public void deleteElementEnd() {
      if (resolveRange(1, DELETE_ELEMENT_END_RESOLVER) == 0) {
        setRangeCache(new DeleteElementEndCache());
      }
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

  /**
   * A resolver for "deleteCharacters" mutation components.
   */
  private static final class DeleteCharactersResolver implements RangeResolver {

    private final String characters;

    DeleteCharactersResolver(String characters) {
      this.characters = characters;
    }

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveDeleteCharacters(characters.substring(0, size));
    }

  }

  /**
   * A resolver for "deleteElementStart" mutation components.
   */
  private static final class DeleteElementStartResolver implements RangeResolver {

    private final String type;
    private final Attributes attributes;

    DeleteElementStartResolver(String type, Attributes attributes) {
      this.type = type;
      this.attributes = attributes;
    }

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveDeleteElementStart(type, attributes);
    }

  }

  /**
   * A resolver for "replaceAttributes" mutation components.
   */
  private static final class ReplaceAttributesResolver implements RangeResolver {

    private final Attributes oldAttributes;
    private final Attributes newAttributes;

    ReplaceAttributesResolver(Attributes oldAttributes, Attributes newAttributes) {
      this.oldAttributes = oldAttributes;
      this.newAttributes = newAttributes;
    }

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveReplaceAttributes(oldAttributes, newAttributes);
    }

  }

  /**
   * A resolver for "updateAttributes" mutation components.
   */
  private static final class UpdateAttributesResolver implements RangeResolver {

    private final AttributesUpdate update;

    UpdateAttributesResolver(AttributesUpdate update) {
      this.update = update;
    }

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveUpdateAttributes(update);
    }

  }

  /**
   * A resolver for "retain" mutation components.
   */
  private static final RangeResolver RETAIN_RESOLVER = new RangeResolver() {

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveRetain(size);
    }

  };

  /**
   * A resolver for "deleteElementEnd" mutation components.
   */
  private static final RangeResolver DELETE_ELEMENT_END_RESOLVER = new RangeResolver() {

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveDeleteElementEnd();
    }

  };

  private final RangeCache retainCache = new RangeCache() {

    @Override
    void resolveRetain(int itemCount) {
      preservationOperation.retain(itemCount);
      annotationResidue.retain(itemCount);
      deletionOperation.retain(itemCount);
      propagatingAnnotations.clear();
    }

    @Override
    void resolveDeleteCharacters(String characters) {
      processDeleteCharacters(characters);
    }

    @Override
    void resolveDeleteElementStart(String type, Attributes attributes) {
      processDeleteElementStart(type, attributes);
    }

    @Override
    void resolveDeleteElementEnd() {
      processDeleteElementEnd();
    }

    @Override
    void resolveReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
      processReplaceAttributes(oldAttributes, newAttributes);
    }

    @Override
    void resolveUpdateAttributes(AttributesUpdate update) {
      processUpdateAttributes(update);
    }

  };

  private final EvaluatingDocOpCursor<DocOp> preservationOperation =
      OperationNormalizer.createNormalizer(new DocOpBuffer());
  private final EvaluatingDocOpCursor<DocOp> annotationResidue =
      OperationNormalizer.createNormalizer(new DocOpBuffer());
  private final EvaluatingDocOpCursor<DocOp> deletionOperation =
      OperationNormalizer.createNormalizer(new DocOpBuffer());

  private final Map<String, ValueUpdate> activeAnnotations = new HashMap<String, ValueUpdate>();
  private final Map<String, ValueUpdate> propagatingAnnotations =
      new HashMap<String, ValueUpdate>();

  private void processDeleteCharacters(String characters) {
    deletionOperation.deleteCharacters(characters);
    delete(characters.length());
  }

  private void processDeleteElementStart(String type, Attributes attributes) {
    deletionOperation.deleteElementStart(type, attributes);
    delete(1);
  }

  private void processDeleteElementEnd() {
    deletionOperation.deleteElementEnd();
    delete(1);
  }

  private void processReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
    annotationResidue.retain(1);
    deletionOperation.retain(1);
    preservationOperation.replaceAttributes(oldAttributes, newAttributes);
    propagatingAnnotations.clear();
  }

  private void processUpdateAttributes(AttributesUpdate update) {
    annotationResidue.retain(1);
    deletionOperation.retain(1);
    preservationOperation.updateAttributes(update);
    propagatingAnnotations.clear();
  }

  private void delete(int size) {
    List<String> keys = new ArrayList<String>();
    List<String> oldValues = new ArrayList<String>();
    List<String> newValues = new ArrayList<String>();
    for (Map.Entry<String, ValueUpdate> entry : propagatingAnnotations.entrySet()) {
      String key = entry.getKey();
      ValueUpdate update = entry.getValue();
      ValueUpdate activeUpdate = activeAnnotations.get(key);
      if (update != null) {
        keys.add(key);
        oldValues.add(activeUpdate != null ? activeUpdate.newValue : update.oldValue);
        newValues.add(update.newValue);
      } else if (activeUpdate != null) {
        keys.add(key);
        oldValues.add(activeUpdate.newValue);
        newValues.add(activeUpdate.oldValue);
      }
    }
    annotationResidue.annotationBoundary(new AnnotationBoundaryMapImpl(
        new String[0],
        keys.toArray(new String[0]),
        oldValues.toArray(new String[0]),
        newValues.toArray(new String[0])));
    annotationResidue.retain(size);
    annotationResidue.annotationBoundary(new AnnotationBoundaryMapImpl(
        keys.toArray(new String[0]),
        new String[0],
        new String[0],
        new String[0]));
  }

  /**
   * Transforms a structure-preserving operation with a deletion operation.
   *
   * @param preservationOp the structure-preserving operation
   * @param deletionOp the deletion operation
   * @return the transformed preservation operation and the transformed
   *         transformed deletion operation
   * @throws TransformException if a problem was encountered during the
   *         transformation process
   */
  Pair<DocOp, Pair<DocOp, DocOp>> transformOperations(
      DocOp preservationOp, DocOp deletionOp) throws TransformException {
    Pair<DocOp, DocOp> transformedDeletionOp;
    try {
      PositionTracker positionTracker = new PositionTracker();

      RelativePosition preservationPosition = positionTracker.getPosition1();
      RelativePosition deletionPosition = positionTracker.getPosition2();

      // The target responsible for processing components of the preservation operation.
      PreservationTarget preservationTarget = new PreservationTarget(preservationPosition);

      // The target responsible for processing components of the deletion operation.
      DeletionTarget deletionTarget = new DeletionTarget(deletionPosition);

      preservationTarget.setOtherTarget(deletionTarget);
      deletionTarget.setOtherTarget(preservationTarget);

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
      preservationOp = preservationTarget.finish();
      transformedDeletionOp = deletionTarget.finish();
    } catch (InternalTransformException e) {
      throw new TransformException(e.getMessage());
    }
    return new Pair<DocOp, Pair<DocOp, DocOp>>(preservationOp,
        transformedDeletionOp);
  }

}
