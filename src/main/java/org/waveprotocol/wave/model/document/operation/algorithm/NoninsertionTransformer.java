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
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.algorithm.PositionTracker.RelativePosition;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A utility class for transforming insertion-free operations.
 *
 * @author Alexandre Mah
 */
final class NoninsertionTransformer {

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

  // TODO: The implementation of AnnotationTracker can be further optimized.
  private abstract class AnnotationTracker {

    // NOTE: The following can be made more efficient with a more specialized
    // data structure with optimized operations for updating and combining maps
    // in the manner below.
    final Map<String, ValueUpdate> tracked = new HashMap<String, ValueUpdate>();
    final Map<String, ValueUpdate> active = new HashMap<String, ValueUpdate>();
    final Map<String, ValueUpdate> temporary = new HashMap<String, ValueUpdate>();
    final Map<String, ValueUpdate> propagating = new HashMap<String, ValueUpdate>();
    final DocOpCursor output;

    AnnotationTracker(DocOpCursor output) {
      this.output = output;
    }

    void commenceDeletion() {
      AnnotationTracker otherTracker = opposingTracker();
      // TODO: This seems pretty awkward. Perhaps we should give
      // AnnotationBoundaryMapImpl an easier builder to use.
      List<String> changeKeys = new ArrayList<String>();
      List<String> changeOldValues = new ArrayList<String>();
      List<String> changeNewValues = new ArrayList<String>();
      for (Map.Entry<String, ValueUpdate> entry : otherTracker.propagating.entrySet()) {
        String key = entry.getKey();
        ValueUpdate update = entry.getValue();
        ValueUpdate forCombining = active.get(key);
        temporary.put(key, forCombining);
        if (update != null) {
          changeKeys.add(key);
          changeNewValues.add(update.newValue);
          if (forCombining != null) {
            changeOldValues.add(forCombining.oldValue);
          } else if (otherTracker.active.containsKey(key)) {
            changeOldValues.add(otherTracker.active.get(key).newValue);
          } else {
            changeOldValues.add(update.oldValue);
          }
        } else if (otherTracker.active.containsKey(key)) {
          ValueUpdate currentActive = otherTracker.active.get(key);
          changeKeys.add(key);
          changeOldValues.add(currentActive.newValue);
          if (forCombining != null) {
            changeNewValues.add(forCombining.newValue);
          } else {
            changeNewValues.add(currentActive.oldValue);
          }
        }
      }
      commit(new AnnotationBoundaryMapImpl(
          new String[0],
          changeKeys.toArray(new String[0]),
          changeOldValues.toArray(new String[0]),
          changeNewValues.toArray(new String[0])));
    }

    void concludeDeletion() {
      AnnotationTracker otherTracker = opposingTracker();
      // TODO: This seems pretty awkward. Perhaps we should give
      // AnnotationBoundaryMapImpl an easier builder to use.
      List<String> endKeys = new ArrayList<String>();
      List<String> changeKeys = new ArrayList<String>();
      List<String> changeOldValues = new ArrayList<String>();
      List<String> changeNewValues = new ArrayList<String>();
      for (Map.Entry<String, ValueUpdate> entry : temporary.entrySet()) {
        String key = entry.getKey();
        ValueUpdate update = entry.getValue();
        if (update != null) {
          changeKeys.add(key);
          changeOldValues.add(update.oldValue);
          changeNewValues.add(update.newValue);
        } else {
          endKeys.add(key);
        }
      }
      sync();
      commit(new AnnotationBoundaryMapImpl(
          endKeys.toArray(new String[0]),
          changeKeys.toArray(new String[0]),
          changeOldValues.toArray(new String[0]),
          changeNewValues.toArray(new String[0])));
      temporary.clear();
    }

    void register(AnnotationBoundaryMap map) {
      for (int i = 0; i < map.endSize(); ++i) {
        tracked.remove(map.getEndKey(i));
      }
      for (int i = 0; i < map.changeSize(); ++i) {
        tracked.put(map.getChangeKey(i), new ValueUpdate(map.getOldValue(i), map.getNewValue(i)));
      }
      process(map);
    }

