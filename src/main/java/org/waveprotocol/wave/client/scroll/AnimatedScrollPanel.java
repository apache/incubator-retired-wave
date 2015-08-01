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

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.Duration;

import org.waveprotocol.wave.client.common.util.MathUtil;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.TimerService;

/**
 * Decorates a scroller, providing a layer of animated scroll movement.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class AnimatedScrollPanel<T> implements ScrollPanel<T>, Task {
  /** Time period for the animation. */
  private final static int ANIMATION_DURATION_MS = 300;

  private final ScrollPanel<? super T> target;
  private final TimerService scheduler;

  // Animation state.
  private Duration counter;
  private double startLocation;
  private double endLocation;

  /**
   * Creates an animating scroller.
   *
   * @param scheduler scheduler for running animations
   * @param target underlying scroller
   */
  @VisibleForTesting
  AnimatedScrollPanel(TimerService scheduler, ScrollPanel<? super T> target) {
    this.scheduler = scheduler;
    this.target = target;
  }

  /**
   * Creates an animation layer over a scroller.
   */
  public static <T> AnimatedScrollPanel<T> create(ScrollPanel<? super T> target) {
    return new AnimatedScrollPanel<T>(SchedulerInstance.getHighPriorityTimer(), target);
  }

  @Override
  public void moveTo(double location) {
    startLocation = getViewport().getStart();
    endLocation = location;
    counter = new Duration();
    execute();
    scheduler.scheduleDelayed(this, 0);
  }

  /**
   * Sets the underlying target's scroll location to the current animation
   * location.
   */
  @Override
  public void execute() {
    int time = counter.elapsedMillis();
    target.moveTo(location(time));
    if (time < ANIMATION_DURATION_MS) {
      // Run again, but delayed so that paint can occur first.
      scheduler.scheduleDelayed(this, 0);
    }
  }

  /** @return the animation's scroll location at a particular time. */
  private double location(int time) {
    // Clip current time to start/end bounds.
    double ctime = MathUtil.clip(0, ANIMATION_DURATION_MS, time);
    // Follow first quadrant of sine.
    double t = (ctime / ANIMATION_DURATION_MS) * (Math.PI / 2);
    double x = Math.sin(t);
    return startLocation + x * (endLocation - startLocation);
  }

  @Override
  public Extent getViewport() {
    return target.getViewport();
  }

  @Override
  public Extent getContent() {
    return target.getContent();
  }

  @Override
  public Extent extentOf(T measurable) {
    return target.extentOf(measurable);
  }
}
