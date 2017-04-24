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

package org.waveprotocol.wave.client.scheduler.knobs;

import junit.framework.TestCase;
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;
import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.Collection;

import static org.mockito.Mockito.*;

/**
 * Tests the controller components for the scheduler.
 *
 */

public class ControllerTest extends TestCase {

  /**
   * Stub implementation of a per-level UI control.  Used both as a dummy and a
   * stub (and no, jmock can not create dummies or stubs).
   */
  private static class StubLevelView implements KnobView {
    private Listener listener;
    private int jobCount;
    private boolean enabled;

    @Override
    public void disable() {
      enabled = false;
    }

    @Override
    public void enable() {
      enabled = true;
    }

    @Override
    public void init(Listener l) {
      this.listener = l;
    }

    @Override
    public void reset() {
      this.listener = null;
    }

    @Override
    public void showCount(int count) {
      this.jobCount = count;
    }

    public Listener getListener() {
      return listener;
    }

    public int getJobCount() {
      return jobCount;
    }

    public boolean isEnabled() {
      return enabled;
    }

    @Override
    public void hideJobs() {
    }

    @Override
    public void showJobs(Collection<String> currentJobs, Collection<String> oldJobs,
        ReadableStringSet suppressedJobs) {
    }
  }

  /** Mocked level view; used in some tests. */
  private KnobView knobView;

  /** Mocked knob-panel view; used in some tests. */
  private KnobsView knobsView;

  /** Stub for a single-level UI control; used in some tests. */
  private StubLevelView stubView;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    knobView = mock(KnobView.class);
    knobsView = mock(KnobsView.class);
    stubView = new StubLevelView();
    for (Priority p : Priority.values()) {
      when(knobsView.create(p)).thenReturn(p.equals(Priority.MEDIUM) ? stubView : new StubLevelView());
    }
  }

  public void testLevelPresenterInitialStateAndClicking() {
    KnobPresenter presenter = new KnobPresenter(knobView);

    presenter.onClicked();
    verify(knobView, times(1)).disable();


    presenter.onClicked();
    verify(knobView, times(2)).enable(); // is enabled by default
  }

  public void testLevelEnabledAndDisabled() {
    KnobPresenter presenter = new KnobPresenter(new StubLevelView());

    presenter.enable();
    assertTrue(presenter.isEnabled());
    presenter.disable();
    assertFalse(presenter.isEnabled());
  }

  public void testKnobsAddLevelForEachPriority() {
    KnobsPresenter presenter = new KnobsPresenter(knobsView);
  }

  public void testClickingOnLevelTogglesRunnability() {
    KnobsPresenter presenter = new KnobsPresenter(knobsView);

    assertNotNull(stubView.getListener());
    assertTrue(presenter.isRunnable(Priority.MEDIUM));
    stubView.getListener().onClicked();
    assertFalse(presenter.isRunnable(Priority.MEDIUM));
    stubView.getListener().onClicked();
    assertTrue(presenter.isRunnable(Priority.MEDIUM));
  }

  public void testUpdatingMediumJobCountUpdatesView() {
    KnobsPresenter presenter = new KnobsPresenter(knobsView);

    Schedulable a = new Schedulable(){};
    presenter.jobAdded(Priority.MEDIUM, a);
    assertEquals(1, stubView.getJobCount());

    presenter.jobAdded(Priority.MEDIUM, new Schedulable(){});
    assertEquals(2, stubView.getJobCount());

    presenter.jobRemoved(Priority.MEDIUM, a);
    assertEquals(1, stubView.getJobCount());

    presenter.jobAdded(Priority.HIGH, new Schedulable(){});
    assertEquals(1, stubView.getJobCount());
  }
}
