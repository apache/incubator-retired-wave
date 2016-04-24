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

import java.io.Serializable;


/*
 * Annotation is an immutable key-value pair of metadata over a range of
 * content.  Annotation can be used to store data or to be interpreted by a
 * client when displaying the data.
 *
 * Example uses of annotation include styling text, supplying spelling
 * corrections, and links to refer that area of text to another document or
 * web site.
 */
public class Annotation implements Serializable {

  /** Some constants for style related annotation keys. */
  public static final String BACKGROUND_COLOR = "style/backgroundColor";
  public static final String COLOR = "style/color";
  public static final String FONT_FAMILY = "style/fontFamily";
  public static final String FONT_SIZE = "style/fontSize";
  public static final String FONT_STYLE = "style/fontStyle";
  public static final String FONT_WEIGHT = "style/fontWeight";
  public static final String TEXT_DECORATION = "style/textDecoration";
  public static final String VERTICAL_ALIGN = "style/verticalAlign";
  public static final String LINK = "link/manual";
  public static final String WAVE_LINK = "link/wave";

  /** The annotation name. */
  private final String name;

  /** The annotation value. */
  private final String value;

  /** The range of this annotation. */
  private Range range;

  /**
   * Constructor.
   *
   * @param name the name of this annotation.
   * @param value the value of this annotation.
   * @param start the starting index of this annotation.
   * @param end the end index of this annotation.
   */
  public Annotation(String name, String value, int start, int end) {
    this.name = name;
    this.value = value;
    this.range = new Range(start, end);
  }

  /**
   * Constructor.
   *
   * @param name the name of this annotation.
   * @param value the value of this annotation.
   * @param range the range of this annotation.
   */
  public Annotation(String name, String value, Range range) {
    this.name = name;
    this.value = value;
    this.range = new Range(range.getStart(), range.getEnd());
  }

  /**
   * Returns the name of this annotation.
   *
   * @return the annotation's name.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the value of this annotation.
   *
   * @return the annotation's value.
   */
  public String getValue() {
    return value;
  }

  /**
   * Returns the range of this annotation.
   *
   * @return the annotation range.
   */
  public Range getRange() {
    return range;
  }

  /**
   * Shifts this annotation by {@code shiftAmount} if it is on a range that
   * is after or covers the given position.
   *
   * @param position the anchor position.
   * @param shiftAmount the amount to shift the annotation range.
   */
  public void shift(int position, int shiftAmount) {
    int start = range.getStart();
    if (start >= position) {
      start += shiftAmount;
    }

    int end = range.getEnd();
    if (end >= position) {
      end += shiftAmount;
    }
    range = new Range(start, end);
  }

  @Override
  public String toString() {
    StringBuilder res = new StringBuilder("Annotation(");
    if (name != null) {
      res.append(name);
      res.append(',');
    }
    if (value != null) {
      res.append(value);
      res.append(',');
    }
    res.append(range.toString());
    return res.toString();
  }
}
