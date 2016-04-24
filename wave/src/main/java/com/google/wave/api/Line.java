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

package com.google.wave.api;

import java.util.Map;

/**
 * Represents a line within a Wave.
 */
public class Line extends Element {

  public static final String ALIGNMENT = "alignment";
  public static final String DIRECTION = "direction";
  public static final String INDENT = "indent";
  public static final String LINE_TYPE = "lineType";

  /**
   * Constructs an unstyled line.
   */
  public Line() {
    super(ElementType.LINE);
  }

  /**
   * Constructs a line with a given set of properties.
   *
   * @param properties the properties of the line.
   */
  public Line(Map<String, String> properties) {
    super(ElementType.LINE, properties);
  }

  /**
   * The type of the line. Allowed values are 'h1', 'h2', 'h3', 'h4',
   * 'h5', and 'li', where h[1-5] is a heading and 'li' an item in a list.
   *
   * @return the type of the line.
   */
  public String getLineType() {
    return getProperty(LINE_TYPE);
  }

  /**
   * Set the type of the line.
   */
  public void setLineType(String lineType) {
    setProperty(LINE_TYPE, lineType);
  }

  /**
   * The indentation level (0,1,2,...). This attribute is only meaningful when
   * applied to lines of type t="li".
   * @return the indent
   */
  public String getIndent() {
    return getProperty(INDENT);
  }

  /**
   * Set the indent for the line.
   */
  public void setIndent(String value) {
    setProperty(INDENT, value);
  }

  /**
   * The alignment of the text in the line. (a, m, r, j) a = align
   * left = centered = aligned right = justified
   * @return the alignment
   */
  public String getAlignment() {
    return getProperty(ALIGNMENT);
  }

  /**
   * Set the alignment for the line.
   */
  public void setAlignment(String attribute) {
    setProperty(ALIGNMENT, attribute);
  }

  /**
   * The display direction of the line l = left to right, r = right to left
   * @return the direction
   */
  public String getDirection() {
    return getProperty(DIRECTION);
  }

  /**
   * Set the direction for the line.
   */
  public void setDirection(String value) {
    setProperty(DIRECTION, value);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * line with the given alignment.
   *
   * @param alignment the alignment to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByAlignment(String alignment) {
    return Restriction.of(ALIGNMENT, alignment);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * line with the given direction.
   *
   * @param direction the direction to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByDirection(String direction) {
    return Restriction.of(DIRECTION, direction);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * line with the given indentation.
   *
   * @param indent the indentation to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByIndent(String indent) {
    return Restriction.of(INDENT, indent);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * line with the given line type.
   *
   * @param lineType the line type to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restricByLineType(String lineType) {
    return Restriction.of(LINE_TYPE, lineType);
  }
}
