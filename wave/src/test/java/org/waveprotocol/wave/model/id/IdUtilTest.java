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

package org.waveprotocol.wave.model.id;

import junit.framework.TestCase;

/**
 * Tests for id util.
 *
 * @author anorth@google.com (Alex North)
 */
public class IdUtilTest extends TestCase implements IdConstants {
  public void testConversationRootIsConversational() {
    WaveletId root = WaveletId.of("example.com", CONVERSATION_ROOT_WAVELET);
    assertTrue(IdUtil.isConversationalId(root));
    assertTrue(IdUtil.isConversationRootWaveletId(root));
  }

  public void testConversationWaveletIsConversational() {
    WaveletId conv = WaveletId.of("example.com", CONVERSATION_WAVELET_PREFIX + TOKEN_SEPARATOR +
        "foo");
    assertTrue(IdUtil.isConversationalId(conv));
  }

  public void testUserDataWaveletaIsNonConversational() {
    WaveletId root = WaveletId.of("example.com", USER_DATA_WAVELET_PREFIX + TOKEN_SEPARATOR
        + "nobody@gwave.com");
    assertFalse(IdUtil.isConversationalId(root));
  }

  public void testUserDataWavelet() {
    WaveletId udw = WaveletId.of("example.com", USER_DATA_WAVELET_PREFIX + TOKEN_SEPARATOR
        + "nobody@gwave.com");
    assertTrue(IdUtil.isUserDataWavelet(udw));
    assertTrue(IdUtil.isUserDataWavelet("nobody@gwave.com", udw));
  }

  public void testBlipIdIsBlipId() {
    assertTrue(IdUtil.isBlipId(BLIP_PREFIX + TOKEN_SEPARATOR + "1"));
    assertTrue(IdUtil.isBlipId(BLIP_PREFIX + TOKEN_SEPARATOR + "1++1"));
  }

  public void testPoorlyEscapedId() {
    // Should not throw any exception.
    assertFalse(IdUtil.isBlipId("This is NOT a blip id!"));
  }

  public void testLegacyBlipIdIsBlipId() {
    assertTrue(IdUtil.isBlipId("12354*42"));
  }

  public void testLegacyDataDocIdIsNotBlipId() {
    assertFalse(IdUtil.isBlipId("m/asd"));
    // Attachment data doc legacy: "*" makes this look like an old blip id.
    assertFalse(IdUtil.isBlipId("attach+1234*56"));
  }

  public void testDataDocIdIsNotBlipId() {
    assertFalse(IdUtil.isBlipId("spell"));
    assertFalse(IdUtil.isBlipId("data+token"));
  }

  public void testGhostBlipIdIsBlipId() {
    String id = GHOST_BLIP_PREFIX + TOKEN_SEPARATOR + "1";
    assertTrue(IdUtil.isBlipId(id));
    assertTrue(IdUtil.isGhostBlipId(id));
  }

  public void testInvalidBlipIdsAreNotBlipIds() {
    assertFalse(IdUtil.isBlipId(""));
    assertFalse(IdUtil.isBlipId(BLIP_PREFIX));
    assertFalse(IdUtil.isBlipId(BLIP_PREFIX + "~" + TOKEN_SEPARATOR));
    assertFalse(IdUtil.isBlipId(BLIP_PREFIX + "~" + TOKEN_SEPARATOR + "1"));
  }

  public void testAttachmentIdIsAttachmentId() {
    String coreId = "Gpt39t8i26";
    assertTrue(IdUtil.isAttachmentDataDocument(IdUtil.join(ATTACHMENT_METADATA_PREFIX, coreId)));
  }

  public void testAttachmentIdIsNotBlipId() {
    String coreId = "Gpt39t8i26";
    assertFalse(IdUtil.isBlipId(IdUtil.join(ATTACHMENT_METADATA_PREFIX, coreId)));
  }

  public void testSplitIdTokens() {
    assertArrayEquals(new String[] {"b"}, IdUtil.split("b"));
    assertArrayEquals(new String[] {"b", ""}, IdUtil.split("b+"));
    assertArrayEquals(new String[] {"b+"}, IdUtil.split("b~+"));
    assertArrayEquals(new String[] {"b+", "d"}, IdUtil.split("b~++d"));
    assertArrayEquals(new String[] {"b", "+d"}, IdUtil.split("b+~+d"));
    assertArrayEquals(new String[] {"b", "1234"}, IdUtil.split("b+1234"));
    assertArrayEquals(new String[] {"user", "fred@gwave.com"}, IdUtil.split("user+fred@gwave.com"));
    assertArrayEquals(new String[] {"12+34"}, IdUtil.split("12~+34"));
  }

  public void testJoinIdTokens() {
    assertEquals("b", IdUtil.join("b"));
    assertEquals("b~+", IdUtil.join("b+"));
    assertEquals("b+1234", IdUtil.join("b", "1234"));
    assertEquals("user+fred@gwave.com", IdUtil.join("user", "fred@gwave.com"));
    assertEquals("12~+34", IdUtil.join("12+34"));
  }

  private static <T> void assertArrayEquals(T[] expected, T[] actual) {
    assertEquals(expected.length, actual.length);
    for (int i = 0; i < expected.length; ++i) {
      assertEquals(expected[i], actual[i]);
    }
  }
}
