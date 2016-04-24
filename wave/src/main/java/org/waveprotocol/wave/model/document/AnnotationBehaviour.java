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

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.util.StringMap;

public interface AnnotationBehaviour {
  /** Bias priority for 'container' elements, which bias inwards. */
  public static double ELEMENT_PRIORITY = 5.0;

  /** Bias priority for the default behaviour, placing it behind elements. */
  public static double DEFAULT_PRIORITY = 1.0;

  /** Cursor movements which may affect bias. */
  public enum CursorDirection {
    /** Last movement was to the left. */
    FROM_LEFT,
    /** Last movement was to the right. */
    FROM_RIGHT,
    /** Last movement was unknown or doesn't affect bias. */
    NEUTRAL;

    /** Converts the last cursor movement into a default bias. */
    public static BiasDirection toBiasDirection(CursorDirection cursor) {
      switch (cursor) {
        case FROM_LEFT:
          return BiasDirection.LEFT;
        case FROM_RIGHT:
          return BiasDirection.RIGHT;
      }
      return BiasDirection.NEITHER;
    }
  }

  /** Which 'side' of the gap the cursor is in. */
  public enum BiasDirection {
    /** A)  B -- default = assoicated with the previous character */
    LEFT,
    /** A  (B -- associated with the next character. */
    RIGHT,
    /** A | B -- right in the middle = unknown. */
    NEITHER;
  }

  /** Which 'side' of the cursor to inherit the annotation from. */
  public enum InheritDirection {
    /** inherit the annotation from the side the cursor is biassed. */
    INSIDE,
    /** inherit the annotation from the side other side to which the cursor is biassed. */
    OUTSIDE,
    /** do not inherit annotation (null is used instead). */
    NEITHER;
  }

  /** If required, indicates what type of content is being inserted and may require annotations. */
  public enum ContentType {
    /** normal text, unstyled. */
    PLAIN_TEXT,
    /** rich text with its own stylings. */
    RICH_TEXT,
  }

  /**
   * Enumerates the semantic families of annotations. An example usage of this is for
   *   copying a document, deciding what annotations should be brought along.
   */
  public enum AnnotationFamily {
    /** Part of the core document model - e.g. font colour or links. */
    CONTENT,
    /** Metadata that adds extra information on top of the document - e.g. spell suggestions. */
    META,
  }

  /**
   * Shows the priority of this behaviour - behaviours with lower priority value get precedence
   * in setting the bias direction.
   */
  double getPriority();

  /**
   * Indicates what type of family this behaviour is defined over.
   */
  AnnotationFamily getAnnotationFamily();

  /**
   * Defines the desired bias of the cursor given the current state of annotations.
   *
   * @param left Key-Value mappings for annotations to the left of the cursor.
   * @param right Key-Value mappings for annotations to the right of the cursor.
   * @param cursor last known value for the cursor movement.
   * @return Desired bias direction.
   */
  BiasDirection getBias(StringMap<Object> left, StringMap<Object> right, CursorDirection cursor);

  /**
   * Defines which side of the cursor to inherit annotations from, given the current state of
   * annotations and some information about what the annotations are being set over.
   * Note that in the case of ranged selections being replaced, the cursor is considered to have
   * the same annotations as the first character of the selected range.
   *
   * @param inside Key-Value mappings for annotations inside the cursor.
   * @param outside Key-Value mappings for annotations outside the cursor.
   * @param type Type of content being replaced
   * @return Desired inheritence behaviour.
   */
  InheritDirection replace(StringMap<Object> inside, StringMap<Object> outside, ContentType type);

  /**
   * Defines the default model behaviour for annotations - that is, does not specify a bias,
   * replacement always comes from inside the cursor, and is prioritised behind elements.
   */
  public static class DefaultAnnotationBehaviour implements AnnotationBehaviour {
    private final AnnotationFamily family;
    public DefaultAnnotationBehaviour(AnnotationFamily family) {
      this.family = family;
    }

    @Override
    public BiasDirection getBias(StringMap<Object> left, StringMap<Object> right,
        CursorDirection cursor) {
      return BiasDirection.NEITHER;
    }
    @Override
    public double getPriority() {
      return DEFAULT_PRIORITY;
    }
    @Override
    public InheritDirection replace(StringMap<Object> inside, StringMap<Object> outside,
        ContentType type) {
      // Default behaviour: inherit, unless rich text in which case leave formatting as-is
      return type == ContentType.RICH_TEXT ? InheritDirection.NEITHER : InheritDirection.INSIDE;
    }
    @Override
    public AnnotationFamily getAnnotationFamily() {
      return family;
    }
  }

  /**
   * One of the standard meta-data annotation behaviours. This follows DefaultAnnotation
   * behaviour, except when selection is replaced, this does not inherit the
   * annotation from either side.
   */
  public static final AnnotationBehaviour REPLACE_NEITHER =
      new DefaultAnnotationBehaviour(AnnotationFamily.META) {
    @Override
    public InheritDirection replace(StringMap<Object> inside, StringMap<Object> outside,
        ContentType type) {
      return InheritDirection.NEITHER;
    }
  };
}
