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

import org.waveprotocol.wave.client.wavepanel.view.ContinuationIndicatorView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicContinuationIndicatorView;

/**
 * Implements a continuation indicator on an inline thread..
 *
 * @param <I> intrinsic participants implementation
 */
public final class ContinuationIndicatorViewImpl
    <I extends IntrinsicContinuationIndicatorView> // \u2620
    extends AbstractStructuredView<ContinuationIndicatorViewImpl.
    Helper<? super I>, I> // \u2620
    implements ContinuationIndicatorView {

  /**
   * Handles structural queries on participants views.
   *
   * @param <I> intrinsic participants implementation
   */
  public interface Helper<I> {
    void remove(I impl);
    InlineThreadView getParent(I impl);
  }

  public ContinuationIndicatorViewImpl(Helper<? super I> helper, I impl) {
    super(helper, impl);
  }

  @Override
  public Type getType() {
    return Type.CONTINUATION_INDICATOR;
  }

  @Override
  public String getId() {
    return impl.getId();
  }
  
  @Override
  public void remove() {
    helper.remove(impl);
  }

  @Override
  public InlineThreadView getParent() {
    return helper.getParent(impl);
  }

  @Override
  public void enable() {
    impl.enable();
  }

  @Override
  public void disable() {
   impl.disable();
  }
}
