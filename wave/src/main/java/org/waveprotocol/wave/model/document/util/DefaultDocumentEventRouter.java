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
import org.waveprotocol.wave.model.document.indexed.DocumentEvent;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.AttributesModified;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.ContentDeleted;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.ContentInserted;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.Type;
import org.waveprotocol.wave.model.document.indexed.DocumentHandler;
import org.waveprotocol.wave.model.util.AttributeListener;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.DeletionListener;
import org.waveprotocol.wave.model.util.ElementListener;

import java.util.Map;

/**
 * Simple document event router that adds a single listener to the wrapped document
 * and dispatches events to the listeners registered on it.
 *
 * @param <N> The node type of the document
 * @param <E> The element type of the document.
 * @param <T> The text node type of the document
 */
public class DefaultDocumentEventRouter<N, E extends N, T extends N> implements
    DocumentEventRouter<N, E, T>, DocumentHandler<N, E, T> {

  private final ObservableMutableDocument<N, E, T> doc;
  private Map<E, CopyOnWriteSet<ElementListener<E>>> elementListenerMap = null;
  private Map<E, CopyOnWriteSet<AttributeListener<E>>> attributeListenerMap = null;
  private Map<E, CopyOnWriteSet<DeletionListener>> deletionListenerMap = null;
  private int listenerCount = 0;

  protected DefaultDocumentEventRouter(ObservableMutableDocument<N, E, T> doc) {
    this.doc = doc;
  }

  /**
   * Create a new document event router wrapping the given document.
   */
  public static <N, E extends N> DocumentEventRouter<N, E, ? extends N> create(
      ObservableMutableDocument<N, E, ?> doc) {
    // It should actually be possible for this constructor to take T rather than
    // a wildcard but eclipse gives a compile error when passing an
    // ObservableMutableDocument<? super E, E, ?>.  It is clearly possible to infer
    // from the bounds on ObservableMutableDocument that the capture of the last '?'
    // extends the capture of the first '? super E' -- but alas it doesn't work.
    return doCreate(doc);
  }

  /**
   * Create above needs to have a bridge call to this function for wildcard capture to
   * turn the ? into a concrete 'T' we can use in the constructor call.
   */
  private static <N, E extends N, T extends N> DocumentEventRouter<N, E, T> doCreate(
      ObservableMutableDocument<N, E, T> doc) {
    return new DefaultDocumentEventRouter<N, E, T>(doc);
  }

  private static class Registration<T> implements ListenerRegistration {

    private final DefaultDocumentEventRouter<?, ?, ?> router;
    private final Object key;
    private final T listener;
    private final Map<?, CopyOnWriteSet<T>> listenerMap;

    private Registration(DefaultDocumentEventRouter<?, ?, ?> router,
        Map<?, CopyOnWriteSet<T>> listenerMap, Object key, T listener) {
      this.router = router;
      this.key = key;
      this.listener = listener;
      this.listenerMap = listenerMap;
    }

    public static <T> Registration<T> create(DefaultDocumentEventRouter<?, ?, ?> router,
        Map<?, CopyOnWriteSet<T>> listenerMap, Object key, T listener) {
      return new Registration<T>(router, listenerMap, key, listener);
    }

    @Override
    public void detach() {
      router.listenerRemoved();
      CopyOnWriteSet<T> listeners = listenerMap.get(key);
      if (listeners != null) {
        listeners.remove(listener);
        if (listeners.isEmpty()) {
          listenerMap.remove(key);
        }
      }
    }

  }

  @Override
  public void onDocumentEvents(EventBundle<N, E, T> bundle) {
    if (attributeListenerMap != null) {
      for (DocumentEvent<N, E, T> event : bundle.getEventComponents()) {
        if (event.getType() == Type.ATTRIBUTES) {
          AttributesModified<N, E, T> am = (AttributesModified<N, E, T>) event;
          E target = am.getElement();
          CopyOnWriteSet<AttributeListener<E>> listeners = attributeListenerMap.get(target);
          if (listeners != null) {
            for (AttributeListener<E> listener : listeners) {
              listener.onAttributesChanged(target, am.getOldValues(), am.getNewValues());
            }
          }
        }
      }
    }
    if (elementListenerMap != null) {
      for (DocumentEvent<N, E, T> event : bundle.getEventComponents()) {
        if (event.getType() == Type.CONTENT_DELETED) {
          // We can't use deleted.getParent since the parent pointer may have been
          // cleared at this point.
          ContentDeleted<N, E, T> deletedEvent = (ContentDeleted<N, E, T>) event;
          E deleted = deletedEvent.getRoot();
          Point<N> deletedLocation = doc.locate(deletedEvent.getLocation());
          E parent = Point.enclosingElement(doc, deletedLocation);
          CopyOnWriteSet<ElementListener<E>> listeners = elementListenerMap.get(parent);
          if (listeners != null) {
            for (ElementListener<E> listener : listeners) {
              listener.onElementRemoved(deleted);
            }
          }
        }
      }
    }
    for (E deleted : bundle.getDeletedElements()) {
      if (deletionListenerMap != null) {
        CopyOnWriteSet<DeletionListener> listeners = deletionListenerMap.get(deleted);
        if (listeners != null) {
          for (DeletionListener listener : listeners) {
            listener.onDeleted();
          }
        }
      }
    }
    if (elementListenerMap != null) {
      for (DocumentEvent<N, E, T> event : bundle.getEventComponents()) {
        if (event.getType() == Type.CONTENT_INSERTED) {
          E inserted = ((ContentInserted<N, E, T>) event).getSubtreeElement();
          E parent = doc.getParentElement(inserted);
          CopyOnWriteSet<ElementListener<E>> listeners = elementListenerMap.get(parent);
          if (listeners != null) {
            for (ElementListener<E> listener : listeners) {
              listener.onElementAdded(inserted);
            }
          }
        }
      }
    }
    removeDeadListeners(bundle);
  }

  private void removeDeadListeners(EventBundle<N, E, T> bundle) {
    for (E deleted : bundle.getDeletedElements()) {
      if (attributeListenerMap != null) {
        removeListeners(attributeListenerMap, deleted);
      }
      if (elementListenerMap != null) {
        removeListeners(elementListenerMap, deleted);
      }
      if (deletionListenerMap != null) {
        removeListeners(deletionListenerMap, deleted);
      }
    }
  }

  @Override
  public ListenerRegistration addChildListener(E parent, ElementListener<E> listener) {
    if (elementListenerMap == null) {
      elementListenerMap = CollectionUtils.newHashMap();
    }
    CopyOnWriteSet<ElementListener<E>> list = ensureListenerList(parent, elementListenerMap);
    list.add(listener);
    listenerAdded();
    return Registration.create(this, elementListenerMap, parent, listener);
  }

  @Override
  public ListenerRegistration addAttributeListener(E target, AttributeListener<E> listener) {
    if (attributeListenerMap == null) {
      attributeListenerMap = CollectionUtils.newHashMap();
    }
    CopyOnWriteSet<AttributeListener<E>> list = ensureListenerList(target, attributeListenerMap);
    list.add(listener);
    listenerAdded();
    return Registration.create(this, attributeListenerMap, target, listener);
  }

  @Override
  public ListenerRegistration addDeletionListener(E target, DeletionListener listener) {
    if (deletionListenerMap == null) {
      deletionListenerMap = CollectionUtils.newHashMap();
    }
    CopyOnWriteSet<DeletionListener> list = ensureListenerList(target, deletionListenerMap);
    list.add(listener);
    listenerAdded();
    return Registration.create(this, deletionListenerMap, target, listener);
  }

  @Override
  public ObservableMutableDocument<N, E, T> getDocument() {
    return doc;
  }

  private <T> CopyOnWriteSet<T> ensureListenerList(E target, Map<E, CopyOnWriteSet<T>> map) {
    CopyOnWriteSet<T> list = map.get(target);
    if (list == null) {
      list = CopyOnWriteSet.createListSet();
      map.put(target, list);
    }
    return list;
  }

  /**
   * Removes and returns the list of listeners for the given element and updates
   * the listener count accordingly.
   */
  private <T> CopyOnWriteSet<T> removeListeners(Map<E, CopyOnWriteSet<T>> map, E elm) {
    CopyOnWriteSet<T> removed = map.remove(elm);
    if (removed != null) {
      listenerCount -= removed.size();
      if (listenerCount == 0) {
        doc.removeListener(this);
      }
    }
    return removed;
  }

  private void listenerRemoved() {
    listenerCount--;
    if (listenerCount == 0) {
      doc.removeListener(this);
    }
  }

  private void listenerAdded() {
    listenerCount++;
    if (listenerCount == 1) {
      doc.addListener(this);
    }
  }

}
