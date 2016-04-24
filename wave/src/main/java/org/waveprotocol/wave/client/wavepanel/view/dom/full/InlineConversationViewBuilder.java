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

import static org.waveprotocol.wave.client.uibuilder.BuilderHelper.compose;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicInlineConversationView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;

/**
 * UI builder for an inline thread.
 *
 */
public final class InlineConversationViewBuilder implements IntrinsicInlineConversationView,
    UiBuilder {
  /** General-purpose collapsible DOM that implements this view. */
  private final CollapsibleBuilder impl;

  /**
   * Creates a UI builder for an inline thread.
   */
  public static InlineConversationViewBuilder create(String id, UiBuilder participants,
      UiBuilder thread) {
    // must not contain ', it is especially troublesome because it cause
    // security issues.
    Preconditions.checkArgument(!id.contains("\'"));
    return new InlineConversationViewBuilder(CollapsibleBuilder.create(id,
        TypeCodes.kind(Type.INLINE_CONVERSATION), compose(participants, thread)));
  }

  @VisibleForTesting
  InlineConversationViewBuilder(CollapsibleBuilder impl) {
    this.impl = impl;
  }

  @Override
  public void setCollapsed(boolean collapsed) {
    impl.setCollapsed(collapsed);
  }

  @Override
  public boolean isCollapsed() {
    return impl.isCollapsed();
  }

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    impl.outputHtml(output);
  }
}
