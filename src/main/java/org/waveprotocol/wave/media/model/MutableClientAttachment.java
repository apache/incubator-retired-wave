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
 * Represents a mutable set of client-side attachment metadata for a single attachment instance on a
 * wavelet.
 * <p/>
 * Implementations of this class should be thread-safe.
 *
 */
public interface MutableClientAttachment extends Attachment {
  /**
   * Sets the relative URL to download this attachment.
   *
   * @param url the URL
   */
  void setAttachmentUrl(String url);

  /**
   * The id of the creator of this attachment.
   *
   * @param userId the non-null id of the user
   */
  void setCreator(String userId);

  /**
   * Sets the download token for the attachment. The download token will used to
   * retrieve the attachment once we deprecate the attachment and thumbnail URLs.
   *
   * @param token the non-null download token
   */
  void setDownloadToken(String token);

  /**
   * Sets the total size of the attachment in bytes if known, or null otherwise.
   *
   * @param size the total size of the attachment, or null.
   */
  void setSize(Long size);

  /**
   * Sets the user-provided filename of the attachment. This may have been sanitized for safer
   * consumption.
   *
   * @param filename the non-null attachment filename
   */
  void setFilename(String filename);

  /**
   * Sets the metadata for an image attachment, creating it if it doesn't
   * already exist.
   *
   * @param width the width of the image
   * @param height the height of the image
   * @return the image metadata representation
   */
  ImageMetadata setImage(int width, int height);

  /**
   * Sets the malware status of the attachment.
   *
   * @param malware if the attachment is known or suspected to be malware, false if it is known not
   * to be malware, or null if the malware status is unknown
   */
  void setMalware(Boolean malware);

  /**
   * Sets the MIME type of the attachment if known.
   *
   * @param mimeType the MIME type as a non-null {@code String}
   */
  void setMimeType(String mimeType);

  /**
   * Sets the metadata for the thumbnail, creating it if it doesn't already exist.
   *
   * @param width the width of the thumbnail
   * @param height the height of the thumbnail
   * @return the thumbnail metadata representation
   */
  ImageMetadata setThumbnail(int width, int height);

  /**
   * Sets the upload status of the attachment.
   *
   * @param status the upload status.
   */
  void setStatus(Status status);

  /**
   * Sets the relative URL to download the thumbnail for this attachment.
   *
   * @param url the URL
   */
  void setThumbnailUrl(String url);

  /**
   * Sets the upload progress of the attachment as bytes. When this value is
   * non-zero and equal to the value returned by {@link #setSize}, the
   * attachment upload is complete.
   *
   * @param uploadProgress the upload progress of the attachment in bytes
   */
  void setUploadedByteCount(long uploadProgress);

  /**
   * Sets the number of retries for a given upload. When the count reaches a
   * threshold, the attachment will be marked with an error condition.
   *
   * @param retryCount the number of times this upload has been retried.
   */
  void setUploadRetryCount(long retryCount);
}
