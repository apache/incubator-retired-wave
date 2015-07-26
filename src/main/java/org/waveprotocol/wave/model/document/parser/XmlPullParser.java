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

package org.waveprotocol.wave.model.document.parser;

import org.waveprotocol.wave.model.util.StringMap;

import java.util.NoSuchElementException;

/**
 * Xml pull parser interface that throws checked exceptions while parsing when
 * malformed xml is encountered.
 *
 */
public interface XmlPullParser {
  /**
   * @return if the parser has more tokens.
   */
  boolean hasNext();

  /**
   * Move to the next token
   * @return The type of item we are at
   * @throws XmlParseException if we encounter malformed xml
   */
  ItemType next() throws XmlParseException, NoSuchElementException;

  /**
   * @return Type of item at current position
   */
  ItemType getCurrentType();

  /**
   * @return character data at current position; only valid when we are at a
   *         text item
   * @throws IllegalArgumentException if the cursor is not at a text node.
   */
  String getText() throws IllegalStateException;

  /**
   * @return tag name for current start element; not valid over other items
   * @throws IllegalStateException if the cursor is not at an element start/end
   */
  String getTagName() throws IllegalStateException;

  /**
   * @return attributes for current start element; not valid over other items
   *
   *         NOTE(user): This returns a new map and the implementation should
   *         hold no reference to it. Callers may use this directly without
   *         making defensive copies.
   * @throws IllegalStateException if the cursor is not at an element start.
   */
  StringMap<String> getAttributes() throws IllegalStateException;

  /**
   * @return the name of the current processing instruction.
   * @throws IllegalStateException if the cursor is not at a processing
   *         instruction.
   */
  String getProcessingInstructionName() throws IllegalStateException;

  /**
   * @return the value of the current processing instruction
   * @throws IllegalStateException if the cursor is not at a processing
   *         instruction
   */
  String getProcessingInstructionValue() throws IllegalStateException;
}
