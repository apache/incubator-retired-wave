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


import junit.framework.TestCase;

import org.waveprotocol.wave.media.model.Attachment.ImageMetadata;
import org.waveprotocol.wave.media.model.Attachment.Status;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.testing.BasicFactories;

/**
 * Tests that the AttachmentDocumentWrapper correctly passes data to and from its underlying
 * MutableDocument.
 *
 */

public class AttachmentDocumentWrapperTest extends TestCase {
  private static final String CREATOR = "a_user";
  private static final String FILENAME = "a file.jpg";
  private static final Long SIZE = 123456789123L;
  private static final String MIME_TYPE = "image/jpeg";
  private static final long UPLOAD_PROGRESS = 12345L;
  private static final Attachment.Status UPLOAD_STATUS = Status.IN_PROGRESS;

  private static final int IMAGE_HEIGHT = 123;
  private static final int IMAGE_WIDTH = 234;
  private static final int THUMB_HEIGHT = 345;
  private static final int THUMB_WIDTH = 456;

  /**
   * Tests that all of the basic fields (ie excluding thumbnail and image metadata) can be correctly
   * read.
   */
  public void testBasicMetadata() {
    MutableClientAttachment wrapper = createBasicWrapper();
    checkBasicFields(wrapper);
    assertNull(wrapper.getContentImageMetadata());
    assertNull(wrapper.getThumbnailImageMetadata());
  }

  /**
   * Tests that all of the fields are correctly populated, including thumbnail and image metadata.
   */
  public void testFullWrapper() {
    AttachmentDocumentWrapper<Doc.N, Doc.E, Doc.T> wrapper = createFullWrapper();
    checkBasicFields(wrapper);
    checkElementFields(wrapper);
  }

  /**
   * Tests that mutations are correctly persisted.
   */
  public void testMutableWrapper() {
    MutableClientAttachment wrapper = createBasicWrapper();

    wrapper.setSize(SIZE + 1);
    wrapper.setFilename(FILENAME + ".foo");
    wrapper.setMalware(true);
    wrapper.setMimeType(MIME_TYPE + "!");
    wrapper.setStatus(UPLOAD_STATUS);
    wrapper.setUploadedByteCount(UPLOAD_PROGRESS + 1);

    wrapper.setThumbnail(THUMB_WIDTH, THUMB_HEIGHT);
    wrapper.setImage(IMAGE_WIDTH, IMAGE_HEIGHT);

    assertEquals(FILENAME + ".foo", wrapper.getFilename());
    assertEquals(MIME_TYPE + "!", wrapper.getMimeType());
    assertEquals(Long.valueOf(SIZE + 1), wrapper.getSize());
    assertEquals(UPLOAD_STATUS, wrapper.getStatus());
    assertEquals(UPLOAD_PROGRESS + 1, wrapper.getUploadedByteCount());

    assertTrue(wrapper.isMalware());

    checkElementFields(wrapper);
  }

  /**
   * Checks that all of the attribute fields have been set correctly.
   *
   * @param wrapper the wrapper to check
   */
  private static void checkBasicFields(MutableClientAttachment wrapper) {
    assertEquals(FILENAME, wrapper.getFilename());
    assertEquals(CREATOR, wrapper.getCreator());
    assertEquals(MIME_TYPE, wrapper.getMimeType());
    assertEquals(SIZE, wrapper.getSize());
    assertEquals(UPLOAD_PROGRESS, wrapper.getUploadedByteCount());
    assertEquals(UPLOAD_STATUS, wrapper.getStatus());

    assertFalse(wrapper.isMalware());
  }

  /**
   * Checks the values on the thumbnail and image metadata fields.
   *
   * @param wrapper the wrapper
   */
  private static void checkElementFields(MutableClientAttachment wrapper) {
    ImageMetadata attachmentImage = wrapper.getContentImageMetadata();
    assertEquals(IMAGE_HEIGHT, attachmentImage.getHeight());
    assertEquals(IMAGE_WIDTH, attachmentImage.getWidth());

    ImageMetadata attachmentThumbnail = wrapper.getThumbnailImageMetadata();
    assertEquals(THUMB_HEIGHT, attachmentThumbnail.getHeight());
    assertEquals(THUMB_WIDTH, attachmentThumbnail.getWidth());
  }

  /**
   * Creates a basic wrapper, missing the optional image and thumbnail metadata fields.
   *
   * @return the new wrapper
   */
  private AttachmentDocumentWrapper<Doc.N, Doc.E, Doc.T> createBasicWrapper() {
    ObservableDocument document = BasicFactories.observableDocumentProvider()
        .parse(String.format(
            "<node key=\"upload_progress\" value=\"%d\"/>"
                + "<node key=\"creator\" value=\"%s\"/>"
                + "<node key=\"attachment_size\" value=\"%d\"/>"
                + "<node key=\"malware\" value=\"false\"/>"
                + "<node key=\"status\" value=\"%s\"/>"
                + "<node key=\"filename\" value=\"%s\"/>"
                + "<node key=\"mime_type\" value=\"%s\"/>",
            UPLOAD_PROGRESS, CREATOR, SIZE, UPLOAD_STATUS, FILENAME, MIME_TYPE));

    return new AttachmentDocumentWrapper<Doc.N, Doc.E, Doc.T>(document);
  }

  /**
   * Creates a full wrapper with image and thumbnail elements populated.
   *
   * @return the new wrapper
   */
  private AttachmentDocumentWrapper<Doc.N, Doc.E, Doc.T> createFullWrapper() {
    ObservableDocument document = BasicFactories.observableDocumentProvider()
        .parse(String.format(
            "<node key=\"upload_progress\" value=\"%d\"/>"
            + "<node key=\"creator\" value=\"%s\"/>"
            + "<node key=\"attachment_size\" value=\"%d\"/>"
            + "<node key=\"malware\" value=\"false\"/>"
            + "<node key=\"status\" value=\"%s\"/>"
            + "<node key=\"filename\" value=\"%s\"/>"
            + "<node key=\"mime_type\" value=\"%s\"/>"
            + "<node key=\"image_height\" value=\"%d\"/>"
            + "<node key=\"image_width\" value=\"%d\"/>"
            + "<node key=\"thumbnail_height\" value=\"%d\"/>"
            + "<node key=\"thumbnail_width\" value=\"%d\"/>",
            UPLOAD_PROGRESS, CREATOR, SIZE, UPLOAD_STATUS, FILENAME, MIME_TYPE, IMAGE_HEIGHT,
            IMAGE_WIDTH, THUMB_HEIGHT, THUMB_WIDTH));

    return new AttachmentDocumentWrapper<Doc.N, Doc.E, Doc.T>(document);
  }
}
