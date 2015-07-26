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

import static org.waveprotocol.wave.model.richtext.RichTextTokenizer.Type.TypeGroup.BLOCK;
import static org.waveprotocol.wave.model.richtext.RichTextTokenizer.Type.TypeGroup.BLOCK_RANGE;
import static org.waveprotocol.wave.model.richtext.RichTextTokenizer.Type.TypeGroup.STYLE;
import static org.waveprotocol.wave.model.richtext.RichTextTokenizer.Type.TypeGroup.TEXTUAL;

import java.util.NoSuchElementException;

/**
 * Normalizes DOM into a linear stream of tokens, used primarily for pasting.
 * This is done in order to facilitate the inspection of HTML content that is
 * pasted for specific handlers such as semantically extracting styles as
 * annotations.
 *
 */
public interface RichTextTokenizer {
  /**
   * Set of allowed tokens. Start/end tokens will never be nested.
   *
   *  LAST_NEW_LINE exists to mark the final newline in the stream. This is
   * useful when dealing with trailing inline content.
   *
   *  TODO(user): Either replace LAST_NEW_LINE with a special method that
   * queries if the current token is the last one of its kind or some extra data
   * on the NEW_LINE token.
   *
   *  TODO(user): These should be registered from the annotation handlers
   * themselves... or at least not so hardcoded.
   */
  public enum Type {
    UNORDERED_LIST_START(BLOCK_RANGE, 1),
    UNORDERED_LIST_END(BLOCK_RANGE),
    ORDERED_LIST_START(BLOCK_RANGE, 1),
    ORDERED_LIST_END(BLOCK_RANGE),

    NEW_LINE(BLOCK),
    LIST_ITEM(BLOCK),

    TEXT(TEXTUAL),

    STYLE_FONT_WEIGHT_START(STYLE),
    STYLE_FONT_WEIGHT_END(STYLE),
    STYLE_FONT_STYLE_START(STYLE),
    STYLE_FONT_STYLE_END(STYLE),
    STYLE_TEXT_DECORATION_START(STYLE),
    STYLE_TEXT_DECORATION_END(STYLE),
    STYLE_COLOR_START(STYLE),
    STYLE_COLOR_END(STYLE),
    STYLE_BG_COLOR_START(STYLE),
    STYLE_BG_COLOR_END(STYLE),
    STYLE_FONT_FAMILY_START(STYLE),
    STYLE_FONT_FAMILY_END(STYLE),
    LINK_START(STYLE),
    LINK_END(STYLE);

    public enum TypeGroup {
      BLOCK_RANGE,
      BLOCK,
      TEXTUAL,
      STYLE;
    }

    private final TypeGroup group;
    private final int indent;

    Type(TypeGroup group) {
      this(group, -1);
    }

    Type(TypeGroup group, int indent) {
      this.group = group;
      this.indent = indent;
    }

    int indent() {
      assert indent >= 0;
      return indent;
    }

    TypeGroup group() {
      return group;
    }

    boolean isStructural() {
      return group != STYLE;
    }

    boolean isBlockLevel() {
      return group == BLOCK || group == BLOCK_RANGE;
    }
    // TODO(user): Add tables, etc.
  }

  /**
   * Returns true if the tokenizer has more tokens.
   *
   * @return true if there are more tokens to be read.
   */
  boolean hasNext();

  /**
   * Move to the next token and return the current type.
   *
   * @return the token type moved to.
   * @exception NoSuchElementException iteration has no more elements.
   */
  Type next();

  /**
   * Returns the type of the current token.
   *
   * @return the current token type.
   */
  Type getCurrentType();

  /**
   * Returns the data associated with the current token. Returns null if there
   * is no data available.
   *
   * @return token data associated with the current token.
   */
  String getData();

  /**
   * Returns a copy of this tokenizer.
   */
  RichTextTokenizer copy();
}
