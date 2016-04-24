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

package org.waveprotocol.wave.model.richtext;

import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema.PermittedCharacters;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.util.ImmutableStateMap.Attribute;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.richtext.RichTextTokenizer.Type;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Builds up a sequence of operations from DOM that has been parsed with
 * a RichTextTokenizer.
 *
 */
public class RichTextMutationBuilder {
  // TODO(user): Centralize these constants.
  private static final String TYPE_ATTR = "t";

  private static final String LI_STYLE_ATTR = "listyle";

  private static final String INDENT_ATTR = "i";

  /** Default annotation values for certain keys */
  private static final StringMap<ReadableStringSet> defaultAnnotations =
      CollectionUtils.createStringMap();

  static {
    defaultAnnotations.put(AnnotationConstants.STYLE_TEXT_DECORATION, CollectionUtils.newStringSet("none"));
    defaultAnnotations.put(AnnotationConstants.STYLE_FONT_WEIGHT, CollectionUtils.newStringSet("normal"));
    defaultAnnotations.put(AnnotationConstants.STYLE_FONT_STYLE, CollectionUtils.newStringSet("normal"));
    defaultAnnotations.put(AnnotationConstants.STYLE_BG_COLOR, CollectionUtils.newStringSet("initial",
        "transparent"));
    // Default font family and color are dependent on user-agent settings, but
    // make up a default color anyway
    defaultAnnotations.put(AnnotationConstants.STYLE_COLOR, CollectionUtils.newStringSet("black"));
  }

  /** This map records all annotations currently started. */
  private final Map<String, Stack<String>> startedAnnotations =
      new HashMap<String, Stack<String>>();

  /** The current delta offset. */
  private int offset;

  /** Current indentation level */
  private final List<Type> structureStack = new ArrayList<Type>();

  /** Count of open elements. */
  private int elementCount;

  /** Last valid cursor position relative from the start. */
  private int lastGoodCursorOffset;

  private int indentationLevel = 0;

  private boolean isFirstToken;

  /**
   * Annotation keys that have been touched.
   */
  private final StringSet affectedKeys = CollectionUtils.createStringSet();

  /** Mapping of key/value pairs to use when particular keys have no mappings in the builder. */
  private final StringMap<String> defaultValueMap;

  public RichTextMutationBuilder() {
    this(CollectionUtils.<String>createStringMap());
  }

  /**
   * @param defaultValueMap annotation (key, value) pairs that are to be used
   *        when no explicit annotation value is set for that key.
   */
  public RichTextMutationBuilder(StringMap<String> defaultValueMap) {
    this.defaultValueMap = defaultValueMap;
    reset();
  }


  private final void reset() {
    offset = 0;
    structureStack.clear();
    elementCount = 0;
    lastGoodCursorOffset = 0;
    startedAnnotations.clear();
    affectedKeys.clear();
    isFirstToken = true;
  }

  /**
   * Returns the offset location that is safe to place the cursor. It is
   * relative from the current location when mutations were applied. For
   * example, if <p></p> is inserted, the full offset would be 2 while this
   * method would return 1 (to be placed within the p).
   *
   * @return A good location offset.
   */
  public int getLastGoodCursorOffset() {
    return lastGoodCursorOffset;
  }

  /**
   * Applies the mutations to a document mutation builder based on a tokenized
   * stream of rich text. Assumes that the current location of the document
   * mutation builder is inside of a <p> tag. Returns a delta offset from
   * the current location typically used to place the caret at the location of
   * the last mutation.
   *
   * @param <N> Node type.
   * @param <E> Element type.
   * @param <T> Text type.
   * @param tokenizer The processed tokenizer.
   * @param builder The Nindo builder.
   * @param doc Readable document used to get element information.
   * @param splitContainer In case of an initial split, this is the container
   *        to use.
   * @return The annotation keys that have been affected.
   */
  public <N, E extends N, T extends N> ReadableStringSet applyMutations(RichTextTokenizer tokenizer,
      Nindo.Builder builder,
      ReadableDocument<N, E, T> doc,
      N splitContainer) {
    reset();

    while (tokenizer.hasNext()) {
      Type tokenType = tokenizer.next();

      switch (tokenType) {
        case NEW_LINE:
        case LIST_ITEM:
          handleNewLine(tokenizer, builder, doc, splitContainer,
              structuralAttributes(tokenizer));
          break;
        default:
          handleBasicMutation(tokenizer, builder, doc, splitContainer);
          break;
      }
      isFirstToken = false;
    }

    // Make sure all annotations have been ended.
    for (String key : startedAnnotations.keySet()) {
      builder.endAnnotation(key);
    }

    lastGoodCursorOffset = offset;

    // Close any remaining open tags. Not adding this to offset because we want
    // the cursor to remain.
    for (int i = 0; i < elementCount; ++i) {
      builder.elementEnd();
      ++offset;
    }
    return affectedKeys;
  }

