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

import java.util.Map;

/**
 * Represents an image within a Wave. The image can either refer to an external
 * resource or a Wave attachment. An external image is defined by the 'url'
 * property, while the Wave attachment is defined by the 'attachmentId'
 * property.
 */
public class Image extends Element {

  public static final String ATTACHMENT_ID = "attachmentId";
  public static final String CAPTION = "caption";
  public static final String HEIGHT = "height";
  public static final String URL = "url";
  public static final String WIDTH = "width";

  /**
   * Constructs an empty image.
   */
  public Image() {
    super(ElementType.IMAGE);
  }

  /**
   * Constructs an image with a given set of properties.
   *
   * @param properties the properties of the image.
   */
  public Image(Map<String, String> properties) {
    super(ElementType.IMAGE, properties);
  }

  /**
   * Constructs a Wave image given an attachment id and a caption.
   *
   * @param attachmentId the attachment id of the wave image.
   * @param caption the caption for the image.
   */
  public Image(String attachmentId, String caption) {
    this();
    setAttachmentId(attachmentId);
    setCaption(caption);
  }

  /**
   * Constructs an external image given a url, image dimensions, and a caption.
   *
   * @param url the url for the external image.
   * @param width the width of the image.
   * @param height the height of the image.
   * @param caption the caption for the image.
   */
  public Image(String url, int width, int height, String caption) {
    this();
    setUrl(url);
    setWidth(width);
    setHeight(height);
    setCaption(caption);
  }

  /**
   * Returns the URL for the image.
   *
   * @return the URL for the image.
   */
  public String getUrl() {
    return getProperty(URL);
  }

  /**
   * Changes the URL for the image to the given url. This will cause the new
   * image to be initialized and loaded.
   *
   * @param url the new image url.
   */
  public void setUrl(String url) {
    setProperty(URL, url);
  }

  /**
   * Sets the fixed width of the image to be displayed.
   *
   * @param width the fixed width of the image.
   */
  public void setWidth(int width) {
    setProperty(WIDTH, Integer.toString(width));
  }

  /**
   * Returns the fixed width of the image.
   *
   * @return the fixed width of the image or -1 if no width was specified.
   *
   * @throws IllegalStateException if no width was specified.
   */
  public int getWidth() {
    String width = getProperty(WIDTH);
    if (width == null) {
      throw new IllegalStateException("This image's width has not been set.");
    }
    return Integer.parseInt(width);
  }

  /**
   * Sets the fixed height of the image to be displayed.
   *
   * @param height the fixed height of the image.
   */
  public void setHeight(int height) {
    setProperty(HEIGHT, Integer.toString(height));
  }

  /**
   * Returns the fixed height of the image.
   *
   * @return the fixed height of the image.
   *
   * @throws IllegalStateException if no height was specified.
   */
  public int getHeight() {
    String height = getProperty(HEIGHT);
    if (height == null) {
      throw new IllegalStateException("This image's height has not been set.");
    }
    return Integer.parseInt(height);
  }

  /**
   * Sets the attacmentId for the Wave image.
   *
   * @param attachmentId the attachment id for the image.
   */
  public void setAttachmentId(String attachmentId) {
    setProperty(ATTACHMENT_ID, attachmentId);
  }

  /**
   * Returns the attachmentId for the image.
   *
   * @return the attachmentId for the image.
   */
  public String getAttachmentId() {
    return getProperty(ATTACHMENT_ID);
  }

  /**
   * Sets the caption for the image.
   *
   * @param caption the caption to display for the image.
   */
  public void setCaption(String caption) {
    setProperty(CAPTION, caption);
  }

  /**
   * Returns the caption for the image.
   *
   * @return the caption for the image.
   */
  public String getCaption() {
    return getProperty(CAPTION);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * image with the given attachment id.
   *
   * @param attachmentId the attachment id to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByAttachmentId(String attachmentId) {
    return Restriction.of(ATTACHMENT_ID, attachmentId);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * image with the given caption.
   *
   * @param caption the caption to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByCaption(String caption) {
    return Restriction.of(CAPTION, caption);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * image with the given URL.
   *
   * @param url the URL to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByUrl(String url) {
    return Restriction.of(URL, url);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * image with the given width.
   *
   * @param width the width to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByWidth(int width) {
    return Restriction.of(WIDTH, Integer.toString(width));
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * image with the given height.
   *
   * @param height the height to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByHeight(int height) {
    return Restriction.of(HEIGHT, Integer.toString(height));
  }
}
