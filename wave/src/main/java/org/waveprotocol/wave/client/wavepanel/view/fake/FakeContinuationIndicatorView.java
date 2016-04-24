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

import org.waveprotocol.wave.client.wavepanel.view.ContinuationIndicatorView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;

/**
 * Fake, pojo implementation of a continuation indicator view.
 */
public final class FakeContinuationIndicatorView implements ContinuationIndicatorView {

  private final FakeInlineThreadView parent;
  private boolean enabled;

  FakeContinuationIndicatorView(FakeInlineThreadView parent) {
    this.parent = parent;
  }

  @Override
  public Type getType() {
    return Type.CONTINUATION_INDICATOR;
  }

  @Override
  public void remove() {
    // no-op
  }

  @Override
  public String getId() {
    return "fakeId";
  }

  @Override
  public void enable() {
    enabled = true;
  }

  @Override
  public void disable() {
    enabled = false;
  }

  protected boolean isEnabled() {
    return enabled;
  }

  @Override
  public InlineThreadView getParent() {
    return parent;
  }
}
