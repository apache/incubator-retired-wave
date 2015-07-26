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

import org.waveprotocol.wave.model.document.util.DocumentEventRouter;

/**
 * A factory for abstract types which adapt document elements.
 *
 * {@link #createInitializer} provides translation from an abstract object
 * Initializer into an initializer of document state.
 * {@link #adapt(DocumentEventRouter, Object)} then provides a translation
 * from document state into an abstract type.
 *
 * @author anorth@google.com (Alex North)
 * @param <E> document element type
 * @param <T> abstract type of objects created
 * @param <I> type of object state initializer
 */
public interface Factory<E, T, I> {
  /**
   * Creates an abstract object for a document element. The document and element
   * must not be modified by this method.
   *
   * @param router the document event router supporting the object
   * @param element the element node to adapt
   * @return an abstract element backed by the document element
   */
  T adapt(DocumentEventRouter<? super E, E, ?> router, E element);

  /**
   * Creates an initializer of document state from an abstract object
   * Initializer.
   *
   * @param initialState initial element state (may be null)
   */
  Initializer createInitializer(I initialState);
}
