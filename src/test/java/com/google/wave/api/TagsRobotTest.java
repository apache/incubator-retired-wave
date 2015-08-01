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

import junit.framework.TestCase;

import java.util.Collections;
import java.util.Iterator;

/**
 * Test cases for {@link Tags}.
 */
public class TagsRobotTest extends TestCase {

  private OperationQueue opQueue = mock(OperationQueue.class);
  private Wavelet wavelet = mock(Wavelet.class);
  private Tags tags = new Tags(Collections.<String>emptyList(), wavelet, opQueue);

  public void testAdd() throws Exception {
    assertTrue(tags.add("tag1"));
    assertTrue(tags.add("tag2"));
    assertFalse(tags.add("tag1"));
    assertEquals(2, tags.size());

    verify(opQueue).modifyTagOfWavelet(wavelet, "tag1", "add");
    verify(opQueue).modifyTagOfWavelet(wavelet, "tag2", "add");
  }

  public void testRemove() throws Exception {
    assertTrue(tags.add("tag1"));
    assertTrue(tags.add("tag2"));

    assertFalse(tags.remove("tag3"));
    assertEquals(2, tags.size());

    assertTrue(tags.remove("tag1"));
    assertEquals(1, tags.size());

    assertTrue(tags.remove("tag2"));
    assertEquals(0, tags.size());

    verify(opQueue).modifyTagOfWavelet(wavelet, "tag1", "remove");
    verify(opQueue).modifyTagOfWavelet(wavelet, "tag2", "remove");
  }

  public void testIterator() throws Exception {
    assertTrue(tags.add("tag1"));
    assertTrue(tags.add("tag2"));
    assertFalse(tags.add("tag1"));

    Iterator<String> it = tags.iterator();
    assertTrue(it.hasNext());
    assertEquals("tag1", it.next());
    assertTrue(it.hasNext());
    assertEquals("tag2", it.next());
    assertFalse(it.hasNext());

    it = tags.iterator();
    try {
      it.remove();
      fail("Should have failed, since remove() was called before next().");
    } catch (IllegalStateException e) {
      // Expected.
    }

    String nextItem = it.next();
    assertEquals("tag1", nextItem);
    assertTrue(tags.contains(nextItem));
    it.remove();
    assertFalse(tags.contains(nextItem));
    assertEquals(1, tags.size());

    try {
      it.remove();
      fail("Should have failed, since remove() has been called after the last next().");
    } catch (IllegalStateException e) {
      // Expected.
    }

    assertEquals("tag2", it.next());

    verify(opQueue).modifyTagOfWavelet(wavelet, "tag1", "remove");
  }
}
