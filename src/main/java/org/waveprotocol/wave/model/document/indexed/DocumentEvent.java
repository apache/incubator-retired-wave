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

package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * Document event algebraic type.
 *
 * WARNING(danilatos): .equals() equality may result in distinct events
 * being treated as equal in some circumstances, such as two bits of deleted
 * content that lived adjacently and had identical xml. Two possible ways to
 * resolve this is to give them a sequence number, or just use object identity
 * for equality. Either would be inconsistent with the nice behaviour of
 * .equals() for the inserting methods. I'm not sure what the best answer is,
 * but for now it means DON'T put the events in a HashSet or similar.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <N>
 * @param <E>
 * @param <T>
 */
public abstract class DocumentEvent<N, E extends N, T extends N> {

  public enum Type {
    ATTRIBUTES,
    TEXT_INSERTED,
    CONTENT_INSERTED,
    TEXT_DELETED,
    CONTENT_DELETED,
    ANNOTATION_CHANGED,
  }

  private final Type type;

  private DocumentEvent(Type type) {
    this.type = type;
  }

  public Type getType() {
    return type;
  }

  /**
   * Describes element attribute modifications
   *
   * Redundant changes (where an attribute is set to its same value) are omitted
   */
  public static final class AttributesModified<N, E extends N, T extends N>
      extends DocumentEvent<N, E, T> {
    private final E element;
    private final Map<String, String> oldValues;
    private final Map<String, String> newValues;

    public AttributesModified(E element,
        Map<String, String> oldValues, Map<String, String> newValues) {
      super(Type.ATTRIBUTES);
      this.element = element;
      HashMap<String, String> oldV = new HashMap<String, String>(oldValues);
      HashMap<String, String> newV = new HashMap<String, String>(newValues);
      List<String> keysToRemove = new ArrayList<String>();
      for (Map.Entry<String, String> entry : oldV.entrySet()) {
        String key = entry.getKey();
        if (!newV.containsKey(key)) {
          newV.put(key, null);
        } else if (ValueUtils.equal(newV.get(key), entry.getValue())) {
          newV.remove(key);
          keysToRemove.add(key);
        }
      }
      for (String key : keysToRemove) {
        oldV.remove(key);
      }
      for (Map.Entry<String, String> entry : newV.entrySet()) {
        if (!oldV.containsKey(entry.getKey())) {
          oldV.put(entry.getKey(), null);
        }
      }
      this.oldValues = Collections.unmodifiableMap(oldV);
      this.newValues = Collections.unmodifiableMap(newV);
    }

    public AttributesModified(E element, AttributesUpdate update) {
      super(Type.ATTRIBUTES);
      this.element = element;
      HashMap<String, String> oldV = new HashMap<String, String>();
      HashMap<String, String> newV = new HashMap<String, String>();

      for (int i = 0; i < update.changeSize(); i++) {
        String oldValue = update.getOldValue(i);
        String newValue = update.getNewValue(i);
        if (ValueUtils.notEqual(newValue, oldValue)) {
          oldV.put(update.getChangeKey(i), oldValue);
          newV.put(update.getChangeKey(i), newValue);
        }
      }

      this.oldValues = Collections.unmodifiableMap(oldV);
      this.newValues = Collections.unmodifiableMap(newV);
    }

    public E getElement() {
      return element;
    }

    /**
     * Returns a map of names to their previous values.
     *
     * The map contains only attributes that changed, and for newly added attributes,
     * the "old" value will be presented as null.
     *
     * @return map of names to old values
     */
    // TODO(danilatos): Change getOldValues() and getNewValues() to an attributes update map?
    public Map<String, String> getOldValues() {
      return oldValues;
    }

    /**
     * Returns a map of names to their current values.
     *
     * The map contains only attributes that changed, and for removed attributes,
     * the new "value" will be presented as null.
     *
     * TODO(danilatos): Remove this method? It is convenient, but redundant with
     * respect to {@link #getOldValues()} and {@link #getElement()}.
     * Alternatively, the new values could be lazily computed.
     *
     * @return map of names to old values
     */
    public Map<String, String> getNewValues() {
      return newValues;
    }

    /**
     * Returns the set of attribute names whose values have changed.
     *
     * This is equivalent to the key set of either {@link #getNewValues()}
     * or {@link #getOldValues()} (they are the same).
     *
     * TODO(danilatos): Remove this method? It is redundant but possibly convenient.
     *
     * @return set of attribute names whose values have changed
     */
    public Set<String> getChangedAttributes() {
      return oldValues.keySet();
    }

    // eclipse generated, clean up later

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((element == null) ? 0 : element.hashCode());
      result = prime * result + ((newValues == null) ? 0 : newValues.hashCode());
      result = prime * result + ((oldValues == null) ? 0 : oldValues.hashCode());
      return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof AttributesModified)) return false;
      AttributesModified other = (AttributesModified) obj;
      if (element == null) {
        if (other.element != null) return false;
      } else if (!element.equals(other.element)) return false;
      if (newValues == null) {
        if (other.newValues != null) return false;
      } else if (!newValues.equals(other.newValues)) return false;
      if (oldValues == null) {
        if (other.oldValues != null) return false;
      } else if (!oldValues.equals(other.oldValues)) return false;
      return true;
    }

    @Override
    public String toString() {
      return "A:" + oldValues + "->" + newValues;
    }
  }

  /**
   * Event describing top-level text insertion (no structural content)
   */
  public static final class TextInserted<N, E extends N, T extends N>
      extends DocumentEvent<N, E, T> {
    public final int location;
    public final String insertedText;
    public TextInserted(int location, String insertedText) {
      super(Type.TEXT_INSERTED);
      this.location = location;
      this.insertedText = insertedText;
    }
    public int getLocation() {
      return location;
    }
    public String getInsertedText() {
      return insertedText;
    }

    // eclipse generated, clean up later

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((insertedText == null) ? 0 : insertedText.hashCode());
      result = prime * result + location;
      return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof TextInserted)) return false;
      TextInserted other = (TextInserted) obj;
      if (insertedText == null) {
        if (other.insertedText != null) return false;
      } else if (!insertedText.equals(other.insertedText)) return false;
      if (location != other.location) return false;
      return true;
    }

    @Override
    public String toString() {
      return "TI:" + insertedText + "@" + location;
    }
  }

  /**
   * Event describing structural content being inserted. May contain text,
   * and any such nested text will not be reported in a TextInserted event.
   */
  public static final class ContentInserted<N, E extends N, T extends N>
      extends DocumentEvent<N, E, T> {
    private final E element;

    public ContentInserted(E element) {
      super(Type.CONTENT_INSERTED);
      this.element = element;
    }

    public E getSubtreeElement() {
      return element;
    }

    // eclipse generated, clean up later

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((element == null) ? 0 : element.hashCode());
      return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof ContentInserted)) return false;
      ContentInserted other = (ContentInserted) obj;
      if (element == null) {
        if (other.element != null) return false;
      } else if (!element.equals(other.element)) return false;
      return true;
    }

    @Override
    public String toString() {
      return "CI:" + element;
    }
  }

  /**
   * Event describing top-level text deletion (no structural content)
   */
  public static final class TextDeleted<N, E extends N, T extends N>
      extends DocumentEvent<N, E, T> {
    public final int location;
    public final String deletedText;

    public TextDeleted(int location, String deletedText) {
      super(Type.TEXT_DELETED);
      this.location = location;
      this.deletedText = deletedText;
    }
    public int getLocation() {
      return location;
    }
    public String getDeletedText() {
      return deletedText;
    }

    // eclipse generated, clean up later

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((deletedText == null) ? 0 : deletedText.hashCode());
      result = prime * result + location;
      return result;
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof TextDeleted)) return false;
      TextDeleted other = (TextDeleted) obj;
      if (deletedText == null) {
        if (other.deletedText != null) return false;
      } else if (!deletedText.equals(other.deletedText)) return false;
      if (location != other.location) return false;
      return true;
    }

    @Override
    public String toString() {
      return "TD:" + deletedText + "@" + location;
    }
  }

  /**
   * Event describing structural content being deleted. May deleted text,
   * and any such nested text will not be reported in a TextDeleted event.
   */
  public static final class ContentDeleted<N, E extends N, T extends N>
      extends DocumentEvent<N, E, T> {

    public static enum TokenType { TEXT, ELEMENT_START, ELEMENT_END }

    /**
     * Representation of a single part of the deleted content. Each removed
     * element start tag, text string, and element end tag is represented by a
     * single Token.
     */
    public static final class Token {
      private final TokenType type;
      private final String tagName;
      private final Map<String, String> attributes;
      private final String text;

      private Token(TokenType type, String tagName, Map<String, String> attributes, String text) {
        this.type = type;
        this.tagName = tagName;
        this.attributes = attributes;
        this.text = text;
      }

      static Token textToken(String text) {
        return new Token(TokenType.TEXT, null, null, text);
      }

      static Token elementStartToken(String tagName, Map<String, String> attributes) {
        return new Token(TokenType.ELEMENT_START, tagName, attributes, null);
      }

      static Token elementEndToken(String tagName) {
        return new Token(TokenType.ELEMENT_END, tagName, null, null);
      }

      public TokenType getType() {
        return type;
      }

      public String getTagName() {
        return tagName;
      }

      public Map<String, String> getAttributes() {
        return attributes;
      }

      public String getText() {
        return text;
      }

      @Override
      public String toString() {
        if (text != null) {
          return text;
        } else if (attributes != null) {
          StringBuilder b = new StringBuilder();
          b.append("<" + tagName);
          for (String key : attributes.keySet()) {
            b.append(" " + key + "=" + attributes.get(key));
          }
          b.append(">");
          return b.toString();
        }
        return "</" + tagName + ">";
      }

      @Override
      public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((tagName == null) ? 0 : tagName.hashCode());
        result = prime * result + ((text == null) ? 0 : text.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Token)) return false;
        Token other = (Token) obj;
        if (attributes == null) {
          if (other.attributes != null) return false;
        } else if (!attributes.equals(other.attributes)) return false;
        if (tagName == null) {
          if (other.tagName != null) return false;
        } else if (!tagName.equals(other.tagName)) return false;
        if (text == null) {
          if (other.text != null) return false;
        } else if (!text.equals(other.text)) return false;
        if (type == null) {
          if (other.type != null) return false;
        } else if (!type.equals(other.type)) return false;
        return true;
      }
    }

    public final int location;
    private final int size;
    private final List<Token> tokens;
    private final E root;

    public static final class Builder<N, E extends N, T extends N> {
      private final int start;
      private int size;
      private final List<Token> tokens;
      private final Stack<String> tagNames;
      private final E root;

      public Builder(int start, E root) {
        this.start = start;
        this.size = 0;
        this.tokens = new ArrayList<Token>();
        this.tagNames = new Stack<String>();
        this.root = root;
      }

      public ContentDeleted<N, E, T> build() {
        return new ContentDeleted<N, E, T>(start, size, tokens, root);
      }

      public void addText(String text) {
        tokens.add(Token.textToken(text));
        this.size += text.length();
      }

      public void addElementStart(String tagName, Map<String, String> elements) {
        tokens.add(Token.elementStartToken(tagName, elements));
        tagNames.add(tagName);
        this.size++;
      }

      public void addElementEnd() {
        tokens.add(Token.elementEndToken(tagNames.pop()));
        this.size++;
      }
    }

    ContentDeleted(int location, int size, List<Token> tokens, E root) {
      super(Type.CONTENT_DELETED);
      this.location = location;
      this.size = size;
      assert tokens != null;
      this.tokens = tokens;
      this.root = root;
    }

    /** Location in the NEW document where the deletion occurred. */
    public int getLocation() {
      return location;
    }

    /**
     * The topmost element that was deleted.
     */
    public E getRoot() {
      return root;
    }

    public int getItemSize() {
      return size;
    }

    public Iterable<Token> getDeletedTokens() {
      return tokens;
    }

    // eclipse generated, clean up later

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + location;
      result = prime * result + size;
      result = prime * result + tokens.hashCode();
      return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (!(obj instanceof ContentDeleted)) return false;
      ContentDeleted other = (ContentDeleted) obj;
      if (location != other.location) return false;
      if (size != other.size) return false;
      if (!tokens.equals(other.tokens)) return false;
      return true;
    }

    @Override
    public String toString() {
      String content = "CD:" + "@" + location + "-" + size + " [";
      for (Token token : tokens) {
        content += token.toString();
      }
      content += "]";
      return content;
    }
  }

  /**
   * Event describing an annotation change.
   */
  public static final class AnnotationChanged<N, E extends N, T extends N>
      extends DocumentEvent<N, E, T> {
    /** Start of the changed range */
    public final int start;
    /** End of the changed range (one past the last affected item) */
    public final int end;
    /** Key with changed value */
    public final String key;
    /** New value for the key over the range */
    public final String newValue;

    public AnnotationChanged(int start, int end, String key, String newValue) {
      super(Type.ANNOTATION_CHANGED);
      this.start = start;
      this.end = end;
      this.key = key;
      this.newValue = newValue;
    }

    @Override
    public String toString() {
      return "AC:@(" + start + "," + end + "):[" + key + ":" + newValue + "]";
    }
  }
}
