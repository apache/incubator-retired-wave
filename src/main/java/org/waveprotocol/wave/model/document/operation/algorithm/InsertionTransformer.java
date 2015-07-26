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
import org.waveprotocol.wave.model.document.operation.algorithm.PositionTracker.RelativePosition;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;

/**
 * A utility class for transforming insertion operations.
 *
 * @author Alexandre Mah
 */
final class InsertionTransformer {

  /**
   * A target of a document mutation which can be used to transform document
   * mutations by making use primarily of information from one mutation with the
   * help of auxiliary information from a second mutation. These targets should
   * be used in pairs.
   */
  private static final class Target implements EvaluatingDocOpCursor<DocOp> {

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
      int oldPosition = relativePosition.get();
      relativePosition.increase(itemCount);
      if (relativePosition.get() < 0) {
        targetDocument.retain(itemCount);
        otherTarget.targetDocument.retain(itemCount);
      } else if (oldPosition < 0) {
        targetDocument.retain(-oldPosition);
        otherTarget.targetDocument.retain(-oldPosition);
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

  /**
   * Transforms a pair of insertion operations.
   *
   * @param clientOp the operation from the client
   * @param serverOp the operation from the server
   * @return the transformed pair of operations
   * @throws TransformException if a problem was encountered during the
   *         transformation process
   */
  OperationPair<DocOp> transformOperations(DocOp clientOp,
      DocOp serverOp) throws TransformException {
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
    return new OperationPair<DocOp>(clientOp, serverOp);
  }

}
