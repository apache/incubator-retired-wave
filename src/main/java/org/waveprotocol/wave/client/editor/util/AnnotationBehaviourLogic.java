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

package org.waveprotocol.wave.client.editor.util;

import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;

import org.waveprotocol.wave.model.document.AnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.BiasDirection;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.ContentType;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.CursorDirection;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.InheritDirection;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.Box;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.Iterator;

/**
 * Implementation of the logic for custom annotation behaviours.
 * See @link{AnnotationBehaviour} for description of the algorithm, this aggregates all the
 * registered behaviours and performs the bias/supplementing they define.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public class AnnotationBehaviourLogic<N> {
  /** Registry storing all known annotation behaviour definitions. */
  private final AnnotationRegistry registry;

  /// Mutable state that should not change inside init/reset

  /** Accessor to the extra annotations currently on the caret. */
  private final CaretAnnotations caret;

  /** Current document being annotated. */
  private final MutableDocument<N, ?, ?> doc;

  /// Mutable state used within behavioural logic

  /** maps for annotation changes */
  StringMap<Object> leftSide = CollectionUtils.createStringMap();
  StringMap<Object> rightSide = CollectionUtils.createStringMap();

  /** Constructor that associates the behaviour with its registry. */
  public AnnotationBehaviourLogic(AnnotationRegistry registry,
      MutableDocument<N, ?, ?> doc, CaretAnnotations caret) {
    this.registry = registry;
    this.doc = doc;
    this.caret = caret;
  }

  /**
   * Re-bias the cursor - based on the current selection positions, and the last known movement.
   * Checks to see whether any annotation or elements desire to update the bias, and if so,
   * obeys the one with the highest priority.
   *
   * Note that if the selection is ranged, we always bias to the right (over the first character)
   *
   * @param start Start of selection
   * @param end End of selection
   * @param lastMovement The previously last known movement of the cursor to get into this state.
   * @return The new bias type.
   */
  public BiasDirection rebias(int start, int end, final CursorDirection lastMovement) {
    Preconditions.checkState(doc != null, "Cannot call out of init/reset cycle.");
    Preconditions.checkPositionIndexes(start, end, doc.size());

    // ranged is special case, always bias to the right of the start
    if (start != end) {
      return BiasDirection.RIGHT;
    }

    // initialise with containers:
    final Box<Double> bestPriority = Box.create(0.0);
    final Box<BiasDirection> bias = Box.create(biasFromContainers(doc.locate(start)));
    if (bias.boxed == BiasDirection.NEITHER) {
      bias.boxed = CursorDirection.toBiasDirection(lastMovement);
    } else {
      bestPriority.boxed = AnnotationBehaviour.ELEMENT_PRIORITY;
    }

    // build maps
    buildBoundaryMaps(start);

    // iterate through and bias based on priority
    leftSide.each( new ProcV<Object>() {
      public void apply(String key, Object value) {
        Iterator<AnnotationBehaviour> behaviours = registry.getBehaviours(key);

        while (behaviours.hasNext()) {
          AnnotationBehaviour behaviour = behaviours.next();
          double priority = behaviour.getPriority();
          if (priority > bestPriority.boxed) {
            bestPriority.boxed = priority;
            bias.boxed = behaviour.getBias(leftSide, rightSide, lastMovement);
          }
        }
      }
    });
    return bias.boxed;
  }

  /**
   * Alter the caret annotations to override the default inherit-from-left when registered
   * annotation behaviour dictates otherwise.
   *
   * @param location Position in the document that the caret resides.
   * @param bias The last direction of movement to get to here.
   * @param type The type of content that this replacement is for
   */
  public void supplementAnnotations(final int location, final BiasDirection bias,
      final ContentType type) {
    Preconditions.checkState(doc != null, "Cannot call out of init/reset cycle.");

    // build context for replacements
    buildBoundaryMaps(location);
    Point<N> pointAt = doc.locate(location);

    // apply with bias
    leftSide.each(new ProcV<Object>() {
      @Override
      public void apply(String key, Object value) {
        if (caret.hasAnnotation(key)) {
          return; // skip, user already set one.
        }

        boolean inheritFromRight = false;
        InheritDirection direction = InheritDirection.INSIDE;
        Iterator<AnnotationBehaviour> behaviours = registry.getBehaviours(key);
        if (!behaviours.hasNext()) {
          // no behaviour defined, so inherit from right if explicitly set:
          inheritFromRight = (bias == BiasDirection.RIGHT);
        } else {
          // Check which side to inherit from
          direction = behaviours.next().replace(rightSide, leftSide, type);
          inheritFromRight = shouldInheritFromRight(bias, direction);
        }

        // Supplement annotations, either from right or with null:
        if (inheritFromRight) {
          String newValue = Annotations.getAlignedAnnotation(doc, location, key, false);
          String oldValue = doc.getAnnotation(location - 1, key);
          if (!ValueUtils.equal(newValue, oldValue)) {
            caret.setAnnotation(key, newValue);
          }
        } else if (direction == InheritDirection.NEITHER) {
          if (location > 0 && doc.getAnnotation(location - 1, key) != null) {
            caret.setAnnotation(key, null);
          }
        }
      }
    });
  }

  /** Fills leftSide, rightSide with key+values for annotations that change over the boundary. */
  private void buildBoundaryMaps(final int location) {
    // init to default state:
    leftSide.clear();
    rightSide.clear();
    final StringMap<String> leftValues = CollectionUtils.createStringMap();
    final StringMap<String> rightValues = CollectionUtils.createStringMap();
    final StringSet keysToCheck = CollectionUtils.createStringSet();

    // collection up non-null annotations on both sides
    if (location > 0) {
      doc.forEachAnnotationAt(location - 1, new ProcV<String>() {
        public void apply(String key, String value) {
          if (value != null) {
            leftValues.put(key, value);
            keysToCheck.add(key);
          }
        }
      });
    }
    if (location < doc.size()) {
      doc.forEachAnnotationAt(location, new ProcV<String>() {
        public void apply(String key, String value) {
          if (value != null) {
            rightValues.put(key, value);
            keysToCheck.add(key);
          }
        }
      });
    }

    // fill in values that change
    keysToCheck.each(new Proc() {
      public void apply(String key) {
        String left = leftValues.get(key);
        String right = rightValues.get(key);
        if (ValueUtils.notEqual(left, right)) {
          leftSide.put(key, left);
          rightSide.put(key, right);
        }
      }
    });
  }

  /** Calculates initial bias from container elements. */
  private BiasDirection biasFromContainers(Point<N> at) {
    // TODO(patcoleman): allow for more container registry.
    boolean atContainerStart = LineContainers.isAtLineStart(doc, at);
    boolean atContainerEnd = LineContainers.isAtLineEnd(doc, at);

    // Logic:
    //   if only at the end or only at the start, bias inwards.
    //   otherwise, don't specialise any bias.
    if (atContainerStart && atContainerEnd) {
      return BiasDirection.NEITHER;
    } else if (atContainerStart) {
      return BiasDirection.RIGHT;
    } else if (atContainerEnd) {
      return BiasDirection.LEFT;
    } else {
      return BiasDirection.NEITHER;
    }
  }

  /** Utility that takes a bias and inheritence, then returns whether to inherit from the right. */
  private boolean shouldInheritFromRight(BiasDirection bias, InheritDirection inherit) {
    switch (inherit) {
      case INSIDE:
        if (bias == BiasDirection.RIGHT) {
          // inherit from inside, which is the character to the right.
          return true;
        }
        break;
      case OUTSIDE:
        if (bias == BiasDirection.LEFT || bias == BiasDirection.NEITHER) {
          // inherit from outside, which is the character to the right
          return true;
        }
        break;
    }
    return false; // otherwise, the desired behaviour is what the model will do for us.
  }
}
