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
package org.waveprotocol.wave.media.model;


/**
 * An attachment. This interface is used both by the client (web-app) and on the server-side.
 */
public interface Attachment {
  public enum Status {
      NOT_UPLOADING,
      SUCCEEDED,
      IN_PROGRESS,
      FAILED_AND_RETRYABLE,
      FAILED_AND_NOT_RETRYABLE;
  }

  /** Metadata for an image, whether the original attachment or a thumbnail. */
  public interface ImageMetadata {
    /** Returns the image width. */
    public int getWidth();
    /** Returns the image height. */
    public int getHeight();
  }

  /** Enumeration of possible error conditions. */
  public enum ErrorCondition {
    /** The uploaded file is larger than the per file limit. */
    FILE_TOO_LARGE,

    /** The user has exceeded their storage quota. */
    QUOTA_EXCEEDED,

    /** The upload has been retried the maximum number of times. */
    TOO_MANY_RETRIES;
  }

  /** Returns the attachmentId, which should never be null. */
  String getAttachmentId();

  /**
   * Gets the user-provided filename of the attachment. This may have been
   * sanitized for safer consumption.
   *
   * @return the attachment filename or null if the filename is not known
   */
  String getFilename();

  /**
   * Gets the user id of the user who created the attachment.
   *
   * @return the user id or null if the creator is unknown (may occur if an
   * attach request comes in before the creation request)
   */
  String getCreator();

  /**
   * Gets the MIME type of the attachment if known.
   *
   * @return the MIME type as a {@code String} or {@code null} if unknown
   */
  String getMimeType();

  /**
   * Gets the relative URL by which this attachment may be retrieved.
   *
   * @return a relative URL or null if the attachment is not yet ready to
   * download
   */
  String getAttachmentUrl();

  /**
   * Gets the relative URL by which this thumbnail may be retrieved.
   *
   * @return a relative URL
   */
  String getThumbnailUrl();

  /**
   * Gets the total size of the attachment in bytes if known, or null
   * otherwise.
   *
   * @return the total size of the attachment, or null.
   */
  Long getSize();

  /**
   * Returns the original attachment's metadata, if the attachment is an image, or null if
   * unavailable.
   */
  ImageMetadata getContentImageMetadata();

  /** Returns the thumbnail image's metadata, or null if unavailable. */
  ImageMetadata getThumbnailImageMetadata();

  /**
   * Gets the upload progress of the attachment as bytes. When this value is
   * non-zero and equal to the value returned by {@link #getSize},
   * the attachment upload is complete.
   *
   * @return the upload progress of the attachment in bytes
   */
  long getUploadedByteCount();

  /**
   * The number of times the upload for this attachment has been retried.
   *
   * @return the number of times the upload has been retried.
   */
  long getUploadRetryCount();

  /**
   * Gets the malware status of the attachment.
   *
   * @return true if the attachment is known or suspected to be malware, false
   * if it is either known not to be malware or if the malware status is unknown
   */
  boolean isMalware();

  /**
   * The status of the attachment.
   *
   * @return the status of the upload.
   */
  Status getStatus();
}
