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

import org.waveprotocol.box.attachment.AttachmentMetadata;
import org.waveprotocol.box.attachment.impl.AttachmentMetadataImpl;
import org.waveprotocol.wave.media.model.Attachment;

/**
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class AttachmentImpl implements Attachment {

  private AttachmentMetadata metadata;
  private Status status = Status.NOT_UPLOADING;

  public AttachmentImpl() {
    metadata = new AttachmentMetadataImpl();
  }

  public void copyMetadata(AttachmentMetadata metadata) {
    this.metadata.copyFrom(metadata);
  }

  @Override
  public String getAttachmentId() {
    return metadata.getAttachmentId();
  }

  @Override
  public String getCreator() {
    return metadata.getCreator();
  }

  @Override
  public String getAttachmentUrl() {
    return metadata.getAttachmentUrl();
  }

  @Override
  public String getThumbnailUrl() {
    return metadata.getThumbnailUrl();
  }

  @Override
  public String getMimeType() {
    return metadata.getMimeType();
  }

  @Override
  public String getFilename() {
    return metadata.getFileName();
  }

  @Override
  public Long getSize() {
    return metadata.getSize();
  }

  @Override
  public ImageMetadata getContentImageMetadata() {
    if (metadata.hasImageMetadata()) {
      return new ImageMetadata() {

        @Override
        public int getWidth() {
          return metadata.getImageMetadata().getWidth();
        }

        @Override
        public int getHeight() {
          return metadata.getImageMetadata().getHeight();
        }
      };
    } else {
      return null;
    }
  }

  @Override
  public ImageMetadata getThumbnailImageMetadata() {
    if (metadata.hasThumbnailMetadata()) {
      return new ImageMetadata() {

        @Override
        public int getWidth() {
          return metadata.getThumbnailMetadata().getWidth();
        }

        @Override
        public int getHeight() {
          return metadata.getThumbnailMetadata().getHeight();
        }
      };
    } else {
      return null;
    }
  }

  @Override
  public long getUploadedByteCount() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public long getUploadRetryCount() {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public boolean isMalware() {
    if (metadata.hasMalware()) {
      return metadata.getMalware();
    }
    return false;
  }

  @Override
  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }
}