    void commit(AnnotationBoundaryMap map) {
      for (int i = 0; i < map.endSize(); ++i) {
        String key = map.getEndKey(i);
        if (!propagating.containsKey(key)) {
          propagating.put(key, active.get(key));
        }
        active.remove(key);
      }
      for (int i = 0; i < map.changeSize(); ++i) {
        String key = map.getChangeKey(i);
        ValueUpdate oldActive = active.get(key);
        if (oldActive == null
            || !ValueUtils.equal(oldActive.oldValue, map.getOldValue(i))
            || !ValueUtils.equal(oldActive.newValue, map.getNewValue(i))) {
          if (!propagating.containsKey(key)) {
            propagating.put(key, active.get(key));
          }
          active.put(key, new ValueUpdate(map.getOldValue(i), map.getNewValue(i)));
        }
      }
      output.annotationBoundary(map);
    }

    void sync() {
      propagating.clear();
    }

    abstract void process(AnnotationBoundaryMap map);
    abstract AnnotationTracker opposingTracker();

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
  private static final RangeResolver retainResolver = new RangeResolver() {

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveRetain(size);
    }

  };

  /**
   * A resolver for "deleteElementEnd" mutation components.
   */
  private static final RangeResolver deleteElementEndResolver = new RangeResolver() {

    @Override
    public void resolve(int size, RangeCache range) {
      range.resolveDeleteElementEnd();
    }

  };

  /**
   * A target of a document mutation which can be used to transform document
   * mutations by making use primarily of information from one mutation with the
   * help of auxiliary information from a second mutation. These targets should
   * be used in pairs.
   */
  private final class Target implements EvaluatingDocOpCursor<DocOp> {

    private final class DeleteCharactersCache extends RangeCache {

      private String characters;

      DeleteCharactersCache(String characters) {
        this.characters = characters;
      }

      @Override
      void resolveRetain(int itemCount) {
        doDeleteCharacters(characters.substring(0, itemCount));
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
        doDeleteElementStart(type, attributes);
        ++depth;
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {
        ++depth;
        ++otherTarget.depth;
      }

      @Override
      void resolveReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
        doDeleteElementStart(type, newAttributes);
        ++depth;
      }

      @Override
      void resolveUpdateAttributes(AttributesUpdate update) {
        doDeleteElementStart(type, attributes.updateWith(update));
        ++depth;
      }

    }

    private final class DeleteElementEndCache extends RangeCache {

      @Override
      void resolveRetain(int itemCount) {
        doDeleteElementEnd();
        --depth;
      }

      @Override
      void resolveDeleteElementEnd() {
        --depth;
        --otherTarget.depth;
      }

    }

    private final class ReplaceAttributesCache extends RangeCache {

      private final Attributes oldAttributes;
      private final Attributes newAttributes;

      ReplaceAttributesCache(Attributes oldAttributes, Attributes newAttributes) {
        this.oldAttributes = oldAttributes;
        this.newAttributes = newAttributes;
      }

      @Override
      void resolveRetain(int itemCount) {
        syncAnnotations();
        targetDocument.replaceAttributes(oldAttributes, newAttributes);
        otherTarget.targetDocument.retain(1);
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {
        otherTarget.doDeleteElementStart(type, newAttributes);
        ++otherTarget.depth;
      }

      @Override
      void resolveReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
        syncAnnotations();
        targetDocument.replaceAttributes(newAttributes, this.newAttributes);
        otherTarget.targetDocument.retain(1);
      }

      @Override
      void resolveUpdateAttributes(AttributesUpdate update) {
        syncAnnotations();
        targetDocument.replaceAttributes(this.oldAttributes.updateWith(update), this.newAttributes);
        otherTarget.targetDocument.retain(1);
      }

    }

    private final class UpdateAttributesCache extends RangeCache {

      private final AttributesUpdate update;

      UpdateAttributesCache(AttributesUpdate update) {
        this.update = update;
      }

      @Override
      void resolveRetain(int itemCount) {
        syncAnnotations();
        targetDocument.updateAttributes(update);
        otherTarget.targetDocument.retain(1);
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {
        otherTarget.doDeleteElementStart(type, attributes.updateWith(update));
        ++otherTarget.depth;
      }

      @Override
      void resolveReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
        syncAnnotations();
        targetDocument.retain(1);
        otherTarget.targetDocument.replaceAttributes(oldAttributes.updateWith(update),
            newAttributes);
      }

