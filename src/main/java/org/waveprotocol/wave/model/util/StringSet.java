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
 * A read-write interface to a set of strings.
 *
 * We define this in favor of using a standard Java collections interface
 * so that we can write an optimized implementation for GWT.
 *
 * Null is not permitted as a value. All methods, even {@link #contains(String)}
 * will throw an exception for null values.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public interface StringSet extends ReadableStringSet {

  /**
   * A function that accepts a String and returns a boolean.
   */
  public interface StringPredicate {
    boolean apply(String x);
  }

  /**
   * @param s must not be null
   * Adds s to the set.  If s is already in the set, does nothing.
   */
  void add(String s);

  /**
   * Removes s from the set.  If s is not in the set, does nothing.
   */
  void remove(String s);

  /**
   * Removes all strings from the set.
   */
  void clear();

  /**
   * Equivalent to calling add(s) for every s in stringsToAdd.
   */
  void addAll(ReadableStringSet stringsToAdd);

  /**
   * Equivalent to calling remove(s) for every s in stringsToRemove.
   */
  void removeAll(ReadableStringSet stringsToRemove);

  /**
   * Call the predicate for each entry in the set, in undefined order.
   * If the predicate returns false, the entry is removed from the
   * set; if it returns true, it remains.
   */
  void filter(StringPredicate filter);
}
