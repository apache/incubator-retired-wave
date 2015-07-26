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

package org.waveprotocol.wave.model.document.operation.automaton;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationMap;

/**
 * The query methods that DocOpAutomaton needs.
 */
public interface AutomatonDocument {

  /**
   * If pos is at an element start, return that element's tag.  Else
   * return null.
   */
  String elementStartingAt(int pos);

  /**
   * If pos is at an element start, return that element's attributes.  Else
   * return null.
   */
  Attributes attributesAt(int pos);

  /**
   * If pos is at an element end, return that element's tag.  Else return
   * null.
   */
  String elementEndingAt(int pos);

  // -1 if no char (i.e., element start or end) at that position
  int charAt(int pos);

  // depth = 0 means enclosing element, depth = 1 means its parent,
  // etc.  null return value means nesting is not that deep.
  String nthEnclosingElementTag(int insertionPoint, int depth);

  // number of chars after insertionPoint before the next element
  // start or end or document end.
  int remainingCharactersInElement(int insertionPoint);

  AnnotationMap annotationsAt(int pos);
  String getAnnotation(int pos, String key);

  int length();

  /**
   * Finds the first item within a range with the specified key having a value
   * other than fromValue.
   *
   * @param start start of the range to search
   * @param end end of the range to search
   * @param key key to search for
   * @param fromValue value to check for change from
   * @return the location of the first change, or -1 if there is no change
   */
  int firstAnnotationChange(int start, int end, String key, String fromValue);

}