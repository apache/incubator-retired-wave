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

import junit.framework.TestCase;

/**
 * Image unit tests.
 *
 * @author scovitz@google.com (Seth Covitz)
 */
public class ImageRobotTest extends TestCase {

  private static final String ATTACHMENT_ID = "attachmentId";
  private static final String CAPTION = "caption";
  private static final int HEIGHT = 240;
  private static final String URL = "url";
  private static final int WIDTH = 320;

  public void testDefaultConstructor() {
    Image image = new Image();
    assertEquals(ElementType.IMAGE, image.getType());
    assertNull(image.getAttachmentId());
    assertNull(image.getCaption());
    assertNull(image.getUrl());
  }

  public void testAttachmentConstructor() {
    Image image = new Image(ATTACHMENT_ID, CAPTION);
    assertEquals(ElementType.IMAGE, image.getType());
    assertEquals(ATTACHMENT_ID, image.getAttachmentId());
    assertEquals(CAPTION, image.getCaption());
    assertNull(image.getUrl());
  }

  public void testImageConstructor() {
    Image image = new Image(URL, WIDTH, HEIGHT, CAPTION);
    assertEquals(ElementType.IMAGE, image.getType());
    assertNull(image.getAttachmentId());
    assertEquals(CAPTION, image.getCaption());
    assertEquals(URL, image.getUrl());
    assertEquals(WIDTH, image.getWidth());
    assertEquals(HEIGHT, image.getHeight());
  }

  public void testGettersAndSetters() {
    Image image = new Image();
    image.setAttachmentId(ATTACHMENT_ID);
    image.setCaption(CAPTION);
    image.setUrl(URL);
    image.setWidth(WIDTH);
    image.setHeight(HEIGHT);
    assertEquals(ATTACHMENT_ID, image.getAttachmentId());
    assertEquals(CAPTION, image.getCaption());
    assertEquals(URL, image.getUrl());
    assertEquals(WIDTH, image.getWidth());
    assertEquals(HEIGHT, image.getHeight());
  }

  public void testGetWidth() {
    Image image = new Image();
    image.setWidth(WIDTH);
    assertEquals(WIDTH, image.getWidth());

    image = new Image();
    try {
      image.getWidth();
      fail("Should have failed with IllegalStateException. Calling getWidth() is not allowed if " +
           "the image doesn't have a width.");
    } catch (IllegalStateException e) {
      // Expected.
    }
  }

  public void testGetHeight() {
    Image image = new Image();
    image.setHeight(HEIGHT);
    assertEquals(HEIGHT, image.getHeight());

    image = new Image();
    try {
      image.getHeight();
      fail("Should have failed with IllegalStateException. Calling getHeight() is not allowed if " +
           "the image doesn't have a height.");
    } catch (IllegalStateException e) {
      // Expected.
    }
  }
}
