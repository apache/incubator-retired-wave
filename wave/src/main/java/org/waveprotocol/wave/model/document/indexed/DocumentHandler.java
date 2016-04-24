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
 * Handler for document events
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <N>
 * @param <E>
 * @param <T>
 */
public interface DocumentHandler<N, E extends N, T extends N> {

  /**
   * Describes a document event.
   */
  public interface EventBundle<N, E extends N, T extends N> {
    /**
     * @return event components, in index order.
     */
    Iterable<DocumentEvent<N, E, T>> getEventComponents();

    /**
     * Gets the element objects that were deleted. The intrinsic state of this
     * objects may be inspected (attributes, tagname, identity). However, since
     * these elements have been detached, the structural relationships from it
     * (siblings, parent, children) are undefined and should not be inspected.
     *
     * The elements are returned in document order.
     *
     * @return a set of deleted elements.
     */
    Iterable<? extends E> getDeletedElements();

    /**
     * Gets the element objects that were inserted. These objects may be used
     * freely, since they are attached and their state is complete.
     *
     * The elements are returned in document order.
     *
     * @return a set of elements inserted in this event.
     */
    Iterable<? extends E> getInsertedElements();

    /**
     * Tests if a particular element was deleted by this event. If true, the
     * element will already appear in {@link #getDeletedElements()}, but this
     * method may run in better-than-linear time.
     *
     * @param element element to test
     * @return true if {@code element} was deleted by this event.
     */
    boolean wasDeleted(E element);

    // NOTE(user):
    // a wasAdded query could be added for symmetry, but there are no use cases
    // for it yet.
  }

  /**
   * Triggered on changes to the document. Handlers should not cause document
   * mutations during this method.
   *
   * @param event change description
   */
  void onDocumentEvents(EventBundle<N, E, T> event);
}
