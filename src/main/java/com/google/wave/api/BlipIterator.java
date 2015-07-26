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

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

/**
 * An iterator over blip content, that consists of text or element.
 *
 * Please note that this iterator does not support {@code remove} operation at
 * the moment.
 *
 * @param <T> the generic type of the target entity to match.
 */
public abstract class BlipIterator<T> implements Iterator<Range> {

  /** The blip to be iterated. */
  protected final Blip blip;

  /** The target to be matched. */
  protected final T target;

  /** The maximum number of iterations allowed. */
  private final int maxHits;

  /** The range size of a match. */
  private final int rangeSize;

  /** The number of allowed iterations left. */
  private int hitsLeft;

  /** The current position of the iterator. */
  protected int position;

  /**
   * Constructor.
   *
   * @param blip the blip to be iterated.
   * @param target the target to be matched.
   * @param maxHits the maximum number of iterations allowed.
   * @param rangeSize the size of a matching range, for example, 1 for element
   *     iteration, or the length of the {@code target} for text/string
   *     iteration.
   */
  protected BlipIterator(Blip blip, T target, int maxHits, int rangeSize) {
    this.blip = blip;
    this.target = target;
    this.maxHits = maxHits;
    this.rangeSize = rangeSize;
    reset();
  }

  @Override
  public boolean hasNext() {
    return hitsLeft != 0  && getNextIndex() != -1;
  }

  @Override
  public Range next() {
    if (hitsLeft == 0) {
      throw new NoSuchElementException();
    }

    int index = getNextIndex();
    if (index == -1) {
      throw new NoSuchElementException();
    }

    hitsLeft--;
    position = index;
    return new Range(position, position + rangeSize);
  }

  /**
   * {@code remove} is not supported.
   *
   * @throws UnsupportedOperationException this operation is not supported yet.
   *     it will throw {@link UnsupportedOperationException} on all invocations.
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  /**
   * Shifts the iterator cursor.
   *
   * @param shiftAmount the shift amount.
   */
  void shift(int shiftAmount) {
    this.position += shiftAmount;
  }

  /**
   * Shifts the iterator cursor to beginning, to reset iteration.
   */
  void reset() {
    this.position = -1;
    this.hitsLeft = maxHits;
  }

  /**
   * Returns the index of the next match.
   *
   * @return the index of the next match, or -1 if there is no more match.
   */
  protected abstract int getNextIndex();

  /**
   * A blip iterator that allows a single iteration over a given range.
   */
  static final class SingleshotIterator extends BlipIterator<Void> {

    /** The starting index of the range. */
    private final int start;

    /**
     * Constructor.
     *
     * @param blip the blip to iterate.
     * @param start the start index of the range to iterate.
     * @param end the end index of the range to iterate.
     */
    public SingleshotIterator(Blip blip, int start, int end) {
      super(blip, null, 1, end - start);
      int length = blip.getContent().length();
      this.start = start;
    }

    @Override
    protected int getNextIndex() {
      return start;
    }
  }

  /**
   * A blip iterator that allows iteration over text/string content.
   */
  static final class TextIterator extends BlipIterator<String> {

    /**
     * Constructor.
     *
     * @param blip the blip to be iterated.
     * @param target the string to be matched.
     * @param maxHits the maximum number of iterations allowed.
     */
    public TextIterator(Blip blip, String target, int maxHits) {
      super(blip, target, maxHits, target.length());
    }

    @Override
    protected int getNextIndex() {
      return blip.getContent().indexOf(target, position + 1);
    }
  }

  /**
   * A blip iterator that allows iteration over element content, such as,
   * gadget, form element, image, and so on.
   */
  static final class ElementIterator extends BlipIterator<ElementType> {

    /** A map of restrictions that would be applied during iteration.  */
    private final Map<String, String> restrictions;

    /**
     * Constructor.
     *
     * @param blip the blip to be iterated.
     * @param target the string to be matched.
     * @param restrictions a map of restrictions.
     * @param maxHits the maximum number of iterations allowed.
     */
    public ElementIterator(Blip blip, ElementType target, Map<String, String> restrictions,
        int maxHits) {
      super(blip, target, maxHits, 1);
      this.restrictions = restrictions;
    }

    @Override
    protected int getNextIndex() {
      int index = -1;
      for (Entry<Integer, Element> entry : blip.getElements().tailMap(position + 1).entrySet()) {
        if (match(entry.getValue())) {
          index = entry.getKey();
          break;
        }
      }
      return index;
    }

    /**
     * Checks whether the given {@code element} is a match or not, by checking
     * the type and the properties.
     *
     * @param element the element to check.
     * @return {@code true} if the element type matches the target type, and
     *     the all filters/restrictions are satisfied.
     */
    private boolean match(Element element) {
      if (element.getType() != target) {
        return false;
      }

      if (restrictions == null) {
        return true;
      }

      for (Entry<String, String> entry : restrictions.entrySet()) {
        if (!entry.getValue().equals(element.getProperty(entry.getKey()))) {
          return false;
        }
      }
      return true;
    }
  }
}
