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

package com.google.wave.api;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents a wave attachment within a Wave.
 */
public class Attachment extends Element {
  public static final String ATTACHMENT_ID = "attachmentId";
  public static final String CAPTION = "caption";
  public static final String MIME_TYPE = "mimeType";
  public static final String DATA = "data";
  public static final String ATTACHMENT_URL = "attachmentUrl";

  private static final Logger LOG = Logger.getLogger(Attachment.class.getName());
  
  /** Attachment data. */
  private byte[] data;

  /**
   * Constructs an attachment with a given set of properties and data.
   * 
   * @param properties the properties of the attachment.
   * @param data the data of the attachment 
   */
  public Attachment(Map<String, String> properties, byte[] data) {
    super(ElementType.ATTACHMENT, properties);
    this.data = data;
  }

  /**
   * Constructs an attachment with data in bytes and caption.
   * 
   * @param caption the caption of the attachment.
   * @param data the attachment data as bytes.
   */
  public Attachment(String caption, byte[] data) {
    super(ElementType.ATTACHMENT);
    setCaption(caption);
    this.data = data;
  }

  @Override
  public void setProperty(String name, String value) {
    if (name.equals(ATTACHMENT_ID) || name.equals(MIME_TYPE) || 
        name.equals(ATTACHMENT_URL) || name.equals(DATA)) {
      throw new IllegalArgumentException(name + " can not be changed.");
    }
    super.setProperty(name, value);
  }

  /**
   * Returns the attachment id for the attachment.
   * 
   * @return the attachment id for the attachment.
   */
  public String getAttachmentId() {
    return getProperty(ATTACHMENT_ID);
  }

  /**
   * Returns the url for the attachment.
   * 
   * @return the url for the attachment.
   */
  public String getAttachmentUrl() {
    return getProperty(ATTACHMENT_URL);
  }

  /**
   * Sets the caption for the attachment.
   * 
   * @param caption the caption to display for the attachment.
   */
  public void setCaption(String caption) {
    setProperty(CAPTION, caption);
  }

  /**
   * Returns the caption for the attachment.
   * 
   * @return the caption for the attachment.
   */
  public String getCaption() {
    return getProperty(CAPTION);
  }

  /**
   * Returns the data for the attachment. Data will be fetched via HTTP if 
   * it's not available.
   * 
   * @return the data for the attachment.
   */
  public byte[] getData() {
    if (data == null) {
      try {
        fetch();
      } catch (IOException e) {
        LOG.info("Error fetching attachment data: " + e);
      }
    }
    return data;
  }

  public boolean hasData() {
    return data != null;
  }

  /**
   * Returns the MIME type for the attachment.
   * 
   * @return the MIME type for the attachment.
   */
  public String getMimeType() {
    return getProperty(MIME_TYPE);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * attachment with the given caption.
   * 
   * @param caption the caption to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByCaption(String caption) {
    return Restriction.of(CAPTION, caption);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * attachment with the given MIME type.
   * 
   * @param mimeType the MIME type to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByMimeType(String mimeType) {
    return Restriction.of(MIME_TYPE, mimeType);
  }
  
  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * attachment with the given attachment id.
   * 
   * @param attachmentId the id of the attachment. 
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByAttachmentId(String attachmentId) {
    return Restriction.of(ATTACHMENT_ID, attachmentId);
  }
  
  private void fetch() throws IOException {
    if (getAttachmentUrl() != null && (!getAttachmentUrl().isEmpty())) {
      InputStream input = null;
      ByteArrayOutputStream output = null;
      try {
        URL url = new URL(getAttachmentUrl());
        input = url.openStream();
        output = new ByteArrayOutputStream();
        int i;
        while ((i = input.read()) != -1) {
          output.write(i);
        }
        this.data = output.toByteArray();
      } finally {
        if (input != null) {
          input.close();
        }
        if (output != null) {
          output.close();
        }
      }
    }
  }
}