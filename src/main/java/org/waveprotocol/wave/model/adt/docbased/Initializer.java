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

package org.waveprotocol.wave.model.adt.docbased;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Map;

/**
 * Provides initialization for a document element.
 *
 * The document interfaces currently only support atomic initialization of
 * attributes on a single element, not atomic initialization of a document
 * subtree. Thus this interface only provides attribute map initialization.
 *
 * @author anorth@google.com (Alex North)
 */
public interface Initializer {
  /**
   * Initializes attributes in a map.
   *
   * @param target receives the initialization values
   * @throws IllegalArgumentException if {@code target} already contains a key
   *         equal to one set by this initializer
   */
  void initialize(Map<String, String> target);

  /**
   * Initializer utilities.
   */
  final class Helper {
    /**
     * Builds an attribute map for an abstract initial state via a
     * {@link Factory} which can build an {@link Initializer}.
     *
     * @param initialState abstract initial state
     * @param initializerFactory translates initial state into an initializer
     * @return an attribute map initialized with the initial state
     */
    public static <I> Map<String, String> buildAttributes(I initialState,
        Factory<?, ?, I> initializerFactory) {
      Map<String, String> attrs = CollectionUtils.newHashMap();
      initializerFactory.createInitializer(initialState).initialize(attrs);
      return attrs;
    }

    /**
     * Initializes an attribute, failing if the target map already contains an
     * initialization for the same key.
     *
     * @throws IllegalArgumentException if the map already contains {@code key}
     */
    public static void initialiseAttribute(Map<String, String> target, String key, String value) {
      if (target.containsKey(key)) {
        Preconditions.illegalArgument("Duplicate initialiser key '" + key + "' in initialiser");
      }
      target.put(key, value);
    }
  }
}
