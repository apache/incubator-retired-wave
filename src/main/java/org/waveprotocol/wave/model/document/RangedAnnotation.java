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

package org.waveprotocol.wave.model.document;

/**
 * The representation of a ranged annotation, which is a key-value pair and the
 * range annotated with it.  The range is maximal in that the values associated
 * with the key before the start and after the end of the range differ from the
 * value inside the range.
 *
 * @author ohler@google.com (Christian Ohler)
 *
 * @param <V> the type of values of annotations
 */
public interface RangedAnnotation<V> {
  /** The key of the key-value pair. */
  String key();

  /** The value of key-value pair. */
  V value();

  /** The index of the first item covered by this annotation. */
  int start();

  /** The index beyond the last item covered by this annotation. */
  int end();
}
