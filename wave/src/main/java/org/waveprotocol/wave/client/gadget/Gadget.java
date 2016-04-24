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

package org.waveprotocol.wave.client.gadget;

import static org.waveprotocol.wave.model.gadget.GadgetConstants.TAGNAME;

import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.doodad.DoodadInstallers;
import org.waveprotocol.wave.client.doodad.DoodadInstallers.BlipInstaller;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.ChunkyElementHandler;
import org.waveprotocol.wave.client.editor.util.EditorDocHelper;
import org.waveprotocol.wave.client.gadget.renderer.GadgetRenderer;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.gadget.GadgetConstants;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;

/**
 * Class for embedding IFrame-based gadgets in the editor.
 *
 */
public final class Gadget {

  private Gadget() {
  } // Non-instantiable.

  /**
   * Registers subclass with ContentElement
   */
  public static void register(ElementHandlerRegistry registry, WaveletName waveletName,
      ConversationBlip blip, ObservableSupplementedWave supplement, ProfileManager profileManager,
      String loginName) {

    GadgetRenderer renderer =
        new GadgetRenderer(waveletName, blip, supplement, profileManager, loginName);

    registry.registerEventHandler(TAGNAME, ChunkyElementHandler.INSTANCE);
    registry.registerRenderingMutationHandler(TAGNAME, renderer);
  }

  public static boolean isGadget(ContentNode node) {
    return EditorDocHelper.isNamedElement(node, GadgetConstants.TAGNAME);
  }

  public static BlipInstaller install(final ProfileManager profileManager,
      final ObservableSupplementedWave supplement, final ParticipantId signedInUser) {
    return new DoodadInstallers.BlipInstaller() {
      @Override
      public void install(Wavelet w, Conversation c, ConversationBlip b, Registries r) {
        WaveletName name = WaveletName.of(w.getWaveId(), w.getId());
        register(r.getElementHandlerRegistry(), name, b, supplement, profileManager,
            signedInUser.getAddress());
      }
    };
  }
}
