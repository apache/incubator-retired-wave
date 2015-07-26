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

package org.waveprotocol.wave.client.editor.content.misc;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * Some helper methods.
 */
public class AnnotationHelper {
  /**
   * HACK(user): There appears to be things between the boundary node and the
   * annotated link. Find out what they are so we don't need this fudge.
   */
  private static final int POSITIONS_TO_LOOK_BACK = 3;

  /**
   * Private constructor- utility class.
   */
  private AnnotationHelper() {
  }

  /**
   * Gets the range of the annotation preceding the element with annotation
   * matching key.
   *
   * NOTE(user): The intention is to return only ranges immediately preceding
   * the element. However, we are looking back nearby for now (see above HACK
   * comment)
   *
   * @param <N>
   * @param <E>
   * @param <T>
   * @param doc
   * @param element
   * @param key
   */
  public static <N, E extends N, T extends N> Range getRangePrecedingElement(
      MutableDocument<N, E, T> doc, E element, String key) {
    int end = doc.getLocation(element);
    return getRangePrecedingLocation(doc, doc.getLocation(element), key);
  }

  /**
   * Gets the range of the annotation preceding the location with annotation
   * matching key.
   *
   * NOTE(user): The intention is to return only ranges immediately preceding
   * the element. However, we are looking back nearby for now (see above HACK
   * comment)
   *
   * @param <N>
   * @param <E>
   * @param <T>
   * @param doc
   * @param end
   * @param key
   */
  public static <N, E extends N, T extends N> Range getRangePrecedingLocation(
      MutableDocument<N, E, T> doc, int end, String key) {
    String currentAnnotation = null;
    int attemptsLeft = POSITIONS_TO_LOOK_BACK;
    while (end > 0 && (currentAnnotation = doc.getAnnotation(end - 1, key)) == null
        && attemptsLeft >= 0) {
      end--;
      attemptsLeft--;
    }
    if (currentAnnotation == null) {
      return null;
    }
    int start = doc.lastAnnotationChange(0, end, key, currentAnnotation);
    return new Range(start, end);
  }
}
