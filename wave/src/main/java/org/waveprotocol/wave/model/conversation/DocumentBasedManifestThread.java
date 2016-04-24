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

package org.waveprotocol.wave.model.conversation;

import org.waveprotocol.wave.model.adt.BasicValue;
import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.CompoundInitializer;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.conversation.DocumentBasedManifestBlip.ThreadInitialiser;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Serializer;

/**
 * This is a manifest thread that uses an element in a document to store its
 * data.
 *
 * @author zdwang@google.com (David Wang)
 */
final class DocumentBasedManifestThread implements ObservableManifestThread {
  private static final String BLIP_TAG = "blip";
  private static final String THREAD_ID_ATTR = "id";
  private static final String INLINE_ATTR = "inline";

  /** The id of the thread, null for the root thread. */
  private final BasicValue<String> id;

  /** Whether this reply is inline, null for the root thread. */
  private final BasicValue<Boolean> inline;

  /** Blips in this thread. */
  private final ObservableElementList<ObservableManifestBlip, String> blips;

  /** Listeners to this thread. */
  private final CopyOnWriteSet<ObservableManifestThread.Listener> listeners =
      CopyOnWriteSet.create();

  /**
   * Creates a document-based thread.
   *
   * @param router router for the document the container belongs to
   * @param container the element that corresponds to the <thread> tag
   */
  static <E> DocumentBasedManifestThread create(DocumentEventRouter<? super E, E, ?> router,
      E container) {
    return new DocumentBasedManifestThread(
        DocumentBasedElementList.create(router, container, BLIP_TAG,
            DocumentBasedManifestBlip.<E> factory()),
        DocumentBasedBasicValue.create(router, container, Serializer.STRING,
            THREAD_ID_ATTR),
        DocumentBasedBasicValue.create(router, container, Serializer.BOOLEAN,
            INLINE_ATTR));
  }

  /**
   * Creates a factory for initializing manifest thread entries in a
   * document-based element list.
   */
  static <E> Factory<E, ObservableManifestThread, ThreadInitialiser>
      factory() {
    return new Factory<E, ObservableManifestThread, ThreadInitialiser>() {
      @Override
      public ObservableManifestThread adapt(DocumentEventRouter<? super E, E, ?> router,
          E element) {
        return DocumentBasedManifestThread.create(router, element);
      }

      @Override
      public Initializer createInitializer(ThreadInitialiser initialState) {
        return new CompoundInitializer(
            DocumentBasedBasicValue.createInitialiser(Serializer.STRING, THREAD_ID_ATTR,
                initialState.id),
            // NOTE(anorth): initialise the inline attribute only if true as
            // empty implies false.
            DocumentBasedBasicValue.createInitialiser(Serializer.BOOLEAN, INLINE_ATTR,
                initialState.isInline ? true : null));
      }
    };
  }

  /**
   * Creates a thread with the given blips list, id attribute and inline
   * attribute. Package-private for testing.
   */
  DocumentBasedManifestThread(ObservableElementList<ObservableManifestBlip, String> blips,
      BasicValue<String> id, ObservableBasicValue<Boolean> inline) {
    ObservableElementList.Listener<ObservableManifestBlip> blipListListener =
        new ObservableElementList.Listener<ObservableManifestBlip>() {
          public void onValueAdded(ObservableManifestBlip entry) {
            triggerOnManifestBlipAdded(entry);
          }

          public void onValueRemoved(ObservableManifestBlip entry) {
            entry.detachListeners();
            triggerOnManifestBlipRemoved(entry);
          }
        };

    this.blips = blips;
    this.blips.addListener(blipListListener);
    this.id = id;
    this.inline = inline;
  }

  @Override
  public String getId() {
    return id.get();
  }

  @Override
  public boolean isInline() {
    Boolean isInline = inline.get();
    return (isInline != null) ? isInline : false;
  }

  @Override
  public ObservableManifestBlip appendBlip(String id) {
    return blips.add(id);
  }

  @Override
  public ObservableManifestBlip insertBlip(int index, String id) {
    return blips.add(index, id);
  }

  @Override
  public ObservableManifestBlip getBlip(int index) {
    return blips.get(index);
  }

  @Override
  public Iterable<ObservableManifestBlip> getBlips() {
    return blips.getValues();
  }

  @Override
  public int indexOf(ManifestBlip blip) {
    return (blip instanceof ObservableManifestBlip) ?
        blips.indexOf((ObservableManifestBlip) blip) : -1;
  }

  @Override
  public boolean removeBlip(ManifestBlip blip) {
    return (blip instanceof ObservableManifestBlip) ?
        blips.remove((ObservableManifestBlip) blip) : false;
  }

  @Override
  public int numBlips() {
    return blips.size();
  }

  @Override
  public void detachListeners() {
    listeners.clear();
  }

  @Override
  public void addListener(ObservableManifestThread.Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(ObservableManifestThread.Listener listener) {
    listeners.remove(listener);
  }

  @Override
  public String toString() {
    return getClass().getName() + "(id = " + id.get() + ", inline = " + inline.get() + ")";
  }

  private void triggerOnManifestBlipAdded(ObservableManifestBlip entry) {
    for (Listener l : listeners) {
      l.onBlipAdded(entry);
    }
  }

  private void triggerOnManifestBlipRemoved(ObservableManifestBlip entry) {
    for (Listener l : listeners) {
      l.onBlipRemoved(entry);
    }
  }
}
