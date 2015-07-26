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


package org.waveprotocol.wave.client.scheduler;

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.user.client.ui.Widget;
import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;

/**
 * Provides a single Scheduler instance for all of the Wave client. This is
 * only a temporary measure while we convert existing classes to use a
 * dependency injected Scheduler.
 *
 */
public class SchedulerInstance {
  private static BrowserBackedScheduler instance;

  private static TimerService low;
  private static TimerService high;
  private static TimerService medium;

  /** Inject a default BrowserBackedScheduler */
  @VisibleForTesting
  public static void setSchedulerInstance(BrowserBackedScheduler instance) {
    SchedulerInstance.instance = instance;
    setDefaultTimerService();
  }

  private static void setDefaultTimerService() {
    low = new SchedulerTimerService(instance, Priority.LOW);
    high = new SchedulerTimerService(instance, Priority.HIGH);
    medium = new SchedulerTimerService(instance, Priority.MEDIUM);
  }

  /** Initialise a default BrowserBackedScheduler if there wasn't one */
  private static void init() {
    if (instance == null) {
      setSchedulerInstance(new BrowserBackedScheduler(GwtSimpleTimer.FACTORY, Controller.NOOP));
    }
  }

  /**
   * @return A shared Scheduler instance.
   */
  public static Scheduler get() {
    init();
    return instance;
  }

  /**
   * @return A shared low-priority timer service based on the scheduler instance.
   */
  public static TimerService getLowPriorityTimer() {
    init();
    return low;
  }

  /**
   * @return A shared high-priority timer service based on the scheduler instance.
   */
  public static TimerService getHighPriorityTimer() {
    init();
    return high;
  }

  /**
   * @return A shared high-priority timer service based on the scheduler instance.
   */
  public static TimerService getMediumPriorityTimer() {
    init();
    return medium;
  }

  /**
   * @return a widget for displaying and controlling the singleton scheduler.
   */
  public static Widget getController() {
    init();
    return instance.getController();
  }
}
