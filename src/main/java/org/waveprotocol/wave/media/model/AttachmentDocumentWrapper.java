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

import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicMap;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.util.Serializer.EnumSerializer;

/**
 * Wraps a MutableDocument representing a set of client-side attachment metadata. No validation is
 * done to ensure that the document conforms to the expected schema, but certain operations may fail
 * if this is not the case.
 * <p/>
 * This class also accepts attribute update event notifications, and rebroadcasts them to a
 * listener.
 * <p/>
 * This class is threadsafe, providing the underlying MutableDocument and Node implementations are
 * thread-safe.
 *
 * @param <N> Node type
 * @param <E> Element node type
 * @param <T> Text node type
 */
public class AttachmentDocumentWrapper<N, E extends N, T extends N>
    implements MutableClientAttachment {

  /**
   * Represents a name of an entity in the document.
   */
  interface Name {
    /**
     * Gets the name as used in the underlying XML.
     *
     * @return the name in string form
     */
    String getName();
  }

  /**
   * Tags and attribute names for the XML map keys.
   */
  // VisibleForTesting
  enum KeyName {
    ATTACHMENT_ID,
    ATTACHMENT_SIZE,
    ATTACHMENT_URL,
    CREATOR,
    DOWNLOAD_TOKEN,
    FILENAME,
    IMAGE_HEIGHT,
    IMAGE_WIDTH,
    MALWARE,
    MIME_TYPE,
    STATUS,
    THUMBNAIL_HEIGHT,
    THUMBNAIL_WIDTH,
    THUMBNAIL_URL,
    UPLOAD_PROGRESS,
    UPLOAD_RETRIES;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

    class ImageMetadataImpl implements ImageMetadata {

    private final int width;
    private final int height;

    public ImageMetadataImpl(int width, int height) {
      this.width = width;
      this.height = height;
    }


    @Override
    public int getWidth() {
      return width;
    }

    @Override
    public int getHeight() {
      return height;
    }

  }

  private final DocumentBasedBasicMap<E, KeyName, String> dataMap;

  private final ObservableMutableDocument<N, E, T> internalDocument;

  private ImageMetadata image;

  private ImageMetadata thumbnail;

  /**
   * Factory method for creating AttachmentDocumentWrappers.
   *
   * @param document the CWM document to wrap
   * @return a new wrapper
   */
  public static <N> AttachmentDocumentWrapper<N, ?, ?> create(
      ObservableMutableDocument<N, ?, ?> document) {
    return internalCreate(document);
  }

  /**
   * Private constructor. Use the factory method ({@link #create}) instead.
   *
   * @param document the document to wrap
   */
  // VisibleForTesting
  AttachmentDocumentWrapper(ObservableMutableDocument<N, E, T> document) {
    internalDocument = document;
    dataMap = DocumentBasedBasicMap.create(DefaultDocumentEventRouter.create(document),
        document.getDocumentElement(), new KeyNameSerializer(), Serializer.STRING, "node",
        "key", "value");
  }

  @Override
  public String getAttachmentId() {
    return dataMap.get(KeyName.ATTACHMENT_ID);
  }

  @Override
  public String getAttachmentUrl() {
    return dataMap.get(KeyName.ATTACHMENT_URL);
  }

  @Override
  public String getCreator() {
    return dataMap.get(KeyName.CREATOR);
  }

  /** Only @VisibleForTesting. */
  ObservableMutableDocument<N, E, T> getDocument() {
    return internalDocument;
  }

  @Override
  public String getFilename() {
    return dataMap.get(KeyName.FILENAME);
  }

  @Override
  public ImageMetadata getContentImageMetadata() {
    if (image == null) {
      Integer width = getAsInt(KeyName.IMAGE_WIDTH);
      Integer height = getAsInt(KeyName.IMAGE_HEIGHT);
      if (width != null && height != null) {
        image = new ImageMetadataImpl(width, height);
      }
    }
    return image;
  }

  @Override
  public String getMimeType() {
    return dataMap.get(KeyName.MIME_TYPE);
  }

  @Override
  public Long getSize() {
    return getAsLong(KeyName.ATTACHMENT_SIZE);
  }

  @Override
  public ImageMetadata getThumbnailImageMetadata() {
    if (thumbnail == null) {
      Integer width = getAsInt(KeyName.THUMBNAIL_WIDTH);
      Integer height = getAsInt(KeyName.THUMBNAIL_HEIGHT);
      if (width != null && height != null) {
        thumbnail = new ImageMetadataImpl(width, height);
      }
    }
    return thumbnail;
  }

  @Override
  public String getThumbnailUrl() {
    return dataMap.get(KeyName.THUMBNAIL_URL);
  }

  @Override
  public long getUploadedByteCount() {
    return getAsLong(KeyName.UPLOAD_PROGRESS);
  }

  @Override
  public long getUploadRetryCount() {
    Long retryCount =  getAsLong(KeyName.UPLOAD_RETRIES);
    return retryCount == null ? 0 : retryCount;
  }

  @Override
  public boolean isMalware() {
    Boolean b = getAsBoolean(KeyName.MALWARE);
    return b != null && b;
  }

  @Override
  public Status getStatus() {
    return Status.valueOf(dataMap.get(KeyName.STATUS));
  }

  @Override
  public void setAttachmentUrl(String url) {
    dataMap.put(KeyName.ATTACHMENT_URL, url);
  }

  @Override
  public void setCreator(String userId) {
    dataMap.put(KeyName.CREATOR, userId);
  }

  @Override
  public void setDownloadToken(String token) {
    dataMap.put(KeyName.DOWNLOAD_TOKEN, token);
  }

  @Override
  public void setFilename(String filename) {
    dataMap.put(KeyName.FILENAME, filename);
  }

  @Override
  public ImageMetadata setImage(int width, int height) {
    dataMap.put(KeyName.IMAGE_WIDTH, Integer.toString(width));
    dataMap.put(KeyName.IMAGE_HEIGHT, Integer.toString(height));
    image = new ImageMetadataImpl(width, height);
    return image;
  }

  @Override
  public void setMalware(Boolean malware) {
    setBooleanAttribute(KeyName.MALWARE, malware);
  }

  @Override
  public void setMimeType(String mimeType) {
    dataMap.put(KeyName.MIME_TYPE, mimeType);
  }

  @Override
  public void setSize(Long size) {
    setLongAttribute(KeyName.ATTACHMENT_SIZE, size);
  }

  /**
   * TODO(user): Correlate all of the various upload / attachment states
   * throughout the code base.
   */
  @Override
  public void setStatus(Status status) {
    dataMap.put(KeyName.STATUS, status.name());
  }

  @Override
  public ImageMetadata setThumbnail(int width, int height) {
    dataMap.put(KeyName.THUMBNAIL_WIDTH, Integer.toString(width));
    dataMap.put(KeyName.THUMBNAIL_HEIGHT, Integer.toString(height));
    thumbnail = new ImageMetadataImpl(width, height);
    return thumbnail;
  }

  @Override
  public void setThumbnailUrl(String url) {
    dataMap.put(KeyName.THUMBNAIL_URL, url);
  }

  @Override
  public void setUploadedByteCount(long uploadProgress) {
    setLongAttribute(KeyName.UPLOAD_PROGRESS, uploadProgress);
  }

  @Override
  public void setUploadRetryCount(long retryCount) {
    setLongAttribute(KeyName.UPLOAD_RETRIES, retryCount);
  }

  /**
   * Convenience method to increment the retry count.
   *
   * @return the retry count after being incremented.
   */
  public long incrementRetryCount() {
    long retryCount = getUploadRetryCount() + 1;
    setUploadRetryCount(retryCount);
    return retryCount;
  }

  private Boolean getAsBoolean(KeyName key) {
    String value = dataMap.get(key);
    return value != null ? Boolean.parseBoolean(value) : null;
  }

  private Integer getAsInt(KeyName key) {
    String value = dataMap.get(key);
    return value != null ? Integer.parseInt(value) : null;
  }

  private Long getAsLong(KeyName key) {
    String value = dataMap.get(key);
    return value != null ? Long.parseLong(value) : null;
  }

  /**
   * Internal factory method implementation. Required because javac is not smart enough to
   * work out the generics if we inline this call.
   *
   * @param document the CWM document to wrap
   * @return a new wrapper
   */
  private static <N, E extends N, T extends N> AttachmentDocumentWrapper<N, ?, ?> internalCreate(
      ObservableMutableDocument<N, E, T> document) {
    return new AttachmentDocumentWrapper<N, E, T>(document);
  }

  private void setBooleanAttribute(KeyName key, boolean value) {
    dataMap.put(key, Boolean.toString(value));
  }

  private void setLongAttribute(KeyName key, long value) {
    dataMap.put(key, Long.toString(value));
  }

  private static class KeyNameSerializer extends EnumSerializer<KeyName> {
    public KeyNameSerializer() {
      super(KeyName.class);
    }

    @Override
    public String toString(KeyName keyName) {
      return super.toString(keyName).toLowerCase();
    }

    @Override
    public KeyName fromString(String s) {
      return super.fromString(s.toUpperCase());
    }
  }
}
