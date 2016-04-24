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

package org.waveprotocol.wave.client.wavepanel.view.impl;

import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.View;

/**
 *
 * @param <I> intrinsic anchor implementation
 */
public final class AnchorViewImpl<I> // \u2620
    extends AbstractStructuredView<AnchorViewImpl.Helper<? super I>, I> implements AnchorView {

  /**
   * Handles structural queries on thread views.
   *
   * @param <I> intrinsic thread implementation
   */
  public interface Helper<I> {
    void attach(I impl, InlineThreadView thread);
    void detach(I impl, InlineThreadView thread);
    InlineThreadView getThread(I impl);
    void remove(I impl);
    View getParent(I impl);
  }

  public AnchorViewImpl(Helper<? super I> helper, I impl) {
    super(helper, impl);
  }

  @Override
  public Type getType() {
    return Type.ANCHOR;
  }

  @Override
  public void remove() {
    helper.remove(impl);
  }

  @Override
  public View getParent() {
    return helper.getParent(impl);
  }

  @Override
  public void attach(InlineThreadView view) {
    helper.attach(impl, view);
  }

  @Override
  public void detach(InlineThreadView view) {
    helper.detach(impl, view);
  }

  @Override
  public InlineThreadView getThread() {
    return helper.getThread(impl);
  }
}
