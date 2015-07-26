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

import static org.waveprotocol.wave.client.uibuilder.BuilderHelper.nonNull;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.close;
import static org.waveprotocol.wave.client.uibuilder.OutputHelper.open;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.uibuilder.BuilderHelper.Component;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.IntrinsicBlipView;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;

/**
 * A implementation of the BlipView that output content as HTML string. This
 * class should be automatically generated from a template, but the template
 * generator is not ready yet.
 *
 */
public class BlipViewBuilder implements UiBuilder, IntrinsicBlipView {
  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    /** CSS */
    @Source("Blip.css")
    Css css();
  }

  /** CSS for this widget. */
  public interface Css extends CssResource {
    /** The topmost blip element. */
    String blip();

    String meta();

    String avatar();

    String metabar();

    String read();

    String unread();

    String metaline();

    String menu();

    String menuOption();

    String menuOptionSelected();

    String time();

    String contentContainer();

    String replies();

    String privateReplies();
  }

  /** An enum for all the components of a blip view. */
  public enum Components implements Component {
    /** Container for default anchors of reply threads. */
    REPLIES("R"),
    /** Container for nested conversations. */
    PRIVATE_REPLIES("P"),
    ;

    private final String postfix;

    Components(String postfix) {
      this.postfix = postfix;
    }

    @Override
    public String getDomId(String baseId) {
      return baseId + postfix;
    }
  }

  /**
   * A unique id for this builder.
   */
  private final String id;
  private final Css css;

  //
  // Structural components.
  //

  private final UiBuilder meta;
  private final UiBuilder replies;
  private final UiBuilder privateReplies;

  /**
   * Creates a new blip view builder with the given id.
   *
   * @param id unique id for this builder, it must only contains alphanumeric
   *        characters
   * @param replies collection of non-inline replies
   */
  public static BlipViewBuilder create(String id, UiBuilder meta, UiBuilder replies,
      UiBuilder privateReplies) {
    // must not contain ', it is especially troublesome because it cause
    // security issues.
    Preconditions.checkArgument(!id.contains("\'"));
    return new BlipViewBuilder(id, nonNull(meta), nonNull(replies), nonNull(privateReplies),
        WavePanelResourceLoader.getBlip().css());
  }

  @VisibleForTesting
  BlipViewBuilder(String id, UiBuilder meta, UiBuilder replies, UiBuilder privateReplies, Css css) {
    this.id = id;
    this.meta = meta;
    this.replies = replies;
    this.privateReplies = privateReplies;
    this.css = css;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    // HACK HACK HACK
    // This code should be automatically generated from UiBinder template, not
    // hand written.
    open(output, id, css.blip(), TypeCodes.kind(Type.BLIP));

    // Meta (no wrapper).
    meta.outputHtml(output);

    // Replies.
    open(output, Components.REPLIES.getDomId(id), css.replies(), null);
    replies.outputHtml(output);
    close(output);

    // Private Replies.
    open(output, Components.PRIVATE_REPLIES.getDomId(id), css.privateReplies(), null);
    privateReplies.outputHtml(output);
    close(output);

    close(output);
  }
}
