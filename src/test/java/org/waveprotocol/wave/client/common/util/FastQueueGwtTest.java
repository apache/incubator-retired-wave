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

package org.waveprotocol.wave.client.common.util;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * @author piotrkaleta@google.com (Piotr Kaleta)
 */
public class FastQueueGwtTest extends GWTTestCase {

  public void testAddNull() throws Exception {
    Queue<Integer> queue = new FastQueue<Integer>();
    queue.add(null);
    Integer value = queue.remove();
    assertEquals(null, value);
  }

  public void testQueue() throws Exception {
    final int TEST_ITERS = 500;
    int nextInt = 0;
    Random random = new Random(1);
    Queue<Integer> queue = new FastQueue<Integer>();
    Queue<Integer> reference = new LinkedList<Integer>();
    for (int i = 0; i < TEST_ITERS; i++) {
      if (random.nextBoolean()) {
        // enqueue test
        Integer value = random.nextDouble() < 0.9 ? nextInt++ : null;
        queue.offer(value);
        reference.offer(value);
        assertEquals(reference.size(), queue.size());
      } else {
        // dequeue test
        Integer actual = queue.poll();
        Integer expected = reference.poll();
        assertEquals(expected, actual);
        assertEquals(reference.size(), queue.size());
      }
    }
    // try the rest
    while (!reference.isEmpty()) {
      Integer actual = queue.poll();
      Integer expected = reference.poll();
      assertEquals(expected, actual);
      assertEquals(reference.size(), queue.size());
    }
  }

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.common.util.tests";
  }
}
