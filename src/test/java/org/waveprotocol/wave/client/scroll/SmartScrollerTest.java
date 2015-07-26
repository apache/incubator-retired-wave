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

package org.waveprotocol.wave.client.scroll;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for {@link SmartScroller}.
 *
 */

public final class SmartScrollerTest extends TestCase {

  // The scrolling interfaces are generic in the type of scroll targets, so
  // strings are used as arbitrary targets.

  /** Sample source in the scroll space. */
  private final static String source = "a";

  /** Sample target in the scroll space. */
  private final static String target = "b";

  @Mock
  private ScrollPanel<String> mockPanel;

  /** Target under test. */
  private SmartScroller<String> scroller;

  @Override
  protected void setUp() {
    MockitoAnnotations.initMocks(this);
    scroller = new SmartScroller<String>(mockPanel);
  }

  private void prepare() {
    when(mockPanel.getViewport()).thenReturn(Extent.of(100, 200));
    when(mockPanel.extentOf(source)).thenReturn(Extent.of(130, 140));
    scroller.moveTo(source);
    reset((Object) mockPanel);
    when(mockPanel.getViewport()).thenReturn(Extent.of(100, 200));
    when(mockPanel.extentOf(source)).thenReturn(Extent.of(130, 140));
  }

  public void testInitialViewport() {
    when(mockPanel.getViewport()).thenReturn(Extent.of(100, 200));
    when(mockPanel.extentOf(source)).thenReturn(Extent.of(130, 140));
    scroller.moveTo(source);
    verify(mockPanel).moveTo(100);
  }

  public void testTransitionToVisibleTargetDoesNotMoveViewport() {
    prepare();

    when(mockPanel.extentOf(target)).thenReturn(Extent.of(160, 170));
    scroller.moveTo(target);
    verify(mockPanel).moveTo(100);
  }

  public void testTransitionToMassiveTargetPutsTopAtTop() {
    prepare();

    when(mockPanel.extentOf(target)).thenReturn(Extent.of(400, 800));
    scroller.moveTo(target);
    verify(mockPanel).moveTo(400);

    when(mockPanel.getViewport()).thenReturn(Extent.of(400, 500));
    when(mockPanel.extentOf(target)).thenReturn(Extent.of(-800, -400));
    scroller.moveTo(target);
    verify(mockPanel).moveTo(-800);
  }

  public void testTransitionToOffscreenTargetPreservesEyelineWhenValid() {
    prepare();

    // Eyeline is at 30.
    when(mockPanel.extentOf(target)).thenReturn(Extent.of(300, 320));
    scroller.moveTo(target);
    verify(mockPanel).moveTo(270);
  }

  public void testTransitionToPartialOffTargetPreservesEyelineWhenValid() {
    prepare();

    // Eyeline is at 30.
    when(mockPanel.extentOf(target)).thenReturn(Extent.of(180, 220));
    scroller.moveTo(target);
    verify(mockPanel).moveTo(150);
  }

  public void testTransitionFromOffscreenTargetHasMinimumViewportShift() {
    prepare();

    when(mockPanel.getViewport()).thenReturn(Extent.of(-100, 0));
    when(mockPanel.extentOf(target)).thenReturn(Extent.of(300, 320));
    scroller.moveTo(target);
    verify(mockPanel).moveTo(220);

    when(mockPanel.getViewport()).thenReturn(Extent.of(220, 320));
    when(mockPanel.extentOf(target)).thenReturn(Extent.of(0, 20));
    scroller.moveTo(target);
    verify(mockPanel).moveTo(0);
  }

  public void testSourceExtentIsDynamic() {
    prepare();

    // Old target shifts from [130,140] to [170,180]
    when(mockPanel.extentOf(source)).thenReturn(Extent.of(170, 180));
    // Verify that scroller picks up new source location.
    when(mockPanel.extentOf(target)).thenReturn(Extent.of(70, 80));
    scroller.moveTo(target);
    verify(mockPanel).moveTo(0);
  }
}
