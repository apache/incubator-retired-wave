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

import org.waveprotocol.wave.model.id.IdGeneratorImpl.Seed;

/**
 * Tests for IdGeneratorImpl.
 * @author zdwang@google.com (David Wang)
 *
 */
public class IdGeneratorTest extends TestCase {
  private static final String DOMAIN = "example.com";

  private IdGenerator idGenerator;

  @Override
  protected void setUp() throws Exception {
    idGenerator = new IdGeneratorImpl(DOMAIN, new Seed() {
      @Override
      public String get() {
        return "seed";
      }
    });
  }
  
  public void testCreateWaveId() {
    WaveId waveId = idGenerator.newWaveId();
    assertEquals(DOMAIN, waveId.getDomain());
    assertEquals("w+seedA", waveId.getId());
  }
  
  public void testCreateConversationWaveletId() {
    WaveletId waveletId = idGenerator.newConversationWaveletId();
    assertEquals(DOMAIN, waveletId.getDomain());
    assertEquals("conv+seedA", waveletId.getId());
  }

  public void testCreateConversationRootWaveletId() {
    WaveletId waveletId = idGenerator.newConversationRootWaveletId();
    assertEquals(DOMAIN, waveletId.getDomain());
    assertEquals("conv+root", waveletId.getId());
  }

  public void testCreateUserDataWaveletId() {
    WaveletId waveletId = idGenerator.newUserDataWaveletId("fred@wavesandbox.com");
    assertEquals("wavesandbox.com", waveletId.getDomain());
    assertEquals("user+fred@wavesandbox.com", waveletId.getId());
  }
  
  public void testCreateUserDataWaveletIdIllegalAddress() {
    try {
      WaveletId waveletId = idGenerator.newUserDataWaveletId("fred");
      fail("Invalid address was accepted");
    } catch (IllegalArgumentException e) {
      // pass
    }
  }

  public void testCreateBlipId() {
    String blipId = idGenerator.newBlipId();
    assertEquals("b+seedA", blipId);
  }

  public void testCreateDataDocumentId() {
    String docId = idGenerator.newDataDocumentId();
    assertEquals("seedA", docId);
  }

  public void testCreateNamespacedId() {
    String id = idGenerator.newId("namespace");
    assertEquals("namespace+seedA", id);
  }

  public void testCreateUniqueToken() {
    String token = idGenerator.newDataDocumentId();
    assertEquals("seedA", token);
  }

  public void testSequentialTokensAreDistinct() {
    assertFalse(idGenerator.newUniqueToken().equals(idGenerator.newUniqueToken()));
  }

  public void testBase64Encoding() {
    assertEquals("A", IdGeneratorImpl.base64Encode(0));
    assertEquals("B", IdGeneratorImpl.base64Encode(1));
    assertEquals("_", IdGeneratorImpl.base64Encode(63));
    assertEquals("BA", IdGeneratorImpl.base64Encode(64));
    assertEquals("BB", IdGeneratorImpl.base64Encode(65));
    assertEquals("B_", IdGeneratorImpl.base64Encode((2 * 64) - 1));
    assertEquals("CA", IdGeneratorImpl.base64Encode(2 * 64));
    assertEquals("D_", IdGeneratorImpl.base64Encode((4 * 64) - 1));
    assertEquals("__", IdGeneratorImpl.base64Encode((64 * 64) -1));
    assertEquals("BAA", IdGeneratorImpl.base64Encode(64 * 64));
    assertEquals("BAAA", IdGeneratorImpl.base64Encode(64 * 64 * 64));
    assertEquals("B_____", IdGeneratorImpl.base64Encode(Integer.MAX_VALUE)); // 2 * 64^5 -1
  }
}
