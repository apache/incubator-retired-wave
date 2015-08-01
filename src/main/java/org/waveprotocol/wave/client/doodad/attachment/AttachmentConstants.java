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

/**
 * This class defines attachment-realated constants shared between the servers
 * and client.
 *
 */

public class AttachmentConstants {
  private AttachmentConstants() {}

  /**
   * Defines the maximum thumbnail height / width in pixels used by the
   * thumbnailers on the server side and thumbnail managers on the client
   * side.
   */
  public static final int MAX_THUMBNAIL_SIZE = 120;

  /**
   * Defines the maximum large thumbnail width in pixels.
   *
   * This is used for the large thumbnails shown by the slideshow viewer and
   * when downsampling images while uploading.
   */
  public static final int MAX_LARGE_THUMBNAIL_WIDTH = 1024;

  /**
   * Defines the maximum large thumbnail height in pixels.
   *
   * This is used for downsampling large images prior to uploading them.
   */
  public static final int MAX_LARGE_THUMBNAIL_HEIGHT = 1024;

  /**
   * All blobs larger than this limit are put in the BlobStore. Otherwise they
   * are stored in Megastore. This is a compromise between a reasonable max
   * for Megastore and a reasonable min for BlobStore.
   */
  public static final int MEGASTORE_MAX_BLOB_BYTES = 64 * 1024; // 64 KB

  /**
   * Thumbnails larger than this will be rejected.
   */
  public static final int MAX_THUMBNAIL_BYTES = MEGASTORE_MAX_BLOB_BYTES;

  /**
   * Blobs larger than this will be rejected.
   */
  public static final int MAX_BLOB_SIZE_MEGABYTES = 20;

  /**
   * Blobs larger than this will be rejected.
   */
  public static final int MAX_BLOB_SIZE_BYTES = MAX_BLOB_SIZE_MEGABYTES * 1024 * 1024;
}
