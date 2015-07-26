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

package org.waveprotocol.wave.model.supplement;

import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.supplement.ObservablePrimitiveSupplement.Listener;
import org.waveprotocol.wave.model.util.ElementListener;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a collection of per-wavelet thread-states, implemented by
 * embedding them in elements of a document.
 *
 * @see WaveletReadStateCollection
 *
 */
final class WaveletThreadStateCollection<E> implements ElementListener<E> {
  private final DocumentEventRouter<? super E, E, ?> router;
  private final E container;

  /** Read state, expressed as a per-wavelet structure. */
  private final Map<WaveletId, WaveletThreadState> waveletSupplements =
      new HashMap<WaveletId, WaveletThreadState>();

  /** Listener to inject into each read-state. */
  private final Listener listener;

  private WaveletThreadStateCollection(DocumentEventRouter<? super E, E, ?> router,
      E container, Listener listener) {
    this.router = router;
    this.container = container;
    this.listener = listener;
  }

  public static <E> WaveletThreadStateCollection<E> create(
      DocumentEventRouter<? super E, E, ?> router, E e, Listener listener) {
    WaveletThreadStateCollection<E> col = new WaveletThreadStateCollection<E>(router,
        e, listener);
    router.addChildListener(e, col);
    col.load();
    return col;
  }

  private ObservableMutableDocument<? super E, E, ?> getDocument() {
    return router.getDocument();
  }

  private void load() {
    ObservableMutableDocument<? super E, E, ?> doc = getDocument();
    E child = DocHelper.getFirstChildElement(doc, doc.getDocumentElement());
    while (child != null) {
      onElementAdded(child);
      child = DocHelper.getNextSiblingElement(doc, child);
    }
  }

  private WaveletId valueOf(E element) {
    String waveletIdStr = getDocument().getAttribute(element, WaveletBasedSupplement.ID_ATTR);
    return WaveletBasedConversation.widFor(waveletIdStr);
  }

  @Override
  public void onElementAdded(E element) {
    ObservableMutableDocument<? super E, E, ?> doc = getDocument();
    assert container.equals(doc.getParentElement(element));
    if (!WaveletBasedSupplement.CONVERSATION_TAG.equals(doc.getTagName(element))) {
      return;
    }

    WaveletId waveletId = valueOf(element);
    if (waveletId != null) {
      WaveletThreadState existing = waveletSupplements.get(waveletId);
      if (existing == null) {
        WaveletThreadState read =
            DocumentBasedWaveletThreadState.create(router, element, waveletId, listener);
        waveletSupplements.put(waveletId, read);
      } else {
        //
        // We can't mutate during callbacks yet.
        // Let's just ignore the latter :(. Clean up on timer?
        //
      }
    } else {
      // XML error: someone added a WAVELET element without an id. Ignore.
      // TODO(user): we should log this
    }
  }

  public void onElementRemoved(E element) {
    if (WaveletBasedSupplement.CONVERSATION_TAG.equals(getDocument().getTagName(element))) {
      WaveletId waveletId = valueOf(element);
      if (waveletId != null) {
        WaveletThreadState state = waveletSupplements.remove(waveletId);
        if (state == null) {
          // Not good - there was a collapsed-state element and we weren't
          // tracking it...
          // TODO(user): this is the same problem as the read state
          // tracker
        }
      }
    }
  }

  private void createEntry(WaveletId waveletId) {
    String waveletIdStr = WaveletBasedConversation.idFor(waveletId);
    ObservableMutableDocument<? super E, E, ?> doc = getDocument();
    E container =
        doc.createChildElement(doc.getDocumentElement(), WaveletBasedSupplement.CONVERSATION_TAG,
            new AttributesImpl(WaveletBasedSupplement.ID_ATTR, waveletIdStr));
  }

  WaveletThreadState getThreadStateSupplement(WaveletId waveletId) {
    Preconditions.checkNotNull(waveletId, "wavelet id must not be null");
    WaveletThreadState wavelet = waveletSupplements.get(waveletId);
    if (wavelet == null) {
      // Create a new container element for tracking state for the wavelet.
      // Callbacks should build it.
      createEntry(waveletId);
      wavelet = waveletSupplements.get(waveletId);
      assert wavelet != null;
    }
    return wavelet;
  }

  void clear() {
    Collection<WaveletId> toRemove = new ArrayList<WaveletId>(waveletSupplements.keySet());
    for (WaveletId waveletId : toRemove) {
      waveletSupplements.get(waveletId).remove();
    }
  }

  ThreadState getThreadState(WaveletId waveletId, String threadId) {
    WaveletThreadState wavelet = waveletSupplements.get(waveletId);
    return wavelet == null ? null : wavelet.getThreadState(threadId);
  }

  public Iterable<String> getStatefulThreads(WaveletId waveletId) {
    return waveletSupplements.containsKey(waveletId) ? getThreadStateSupplement(waveletId)
        .getThreads() : Collections.<String> emptySet();
  }

  public Iterable<WaveletId> getStatefulWavelets() {
    return waveletSupplements.keySet();
  }

  void setThreadState(WaveletId waveletId, String threadId, ThreadState value) {
    if (value == null) {
      WaveletThreadState wavelet = waveletSupplements.get(waveletId);
      if (wavelet != null) {
         wavelet.setThreadState(threadId, value);
      }
    } else {
       getThreadStateSupplement(waveletId).setThreadState(threadId, value);
    }
  }
}
