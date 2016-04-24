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
import org.waveprotocol.wave.client.doodad.attachment.render.ImageThumbnailView;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.media.model.Attachment;
import org.waveprotocol.wave.media.model.Attachment.ImageMetadata;
import org.waveprotocol.wave.media.model.Attachment.Status;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Handler for the attachment logic for thumbnail doodads.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ImageThumbnailAttachmentHandler implements SimpleAttachmentManager.Listener {

  // @NotInternationalized
  private static final String attachmentLoadingFailedTooltip = "Loading attachment failed.";
  private static final String attachmentMalwareDetected =
      "The file contains a virus or other malware and has been disabled.";

  // Memory leak? Should be OK if this handler is per-wavelet
  private final IdentityMap<Attachment, ContentElement> doodads =
      CollectionUtils.createIdentityMap();
  private ImageThumbnailRenderer renderer;

  /** NOTE(patcoleman): Not in ctor due to circular dependency. */
  void setRenderer(ImageThumbnailRenderer renderer) {
    Preconditions.checkArgument(renderer != null, "can't bind attachment handler to a null renderer");
    Preconditions.checkState(this.renderer == null, "renderer should only be set once");
    this.renderer = renderer;
  }

  /**
   * Set up attachment handling for the given element
   *
   * @param e the element
   * @param attachment the attachment
   */
  public void init(ContentElement e, Attachment attachment) {
    doodads.put(attachment, e);
    onContentUpdated(attachment);
    onThumbnailUpdated(attachment);
    onUploadStatusUpdated(attachment);
  }

  /**
   * Inverse of {@link #init}.
   */
  public void cleanup(ContentElement e, Attachment a) {
    doodads.remove(a);
  }

  private ContentElement getElement(Attachment c) {
    ContentElement e = doodads.get(c);
    if (e != null) {
      if (!e.isContentAttached()) {
        // Lazy removal. Perhaps do it from the node mutation event handler?
        doodads.remove(c);
      } else {
        return e;
      }
    }
    return null;
  }

  @Override
  public void onContentUpdated(Attachment c) {
    ContentElement e = getElement(c);
    // TODO(nigeltao): can e ever be null?
    if (e == null) {
      return;
    }

    String url = c.getAttachmentUrl();
    if (url != null) {
      renderer.getView(e).setAttachmentUrl(url);
    }

    ImageMetadata metadata = c.getContentImageMetadata();
    if (metadata != null) {
      renderer.getView(e).setAttachmentSize(metadata.getWidth(), metadata.getHeight());
    }
  }

  @Override
  public void onThumbnailUpdated(Attachment c) {
    ContentElement e = getElement(c);
    if (e == null) {
      return;
    }

    if (c.isMalware()) {
      renderer.getView(e).displayDeadImage(attachmentMalwareDetected);
      return;
    }

    String url = c.getThumbnailUrl();
    if (url != null) {
      renderer.getView(e).setThumbnailUrl(url);
    }

    ImageMetadata metadata = c.getThumbnailImageMetadata();
    if (metadata != null) {
      renderer.getView(e).setThumbnailSize(metadata.getWidth(), metadata.getHeight());
    }

    if (metadata == null && c.getStatus() == Status.FAILED_AND_NOT_RETRYABLE) {
      renderer.getView(e).displayDeadImage(
          attachmentLoadingFailedTooltip);
    }
  }

  @Override
  public void onUploadStatusUpdated(Attachment c) {
    ContentElement e = getElement(c);
    if (e == null) {
      return;
    }

    ImageThumbnailView v = renderer.getView(e);
    switch (c.getStatus()) {
      case NOT_UPLOADING:
      case SUCCEEDED:
        v.hideUploadProgress();
        break;
      case IN_PROGRESS:
      case FAILED_AND_RETRYABLE:
        v.setUploadProgress(((double)c.getUploadedByteCount())/c.getSize());
        v.showUploadProgress();
        break;
      case FAILED_AND_NOT_RETRYABLE:
        v.hideUploadProgress();
        v.displayDeadImage(attachmentLoadingFailedTooltip);
        break;
      default:
        throw new IllegalStateException();
    }
  }
}
