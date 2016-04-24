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

package org.waveprotocol.wave.client.doodad.attachment.testing;

import org.waveprotocol.wave.media.model.Attachment;

/**
 * Super simple attachment
 *
 */
// TODO(nigeltao): Replace this Fake by a mockito Mock.
public class FakeAttachment implements Attachment {

  private final String url;
  private final int thumbWidth;
  private final int thumbHeight;
  private final String mimeType;

  public FakeAttachment(String url, int thumbWidth, int thumbHeight, String mimeType) {
    this.url = url;
    this.thumbWidth = thumbWidth;
    this.thumbHeight = thumbHeight;
    this.mimeType = mimeType;
  }

  public String getAttachmentId() {
    return url;
  }

  public String getCreator() {
    return "foobar@example.com";
  }

  public String getMimeType() {
    return mimeType;
  }

  public boolean isMalware() {
    return false;
  }

  public ImageMetadata getContentImageMetadata() {
    // Pretend the image height and width match the thumbnail height and width.
    if (thumbWidth != 0 && thumbHeight != 0) {
      return new ImageMetadata() {
        @Override
        public int getHeight() {
          return thumbHeight;
        }

        @Override
        public int getWidth() {
          return thumbWidth;
        }
      };
    }
    return null;
  }

  public ImageMetadata getThumbnailImageMetadata() {
    if (thumbWidth != 0 && thumbHeight != 0) {
      return new ImageMetadata() {
        @Override
        public int getHeight() {
          return thumbHeight;
        }

        @Override
        public int getWidth() {
          return thumbWidth;
        }
      };
    }
    return null;
  }


  @Override
  public Status getStatus() {
    return Status.NOT_UPLOADING;
  }

  @Override
  public String getAttachmentUrl() {
    return url;
  }

  @Override
  public String getFilename() {
    throw new AssertionError("Not implemented");
  }

  @Override
  public Long getSize() {
    throw new AssertionError("Not implemented");
  }

  @Override
  public String getThumbnailUrl() {
    return url;
  }

  @Override
  public long getUploadRetryCount() {
    return 0;
  }

  @Override
  public long getUploadedByteCount() {
    return 0;
  }

}
