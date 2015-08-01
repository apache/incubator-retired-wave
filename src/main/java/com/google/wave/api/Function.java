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

import java.util.Map;

/**
 * A function that computes a result object of type {@code V} from an input of
 * type {@code K}.
 *
 * @param <K> the type of the function input.
 * @param <V> the type of the function output.
 */
public interface Function<K, V> {

  /**
   * Computes {@code V} from the given {@code K}.
   *
   * @param source the source object.
   * @return an instance of {@code V}, based on {@code source}.
   */
  V call(K source);

  /**
   * A {@link Function} that computes a {@link BlipContent} from another
   * {@link BlipContent}.
   *
   * Note: The sole purpose of this interface is to eliminate compiler warning
   * about generic array of {@link Function} creation, since the {@code insert},
   * {@code insertAfter}, {@code replace}, and {@code updateElements} method
   * inside {@link BlipContentRefs} take a varargs of functions.
   */
  static interface BlipContentFunction extends Function<BlipContent, BlipContent> { }

  /**
   * A {@link Function} that computes a {@link Map} from a {@link BlipContent}.
   *
   * Note: The sole purpose of this interface is to eliminate compiler warning
   * about generic array of {@link Function} creation, since the {@code insert},
   * {@code insertAfter}, {@code replace}, and {@code updateElements} method
   * inside {@link BlipContentRefs} take a varargs of functions.
   */
  static interface MapFunction extends Function<BlipContent, Map<String, String>> { }
}