  private Attributes structuralAttributes(RichTextTokenizer tokenizer) {
    Type currentStructureType = peek();
    List<Attribute> attrList = new ArrayList<Attribute>();

    if (currentStructureType != null) {
      int indent = indentationLevel;

      if (tokenizer.getCurrentType() == Type.LIST_ITEM) {
        switch (currentStructureType) {
        case UNORDERED_LIST_START:
          indent--;
          attrList.add(new Attribute(TYPE_ATTR, "li"));
          break;
        case ORDERED_LIST_START:
          indent--;
          attrList.add(new Attribute(TYPE_ATTR, "li"));
          attrList.add(new Attribute(LI_STYLE_ATTR, "decimal"));
          break;
        }
      } else if (tokenizer.getData() != null) {
        attrList.add(new Attribute(TYPE_ATTR, tokenizer.getData()));
      }

      if (indent > 0) {
        attrList.add(new Attribute(INDENT_ATTR, "" + indent));
      }
    }

    AttributesImpl attributes = AttributesImpl.fromUnsortedAttributes(attrList);

    return attributes;
  }

  private <N, E extends N, T extends N> void handleBasicMutation(RichTextTokenizer tokenizer,
      Nindo.Builder builder, ReadableDocument<N, E, T> doc, N splitContainer) {
    Type currentType = tokenizer.getCurrentType();
    switch (currentType) {
      case TEXT:
        if (tokenizer.getData() != null) {
          builder.characters(PermittedCharacters.BLIP_TEXT.coerceString(tokenizer.getData()));
          offset += tokenizer.getData().length();
        }
        break;
      case STYLE_FONT_WEIGHT_START:
        startAnnotation(builder, AnnotationConstants.STYLE_FONT_WEIGHT, tokenizer.getData());
        break;
      case STYLE_FONT_WEIGHT_END:
        endAnnotation(builder, AnnotationConstants.STYLE_FONT_WEIGHT);
        break;
      case STYLE_FONT_STYLE_START:
        startAnnotation(builder, AnnotationConstants.STYLE_FONT_STYLE, tokenizer.getData());
        break;
      case STYLE_FONT_STYLE_END:
        endAnnotation(builder, AnnotationConstants.STYLE_FONT_STYLE);
        break;
      case STYLE_TEXT_DECORATION_START:
        startAnnotation(builder, AnnotationConstants.STYLE_TEXT_DECORATION, tokenizer.getData());
        break;
      case STYLE_TEXT_DECORATION_END:
        endAnnotation(builder, AnnotationConstants.STYLE_TEXT_DECORATION);
        break;
      case STYLE_COLOR_START:
        startAnnotation(builder, AnnotationConstants.STYLE_COLOR, tokenizer.getData());
        break;
      case STYLE_COLOR_END:
        endAnnotation(builder, AnnotationConstants.STYLE_COLOR);
        break;
      case STYLE_BG_COLOR_START:
        startAnnotation(builder, AnnotationConstants.STYLE_BG_COLOR, tokenizer.getData());
        break;
      case STYLE_BG_COLOR_END:
        endAnnotation(builder, AnnotationConstants.STYLE_BG_COLOR);
        break;
      case STYLE_FONT_FAMILY_START:
        startAnnotation(builder, AnnotationConstants.STYLE_FONT_FAMILY, tokenizer.getData());
        break;
      case STYLE_FONT_FAMILY_END:
        endAnnotation(builder, AnnotationConstants.STYLE_FONT_FAMILY);
        break;
      case LINK_START:
        startAnnotation(builder, AnnotationConstants.LINK_MANUAL, tokenizer.getData());
        break;
      case LINK_END:
        endAnnotation(builder, AnnotationConstants.LINK_MANUAL);
        break;
      case UNORDERED_LIST_START:
      case ORDERED_LIST_START:
        push(currentType);
        break;
      case UNORDERED_LIST_END:
      case ORDERED_LIST_END:
        pop();
        break;
      default:
        throw new IllegalStateException("Unhandled token: " +
            currentType.toString());
    }
  }

  private void push(Type type) {
    structureStack.add(type);
    indentationLevel += type.indent();
  }

