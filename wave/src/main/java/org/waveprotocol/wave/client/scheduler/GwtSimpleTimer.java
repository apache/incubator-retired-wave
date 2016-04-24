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

import com.google.gwt.core.client.Duration;
import com.google.gwt.user.client.Timer;

/**
 * GWT-based SimpleTimer implementation
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class GwtSimpleTimer implements SimpleTimer {

  /**
   * Factory for creating a GWT-based simple timer
   *
   * TODO(danilatos): Disallow creation of more than one timer, assuming its use
   * will be in the one and only scheduler?
   */
  public static final SimpleTimer.Factory FACTORY = new SimpleTimer.Factory() {
    public SimpleTimer create(Runnable task) {
      return new GwtSimpleTimer(task);
    }
  };

  /** The GWT timer used */
  private final Timer timer = new Timer() {
    @Override
    public void run() {
      task.run();
    }
  };

  /** The task to schedule */
  private final Runnable task;

  private GwtSimpleTimer(Runnable task) {
    this.task = task;
  }

  /** {@inheritDoc} */
  public double getTime() {
    return Duration.currentTimeMillis();
  }

  /** {@inheritDoc} */
  public void schedule() {
    // NOTE(danilatos): 1 instead of 0, because of some assertion in GWT's Timer
    // TODO(danilatos): Consider directly using setTimeout and not
    // GWT's timer, or fixing GWT's timer? There are some browser-specific
    // tunings in their implementation, so it doesn't make sense to just
    // ignore it.
    timer.schedule(1);
  }

  /** {@inheritDoc} */
  public void cancel() {
    timer.cancel();
  }

  /** {@inheritDoc} */
  public void schedule(double when) {
    int interval = (int) (when - getTime());
    timer.schedule(interval >= 1 ? interval : 1);
  }

}
