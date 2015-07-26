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

import org.waveprotocol.wave.model.adt.ObservableSingleton;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedSingleton;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.WaveViewListener;
import org.waveprotocol.wave.model.wave.Wavelet;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * A conversation view backed by a wave.
 *
 * @author anorth@google.com (Alex North)
 */
public class WaveBasedConversationView implements ObservableConversationView, WaveViewListener {
  /**
   * Wraps a wavelet to provide a conversation, or not, depending
   * on whether it has a manifest.
   */
  private final class ConversationContainer implements
      ObservableSingleton.Listener<DocumentBasedManifest> {
    private final ObservableWavelet wavelet;
    private final ObservableSingleton<DocumentBasedManifest, Void> manifestContainer;
    private WaveletBasedConversation conversation;

    ConversationContainer(ObservableWavelet wavelet,
        ObservableSingleton<DocumentBasedManifest, Void> manifestContainer) {
      this.wavelet = wavelet;
      this.manifestContainer = manifestContainer;
    }

    ObservableWavelet getWavelet() {
      return wavelet;
    }

    WaveletBasedConversation getConversation() {
      if (conversation == null) {
        if (manifestContainer.hasValue()) {
          conversation = WaveletBasedConversation.create(
              WaveBasedConversationView.this, wavelet, manifestContainer.get(), idGenerator);
        }
      }
      return conversation;
    }

    @Override
    public void onValueChanged(DocumentBasedManifest oldValue, DocumentBasedManifest newValue) {
      if (oldValue != null) {
        assert conversation != null;
        conversations.remove(wavelet);
        conversation.destroy();
        triggerOnConversationRemoved(conversation);
        conversation = null;
      }
      if (newValue != null) {
        assert conversation == null;
        conversation = WaveletBasedConversation.create(WaveBasedConversationView.this, wavelet,
            newValue, idGenerator);
        conversations.put(wavelet, conversation);
        triggerOnConversationAdded(conversation);
      }
    }
  }

  /** Id of this group of conversations. */
  private final String id;

  /** Backing wave view. */
  private final ObservableWaveView waveView;

  /** Generator for new ids. */
  private final IdGenerator idGenerator;

  /** A container for every conversational wavelet in view. */
  private final IdentityMap<Wavelet, ConversationContainer> containers =
      CollectionUtils.createIdentityMap();

  /** Conversation adapters keyed by their wavelets. */
  private final Map<Wavelet, WaveletBasedConversation> conversations = CollectionUtils.newHashMap();

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  /**
   * Creates a conversation view on an wave view.
   */
  public static WaveBasedConversationView create(ObservableWaveView waveView,
      IdGenerator idGenerator) {
    String id = idFor(waveView.getWaveId());
    WaveBasedConversationView convView = new WaveBasedConversationView(id, waveView, idGenerator);
    waveView.addListener(convView);
    return convView;
  }

  /**
   * Computes the conversation view id for a wave.
   */
  public static String idFor(WaveId wave) {
    return ModernIdSerialiser.INSTANCE.serialiseWaveId(wave);
  }

  private WaveBasedConversationView(
      String id, ObservableWaveView waveView, IdGenerator idGenerator) {
    this.id = id;
    this.waveView = waveView;
    this.idGenerator = idGenerator;

    for (ObservableWavelet wavelet : waveView.getWavelets()) {
      if (IdUtil.isConversationalId(wavelet.getId())) {
        createContainer(wavelet);
      }
    }
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public Collection<WaveletBasedConversation> getConversations() {
    return Collections.unmodifiableCollection(conversations.values());
  }

  @Override
  public WaveletBasedConversation getConversation(String conversationId) {
    return getConversation(WaveletBasedConversation.widFor(conversationId));
  }

  @Override
  public WaveletBasedConversation getRoot() {
    ObservableWavelet rootWavelet = waveView.getRoot();
    return (rootWavelet != null) ? conversations.get(rootWavelet) : null;
  }

  @Override
  public WaveletBasedConversation createRoot() {
    ObservableWavelet rootWavelet = waveView.createRoot();
    return createNewConversation(rootWavelet);
  }

  @Override
  public WaveletBasedConversation createConversation() {
    ObservableWavelet wavelet;
    wavelet = waveView.createWavelet();
    return createNewConversation(wavelet);
  }

  // WaveViewListener.

  @Override
  public void onWaveletAdded(ObservableWavelet wavelet) {
    if (IdUtil.isConversationalId(wavelet.getId())) {
      createContainer(wavelet);
    }
  }

  @Override
  public void onWaveletRemoved(ObservableWavelet wavelet) {
    containers.remove(wavelet);
    WaveletBasedConversation conversation = conversations.remove(wavelet);
    if (conversation != null) {
      triggerOnConversationRemoved(conversation);
    }
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  /**
   * Gets the wave view backing this conversation view.
   */
  public ObservableWaveView getWaveView() {
    return waveView;
  }

  /**
   * Gets the conversation backed by a wavelet; null if the wavelet is not in
   * view or does not have conversation structure.
   */
  public WaveletBasedConversation getConversation(WaveletId id) {
    if (!IdUtil.isConversationalId(id)) {
      Preconditions.illegalArgument("Wavelet id " + id + " is not conversational");
    }
    ObservableWavelet wavelet = waveView.getWavelet(id);
    if (wavelet != null) {
      return conversations.get(wavelet);
    }
    return null;
  }

  /**
   * Creates and stores a conversation container. If the container has a
   * conversation then initializes that too.
   */
  private ConversationContainer createContainer(ObservableWavelet wavelet) {
    ObservableDocument manifestDoc = wavelet.getDocument(IdConstants.MANIFEST_DOCUMENT_ID);
    DocEventRouter router = DefaultDocEventRouter.create(manifestDoc);
    ObservableSingleton<DocumentBasedManifest, Void> manifestContainer =
        DocumentBasedSingleton.create(router, manifestDoc.getDocumentElement(),
           DocumentBasedManifest.MANIFEST_TOP_TAG, DocumentBasedManifest.FACTORY);
    ConversationContainer container = new ConversationContainer(wavelet, manifestContainer);
    manifestContainer.addListener(container);
    containers.put(wavelet, container);

    WaveletBasedConversation conversation = container.getConversation();
    if (conversation != null) {
      conversations.put(wavelet, conversation);
      triggerOnConversationAdded(conversation);
    }

    return container;
  }

  /**
   * Creates a new conversation model on a wavelet.
   *
   * @param wavelet wavelet with which to back the conversation
   * @return a conversation
   */
  private WaveletBasedConversation createNewConversation(ObservableWavelet wavelet) {
    assert IdUtil.isConversationalId(wavelet.getId());
    // Add conversation structure to the wavelet, then get the conversation
    // which may be constructed in the waiter.
    WaveletBasedConversation.makeWaveletConversational(wavelet);
    // The container will have created the conversation.
    assert conversations.containsKey(wavelet);
    return conversations.get(wavelet);
  }

  private void triggerOnConversationAdded(WaveletBasedConversation conversation) {
    for (Listener l : listeners) {
      l.onConversationAdded(conversation);
    }
  }

  private void triggerOnConversationRemoved(WaveletBasedConversation conversation) {
    for (Listener l : listeners) {
      l.onConversationRemoved(conversation);
    }
  }
}