      @Override
      void resolveUpdateAttributes(AttributesUpdate update) {
        syncAnnotations();
        Map<String, String> updated = new HashMap<String, String>();
        for (int i = 0; i < update.changeSize(); ++i) {
          updated.put(update.getChangeKey(i), update.getNewValue(i));
        }
        AttributesUpdate newUpdate = new AttributesUpdateImpl();
        // TODO: This is a little silly. We should do this a better way.
        for (int i = 0; i < this.update.changeSize(); ++i) {
          String key = this.update.getChangeKey(i);
          String newOldValue = updated.containsKey(key) ? updated.get(key)
              : this.update.getOldValue(i);
          newUpdate = newUpdate.composeWith(new AttributesUpdateImpl(key,
              newOldValue, this.update.getNewValue(i)));
        }
        targetDocument.updateAttributes(newUpdate);
        Set<String> keySet = new HashSet<String>();
        for (int i = 0; i < this.update.changeSize(); ++i) {
          keySet.add(this.update.getChangeKey(i));
        }
        AttributesUpdate transformedAttributes = update.exclude(keySet);
        otherTarget.targetDocument.updateAttributes(transformedAttributes);
      }

    }

    private final RangeCache retainCache = new RangeCache() {

      @Override
      void resolveRetain(int itemCount) {
        syncAnnotations();
        targetDocument.retain(itemCount);
        otherTarget.targetDocument.retain(itemCount);
      }

      @Override
      void resolveDeleteCharacters(String characters) {
        otherTarget.doDeleteCharacters(characters);
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {
        otherTarget.doDeleteElementStart(type, attributes);
        ++otherTarget.depth;
      }

      @Override
      void resolveDeleteElementEnd() {
        otherTarget.doDeleteElementEnd();
        --otherTarget.depth;
      }

      @Override
      void resolveReplaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
        syncAnnotations();
        targetDocument.retain(1);
        otherTarget.targetDocument.replaceAttributes(oldAttributes, newAttributes);
      }

      @Override
      void resolveUpdateAttributes(AttributesUpdate update) {
        syncAnnotations();
        targetDocument.retain(1);
        otherTarget.targetDocument.updateAttributes(update);
      }

    };

    /**
     * The target to which to write the transformed mutation.
     */
    private final EvaluatingDocOpCursor<DocOp> targetDocument;

    /**
     * The position of the processing cursor associated with this target
     * relative to the position of the processing cursor associated to the
     * opposing target. All positional calculations are based on cursor
     * positions in the original document on which the two original operations
     * apply.
     */
    private final RelativePosition relativePosition;

    /**
     * An annotation tracker that tracks annotation modifications at the current
     * cursor position.
     */
    private final AnnotationTracker annotationTracker;

    /**
     * The target that is used opposite this target in the transformation.
     */
    private Target otherTarget;

    /**
     * A cache for the effect of mutation components which affect ranges.
     */
    private RangeCache rangeCache = retainCache;

    /**
     * The current depth of element deletions.
     */
    private int depth = 0;

    Target(EvaluatingDocOpCursor<DocOp> targetDocument, RelativePosition relativePosition,
        AnnotationTracker annotationTracker) {
      this.targetDocument = targetDocument;
      this.relativePosition = relativePosition;
      this.annotationTracker = annotationTracker;
    }

    // TODO: See if we can remove this explicit method and find a
    // better way to do this using a constructor or factory.
    public void setOtherTarget(Target otherTarget) {
      this.otherTarget = otherTarget;
    }

    @Override
    public DocOp finish() {
      return targetDocument.finish();
    }

    @Override
    public void retain(int itemCount) {
      resolveRange(itemCount, retainResolver);
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
      int resolutionSize = resolveRange(chars.length(), new DeleteCharactersResolver(chars));
      if (resolutionSize >= 0) {
        rangeCache = new DeleteCharactersCache(chars.substring(resolutionSize));
      }
    }

    @Override
    public void deleteElementStart(String tag, Attributes attrs) {
      if (resolveRange(1, new DeleteElementStartResolver(tag, attrs)) == 0) {
        rangeCache = new DeleteElementStartCache(tag, attrs);
      }
    }

