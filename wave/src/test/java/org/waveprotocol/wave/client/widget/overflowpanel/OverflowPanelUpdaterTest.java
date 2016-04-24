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

package org.waveprotocol.wave.client.widget.overflowpanel;

import org.waveprotocol.wave.client.widget.overflowpanel.OverflowPanelUpdater.OverflowPanel;

import junit.framework.TestCase;

import java.util.HashSet;
import java.util.Set;

/**
 * Puts the overflow decorator algorithm through various cases to test interaction
 * with a pojo overflow decorator.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */

public class OverflowPanelUpdaterTest extends TestCase {
  // Check the case where nothing has overflowed
  public void testAllVisibleAndShown() {
    MockOverflowPanel panel = new MockOverflowPanel(10);
    panel.setLastVisible(9);

    OverflowPanelUpdater controller = new OverflowPanelUpdater(panel);
    controller.updateState();
    panel.check(1);
  }

  // Check the case where everything has overflowed
  public void testAllVisibleNoneShown() {
    MockOverflowPanel panel = new MockOverflowPanel(10);
    panel.setLastVisible(-1);
    OverflowPanelUpdater controller = new OverflowPanelUpdater(panel);
    controller.updateState();
    panel.check(1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
  }

  // Check the case where some things have overflowed
  public void testAllVisibleSomeShown() {
    MockOverflowPanel panel = new MockOverflowPanel(10);
    panel.setLastVisible(4);
    OverflowPanelUpdater controller = new OverflowPanelUpdater(panel);
    controller.updateState();
    panel.check(1, 5, 6, 7, 8, 9);
  }

  // Check the case where some things have overflowed, and invisible items are included
  public void testSomeVisibleSomeShown() {
    MockOverflowPanel panel = new MockOverflowPanel(10, 2, 6, 7);
    panel.setLastVisible(4);
    OverflowPanelUpdater controller = new OverflowPanelUpdater(panel);
    controller.updateState();
    panel.check(1, 5, 8, 9);
  }

  // Check the case where invisible items are right on the border
  public void testSomeVisibleSomeShownBorder() {
    MockOverflowPanel panel = new MockOverflowPanel(10, 2, 6, 7);
    panel.setLastVisible(6);
    OverflowPanelUpdater controller = new OverflowPanelUpdater(panel);
    controller.updateState();
    panel.check(1, 8, 9);
  }

  // Check that calling multiple times still works fine
  public void checkMultipleCalls() {
    MockOverflowPanel panel = new MockOverflowPanel(10);
    OverflowPanelUpdater controller = new OverflowPanelUpdater(panel);

    // resize!
    panel.setLastVisible(9);
    controller.updateState();
    // resize!
    panel.setLastVisible(-1);
    controller.updateState();
    // resize!
    panel.setLastVisible(7);
    controller.updateState();

    panel.check(3, 8, 9);
  }

  ///
  /// MOCK
  ///

  // Simple mock utility for a POJO overflow panel.
  private static class MockOverflowPanel implements OverflowPanel {
    // parameters
    private final int numWidgets;
    private final Set<Integer> invisibleWidgets = new HashSet<Integer>();

    // mutable state trackers
    private int onBeginCount = 0;
    private int onEndCount = 0;
    private int lastNotHidden = 0;
    private boolean moreButtonIsVisible = false;
    private final Set<Integer> inOverflowBucket = new HashSet<Integer>();

    // set up a mock object
    public MockOverflowPanel(int numWidgets, int... invisible) {
      this.numWidgets = numWidgets;
      for (int index : invisible) {
        invisibleWidgets.add(index);
      }
    }

    public void setLastVisible(int lastIndex) {
      lastNotHidden = lastIndex;
      while (lastNotHidden >= 0 && hasOverflowed(lastIndex)) {
        lastIndex--;
      }
    }

    // check that the state is correct.
    public void check(int callCount, int... inOverflow) {
      assertEquals(callCount, onBeginCount);
      assertEquals(onBeginCount, onEndCount);
      assertEquals(inOverflow.length, inOverflowBucket.size());
      for (int index : inOverflow) {
        assertTrue(inOverflowBucket.contains(index));
      }
      assertEquals(inOverflow.length > 0, moreButtonIsVisible);
    }

    // mocked methods:
    @Override public boolean hasOverflowed(int index) {
      return index > lastNotHidden;
    }
    @Override public boolean isVisible(int index) {
      return !invisibleWidgets.contains(index);
    }
    @Override public void onBeginOverflowLayout() {
      onBeginCount++;
    }
    @Override public void onEndOverflowLayout() {
      onEndCount++;
    }
    @Override public void moveToOverflowBucket(int index) {
      if (isVisible(index)) {
        assertTrue(
            "Must not move into bucket something already there", inOverflowBucket.add(index));
      }
    }
    @Override public int getWidgetCount() {
      return numWidgets;
    }
    @Override public void showMoreButton() {
      moreButtonIsVisible = true;
    }
  }
}
