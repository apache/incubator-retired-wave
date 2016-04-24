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


import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.mockito.ArgumentCaptor;
import org.waveprotocol.wave.model.adt.ObservableStructuredValue;
import org.waveprotocol.wave.model.conversation.DocumentBasedManifest.AnchorKey;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * Tests for manifest backed by a document.
 *
 * @author anorth@google.com (Alex North)
 */

public class DocumentBasedManifestTest extends TestCase {

  /**
   * Tests that the manifest appropriately delegates to its composite values.
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public void testWiringUp() {
    // Manifest's delegates
    final ObservableManifestThread rootThread = mock(ObservableManifestThread.class);
    final ObservableStructuredValue<DocumentBasedManifest.AnchorKey, String> anchorValue =
        mock(ObservableStructuredValue.class);

    // Holder for the listener registered with the anchor
    ArgumentCaptor<ObservableStructuredValue.Listener> anchorListener =
        ArgumentCaptor.forClass(ObservableStructuredValue.Listener.class);

    DocumentBasedManifest target = new DocumentBasedManifest(rootThread,
        anchorValue);
    verify(anchorValue).addListener(anchorListener.capture());

    // Check root thread accessor.
    assertSame(rootThread, target.getRootThread());

    // Check fetching anchor.
    when(anchorValue.get(AnchorKey.WAVELET)).thenReturn("waveletid");
    when(anchorValue.get(AnchorKey.BLIP)).thenReturn("blipid");

    AnchorData anchor = target.getAnchor();
    assertEquals("waveletid", anchor.getConversationId());
    assertEquals("blipid", anchor.getBlipId());

    // Check setting anchor.
    target.setAnchor(new AnchorData("newwavelet", "newblip"));
    verify(anchorValue).set(CollectionUtils.immutableMap(AnchorKey.WAVELET, "newwavelet",
          AnchorKey.BLIP, "newblip"));

    // Test manifest listener.
    final ObservableManifest.Listener manifestListener = mock(ObservableManifest.Listener.class);
    target.addListener(manifestListener);

    when(anchorValue.get(AnchorKey.WAVELET)).thenReturn("newwavelet");
    when(anchorValue.get(AnchorKey.BLIP)).thenReturn("newblip");

    anchorListener.getValue().onValuesChanged(
        CollectionUtils.immutableMap(AnchorKey.WAVELET, "waveletid", AnchorKey.BLIP, "blipid"),
        CollectionUtils.immutableMap(AnchorKey.WAVELET, "newwavelet", AnchorKey.BLIP, "newblip"));

    verify(manifestListener).onAnchorChanged(new AnchorData("waveletid", "blipid"),
        new AnchorData("newwavelet", "newblip"));

    // Check removal of listener.
    target.removeListener(manifestListener);

    when(anchorValue.get(AnchorKey.WAVELET)).thenReturn("waveletid");
    when(anchorValue.get(AnchorKey.BLIP)).thenReturn("blipid");
    anchorListener.getValue().onValuesChanged(
        CollectionUtils.immutableMap(AnchorKey.WAVELET, "waveletid", AnchorKey.BLIP, "blipid"),
        CollectionUtils.immutableMap(AnchorKey.WAVELET, "newwavelet", AnchorKey.BLIP, "newblip"));
    verifyNoMoreInteractions(manifestListener);
  }
}
