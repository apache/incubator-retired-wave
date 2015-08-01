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
import org.waveprotocol.wave.model.document.operation.algorithm.RangeNormalizer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.testing.reference.PositionTracker.RelativePosition;

/**
 * A utility class for transforming deletion operations.
 *
 * @author Alexandre Mah
 */
final class DeletionDeletionTransformer {

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
        targetDocument.deleteCharacters(characters.substring(0, itemCount));
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
        targetDocument.deleteElementStart(type, attributes);
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {}

    }

    private final class DeleteElementEndCache extends RangeCache {

      @Override
      void resolveRetain(int itemCount) {
        targetDocument.deleteElementEnd();
      }

      @Override
      void resolveDeleteElementEnd() {}

    }

    private final RangeCache retainCache = new RangeCache() {

      @Override
      void resolveRetain(int itemCount) {
        targetDocument.retain(itemCount);
        otherTarget.targetDocument.retain(itemCount);
      }

      @Override
      void resolveDeleteCharacters(String characters) {
        otherTarget.targetDocument.deleteCharacters(characters);
      }

      @Override
      void resolveDeleteElementStart(String type, Attributes attributes) {
        otherTarget.targetDocument.deleteElementStart(type, attributes);
      }

      @Override
      void resolveDeleteElementEnd() {
        otherTarget.targetDocument.deleteElementEnd();
      }

    };

    /**
     * The target to which to write the transformed mutation.
     */
    private final EvaluatingDocOpCursor<DocOp> targetDocument =
        new RangeNormalizer<DocOp>(new DocOpBuffer());

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
    private Target otherTarget;

    /**
     * A cache for the effect of mutation components which affect ranges.
     */
    private RangeCache rangeCache = retainCache;

    Target(RelativePosition relativePosition) {
      this.relativePosition = relativePosition;
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
      if (resolveRange(1, DELETE_ELEMENT_END_RESOLVER) == 0) {
        rangeCache = new DeleteElementEndCache();
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

  }

  /**
   * Transforms a pair of deletion operations.
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
      Target clientTarget = new Target(clientPosition);

      // The target responsible for processing components of the server operation.
      Target serverTarget = new Target(serverPosition);

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
