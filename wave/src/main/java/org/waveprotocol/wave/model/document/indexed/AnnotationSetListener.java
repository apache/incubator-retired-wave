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

package org.waveprotocol.wave.model.document.indexed;

/**
 * Simple listener for changes to annotations.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface AnnotationSetListener<V> {

  /**
   * Called when a potential annotation change occurs.
   *
   * The caller need not guarantee that all of the range did not already have
   * the new value for the given key.
   *
   * @param start beginning of the range
   * @param end end of the range
   * @param key key that changed
   * @param newValue new value, which may be null
   */
  void onAnnotationChange(int start, int end, String key, V newValue);
}
