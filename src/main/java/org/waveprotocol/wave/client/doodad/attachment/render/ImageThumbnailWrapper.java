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

import org.waveprotocol.wave.client.doodad.attachment.ImageThumbnail;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.misc.Caption;
import org.waveprotocol.wave.media.model.Attachment;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.document.util.XmlStringBuilderDoc;

/**
 * A per-instance wrapper to provide an interface over image thumbnail elements
 *
 * TODO(danilatos): Find a nice way to get rid of this?
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ImageThumbnailWrapper {
  /**
   * Property for storing an instance of this wrapper on a thumbnail element
   */
  static final Property<ImageThumbnailWrapper> PROPERTY = Property.immutable("wrapper");

  /** Element being wrapped. */
  private final ContentElement element;

  /** Attachment. */
  private Attachment attachment;

  /**
   * @param element the element being wrapped
   */
  public ImageThumbnailWrapper(ContentElement element) {
    this.element = element;
  }

  /**
   * Gets the wrapper for an element, if it has one.
   *
   * @return wrapper for {@code e}.
   */
  public static ImageThumbnailWrapper of(ContentElement e) {
    return e.getProperty(ImageThumbnailWrapper.PROPERTY);
  }

  void setAttachment(Attachment a) {
    attachment = a;
  }

  /**
   * @return attachment of the thumbnail.
   */
  public Attachment getAttachment() {
    return attachment;
  }

  /**
   * Sets the attachment id of this doodad.
   */
  public void setAttachmentId(String id) {
    element.getMutableDoc().setElementAttribute(element, ImageThumbnail.ATTACHMENT_ATTR, id);
  }

  /**
   * Sets the caption text of this doodad.
   */
  public void setCaptionText(String text) {
    CMutableDocument doc = element.getMutableDoc();
    ContentElement caption = DocHelper.getElementWithTagName(doc, Caption.TAGNAME, element);
    if (caption != null) {
      doc.emptyElement(caption);
      doc.insertText(Point.<ContentNode> end(caption), text);
    }
  }

  /**
   * Gets the text of the image caption
   */
  public String getCaptionText() {
    CMutableDocument doc = element.getMutableDoc();
    return DocHelper.getText(doc, doc, doc.getLocation(element),
        doc.getLocation(Point.end((ContentNode) element)));
  }

  /**
   * Deletes this doodad from its document.
   */
  public void delete() {
    element.getMutableDoc().deleteNode(element);
  }

  /**
   * Renders this wrapper's element into a string builder.
   *
   * @param builder  builder into which this wrapper's element is appended,
   *                 or null if this wrapper should produce a builder
   * @return resulting XML builder.
   */
  public XmlStringBuilderDoc<? super ContentElement, ContentElement, ?>
      appendInto(XmlStringBuilderDoc<? super ContentElement, ContentElement, ?> builder) {
    if (builder == null) {
      builder = XmlStringBuilderDoc.createEmpty(element.getMutableDoc());
    }
    builder.appendNode(element);
    return builder;
  }

  public ContentElement getElement() {
    return element;
  }
}
