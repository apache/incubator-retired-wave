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

package org.waveprotocol.box.server.attachment;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.waveprotocol.box.attachment.AttachmentMetadata;
import org.waveprotocol.box.attachment.ImageMetadata;
import org.waveprotocol.box.attachment.impl.AttachmentMetadataImpl;
import org.waveprotocol.box.attachment.impl.ImageMetadataImpl;
import org.waveprotocol.box.server.persistence.AttachmentStore;
import org.waveprotocol.box.server.persistence.AttachmentStore.AttachmentData;
import org.waveprotocol.box.server.rpc.AttachmentServlet;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.jvm.JavaWaverefEncoder;

/**
 * Serves storing and getting of attachments.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class AttachmentService {
  private static final Logger LOG = Logger.getLogger(AttachmentService.class.getName());

  public static final String THUMBNAIL_MIME_TYPE = "image/jpeg";
  public static final String THUMBNAIL_FORMAT_NAME = "jpeg";

  public static final int THUMBNAIL_PATTERN_WIDTH = 95;
  public static final int THUMBNAIL_PATTERN_HEIGHT = 60;

  private static final int MAX_THUMBNAIL_WIDTH = 200;
  private static final int MAX_THUMBNAIL_HEIGHT = 200;

  private final AttachmentStore store;

  @Inject
  private AttachmentService(AttachmentStore store) {
    this.store = store;
  }

  public AttachmentMetadata getMetadata(AttachmentId attachmentId) throws IOException {
    return store.getMetadata(attachmentId);
  }

  public AttachmentData getAttachment(AttachmentId attachmentId) throws IOException {
    return store.getAttachment(attachmentId);
  }

  public AttachmentData getThumbnail(AttachmentId attachmentId) throws IOException {
    return store.getThumbnail(attachmentId);
  }

  public void storeAttachment(AttachmentId attachmentId, InputStream in, WaveletName waveletName,
      String fileName, ParticipantId creator) throws IOException {
    store.storeAttachment(attachmentId, in);
    buildAndStoreMetadataWithThumbnail(attachmentId, waveletName, fileName, creator);
  }

  public AttachmentMetadata buildAndStoreMetadataWithThumbnail(AttachmentId attachmentId,
      WaveletName waveletName, String fileName, ParticipantId creator) throws IOException {
    AttachmentData data = store.getAttachment(attachmentId);
    if (data == null) {
      throw new IOException("No such atachment " + attachmentId.serialise());
    }
    AttachmentMetadata metadata = new AttachmentMetadataImpl();
    metadata.setAttachmentId(attachmentId.serialise());
    metadata.setAttachmentUrl(AttachmentServlet.ATTACHMENT_URL + "/" + attachmentId.serialise());
    metadata.setThumbnailUrl(AttachmentServlet.THUMBNAIL_URL + "/" + attachmentId.serialise());
    metadata.setWaveRef(waveletName2WaveRef(waveletName));
    metadata.setFileName(fileName);
    String contentType = getMimeType(fileName);
    metadata.setMimeType(contentType);
    metadata.setSize(data.getSize());
    metadata.setCreator((creator != null) ? creator.getAddress() : "");
    BufferedImage image = null;
    try {
      image = ImageIO.read(data.getInputStream());
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "Identifying attachment", ex);
    }
    if (image != null) {
      ImageMetadata imageMetadata = new ImageMetadataImpl();
      imageMetadata.setWidth(image.getWidth());
      imageMetadata.setHeight(image.getHeight());
      metadata.setImageMetadata(imageMetadata);
      try {
        BufferedImage thumbnail = makeThumbnail(image);
        storeThumbnail(attachmentId, thumbnail);
        ImageMetadata thumbnailMetadata = new ImageMetadataImpl();
        thumbnailMetadata.setWidth(thumbnail.getWidth());
        thumbnailMetadata.setHeight(thumbnail.getHeight());
        metadata.setThumbnailMetadata(thumbnailMetadata);
      } catch (IOException ex) {
        LOG.log(Level.SEVERE, "Building attachment thumbnail", ex);
      }
    } else {
      ImageMetadata thumbnailMetadata = new ImageMetadataImpl();
      thumbnailMetadata.setWidth(THUMBNAIL_PATTERN_WIDTH);
      thumbnailMetadata.setHeight(THUMBNAIL_PATTERN_HEIGHT);
      metadata.setThumbnailMetadata(thumbnailMetadata);
    }
    store.storeMetadata(attachmentId, metadata);
    return metadata;
  }

  private static BufferedImage makeThumbnail(BufferedImage image) {
    int imageWidth = image.getWidth();
    int imageHeight = image.getHeight();
    Preconditions.checkState(imageHeight != 0);
    Preconditions.checkState(imageWidth != 0);
    int thumbnailWidth = imageWidth < MAX_THUMBNAIL_WIDTH ? imageWidth : MAX_THUMBNAIL_WIDTH;
    int thumbnailHeight = imageHeight < MAX_THUMBNAIL_HEIGHT ? imageHeight : MAX_THUMBNAIL_HEIGHT;
    if (imageWidth * thumbnailHeight < imageHeight * thumbnailWidth) {
      thumbnailWidth = imageWidth * thumbnailHeight / imageHeight;
    } else {
      thumbnailHeight = imageHeight * thumbnailWidth / imageWidth;
    }
    BufferedImage thumbnail = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = thumbnail.createGraphics();
    try {
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setBackground(Color.BLACK);
      g.clearRect(0, 0, thumbnailWidth, thumbnailHeight);
      g.drawImage(image, 0, 0, thumbnailWidth, thumbnailHeight, null);
    } finally {
      g.dispose();
    }
    return thumbnail;
  }

  private void storeThumbnail(AttachmentId attachemntId, BufferedImage thumbnail) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ImageIO.write(thumbnail, THUMBNAIL_FORMAT_NAME, out);
    store.storeThumbnail(attachemntId, new ByteArrayInputStream(out.toByteArray()));
  }

  private static String waveletName2WaveRef(WaveletName waveletName) {
    WaveRef waveRef = WaveRef.of(waveletName.waveId, waveletName.waveletId);
    return JavaWaverefEncoder.encodeToUriPathSegment(waveRef);
  }

  private static String getMimeType(String fileName) {
    String mimeType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
    if (mimeType == null) {
      return "application/octet-stream";
    }
    return mimeType;
  }
}
