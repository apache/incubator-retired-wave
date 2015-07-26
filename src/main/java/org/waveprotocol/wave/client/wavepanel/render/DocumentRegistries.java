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

package org.waveprotocol.wave.client.wavepanel.render;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.doodad.DoodadInstallers.BlipInstaller;
import org.waveprotocol.wave.client.doodad.DoodadInstallers.ConversationInstaller;
import org.waveprotocol.wave.client.doodad.DoodadInstallers.GlobalInstaller;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.Collection;
import java.util.Collections;

/**
 * A collection of doodad handlers, used to create {@link Registries} for
 * documents on demand.
 *
 * Doodad handlers are categorised into global, per-conversation, and per-blip,
 * according to the needs of each particular doodad handler.
 *
 */
public final class DocumentRegistries {

  /**
   * Builder that collects doodads to be installed. After collection, the set of
   * handlers is concretized into a {@link DocumentRegistries}, and handlers can
   * not be installed after that.
   */
  public static final class Builder {
    private Collection<GlobalInstaller> gis;
    private Collection<ConversationInstaller> cis;
    private Collection<BlipInstaller> bis;

    public Builder use(ConversationInstaller c) {
      (cis = lazyCreate(cis)).add(c);
      return this;
    }

    public Builder use(BlipInstaller b) {
      (bis = lazyCreate(bis)).add(b);
      return this;
    }

    public Builder use(GlobalInstaller g) {
      (gis = lazyCreate(gis)).add(g);
      return this;
    }

    public DocumentRegistries build() {
      Registries root = Editor.ROOT_REGISTRIES;
      for (GlobalInstaller gi : nonNull(gis)) {
        gi.install(root);
      }
      return new DocumentRegistries(root, nonNull(cis), nonNull(bis));
    }

    private static <T> Collection<T> lazyCreate(Collection<T> xs) {
      return xs == null ? CollectionUtils.<T> createQueue() : xs;
    }

    private static <T> Collection<T> nonNull(Collection<T> xs) {
      return xs == null ? Collections.<T> emptyList() : xs;
    }
  }

  /**
   * Per-conversation registry bundle.
   */
  private class ConversationRegistries {
    /** Registries from which blip registries are forked. */
    // Intentionally make 'registries' refer to the per-conv one.
    @SuppressWarnings("hiding")
    private final Registries registries;

    private final IdentityMap<ConversationBlip, Registries> blipRegistries =
      CollectionUtils.createIdentityMap();

    // Context used to install per-blip handlers.

    private final Wavelet w;
    private final Conversation c;

    ConversationRegistries(Registries registries, Wavelet w, Conversation c) {
      this.registries = registries;
      this.w = w;
      this.c = c;
    }

    /**
     * Extends this conversation's registres for a blip. Per-blip doodads are
     * installed in the new registries.
     */
    private Registries extendFor(ConversationBlip b) {
      Registries r = registries.createExtension();
      for (BlipInstaller bi : bis) {
        bi.install(w, c, b, r);
      }
      return r;
    }

    /**
     * @return the registries for a blip, lazily creating one if none exists.
     */
    Registries getBlipRegistries(ConversationBlip b) {
      Registries rs = blipRegistries.get(b);
      if (rs == null) {
        rs = extendFor(b);
        blipRegistries.put(b, rs);
      }
      return rs;
    }
  }

  /** Root registries, from which conversation registries are forked. */
  private final Registries registries;

  /** Per-conversation doodads to install in each per-conversation registry. */
  private final Collection<ConversationInstaller> cis;

  /** Per-blip doodads to install in each per-blip registry. */
  private final Collection<BlipInstaller> bis;

  /**
   * Per-conversation registries (which each contain their own per-blip
   * registries.
   */
  private final IdentityMap<Conversation, ConversationRegistries> conversationRegistries =
      CollectionUtils.createIdentityMap();

  private DocumentRegistries(Registries r, Collection<ConversationInstaller> cis,
      Collection<BlipInstaller> bis) {
    this.registries = r;
    this.cis = cis;
    this.bis = bis;
  }

  /** @return a builder for the registries collection. */
  public static Builder builder() {
    return new Builder();
  }

  /** Creates a per-conversation registries bundle. */
  private ConversationRegistries extendFor(Conversation c) {
    Registries r = registries.createExtension();
    for (ConversationInstaller ci : cis) {
      ci.install(((WaveletBasedConversation) c).getWavelet(), c, r);
    }
    return new ConversationRegistries(r, ((WaveletBasedConversation) c).getWavelet(), c);
  }

  /**
   * Loads the registries bundle for a conversation. If no bundle currently
   * exists, a new one is created, and the per-conversation doodads are
   * installed in it. Never returns null.
   */
  private ConversationRegistries getConversationRegistries(Conversation c) {
    ConversationRegistries rs = conversationRegistries.get(c);
    if (rs == null) {
      rs = extendFor(c);
      conversationRegistries.put(c, rs);
    }
    return rs;
  }

  /**
   * Loads the registries bundle for a blip. If no bundle currently exists, a
   * new one is created, and the per-blip doodads are installed in it. Never
   * returns null.
   */
  public Registries get(ConversationBlip blip) {
    return getConversationRegistries(blip.getConversation()).getBlipRegistries(blip);
  }
}
