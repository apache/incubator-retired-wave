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

package org.waveprotocol.wave.model.util;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.OffsetList.Container;
import org.waveprotocol.wave.model.util.OffsetList.LocationAction;

/**
 * Tests for EvaluableOffsetList.
 *
 */

public class EvaluableOffsetListTest extends TestCase {

  public void testPerformActionAtEnd() {
    final OffsetList<Integer> offsetList = new EvaluableOffsetList<Integer, Void>();
    for (int i = 1; i <= 10; ++i) {
      offsetList.sentinel().insertBefore(i, i);
    }
    offsetList.performActionAt(55, new LocationAction<Integer, Void>() {
      @Override
      public Void performAction(Container<Integer> container, int offset) {
        assertTrue(container == offsetList.sentinel());
        assertNull(container.getValue());
        assertEquals(0, offset);
        return null;
      }
    });
  }

  public void testPerformAction() {
    performTest(0, 1, 0);
    performTest(1, 2, 0);
    performTest(2, 2, 1);
    performTest(3, 3, 0);
    performTest(4, 3, 1);
    performTest(10, 5, 0);
    performTest(14, 5, 4);
    performTest(15, 6, 0);
    performTest(54, 10, 9);
  }

  private void performTest(int location, final Integer expectedValue, final int expectedOffset) {
    OffsetList<Integer> offsetList = new EvaluableOffsetList<Integer, Void>();
    for (int i = 1; i <= 10; ++i) {
      offsetList.sentinel().insertBefore(i, i);
    }
    offsetList.performActionAt(location, new LocationAction<Integer, Void>() {
      @Override
      public Void performAction(Container<Integer> container, int offset) {
        assertEquals(expectedValue, container.getValue());
        assertEquals(expectedOffset, offset);
        return null;
      }
    });
  }

}
