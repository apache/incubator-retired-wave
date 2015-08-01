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
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;

/**
 * Manifest blip that uses an element in a document to store its data.
 *
 * @author zdwang@google.com (David Wang)
 */
final class DocumentBasedManifestBlip implements ObservableManifestBlip {
  /**
   * Initialisation data for a thread.
   * Package-private for testing.
   */
  static final class ThreadInitialiser {
    final String id;
    final boolean isInline;

    public ThreadInitialiser(String id, boolean isInline) {
      Preconditions.checkNotNull(id, "Null thread id");
      this.id = id;
      this.isInline = isInline;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ThreadInitialiser)) {
        return false;
      }
      ThreadInitialiser other = (ThreadInitialiser) obj;
      return id.equals(other.id) && (isInline == other.isInline);
    }

    @Override
    public int hashCode() {
      return id.hashCode() * 37 + Boolean.valueOf(isInline).hashCode();
    }
  }

  private static final String THREAD_TAG = "thread";
  private static final String BLIP_ID_ATTR = "id";

  /** The id of the blip. */
  private final BasicValue<String> id;

  /** Replies to this blip, both inline and not. */
  private final ObservableElementList<ObservableManifestThread, ThreadInitialiser> replies;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  /**
   * Creates a document-based blip.
   *
   * @param router the document access that the element belongs
   * @param container the <blip> element
   */
  static <E> DocumentBasedManifestBlip create(DocumentEventRouter<? super E, E, ?> router,
      E container) {
    return new DocumentBasedManifestBlip(
        DocumentBasedElementList.create(router, container, THREAD_TAG,
            DocumentBasedManifestThread.<E> factory()),
        DocumentBasedBasicValue.create(router, container, Serializer.STRING, BLIP_ID_ATTR));
  }

  /**
   * Creates a factory for initializing manifest blip entries in a
   * document-based element list.
   */
  static <E> Factory<E, ObservableManifestBlip, String> factory() {
    return new Factory<E, ObservableManifestBlip, String>() {
      @Override
      public ObservableManifestBlip adapt(DocumentEventRouter<? super E, E, ?> router,
          E element) {
        return DocumentBasedManifestBlip.create(router, element);
      }

      @Override
      public Initializer createInitializer(String blipId) {
        return DocumentBasedBasicValue.createInitialiser(Serializer.STRING, BLIP_ID_ATTR, blipId);
      }
    };
  }

  /**
   * Creates a DocumentBasedManifestBlip. Package-private for testing.
   *
   * @param replies the replies list
   * @param id the id attribute
   */
  DocumentBasedManifestBlip(
      ObservableElementList<ObservableManifestThread, ThreadInitialiser> replies,
      BasicValue<String> id) {
    ObservableElementList.Listener<ObservableManifestThread> repliesListener =
        new ObservableElementList.Listener<ObservableManifestThread>() {
          public void onValueAdded(ObservableManifestThread entry) {
            triggerOnManifestThreadAdded(entry);
          }

          public void onValueRemoved(ObservableManifestThread entry) {
            entry.detachListeners();
            triggerOnManifestThreadRemoved(entry);
          }
        };

    this.replies = replies;
    this.replies.addListener(repliesListener);
    this.id = id;
  }

  @Override
  public String getId() {
    return id.get();
  }

  @Override
  public ObservableManifestThread appendReply(String id, boolean inline) {
    return replies.add(new ThreadInitialiser(id, inline));
  }

  @Override
  public ObservableManifestThread insertReply(int index, String id, boolean inline) {
    return replies.add(index, new ThreadInitialiser(id, inline));
  }

  @Override
  public ObservableManifestThread getReply(int index) {
    return replies.get(index);
  }

  @Override
  public Iterable<ObservableManifestThread> getReplies() {
    return replies.getValues();
  }

  @Override
  public int indexOf(ManifestThread reply) {
    return (reply instanceof ObservableManifestThread) ?
        replies.indexOf((ObservableManifestThread) reply) : -1;
  }

  @Override
  public boolean removeReply(ManifestThread reply) {
    return (reply instanceof ObservableManifestThread) ?
        replies.remove((ObservableManifestThread) reply) : false;
  }

  @Override
  public int numReplies() {
    return replies.size();
  }

  @Override
  public void detachListeners() {
    listeners.clear();
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  @Override
  public String toString() {
    return getClass().getName() + "(id = " + id.get() + ")";
  }

  private void triggerOnManifestThreadAdded(ObservableManifestThread entry) {
    for (Listener l : listeners) {
      l.onReplyAdded(entry);
    }
  }

  private void triggerOnManifestThreadRemoved(ObservableManifestThread entry) {
    for (Listener l : listeners) {
      l.onReplyRemoved(entry);
    }
  }
}
