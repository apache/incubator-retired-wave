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

import org.waveprotocol.wave.client.doodad.attachment.render.ImageThumbnailRenderer;
import org.waveprotocol.wave.client.doodad.attachment.render.ImageThumbnailWrapper;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.misc.Caption;
import org.waveprotocol.wave.client.editor.selection.content.ValidSelectionStrategy;
import org.waveprotocol.wave.client.editor.util.EditorDocHelper;
import org.waveprotocol.wave.model.document.util.FilteredView.Skip;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * Editable image thumbnail
 *
 */
public class ImageThumbnail {

  /**
   * Allows propagation of image thumbnail events to external hooks.
   */
  public interface ThumbnailActionHandler {

    /**
     * Called when the user clicks the thumbnail.
     *
     * @return true if the action was handled in a special manner, false to allow default behaviour
     */
    boolean onClick(ImageThumbnailWrapper thumbnail);
  }

  public static final String TAGNAME = "image";

  /**
   * The attachment id attribute
   */
  public static final String ATTACHMENT_ATTR = "attachment";

  /**
   * Potential attribute names
   */
  protected static final String[] ATTRIBUTE_NAMES = new String[] {ATTACHMENT_ATTR};

  public static final Property<ContentElement> CAPTION = Property.immutable("Caption");

  /**
   * Render style attribute
   */
  public static final String STYLE_ATTR = "style";

  /**
   * Image displayed full size without thumbnail chrome
   */
  public static final String STYLE_FULL = "full";

  /**
   * Registers subclass with ContentElement
   *
   * @param actionHandler May be null. If not, allows external hooks into
   *        thumbnail events for extended behaviour.
   */
  public static void register(ElementHandlerRegistry registry,
      SimpleAttachmentManager attachmentsManager, ThumbnailActionHandler actionHandler) {
    // TODO(danilatos): Generify
    ImageThumbnailAttachmentHandler attachmentListener = new ImageThumbnailAttachmentHandler();
    ImageThumbnailRenderer renderer = new ImageThumbnailRenderer(attachmentsManager, attachmentListener);
    NodeEventHandler eventHandler = new ImageThumbnailNodeEventHandler(
        EditorStaticDeps.logger, renderer, actionHandler);
    attachmentListener.setRenderer(renderer);


    // TODO(danilatos/hearnden): These listeners need to be cleaned up.
    // E.g. currently they are created in WaveManager?
    attachmentsManager.addListener(attachmentListener);

    registry.registerEventHandler(TAGNAME, eventHandler);
    registry.registerRenderingMutationHandler(TAGNAME, renderer);

    // let the editor know image persistent tag can't have selection, but contains things that can
    ValidSelectionStrategy.registerTagForSelections(
        ImageThumbnail.TAGNAME, true, Skip.SHALLOW);
  }

  /**
   * @param attachmentId
   * @param xmlCaption
   * @return A content xml string containing an image thumbnail
   */
  public static XmlStringBuilder constructXml(
      String attachmentId, String xmlCaption) {
    return XmlStringBuilder.createText(xmlCaption).wrap(Caption.TAGNAME).wrap(
        TAGNAME, ATTACHMENT_ATTR, attachmentId);
  }

  /**
   * @param attachmentId
   * @param full whether it's full size
   * @param xmlCaption
   */
  public static XmlStringBuilder constructXml(
      String attachmentId, boolean full, String xmlCaption) {
    if (!full) {
      return constructXml(attachmentId, xmlCaption);
    }
    return XmlStringBuilder.createText(xmlCaption).wrap(Caption.TAGNAME).wrap(
        TAGNAME, ATTACHMENT_ATTR, attachmentId, STYLE_ATTR, STYLE_FULL);
  }

  private ImageThumbnail() {}

  /**
   * Determines whether a node is an image thumbnail.
   *
   * @param node the node to be checked.
   * @return true if the node is an image thumbnail, false otherwise.
   */
  public static boolean isThumbnailElement(ContentNode node) {
    assert node != null;
    return EditorDocHelper.isNamedElement(node, TAGNAME);
  }
}
