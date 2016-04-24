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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A class that represents wavelet's tags. This class supports various
 * tag related operations, such as, adding or removing tag from a wavelet.
 */
public class Tags implements Iterable<String>, Serializable {

  /** A set of string that represents wavelet tags. */
  private final Set<String> tags;

  /** The wavelet that this tag list represents. */
  private final Wavelet wavelet;

  /** The operation queue to queue operation to the robot proxy. */
  private final OperationQueue operationQueue;

  /**
   * Constructor.
   *
   * @param tags a collection of initial tags of the wavelet.
   * @param wavelet the wavelet that this tag list represents.
   * @param operationQueue the operation queue to queue operation to the robot
   *     proxy.
   */
  public Tags(Collection<String> tags, Wavelet wavelet, OperationQueue operationQueue) {
    this.tags = new LinkedHashSet<String>(tags);
    this.wavelet = wavelet;
    this.operationQueue = operationQueue;
  }

  /**
   * Adds the given tag id if it doesn't exist.
   *
   * @param tag the tag that will be added.
   * @return {@code true} if the given tag does not exist yet
   *     in the owning wavelet, which means that a new
   *     {@code wavelet.modifyTag()} has been queued. Otherwise, returns
   *     {@code false}.
   */
  public boolean add(String tag) {
    if (tags.contains(tag)) {
      return false;
    }

    operationQueue.modifyTagOfWavelet(wavelet, tag, "add");
    tags.add(tag);
    return true;
  }

  /**
   * Removes the given tag id if it exists.
   *
   * @param tag the tag that will be removed.
   * @return {@code true} if the given tag exists in the owning wavelet, which
   *     means that a new {@code wavelet.modifyTag()} has been queued.
   *     Otherwise, returns {@code false}.
   */
  public boolean remove(String tag) {
    if (!tags.contains(tag)) {
      return false;
    }

    operationQueue.modifyTagOfWavelet(wavelet, tag, "remove");
    tags.remove(tag);
    return true;
  }

  /**
   * Checks whether the owning wavelet has the given tag or not.
   *
   * @param tag the name of the tag to check.
   * @return {@code true} if the wavelet has the given tag.
   *     Otherwise, returns {@code false}.
   */
  public boolean contains(String tag) {
    return tags.contains(tag);
  }

  /**
   * Returns the number of tags in the owning wavelet.
   *
   * @return the number of tags.
   */
  public int size() {
    return tags.size();
  }

  /**
   * Checks whether the owning wavelet has any tag or not.
   *
   * @return {@code true} if the owning wavelet has at least one tag. Otherwise,
   *     returns {@code false}.
   */
  public boolean isEmpty() {
    return tags.isEmpty();
  }

  @Override
  public Iterator<String> iterator() {
    return new TagsIterator();
  }

  /**
   * An iterator for {@link Tags}.
   */
  private class TagsIterator implements Iterator<String> {

    /** The iterator of the underlying tag store. */
    private final Iterator<String> storeIterator;

    /** The pointer to the current tag. */
    private String currentTag;

    /** A flag that denotes whether {@link #remove} has been called or not. */
    private boolean removeCalled;

    /**
     * Constructor.
     */
    private TagsIterator() {
      this.storeIterator = tags.iterator();
    }

    @Override
    public boolean hasNext() {
      return storeIterator.hasNext();
    }

    @Override
    public String next() {
      removeCalled = false;
      currentTag = storeIterator.next();
      return currentTag;
    }

    @Override
    public void remove() {
      if (removeCalled) {
        throw new IllegalStateException("remove() has been called after the last call to next().");
      }

      if (currentTag == null) {
        throw new IllegalStateException("Please call next() first before calling remove().");
      }

      removeCalled = true;
      operationQueue.modifyTagOfWavelet(wavelet, currentTag, "remove");
      storeIterator.remove();
    }
  }
}