    @Override
    public void deleteElementEnd() {
      if (resolveRange(1, deleteElementEndResolver) == 0) {
        rangeCache = new DeleteElementEndCache();
      }
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      if (resolveRange(1, new ReplaceAttributesResolver(oldAttrs, newAttrs)) == 0) {
        rangeCache = new ReplaceAttributesCache(oldAttrs, newAttrs);
      }
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
      if (resolveRange(1, new UpdateAttributesResolver(attrUpdate)) == 0) {
        rangeCache = new UpdateAttributesCache(attrUpdate);
      }
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
      annotationTracker.register(map);
    }

    /**
     * Resolves the transformation of a range.
     *
     * @param size the requested size to resolve
     * @param resolver the resolver to use
     * @return the portion of the requested size that was resolved, or -1 to
     *         indicate that the entire range was resolved
     */
    private int resolveRange(int size, RangeResolver resolver) {
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

    private void syncAnnotations() {
      annotationTracker.sync();
      otherTarget.annotationTracker.sync();
    }

    private void doDeleteCharacters(String chars) {
      annotationTracker.commenceDeletion();
      targetDocument.deleteCharacters(chars);
      annotationTracker.concludeDeletion();
    }

    private void doDeleteElementStart(String type, Attributes attrs) {
      annotationTracker.commenceDeletion();
      targetDocument.deleteElementStart(type, attrs);
      annotationTracker.concludeDeletion();
    }

    private void doDeleteElementEnd() {
      annotationTracker.commenceDeletion();
      targetDocument.deleteElementEnd();
      annotationTracker.concludeDeletion();
    }

  }

  private final EvaluatingDocOpCursor<DocOp> clientOperation =
      OperationNormalizer.createNormalizer(new DocOpBuffer());
  private final EvaluatingDocOpCursor<DocOp> serverOperation =
      OperationNormalizer.createNormalizer(new DocOpBuffer());

  private final AnnotationTracker clientAnnotationTracker =
      new AnnotationTracker(clientOperation) {

    @Override
    public void process(AnnotationBoundaryMap map) {
      // TODO: This seems pretty awkward. Perhaps we should give
      // AnnotationBoundaryMapImpl an easier builder to use.
      List<String> clientEndKeys = new ArrayList<String>();
      List<String> clientChangeKeys = new ArrayList<String>();
      List<String> clientChangeOldValues = new ArrayList<String>();
      List<String> clientChangeNewValues = new ArrayList<String>();
      List<String> serverEndKeys = new ArrayList<String>();
      List<String> serverChangeKeys = new ArrayList<String>();
      List<String> serverChangeOldValues = new ArrayList<String>();
      List<String> serverChangeNewValues = new ArrayList<String>();
      for (int i = 0; i < map.endSize(); ++i) {
        String key = map.getEndKey(i);
        ValueUpdate serverValues = serverAnnotationTracker.tracked.get(key);
        clientEndKeys.add(key);
        if (serverValues != null) {
          serverChangeKeys.add(key);
          serverChangeOldValues.add(serverValues.oldValue);
          serverChangeNewValues.add(serverValues.newValue);
        }
      }
      for (int i = 0; i < map.changeSize(); ++i) {
        String key = map.getChangeKey(i);
        String oldValue = map.getOldValue(i);
        String newValue = map.getNewValue(i);
        ValueUpdate serverValues = serverAnnotationTracker.tracked.get(key);
        clientChangeKeys.add(key);
        clientChangeNewValues.add(newValue);
        if (serverValues != null) {
          clientChangeOldValues.add(serverValues.newValue);
          serverEndKeys.add(key);
        } else {
          clientChangeOldValues.add(oldValue);
        }
      }
      commit(new AnnotationBoundaryMapImpl(
          clientEndKeys.toArray(new String[0]),
          clientChangeKeys.toArray(new String[0]),
          clientChangeOldValues.toArray(new String[0]),
          clientChangeNewValues.toArray(new String[0])));
      serverAnnotationTracker.commit(new AnnotationBoundaryMapImpl(
          serverEndKeys.toArray(new String[0]),
          serverChangeKeys.toArray(new String[0]),
          serverChangeOldValues.toArray(new String[0]),
          serverChangeNewValues.toArray(new String[0])));
    }

    @Override
    AnnotationTracker opposingTracker() {
      return serverAnnotationTracker;
    }

  };

