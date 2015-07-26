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

package org.waveprotocol.box.common;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A iterator which may throw a checked exception from query methods.
 *
 * If an exception is thrown the iterator becomes invalid.
 *
 * @author anorth@google.com (Alex North)
 * @param <T> type iterated over
 * @param <E> type of the exception thrown on failure
 */
public interface ExceptionalIterator<T, E extends Exception> {
  /**
   * An empty iterator.
   */
  final static class Empty {
    /** Creates an empty iterator. */
    public static <T, E extends Exception> ExceptionalIterator<T, E> create() {
      return new ExceptionalIterator<T, E>() {
        @Override
        public boolean hasNext() {
          return false;
        }

        @Override
        public T next() {
          throw new NoSuchElementException();
        }
      };
    }
    private Empty() {}
  }

  /**
   * An exceptional iterator which wraps an ordinary iterator.
   */
  final static class FromIterator {
    /**
     * Creates an iterator which adapts an ordinary iterator. It never throws
     * {@code E}.
     *
     * @param it iterator to adapt
     */
    public static <T, E extends Exception> ExceptionalIterator<T, E> create(final Iterator<T> it) {
      return new ExceptionalIterator<T, E>() {
        @Override
        public boolean hasNext() {
          return it.hasNext();
        }

        @Override
        public T next() {
          return it.next();
        }
      };
    }

    /**
     * Creates an iterator which adapts an ordinary iterator but throws an
     * exception when the adapted iterator finishes, when hasNext() returns
     * false.
     *
     * @param it iterator to adapt
     * @param exception exception to throw when the iterator is finished
     */
    public static <T, E extends Exception> ExceptionalIterator<T, E> create(final Iterator<T> it,
        final E exception) {
      return new ExceptionalIterator<T, E>() {
        @Override
        public boolean hasNext() throws E {
          if (it.hasNext()) {
            return true;
          }
          throw exception;
        }

        @Override
        public T next() {
          return it.next();
        }
      };
    }

    private FromIterator() {}
  }

  /**
   * An exceptional iterator which always throws an exception.
   */
  final static class Failing {
    /**
     * Creates an iterator which just throws an exception.
     *
     * @param exception exception to throw
     */
    public static <T, E extends Exception> ExceptionalIterator<T, E> create(final E exception) {
      return new ExceptionalIterator<T, E>() {
        @Override
        public boolean hasNext() throws E {
          throw exception;
        }

        @Override
        public T next() throws E {
          throw exception;
        }
      };
    }
  }

  /**
   * Returns true if the iteration has more elements.
   *
   * @throws E if an underlying query fails
   */
  boolean hasNext() throws E;

  /**
   * Returns the next element in the iteration.
   *
   * @throws E if an underlying query fails
   */
  T next() throws E;
}