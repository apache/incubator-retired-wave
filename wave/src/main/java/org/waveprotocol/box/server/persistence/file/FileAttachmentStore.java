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

package org.waveprotocol.box.server.persistence.file;

import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.waveprotocol.box.attachment.AttachmentMetadata;
import org.waveprotocol.box.attachment.AttachmentProto;
import org.waveprotocol.box.attachment.proto.AttachmentMetadataProtoImpl;
import org.waveprotocol.box.server.persistence.AttachmentStore;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.util.CharBase64;

import java.io.*;

/**
 * An implementation of AttachmentStore which uses files on disk
 *
 * @author josephg@gmail.com (Joseph Gentle)
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class FileAttachmentStore implements AttachmentStore {

  private final static String META_EXT = ".meta";
  private final static String THUMBNAIL_EXT = ".thumbnail";

  /**
   * The directory in which the attachments are stored.
   */
  private final String basePath;

  @Inject
  public FileAttachmentStore(Config config) {
    this.basePath = config.getString("core.attachment_store_directory");
    new File(basePath).mkdirs();
  }

  @Override
  public AttachmentMetadata getMetadata(AttachmentId attachmentId) throws IOException {
    File file = new File(getMetadataPath(attachmentId));
    if (!file.exists()) {
      return null;
    }
    AttachmentProto.AttachmentMetadata protoMetadata =
        AttachmentProto.AttachmentMetadata.parseFrom(new FileInputStream(file));
    return new AttachmentMetadataProtoImpl(protoMetadata);
  }

  @Override
  public AttachmentData getAttachment(AttachmentId attachmentId) throws IOException {
    final File file = new File(getAttachmentPath(attachmentId));
    if (!file.exists()) {
      return null;
    }
    return new AttachmentData() {

      @Override
      public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
      }

      @Override
      public long getSize() {
        return file.length();
      }
    };
  }

  @Override
  public AttachmentData getThumbnail(AttachmentId attachmentId) throws IOException {
    final File file = new File(getThumbnailPath(attachmentId));
    if (!file.exists()) {
      return null;
    }
    return new AttachmentData() {

      @Override
      public InputStream getInputStream() throws IOException {
        return new FileInputStream(file);
      }

      @Override
      public long getSize() {
        return file.length();
      }
    };
  }

  @Override
  public void storeMetadata(AttachmentId attachmentId, AttachmentMetadata metaData) throws IOException {
    File file = new File(getMetadataPath(attachmentId));
    if (file.exists()) {
      throw new IOException("Attachment already exist");
    }
    FileOutputStream stream = new FileOutputStream(file);
    AttachmentMetadataProtoImpl proto = new AttachmentMetadataProtoImpl(metaData);
    proto.getPB().writeTo(stream);
    stream.close();
  }

  @Override
  public void storeAttachment(AttachmentId attachmentId, InputStream data) throws IOException {
    File file = new File(getAttachmentPath(attachmentId));
    if (file.exists()) {
      throw new IOException("Attachment already exist");
    }
    FileOutputStream stream = new FileOutputStream(file);
    writeTo(data, stream);
    stream.close();
  }

  @Override
  public void storeThumbnail(AttachmentId attachmentId, InputStream data) throws IOException {
    File file = new File(getThumbnailPath(attachmentId));
    if (file.exists()) {
      throw new IOException("Attachment already exist");
    }
    FileOutputStream stream = new FileOutputStream(file);
    writeTo(data, stream);
    stream.close();
  }

  @Override
  public void deleteAttachment(AttachmentId attachmentId) {
    File file = new File(getAttachmentPath(attachmentId));
    if (file.exists()) {
      file.delete();
    }
  }

  private String getMetadataPath(AttachmentId attachmentId) {
    return basePath + File.separatorChar + encodeId(attachmentId) + META_EXT;
  }

  private String getAttachmentPath(AttachmentId attachmentId) {
    return basePath + File.separatorChar + encodeId(attachmentId);
  }

  private String getThumbnailPath(AttachmentId attachmentId) {
    return basePath + File.separatorChar + encodeId(attachmentId) + THUMBNAIL_EXT;
  }

  private static void writeTo(InputStream source, OutputStream dest) throws IOException {
    byte[] buffer = new byte[256];
    int length;
    while ((length = source.read(buffer)) != -1) {
      dest.write(buffer, 0, length);
    }
  }

  private static String encodeId(AttachmentId id) {
    try {
      return CharBase64.encode(id.serialise().getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

}