  private final AnnotationTracker serverAnnotationTracker =
      new AnnotationTracker(serverOperation) {

    @Override
    public void process(AnnotationBoundaryMap map) {
      // TODO: This seems pretty awkward. Perhaps we should give
      // AnnotationBoundaryMapImpl an easier builder to use.
      List<String> serverEndKeys = new ArrayList<String>();
      List<String> serverChangeKeys = new ArrayList<String>();
      List<String> serverChangeOldValues = new ArrayList<String>();
      List<String> serverChangeNewValues = new ArrayList<String>();
      List<String> clientEndKeys = new ArrayList<String>();
      List<String> clientChangeKeys = new ArrayList<String>();
      List<String> clientChangeOldValues = new ArrayList<String>();
      List<String> clientChangeNewValues = new ArrayList<String>();
      for (int i = 0; i < map.endSize(); ++i) {
        String key = map.getEndKey(i);
        ValueUpdate clientValues = clientAnnotationTracker.tracked.get(key);
        if (clientValues != null) {
          clientChangeKeys.add(key);
          clientChangeOldValues.add(clientValues.oldValue);
          clientChangeNewValues.add(clientValues.newValue);
        } else {
          serverEndKeys.add(key);
        }
      }
      for (int i = 0; i < map.changeSize(); ++i) {
        String key = map.getChangeKey(i);
        String oldValue = map.getOldValue(i);
        String newValue = map.getNewValue(i);
        ValueUpdate clientValues = clientAnnotationTracker.tracked.get(key);
        if (clientValues != null) {
          clientChangeKeys.add(key);
          clientChangeOldValues.add(newValue);
          clientChangeNewValues.add(clientValues.newValue);
        } else {
          serverChangeKeys.add(key);
          serverChangeOldValues.add(oldValue);
          serverChangeNewValues.add(newValue);
        }
      }
      commit(new AnnotationBoundaryMapImpl(
          serverEndKeys.toArray(new String[0]),
          serverChangeKeys.toArray(new String[0]),
          serverChangeOldValues.toArray(new String[0]),
          serverChangeNewValues.toArray(new String[0])));
      clientAnnotationTracker.commit(new AnnotationBoundaryMapImpl(
          clientEndKeys.toArray(new String[0]),
          clientChangeKeys.toArray(new String[0]),
          clientChangeOldValues.toArray(new String[0]),
          clientChangeNewValues.toArray(new String[0])));
    }

    @Override
    AnnotationTracker opposingTracker() {
      return clientAnnotationTracker;
    }

  };

  /**
   * Transforms a pair of insertion-free operations.
   *
   * @param clientOp the operation from the client
   * @param serverOp the operation from the server
   * @return the transformed pair of operations
   * @throws TransformException if a problem was encountered during the
   *         transformation process
   */
  OperationPair<DocOp> transformOperations(DocOp clientOp,
      DocOp serverOp) throws TransformException {
    try {
      PositionTracker positionTracker = new PositionTracker();

      RelativePosition clientPosition = positionTracker.getPosition1();
      RelativePosition serverPosition = positionTracker.getPosition2();

      // The target responsible for processing components of the client operation.
      Target clientTarget = new Target(clientOperation, clientPosition, clientAnnotationTracker);

      // The target responsible for processing components of the server operation.
      Target serverTarget = new Target(serverOperation, serverPosition, serverAnnotationTracker);

      clientTarget.setOtherTarget(serverTarget);
      serverTarget.setOtherTarget(clientTarget);

      // Incrementally apply the two operations in a linearly-ordered interleaving
      // fashion.
      int clientIndex = 0;
      int serverIndex = 0;
      while (clientIndex < clientOp.size()) {
        clientOp.applyComponent(clientIndex++, clientTarget);
        while (clientPosition.get() > 0) {
          if (serverIndex >= serverOp.size()) {
            throw new TransformException("Ran out of " + serverOp.size()
                + " server op components after " + clientIndex + " of " + clientOp.size()
                + " client op components, with " + clientPosition.get() + " spare positions");
          }
          serverOp.applyComponent(serverIndex++, serverTarget);
        }
      }
      while (serverIndex < serverOp.size()) {
        serverOp.applyComponent(serverIndex++, serverTarget);
      }
      clientOp = clientTarget.finish();
      serverOp = serverTarget.finish();
    } catch (InternalTransformException e) {
      throw new TransformException(e.getMessage());
    }
    return new OperationPair<DocOp>(clientOp, serverOp);
  }

}
