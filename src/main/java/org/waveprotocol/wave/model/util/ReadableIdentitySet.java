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

package org.waveprotocol.wave.model.util;

/**
 * A read only interface to a set of elements.
 *
 * This is used in favor of using a standard Java collections interface so that
 * Javascript-optimized implementations can be used in GWT code.
 *
 * Null is not permitted as a value. All methods, including
 * {@link #contains(Object)}, will reject null values.
 *
 * @author ohler@google.com (Christian Ohler)
 * @author hearnden@google.com (David Hearnden)
 */
public interface ReadableIdentitySet<T> {

  /**
   * A procedure that accepts an element from the set.
   */
  public interface Proc<T> {
    void apply(T element);
  }

  /** @return true if and only if {@code s} is in this set. */
  boolean contains(T s);

  /** @return true if and only if this set is empty. */
  boolean isEmpty();

  /**
   * @return some element in the set. If the set is empty, null is returned.
   */
  T someElement();

  /**
   * Calls a procedure with each element in this set, in undefined order.
   */
  void each(Proc<? super T> procedure);

  /**
   * @return the size of this set. Note: Depending on the underlying
   *         implementation, this may be a linear operation.
   */
  int countEntries();
}
