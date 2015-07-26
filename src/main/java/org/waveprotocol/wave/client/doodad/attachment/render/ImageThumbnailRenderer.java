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

package org.waveprotocol.wave.client.doodad.attachment.render;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;

import java.util.logging.Logger;
import org.waveprotocol.wave.client.doodad.attachment.AttachmentConstants;
import org.waveprotocol.wave.client.doodad.attachment.ImageThumbnail;
import org.waveprotocol.wave.client.doodad.attachment.ImageThumbnailAttachmentHandler;
import org.waveprotocol.wave.client.doodad.attachment.SimpleAttachmentManager;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.gwt.GwtRenderingMutationHandler;
import org.waveprotocol.wave.media.model.Attachment;

/**
 * Renderer implementation for ImageThumbnails.
 *
 */
public class ImageThumbnailRenderer extends GwtRenderingMutationHandler {

  private static final Logger LOG = Logger.getLogger(ImageThumbnailRenderer.class.getName());

  private final SimpleAttachmentManager manager;
  private final ImageThumbnailAttachmentHandler attachmentHandler;

  /**
   * Constructor
   */
  public ImageThumbnailRenderer(SimpleAttachmentManager manager,
      ImageThumbnailAttachmentHandler handler) {
    super(Flow.USE_WIDGET);
    this.manager = manager;
    this.attachmentHandler = handler;
  }

  @Override
  protected Widget createGwtWidget(Renderable element) {
    ImageThumbnailWidget widget = new ImageThumbnailWidget();

    // TODO(danilatos/reuben): Can we remove the dependency on this max size constant?
    int maxSize = AttachmentConstants.MAX_THUMBNAIL_SIZE;
    widget.setThumbnailSize(maxSize, maxSize * 3 / 4);

    return widget;
  }

  private ImageThumbnailWidget getWidget(ContentElement e) {
    return (ImageThumbnailWidget) getGwtWidget(e);
  }

  @Override
  public Element getContainerNodelet(Widget w) {
    return ((ImageThumbnailWidget) w).getCaptionContainer();
  }

  /**
   * @param e an element
   * @return the view for the element
   */
  public ImageThumbnailView getView(ContentElement e) {
    return getWidget(e);
  }

  @Override
  public void onActivationStart(ContentElement element) {
    element.setProperty(ImageThumbnailWrapper.PROPERTY, new ImageThumbnailWrapper(element));

    fanoutAttrs(element);
  }

  @Override
  public void onDeactivated(ContentElement element) {
    ImageThumbnailWrapper w = ImageThumbnailWrapper.of(element);
    if (w.getAttachment() != null) {
      attachmentHandler.cleanup(element, w.getAttachment());
    }
    element.setProperty(ImageThumbnailWrapper.PROPERTY, null);
  }

  @Override
  public void onAttributeModified(ContentElement element, String name,
      String oldValue, String newValue) {
    if (ImageThumbnail.STYLE_ATTR.equals(name)) {
      ImageThumbnailView view = getView(element);
      view.setFullSizeMode(ImageThumbnail.STYLE_FULL.equals(newValue));
    } else if (ImageThumbnail.ATTACHMENT_ATTR.equals(name)) {
      ImageThumbnailWrapper w = ImageThumbnailWrapper.of(element);
      assert w != null;

      Attachment newAttachment = manager.getAttachment(newValue);
      Attachment oldAttachment = w.getAttachment();
      if (newAttachment != oldAttachment) {
        if (oldAttachment != null) {
          attachmentHandler.cleanup(element, oldAttachment);
        }
        if (newAttachment != null) {
          ImageThumbnailWrapper.of(element).setAttachment(newAttachment);
          attachmentHandler.init(element, newAttachment);
        }
      }
    }
    super.onAttributeModified(element, name, oldValue, newValue);
  }
}
