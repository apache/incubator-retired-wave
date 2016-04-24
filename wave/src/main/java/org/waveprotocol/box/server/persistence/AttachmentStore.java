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
package org.waveprotocol.box.server.persistence;

import org.waveprotocol.box.attachment.AttachmentMetadata;
import org.waveprotocol.wave.media.model.AttachmentId;

import java.io.IOException;
import java.io.InputStream;

/**
 * An attachment store is a place for storing attachment data.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public interface AttachmentStore {

  interface AttachmentData {

    public InputStream getInputStream() throws IOException;

    public long getSize();
  }

  /**
   * Fetch an attachment metadata.
   *
   * @param attachmentId
   * @return the attachment metadata, or null if the metadata
   * does not exist
   */
  AttachmentMetadata getMetadata(AttachmentId attachmentId) throws IOException;

  /**
   * Fetch an attachment with the specified id.
   *
   * @param attachmentId
   * @return the attachment with the specified id, or null if the attachment
   * does not exist
   */
  AttachmentData getAttachment(AttachmentId attachmentId) throws IOException;

  /**
   * Fetch an attachment thumbnail.
   *
   * @param attachmentId
   * @return the attachment thumbnail
   */
  AttachmentData getThumbnail(AttachmentId attachmentId) throws IOException;

  /**
   * Store a new attachment with the specified id and data.
   *
   * @param attachmentId
   * @param metaData attachment metadata
   * @throws IOException
   */
  void storeMetadata(AttachmentId attachmentId, AttachmentMetadata metaData) throws IOException;

  /**
   * Store a new attachment with the specified id and data.
   *
   * @param attachmentId
   * @param data A stream which contains the data to be stored
   * @throws IOException
   */
  void storeAttachment(AttachmentId attachmentId, InputStream data) throws IOException;

  /**
   * Store a new attachment with the specified id and data.
   *
   * @param attachmentId
   * @param metaData attachment metadata
   * @throws IOException
   */
  void storeThumbnail(AttachmentId attachmentId, InputStream dataData) throws IOException;

  /**
   * Delete the specified attachment from the store. If the attachment does
   * not exist, this has no effect.
   *
   * The behavior of calling any methods on an open AttachmentData object is
   * undefined (implementation-specific).
   *
   * @param attachmentId
   */
  void deleteAttachment(AttachmentId attachmentId);
}
