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

package org.waveprotocol.wave.model.document.util;

/**
 * Useful extra functionality for dealing with elements, that do not require a
 * document context (so this interface may be implemented by singletons, etc).
 *
 * NOTE(danilatos): This is a temporary interface and is likely to be merged
 * into others soon
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface ElementManager<E> {

  /**
   * Get a transient property for a given element
   *
   * @see Property
   */
  <T> T getProperty(Property<T> property, E element);

  /**
   * Set a transient property on a given element
   *
   * @see Property
   */
  <T> void setProperty(Property<T> property, E element, T value);

  /**
   * Find out if an element is still usable or not. Useful for assisting with
   * other cleanup work.
   *
   * @param element An element reference, possibly no longer valid
   * @return true if the element is invalid, for example if it has been removed
   *         from the document
   */
  boolean isDestroyed(E element);
}
