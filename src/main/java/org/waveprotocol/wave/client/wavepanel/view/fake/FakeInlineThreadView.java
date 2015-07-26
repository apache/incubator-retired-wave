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

package org.waveprotocol.wave.client.wavepanel.view.fake;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.common.util.LinkedSequence;
import org.waveprotocol.wave.client.wavepanel.view.ContinuationIndicatorView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.View;

/**
 * Fake, pojo implementation of a thread view.
 *
 */
public final class FakeInlineThreadView extends FakeThreadView implements InlineThreadView {

  private final FakeContinuationIndicatorView continuationIndicator;
  private FakeAnchor container;
  private boolean collapsed;

  FakeInlineThreadView(FakeRenderer renderer, LinkedSequence<FakeBlipView> blips) {
    super(renderer, blips);
    this.continuationIndicator = new FakeContinuationIndicatorView(this);
  }

  @Override
  public Type getType() {
    return Type.INLINE_THREAD;
  }

  @Override
  public View getParent() {
    return container;
  }

  @Override
  public void remove() {
    container.detach(this);
  }

  void detach() {
    container = null;
  }

  void attachTo(FakeAnchor a) {
    Preconditions.checkState(container == null);
    this.container = a;
  }

  // Uninteresting below.

  @Override
  public void setCollapsed(boolean collapsed) {
    this.collapsed = collapsed;
  }

  @Override
  public boolean isCollapsed() {
    return collapsed;
  }

  @Override
  public String toString() {
    return "InlineThread " + super.blipsToString();
  }

  @Override
  public ContinuationIndicatorView getReplyIndicator() {
    return continuationIndicator;
  }

  @Override
  public void setTotalBlipCount(int totalBlipCount) {
    // no-op
  }

  @Override
  public void setUnreadBlipCount(int unreadBlipCount) {
    // no-op
  }
}