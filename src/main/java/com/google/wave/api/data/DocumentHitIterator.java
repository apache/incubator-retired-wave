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
package com.google.wave.api.data;

import com.google.common.base.Preconditions;
import com.google.wave.api.ElementType;
import com.google.wave.api.Range;

import java.util.Map;
import java.util.Map.Entry;

/**
 * A interface encapsulating iterating through a document.
 *
 * <p>
 * At some point we could consider turning this into an actual Iterable/Iterator
 * thing
 *
 */
public interface DocumentHitIterator {

  /**
   * @returns the next range yielded by this Iterator or null if we're done.
   */
  Range next();

  /**
   * Notify the iterator that the underlying document has changed; a change with
   * size delta has been applied at where.
   */
  void shift(int where, int delta);

  /**
   * Singleshot returns a single range.
   */
  class Singleshot implements DocumentHitIterator {

    private final Range range;
    private boolean called;

    public Singleshot(Range range) {
      this.called = false;
      this.range = range;
    }

    @Override
    public Range next() {
      if (called) {
        return null;
      }
      called = true;
      return this.range;
    }

    @Override
    public void shift(int where, int delta) {
    }
  }

  /**
   * TextMatcher yields all ranges in the document matching the searched for
   * string.
   */
  class TextMatcher implements DocumentHitIterator {

    private final ApiView apiView;
    private final String searchFor;
    private int from;
    private int hitsLeft;

    public TextMatcher(ApiView apiView, String searchFor, int maxHits) {
      Preconditions.checkNotNull(apiView, "Api view must not be null");
      Preconditions.checkNotNull(searchFor, "The string to search for must not be null");

      this.apiView = apiView;
      this.searchFor = searchFor;
      this.from = -1;
      this.hitsLeft = maxHits;
    }

    @Override
    public Range next() {
      if (hitsLeft == 0) {
        return null;
      }
      hitsLeft--;
      String searchIn = apiView.apiContents();
      int next = searchIn.indexOf(searchFor, from + 1);
      if (next == -1) {
        return null;
      }
      from = next;
      return new Range(from, from + searchFor.length());
    }

    @Override
    public void shift(int where, int delta) {
      if (from != -1) {
        if (where - 1 <= from) {
          from += delta;
        }
      }
    }
  }

  /**
   * ElementMatcher yields all ranges in the document matching the searched for
   * element type.
   */
  class ElementMatcher implements DocumentHitIterator {

    private int hitsLeft;
    private int index;
    private final ApiView apiView;
    private final ElementType elementType;
    private final Map<String, String> restrictions;

    public ElementMatcher(
        ApiView apiView, ElementType elementType, Map<String, String> restrictions, int maxRes) {
      Preconditions.checkNotNull(apiView, "Api view must not be null");
      Preconditions.checkNotNull(elementType, "The type of element to search for must not be null");
      Preconditions.checkNotNull(restrictions, "The search restricitions must not be null");

      this.elementType = elementType;
      this.apiView = apiView;
      this.index = -1;
      this.hitsLeft = maxRes;
      this.restrictions = restrictions;
    }

    @Override
    public Range next() {
      if (hitsLeft == 0) {
        return null;
      }
      hitsLeft--;

      for (ApiView.ElementInfo elementInfo : apiView.getElements()) {
        if (elementInfo.element.getType().equals(elementType) && elementInfo.apiPosition > index) {
          boolean allMatched = true;
          for (Entry<String, String> entry : restrictions.entrySet()) {
            if (!entry.getValue().equals(elementInfo.element.getProperty(entry.getKey()))) {
              allMatched = false;
              break;
            }
          }
          if (!allMatched) {
            continue;
          }
          index = elementInfo.apiPosition;
          return new Range(index, index + 1);
        }
      }
      return null;
    }

    @Override
    public void shift(int where, int delta) {
    }
  }
}
