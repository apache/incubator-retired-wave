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

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;

import org.waveprotocol.wave.client.uibuilder.BuilderHelper.Component;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;

/**
 * A top level conversation builder. Each inherited class must implements the
 * outputHtml method and the structured produced must contains all the
 * components.
 *
 */
public abstract class TopConversationViewBuilder implements UiBuilder {

  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    @Source("toolbar_empty.png")
    ImageResource emptyToolbar();

    // Note: the CSS file contains a gwt-image reference, so must be defined
    // after the referenced images in this interface.
    @Source("Conversation.css")
    Css css();
  }

  /** CSS for this widget. */
  public interface Css extends CssResource {
    String fixedSelf();
    String fixedThread();
    String toolbar();
  }

  // Inner class, to avoid class loading of resource loader in test environment.
  static class CssConstants {
    // Called from @eval in css.
    final static String THREAD_TOP_CSS = ParticipantsViewBuilder.COLLAPSED_HEIGHT_PX
        + WavePanelResourceLoader.getConversation().emptyToolbar().getHeight() + "px";
  }

  /** An enum for all the components of a blip view. */
  public enum Components implements Component {
    /** Container of the main thread. */
    THREAD_CONTAINER("T"),
    /** Container of the toolbar. */
    TOOLBAR_CONTAINER("B"), ;

    private final String suffix;

    Components(String postfix) {
      this.suffix = postfix;
    }

    @Override
    public String getDomId(String baseId) {
      return baseId + suffix;
    }
  }

  TopConversationViewBuilder() {
  }
}
