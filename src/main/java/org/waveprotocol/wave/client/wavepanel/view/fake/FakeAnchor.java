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

import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.View;

/**
 * Fake, pojo implementation of a thread anchor.
 *
 */
public final class FakeAnchor implements AnchorView {

  private FakeBlipView blip;
  private FakeBlipMetaView meta;

  private FakeInlineThreadView attached;

  FakeAnchor() {
  }

  void setContainer(FakeBlipView container) {
    Preconditions.checkState(blip == null && meta == null);
    this.blip = container;
  }

  void setContainer(FakeBlipMetaView container) {
    Preconditions.checkState(blip == null && meta == null);
    this.meta = container;
  }

  @Override
  public void attach(InlineThreadView view) {
    Preconditions.checkState(attached == null);
    Preconditions.checkArgument(view != null);
    attached = ((FakeInlineThreadView) view);
    attached.attachTo(this);
  }

  @Override
  public void detach(InlineThreadView view) {
    Preconditions.checkArgument(view != null);
    Preconditions.checkState(attached == view);
    attached.detach();
    attached = null;
  }

  @Override
  public FakeInlineThreadView getThread() {
    return attached;
  }

  @Override
  public Type getType() {
    return Type.ANCHOR;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Anchors do not remove themselves");
  }

  @Override
  public View getParent() {
    return blip != null ? blip : meta;
  }

  @Override
  public String toString() {
    return "Anchor [" + (attached != null ? attached : "") + "]";
  }
}