  private void pop() {
    if (structureStack.size() > 0) {
      indentationLevel -= structureStack.remove(structureStack.size() - 1).indent();
    }
  }

  private Type peek() {
    if (structureStack.size() == 0) {
      return null;
    }
    return structureStack.get(structureStack.size() - 1);
  }

  private void startAnnotation(Nindo.Builder builder, String annotationKey,
      String annotationValue) {
    Stack<String> annotationStack = startedAnnotations.get(annotationKey);
    if (annotationStack == null) {
      annotationStack = new Stack<String>();
      startedAnnotations.put(annotationKey, annotationStack);
      affectedKeys.add(annotationKey);
    }
    String current = annotationStack.isEmpty() ? null : annotationStack.peek();
    // Avoid no-ops
    if (ValueUtils.notEqual(annotationValue, current)) {
      if (current == null && isDefaultValue(annotationKey, annotationValue)) {
        // If the current annotation is the default, and the new annotation
        // value is also the default, we don't need to set the annotation value
      } else {
        builder.startAnnotation(annotationKey, annotationValue);
      }
    }
    annotationStack.push(annotationValue);
  }

  private void endAnnotation(Nindo.Builder builder, String annotationKey) {
    Stack<String> annotationStack = startedAnnotations.get(annotationKey);
    Preconditions.checkNotNull(annotationStack, "cannot end unstarted annotation");

    String current = annotationStack.pop();

    // If there are no more entries, and the current annotation is non-null.
    if (annotationStack.isEmpty() && current != null) { // avoid no-ops
      if (!isDefaultValue(annotationKey, current)) {
        builder.endAnnotation(annotationKey);
      }

      // Conditionally null out certain keys if they are in a special list
      if (defaultValueMap.containsKey(annotationKey)) {
        builder.startAnnotation(annotationKey, defaultValueMap.get(annotationKey));
      } else {
        startedAnnotations.remove(annotationKey);
      }
    } else {
      String nextAnnotation = annotationStack.peek();
      if (ValueUtils.notEqual(current, nextAnnotation)) {
        // There's another entry in the stack and it is different from the
        // current-
        // If the entry in the stack is the same as default, just end the annotation.
        // Otherwise, start the next one.
        if (isDefaultValue(annotationKey, nextAnnotation)) {
          builder.endAnnotation(annotationKey);
        } else {
          builder.startAnnotation(annotationKey, nextAnnotation);
        }
      }
    }
  }

  private void startElement(Nindo.Builder builder, String tagName,
      Attributes attributes) {
    ++elementCount;
    builder.elementStart(tagName, attributes);
    ++offset;
  }

  private void endElement(Nindo.Builder builder) {
    if (--elementCount < 0) {
      throw new IllegalStateException("Element count is negative.");
    }
    builder.elementEnd();
    ++offset;
  }

  // TODO(user): Consider and handle the case where the current container might
  // not be a line container (i.e. inside a caption)
  private <N, E extends N, T extends N> void handleNewLine(RichTextTokenizer tokenizer,
      Nindo.Builder builder, ReadableDocument<N, E, T> doc, N splitContainer,
      Attributes attributes) {

    // HACK(user): splitContainer is the container at insertion point. We
    // only want to append new lines if we are pasting into a line container.
    // There are more cases, i.e. if we are pasting in a caption, but that
    // doesn't happen at the moment. This catches most cases, and the problem
    // should go away when we stop using Nindo.Builder and use
    // mutable document instead.
    if (lcCanHandleNewLine(doc, splitContainer)) {
      lcHandleNewLine(tokenizer, builder, doc, splitContainer, attributes);
    }
  }

  private <N, E extends N, T extends N> void lcHandleNewLine(RichTextTokenizer tokenizer,
      Nindo.Builder builder, ReadableDocument<N, E, T> doc, N splitContainer,
      Attributes attributes) {
    // TODO(user): Don't create a new paragraph if the attributes are the same,
    // and is first token.
    boolean isLastToken = !tokenizer.hasNext();
    if ((!isFirstToken && !isLastToken) || !attributes.isEmpty()) {
      startElement(builder, LineContainers.LINE_TAGNAME, attributes);
      endElement(builder);
    }
  }

  private <N, E extends N, T extends N> boolean lcCanHandleNewLine(ReadableDocument<N, E, T> doc,
      N container) {
    return LineContainers.isLineContainer(doc, Point.enclosingElement(doc, container));
  }

  private boolean isDefaultValue(String key, String value) {
    return defaultAnnotations.containsKey(key) && defaultAnnotations.get(key).contains(value);
  }
}
