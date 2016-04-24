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

import java.util.Set;

/**
 * A read only interface to a set of strings.
 *
 * We define this in favor of using a standard Java collections interface so
 * that we can write an optimized implementation for GWT.
 *
 * Null is not permitted as an element. All methods, even {@link #contains(String)}
 * will reject null elements.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public interface ReadableStringSet {

  /**
   * A procedure that accepts an element from the set.
   */
  public interface Proc {
    void apply(String element);
  }

  /**
   * Returns true iff s is in the set.
   */
  boolean contains(String s);

  /**
   * @return some element in the set. If the set is empty, null is returned.
   */
  String someElement();

  /**
   * Returns true iff the set is empty.
   */
  boolean isEmpty();

  /**
   * Call the callback for each element in the map, in undefined order.
   */
  void each(Proc callback);

  /**
   * Returns true iff every element of this is also in other.
   */
  boolean isSubsetOf(ReadableStringSet other);

  /**
   * Returns true iff every element of this is also in other.
   */
  boolean isSubsetOf(Set<String> other);

  /**
   * Count the number of entries in the set.
   *
   * Note: Depending on the underlying map implementation, this may be a
   * time-consuming operation.
   */
  int countEntries();
}
