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

import static org.waveprotocol.wave.client.uibuilder.OutputHelper.close;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.openWith;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.uibuilder.HtmlClosure;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicInlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;

/**
 * UI builder for an inline thread.
 */
public final class InlineThreadViewBuilder implements IntrinsicInlineThreadView, UiBuilder {

  /**
   * This final HtmlClosure sub class implements the internal structure of the inline thread.
   * It encapsulates the blips in a div container and then creates the continuation indicator
   * as a sibling of the blip container.  The InlineThreadViewBuilder class uses this to marry
   * these two components together before passing the closure off to a CollapsibleBuilder
   * which expects a single entity as its contents.
   */
  final static class InlineThreadStructure implements HtmlClosure {

    /** The closure representing the collection of blips. */
    private final HtmlClosure blips;

    /** The UiBuilder that will render the inline thread continuation indicator. */
    private final UiBuilder continuationIndicator;

    /**
     * Creates a new InlineThreadStructure instance by combining the blips and
     * continuation indicator.
     */
    public static InlineThreadStructure create(HtmlClosure blips, UiBuilder continuationIndicator) {
      return new InlineThreadStructure(blips, continuationIndicator);
    }

    @VisibleForTesting
    InlineThreadStructure(HtmlClosure blips, UiBuilder continnuationIndicator) {
      this.blips = blips;
      this.continuationIndicator = continnuationIndicator;
    }

    @Override
    public void outputHtml(SafeHtmlBuilder output) {
      // For whitespace in an inline thread to get click events, it needs zoom:1
      // for some reason.
      String extra = UserAgent.isIE() ? "style='zoom:1' unselectable='on'" : null;
      openWith(output, null, null, null, extra);
      blips.outputHtml(output);
      close(output);
      continuationIndicator.outputHtml(output);
    }
  }

  /** DOM id. */
  private final String id;

  /** General-purpose collapsible DOM that implements this view. */
  private final CollapsibleBuilder impl;

  /**
   * Creates a UI builder for an inline thread.
   */
  public static InlineThreadViewBuilder create(String id, HtmlClosure blips,
      UiBuilder continuationIndicator) {
    // must not contain ', it is especially troublesome because it cause
    // security issues.
    Preconditions.checkArgument(!id.contains("\'"));
    InlineThreadStructure structure = InlineThreadStructure.create(blips, continuationIndicator);
    return new InlineThreadViewBuilder(
        id, CollapsibleBuilder.create(id, TypeCodes.kind(Type.INLINE_THREAD), structure));
  }

  @VisibleForTesting
  InlineThreadViewBuilder(String id, CollapsibleBuilder impl) {
    this.id = id;
    this.impl = impl;
  }

  @Override
  public String getId() {
    return id;
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
  public void setTotalBlipCount(int totalBlipCount) {
    impl.setTotalBlipCount(totalBlipCount);
  }

  @Override
  public void setUnreadBlipCount(int unreadBlipCount) {
    impl.setUnreadBlipCount(unreadBlipCount);
  }

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    impl.outputHtml(output);
  }
}