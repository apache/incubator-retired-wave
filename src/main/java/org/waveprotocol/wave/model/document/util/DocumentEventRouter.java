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

import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.util.AttributeListener;
import org.waveprotocol.wave.model.util.DeletionListener;
import org.waveprotocol.wave.model.util.ElementListener;

/**
 * Wrapper around a document that provides filtered abstract event registration
 * for a document.
 *
 * @param <N> The node type of the document
 * @param <E> The element type of the document.
 * @param <T> The text node type of the document
 */
public interface DocumentEventRouter<N, E extends N, T extends N> {

  /**
   * Attaches an attribute listener to an element in the document.  The listener
   * will only receive events to the target element and is removed when the element
   * is deleted.
   */
  ListenerRegistration addAttributeListener(E target, AttributeListener<E> listener);

  /**
   * Attaches a deletion listener to an element in the document. The document listener
   * will be notified when the element is deleted, and will be detached before being
   * notified.
   */
  ListenerRegistration addDeletionListener(E target, DeletionListener listener);

  /**
   * Attaches an element listener that listens on children being added to and
   * removed from the specified parent.  The listener will be removed if the
   * element is deleted.
   */
  ListenerRegistration addChildListener(E parent, ElementListener<E> listener);

  /**
   * Returns the document wrapped by this object.
   */
  ObservableMutableDocument<N, E, T> getDocument();
}
