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

import java.util.Map;

/**
 * A document listener that is only interested in attribute changes.
 *
 * @param <E> document's element type
 */
public interface AttributeListener<E> {
  /**
   * Notifies this listener that attributes of an element within a document have
   * changed.
   *
   * @param element element that changed
   * @param oldValues attribute values that have been deleted
   * @param newValues attribute values that have been inserted
   */
  void onAttributesChanged(E element, Map<String, String> oldValues, Map<String, String> newValues);
}
