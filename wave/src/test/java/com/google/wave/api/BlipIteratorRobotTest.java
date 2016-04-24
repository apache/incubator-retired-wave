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
import static org.mockito.Mockito.when;

import com.google.wave.api.BlipIterator.ElementIterator;
import com.google.wave.api.BlipIterator.SingleshotIterator;
import com.google.wave.api.BlipIterator.TextIterator;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Test cases for {@link BlipIterator}.
 */
public class BlipIteratorRobotTest extends TestCase {

  private Wavelet wavelet;

  @Override
  protected void setUp() throws Exception {
    wavelet = mock(Wavelet.class);
    when(wavelet.getOperationQueue()).thenReturn(new OperationQueue());
    when(wavelet.getWaveId()).thenReturn(WaveId.of("example.com", "wave1"));
    when(wavelet.getWaveletId()).thenReturn(WaveletId.of("example.com", "wavelet1"));
  }

  public void testSingleshotIterator() throws Exception {
    Blip blip = new Blip("blip1", "\n1 1 1", null, null, wavelet);
    SingleshotIterator iterator = new BlipIterator.SingleshotIterator(blip, 0, 1);
    assertTrue(iterator.hasNext());
    Range range = iterator.next();
    assertEquals(0, range.getStart());
    assertEquals(1, range.getEnd());
    assertFalse(iterator.hasNext());
  }

  public void testTextIteratorWithoutShift() {
    Blip blip = new Blip("blip1", "\n1 1 1", null, null, wavelet);
    TextIterator iterator = new BlipIterator.TextIterator(blip, "1", -1);

    for (int i = 0; i < 3; ++i) {
      assertTrue(iterator.hasNext());

      Range range = iterator.next();
      assertNotNull(range);
      assertEquals(i * 2 + 1, range.getStart());
      assertEquals(i * 2 + 2, range.getEnd());
    }
    assertFalse(iterator.hasNext());
  }

  public void testTextIteratorDeletingMatches() {
    Blip blip = new Blip("blip1", "\n1 1 1", null, null, wavelet);
    TextIterator iterator = new BlipIterator.TextIterator(blip, "1", -1);

    for (int i = 0; i < 3; ++i) {
      assertTrue(iterator.hasNext());
      Range range = iterator.next();

      assertEquals(i + 1, range.getStart());
      blip.setContent(blip.getContent().substring(0, range.getStart()) +
          blip.getContent().substring(range.getEnd()));
      iterator.shift(-1);
    }
    assertFalse(iterator.hasNext());
    assertEquals("\n  ", blip.getContent());
  }

  public void testTextIteratorShiftInsertAfter() {
    Blip blip = new Blip("blip1", "\nfoofoofoo", null, null, wavelet);
    TextIterator iterator = new BlipIterator.TextIterator(blip, "foo", -1);

    for (int i = 0; i < 3; ++i) {
      assertTrue(iterator.hasNext());
      Range range = iterator.next();

      assertEquals(i * 6 + 1, range.getStart());
      blip.setContent(blip.getContent().substring(0, range.getEnd()) + "foo" +
          blip.getContent().substring(range.getEnd()));
      iterator.shift(range.getEnd() - range.getStart() + 2);
    }
    assertFalse(iterator.hasNext());
    assertEquals("\nfoofoofoofoofoofoo", blip.getContent());
  }

  public void testElementIterator() {
    Element element1 = new Gadget("http://www.google.com/gadget.xml");
    Element element2 = new Image("attachment1", "the coolest photo");
    Element element3 = new Gadget("http://www.google.com/foo.xml");
    Element element4 = new Gadget("http://www.google.com/gadget.xml");
    SortedMap<Integer, Element> elements = new TreeMap<Integer, Element>();
    elements.put(1, element1);
    elements.put(2, element2);
    elements.put(4, element3);
    elements.put(5, element4);

    Blip blip = new Blip("blip1", Collections.<String>emptyList(), "\n  a  ",
        Collections.<String>emptyList(), null, -1, -1, null, null, new ArrayList<Annotation>(), elements,
        new ArrayList<String>(), wavelet);

    Map<String, String> restrictions = new HashMap<String, String>();
    restrictions.put("url", "http://www.google.com/gadget.xml");

    ElementIterator iterator = new BlipIterator.ElementIterator(blip, ElementType.GADGET,
        restrictions, -1);

    List<Range> hits = new ArrayList<Range>();
    while (iterator.hasNext()) {
      hits.add(iterator.next());
    }
    assertEquals(2, hits.size());
    assertEquals(1, hits.get(0).getStart());
    assertEquals(2, hits.get(0).getEnd());
    assertEquals(5, hits.get(1).getStart());
    assertEquals(6, hits.get(1).getEnd());
  }
}
