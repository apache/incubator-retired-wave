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

package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.client.scheduler.Scheduler.IncrementalTask;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Queue;

/**
 * Render blips one by one in a specified order.
 *
 */
public final class BlipQueueRenderer implements IncrementalTask {
  public interface PagingHandler {
    void pageIn(ConversationBlip blip);

    void pageOut(ConversationBlip blip);
  }

  private final TimerService timer;
  private final PagingHandler pager;
  private final Queue<ConversationBlip> toPageIn = CollectionUtils.createQueue();

  @VisibleForTesting
  BlipQueueRenderer(PagingHandler pager, TimerService timer) {
    this.pager = pager;
    this.timer = timer;
  }

  public static BlipQueueRenderer create(PagingHandler pager) {
    return new BlipQueueRenderer(pager, SchedulerInstance.getHighPriorityTimer());
  }

  /**
   * Adds a blip to be rendered asynchronously.
   *
   * @param blip blip to render
   */
  public void add(ConversationBlip blip) {
    if (toPageIn.isEmpty()) {
      timer.schedule(this);
    }
    toPageIn.add(blip);
  }

  /**
   * Renders all blips in the queue synchronously.
   */
  public void flush() {
    if (!toPageIn.isEmpty()) {
      while (execute()) {
      }
      timer.cancel(this);
    }
  }

  @Override
  public boolean execute() {
    pager.pageIn(toPageIn.remove());
    return !toPageIn.isEmpty();
  }
}
