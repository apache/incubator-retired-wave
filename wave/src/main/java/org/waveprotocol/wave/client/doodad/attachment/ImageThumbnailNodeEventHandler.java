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

package org.waveprotocol.wave.client.doodad.attachment;

import com.google.gwt.user.client.Window;

import org.waveprotocol.wave.client.doodad.attachment.ImageThumbnail.ThumbnailActionHandler;
import org.waveprotocol.wave.client.doodad.attachment.render.ImageThumbnailRenderer;
import org.waveprotocol.wave.client.doodad.attachment.render.ImageThumbnailView.ImageThumbnailViewListener;
import org.waveprotocol.wave.client.doodad.attachment.render.ImageThumbnailWrapper;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Node event handler for ImageThumbnail.
 *
 * NOTE(user): We can probably make this more general and use it for different
 * types of doodads.
 *
 */
final class ImageThumbnailNodeEventHandler extends NodeEventHandlerImpl {

  private final ThumbnailActionHandler actionHandler;

  private final LoggerBundle logger;

  ImageThumbnailRenderer renderer;

  ImageThumbnailNodeEventHandler(LoggerBundle logger,
      ImageThumbnailRenderer renderer, ThumbnailActionHandler actionHandler) {
    this.logger = logger;
    this.renderer = renderer;
    this.actionHandler = actionHandler;
  }

  @SuppressWarnings("unchecked") // Limitations of java's type system
  @Override
  public void onActivated(final ContentElement element) {
    renderer.getView(element).setListener(new ImageThumbnailViewListener() {
      public void onRequestSetFullSizeMode(boolean isOn) {
        element.getMutableDoc().setElementAttribute(element,
            ImageThumbnail.STYLE_ATTR, isOn ? ImageThumbnail.STYLE_FULL : null);
      }
      public void onClickImage() {
        ImageThumbnailWrapper thumbnail = ImageThumbnailWrapper.of(element);
        if (!actionHandler.onClick(thumbnail)) {
          String url = thumbnail.getAttachment().getAttachmentUrl();
          if (url != null) {
            // TODO(nigeltao): Is it necessary to open a window here? All attachments are set to
            // content-disposition=attachment which means the browser should download them.
            // The current implementation means we always get a blank tab.
            Window.open(url, thumbnail.getCaptionText(), "");
          }
        }
      }
    });
  }

  /**
   * Removes the entire thumbnail on backspace after
   *
   * {@inheritDoc}
   */
  @Override
  public boolean handleBackspaceAfterNode(ContentElement element, EditorEvent event) {
    logger.trace().log("backspace after", element);
    element.getMutableDoc().deleteNode(element);
    return true;
  }

  /**
   * Removes the entire thumbnail on delete before
   *
   * {@inheritDoc}
   */
  @Override
  public boolean handleDeleteBeforeNode(ContentElement element, EditorEvent event) {
    logger.trace().log("Delete before", element);
    element.getMutableDoc().deleteNode(element);
    return true;
  }

  /**
   * Handles a left arrow that occurred with the caret immediately
   * after this node, by moving caret to end of caption
   *
   * {@inheritDoc}
   */
  @Override
  public boolean handleLeftAfterNode(ContentElement element, EditorEvent event) {
    element.getSelectionHelper().setCaret(
        Point.<ContentNode> end(getCaption(element)));
    return true;
  }

  /**
   * Handles a right arrow that occurred with the caret immediately
   * before this node, by moving caret to beginning of caption
   *
   * {@inheritDoc}
   */
  @Override
  public boolean handleRightBeforeNode(ContentElement element, EditorEvent event) {
    element.getSelectionHelper().setCaret(
        Point.start(element.getRenderedContentView(), getCaption(element)));
    return true;
  }

  @Override
  public boolean handleLeftAtBeginning(ContentElement element, EditorEvent event) {
    // NOTE(danilatos): The use of location mapper will normalise into text nodes,
    // masking a weird firefox selection bug except when there is no adjacent text node.
    element.getSelectionHelper().setCaret(element.getLocationMapper().getLocation(
        Point.before(element.getRenderedContentView(), element)));
    return true;
  }

  @Override
  public boolean handleRightAtEnd(ContentElement element, EditorEvent event) {
    // NOTE(danilatos): The use of location mapper will normalise into text nodes,
    // masking a weird firefox selection bug except when there is no adjacent text node.
    element.getSelectionHelper().setCaret(element.getLocationMapper().getLocation(
        Point.after(element.getRenderedContentView(), element)));
    return true;
  }

  private ContentElement getCaption(ContentElement element) {
    // TODO(danilatos): Enforce correctness via schema
    return (ContentElement) element.getFirstChild();
  }
}
