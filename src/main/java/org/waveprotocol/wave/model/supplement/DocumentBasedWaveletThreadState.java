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

import org.waveprotocol.wave.model.supplement.ObservablePrimitiveSupplement.Listener;

import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicMap;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.Serializer;


/**
 * Implements the per-wavelet thread state, using the
 * {@link DocumentBasedBasicMap} embedding for per-thread state storage.
 *
 *
 * @param <E> element type of the document implementation
 */
final class DocumentBasedWaveletThreadState<E> implements WaveletThreadState {
  private final ObservableBasicMap<String, ThreadState> threadStates;
  private final DocumentEventRouter<? super E, E, ?> router;
  private final E container;
  private final Serializer.EnumSerializer<ThreadState> threadStateSerializer = new
      Serializer.EnumSerializer<ThreadState>(ThreadState.class);


  DocumentBasedWaveletThreadState(
      DocumentEventRouter<? super E, E, ?> router, E container) {
    this.router = router;
    this.container = container;
    threadStates = DocumentBasedBasicMap.create(router, container, Serializer.STRING,
        threadStateSerializer, WaveletBasedSupplement.THREAD_TAG, WaveletBasedSupplement.ID_ATTR,
        WaveletBasedSupplement.STATE_ATTR);
  }

  /**
   * Creates a document based store for thread state.
   *
   * @param document document
   * @param container element in which the collapsed state is contained
   * @param id wavelet id being tracked
   * @param listener listener for collapsed-state changes
   * @return a new collapsed-state tracker.
   */
  public static <E> DocumentBasedWaveletThreadState<E> create(
      DocumentEventRouter<? super E, E, ?> router, E container, WaveletId id,
      Listener listener) {
    DocumentBasedWaveletThreadState<E> x =
        new DocumentBasedWaveletThreadState<E>(router, container);
    x.installListeners(id, listener);
    return x;
  }

  /**
   * Injects listeners into the underlying ADTs that translate their events into
   * primitive-supplement events.
   *
   * @param wid
   * @param listener
   */
  private void installListeners(final WaveletId wid, final Listener listener) {
    threadStates.addListener(new ObservableBasicMap.Listener<String, ThreadState>() {
      @Override
      public void onEntrySet(String key, ThreadState oldValue, ThreadState newValue) {
        listener.onThreadStateChanged(wid, key, oldValue, newValue);
      }
    });
  }

  @Override
  public void remove() {
    router.getDocument().deleteNode(container);
  }

  @Override
  public void setThreadState(String threadId, ThreadState val) {
    if (val == null) {
      threadStates.remove(threadId);
    } else {
      threadStates.put(threadId, val);
    }
  }

  @Override
  public ThreadState getThreadState(String threadId) {
    return threadStates.get(threadId);
  }

  @Override
  public Iterable<String> getThreads() {
    return threadStates.keySet();
  }
}
