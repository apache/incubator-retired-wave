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

package com.google.wave.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.wave.api.impl.WaveletData;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Test cases for {@link Wavelet}.
 */
public class WaveletRobotTest extends TestCase {

  private static final WaveId WAVE_ID = WaveId.of("example.com", "wave1");
  private static final WaveletId WAVELET_ID = WaveletId.of("example.com", "wavelet1");

  private OperationQueue opQueue;
  private Wavelet wavelet;
  private Blip rootBlip;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    rootBlip = mock(Blip.class);
    Map<String, String> dataDocument = new HashMap<String, String>();
    Set<String> tags = Collections.emptySet();
    Map<String, Blip> blips = new HashMap<String, Blip>();
    blips.put("blip1", rootBlip);
    Set<String> participants = new LinkedHashSet<String>();
    participants.add("foo@bar.com");
    Map<String, String> roles = new HashMap<String, String>();
    Map<String, BlipThread> threads = new HashMap<String, BlipThread>();

    opQueue = mock(OperationQueue.class);
    wavelet = new Wavelet(WAVE_ID, WAVELET_ID, "foo@bar.com", 1l, 1l, "Hello world", "blip1",
        null, roles, participants, dataDocument, tags, blips, threads, opQueue);
  }

  public void testSetTitle() throws Exception {
    when(rootBlip.getContent()).thenReturn("\nOld title\n\nContent");
    wavelet.setTitle("New title");
    verify(opQueue).setTitleOfWavelet(wavelet, "New title");
    verify(rootBlip).setContent("\nNew title\n\nContent");
  }

  public void testSetTitleAdjustRootBlipWithOneLineProperly() throws Exception {
    when(rootBlip.getContent()).thenReturn("\nOld title");
    wavelet.setTitle("New title");
    verify(opQueue).setTitleOfWavelet(wavelet, "New title");
    verify(rootBlip).setContent("\nNew title\n");
  }

  public void testSetTitleAdjustEmptyRootBlipProperly() throws Exception {
    when(rootBlip.getContent()).thenReturn("\n");
    wavelet.setTitle("New title");
    verify(opQueue).setTitleOfWavelet(wavelet, "New title");
    verify(rootBlip).setContent("\nNew title\n");
  }

  public void testSetRobotAddress() throws Exception {
    assertNull(wavelet.getRobotAddress());
    wavelet.setRobotAddress("foo@appspot.com");
    assertEquals("foo@appspot.com", wavelet.getRobotAddress());

    try {
      wavelet.setRobotAddress("bar@appspot.com");
      fail("Should have failed when trying to call Wavelet.setRobotAddress() for the second time");
    } catch (IllegalStateException e) {
      assertEquals("Robot address has been set previously to foo@appspot.com", e.getMessage());
    }
  }

  public void testGetDomain() throws Exception {
    assertEquals("example.com", wavelet.getDomain());
  }

  public void testProxyFor() throws Exception {
    OperationQueue proxiedQueue = mock(OperationQueue.class);
    when(opQueue.proxyFor("user2")).thenReturn(proxiedQueue);

    wavelet.setRobotAddress("foo+user1#5@appspot.com");
    Wavelet proxiedWavelet = wavelet.proxyFor("user2");

    assertTrue(wavelet.getParticipants().contains("foo+user2#5@appspot.com"));
    assertEquals(proxiedQueue, proxiedWavelet.getOperationQueue());
  }

  public void testProxyForShouldFailIfProxyIdIsInvalid() throws Exception {
    try {
      wavelet.proxyFor("foo@gmail.com");
      fail("Should have failed since proxy id is not encoded.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }

  public void testSubmitWith() throws Exception {
    OperationQueue otherOpQueue = mock(OperationQueue.class);
    Set<String> participants = new LinkedHashSet<String>();
    participants.add("foo@bar.com");
    Map<String, String> roles = new HashMap<String, String>();
    Map<String, BlipThread> threads = new HashMap<String, BlipThread>();

    Wavelet other = new Wavelet(WAVE_ID, WAVELET_ID, "foo@bar.com", 1l, 1l, "Hello world",
        "blip1", null, roles, participants, new HashMap<String, String>(),
        Collections.<String>emptySet(), new HashMap<String, Blip>(), threads, otherOpQueue);

    wavelet.submitWith(other);
    verify(opQueue).submitWith(otherOpQueue);
  }

  public void testReply() throws Exception {
    assertEquals(1, wavelet.getBlips().size());

    Blip replyBlip1 = mock(Blip.class);
    when(replyBlip1.getBlipId()).thenReturn("replyblip1");
    Blip replyBlip2 = mock(Blip.class);
    when(replyBlip2.getBlipId()).thenReturn("replyblip2");

    when(opQueue.appendBlipToWavelet(wavelet, "\n")).thenReturn(replyBlip1);
    when(opQueue.appendBlipToWavelet(wavelet, "\nFoo")).thenReturn(replyBlip2);

    try {
      wavelet.reply(null);
      fail("Should have failed when calling Wavelet.reply(null).");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    try {
      wavelet.reply("Foo");
      fail("Should have failed when calling Wavelet.reply(String) with arg that doesn't start " +
          "with a newline char.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    Blip newBlip1 = wavelet.reply("\n");
    Blip newBlip2 = wavelet.reply("\nFoo");

    //operation must be submitted before it is added to wavelet, so should still be 1
    assertEquals(1, wavelet.getBlips().size());
    assertTrue(newBlip1.getBlipId().contains("replyblip1"));
    assertTrue(newBlip2.getBlipId().contains("replyblip2"));
  }

  public void testDeleteByBlip() throws Exception {
    Blip parentBlip = mock(Blip.class);
    when(parentBlip.getBlipId()).thenReturn("parentblipid");

    Blip childBlip = mock(Blip.class);
    when(childBlip.getBlipId()).thenReturn("childblipid");
    when(childBlip.getParentBlip()).thenReturn(parentBlip);

    Map<String, Blip> blips = new HashMap<String, Blip>();
    blips.put("parentblipid", parentBlip);
    blips.put("childblipid", childBlip);

    Set<String> participants = new LinkedHashSet<String>();
    participants.add("foo@bar.com");

    Map<String, String> roles = new HashMap<String, String>();
    Map<String, BlipThread> threads = new HashMap<String, BlipThread>();

    wavelet = new Wavelet(WAVE_ID, WAVELET_ID, "foo@bar.com", 1l, 1l, "Hello world",
        "parentblipid", null, roles, participants, new HashMap<String, String>(),
        new LinkedHashSet<String>(), blips, threads, opQueue);

    assertEquals(2, wavelet.getBlips().size());
    wavelet.delete(childBlip);

    assertEquals(1, wavelet.getBlips().size());
    verify(parentBlip).deleteChildBlipId("childblipid");
    verify(opQueue).deleteBlip(wavelet, "childblipid");
  }

  public void testDeleteByBlipId() throws Exception {
    assertEquals(1, wavelet.getBlips().size());
    assertEquals("blip1", wavelet.getBlips().entrySet().iterator().next().getKey());

    wavelet.delete("blip1");
    assertEquals(0, wavelet.getBlips().size());
    verify(opQueue).deleteBlip(wavelet, "blip1");
  }

  public void testSerializeAndDeserialize() throws Exception {
    Blip blipOne = mock(Blip.class);
    when(blipOne.getBlipId()).thenReturn("blip1");

    Map<String, String> dataDocument = new HashMap<String, String>();
    Set<String> tags = Collections.<String>emptySet();
    Map<String, Blip> blips = new HashMap<String, Blip>();
    blips.put("blip1", blipOne);
    Set<String> participants = new LinkedHashSet<String>();
    participants.add("foo@bar.com");

    Map<String, String> roles = new HashMap<String, String>();
    Map<String, BlipThread> threads = new HashMap<String, BlipThread>();

    OperationQueue opQueue = mock(OperationQueue.class);
    Wavelet expectedWavelet = new Wavelet(WaveId.of("example.com", "wave1"),
        WaveletId.of("example.com", "wavelet1"), "foo@bar.com", 1l, 1l, "Hello world",
        "blip1", null, roles, participants, dataDocument, tags, blips, threads, opQueue);

    WaveletData waveletData = expectedWavelet.serialize();
    Wavelet actualWavelet = Wavelet.deserialize(opQueue, blips, threads, waveletData);

    assertEquals(expectedWavelet.getWaveId(), actualWavelet.getWaveId());
    assertEquals(expectedWavelet.getWaveletId(), actualWavelet.getWaveletId());
    assertEquals(expectedWavelet.getRootBlip().getBlipId(),
        actualWavelet.getRootBlip().getBlipId());

    assertEquals(expectedWavelet.getCreationTime(), actualWavelet.getCreationTime());
    assertEquals(expectedWavelet.getCreator(), actualWavelet.getCreator());
    assertEquals(expectedWavelet.getLastModifiedTime(), actualWavelet.getLastModifiedTime());
    assertEquals(expectedWavelet.getTitle(), actualWavelet.getTitle());

    assertEquals(expectedWavelet.getParticipants().size(), actualWavelet.getParticipants().size());
    assertEquals(expectedWavelet.getTags().size(), actualWavelet.getTags().size());
    assertEquals(expectedWavelet.getDataDocuments().size(),
        actualWavelet.getDataDocuments().size());
  }
}
