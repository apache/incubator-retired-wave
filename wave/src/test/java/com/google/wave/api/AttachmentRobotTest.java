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
 * @author jli@google.com (Jimin Li)
 *
 */
public class AttachmentRobotTest extends TestCase {
 
  public static final String CAPTION = "caption";
  public static final byte[] DATA = "data".getBytes();

  public void testConstructorWithCaptionAndData() { 
    Attachment attachment = new Attachment(CAPTION, DATA);
    assertEquals(ElementType.ATTACHMENT, attachment.getType());
    assertEquals(CAPTION, attachment.getCaption());
    assertEquals(DATA, attachment.getData());
  }

  public void testSetProperty(){
    Attachment attachment = new Attachment(CAPTION, DATA);
    try {
      attachment.setProperty(Attachment.ATTACHMENT_ID, "attachment1");
      fail("Should have thrown exception when trying to set attachment id.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
    
    try {
      attachment.setProperty(Attachment.MIME_TYPE, "m1");
      fail("Should have thrown exception when trying to set mime type.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    try {
      attachment.setProperty(Attachment.ATTACHMENT_URL, "a_url");
      fail("Should have thrown exception when trying to set attachment url.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }

    try {
      attachment.setProperty(Attachment.DATA, "data");
      fail("Should have thrown exception when trying to set data.");
    } catch (IllegalArgumentException e) {
      // Expected.
    }
    attachment.setCaption("new caption");
    assertEquals("new caption", attachment.getCaption());
  } 
}
