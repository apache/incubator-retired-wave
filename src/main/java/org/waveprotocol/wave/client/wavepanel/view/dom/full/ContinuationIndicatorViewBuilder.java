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

import static org.waveprotocol.wave.client.uibuilder.OutputHelper.append;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.close;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.openWith;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;

import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicContinuationIndicatorView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;

/**
 * This class is the view builder for the inline continuation indicator.
 */
public final class ContinuationIndicatorViewBuilder implements UiBuilder,
    IntrinsicContinuationIndicatorView {

  public interface Resources extends ClientBundle {
    @Source("ContinuationIndicator.css")
    Css css();
    
    @Source("continuation_icon.png")
    ImageResource continuationIcon();
  }

  public interface Css extends CssResource {
    String indicator();
    String icon();
    String bar();
  }
  
  /** A unique id for this builder. */
  private final String id;

  /** The css resources for the builder. */
  private final Css css;

  //
  // Intrinsic state.
  //

  private boolean enabled = true;

  /**
   * Creates a new reply box view builder with the given id.
   *
   * @param id unique id for this builder, it must only contains alphanumeric
   *        characters
   */
  public static ContinuationIndicatorViewBuilder create(String id) {
    return new ContinuationIndicatorViewBuilder(
        WavePanelResourceLoader.getContinuationIndicator().css(), id);
  }

  @VisibleForTesting
  ContinuationIndicatorViewBuilder(Css css, String id) {
    // must not contain ', it is especially troublesome because it cause
    // security issues.
    Preconditions.checkArgument(!id.contains("\'"));
    this.css = css;
    this.id = id;
  }

  //
  // DomImpl nature.
  //

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    openWith(output, id, css.indicator(), TypeCodes.kind(Type.CONTINUATION_INDICATOR), 
        enabled ? "" : "style='display:none'");
    {
      append(output, null, css.icon(), null);
      append(output, null, css.bar(), null);
    }
    close(output);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void enable() {
    setEnabled(true);
  }

  @Override
  public void disable() {
    setEnabled(false);
  }
  
  private void setEnabled( boolean enabled ) {
    this.enabled = enabled;
  }
}