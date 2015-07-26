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

import junit.framework.TestCase;

import org.waveprotocol.box.server.persistence.AttachmentStore.AttachmentData;
import org.waveprotocol.wave.media.model.AttachmentId;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Test cases for the Attachment Stores.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public abstract class AttachmentStoreTestBase extends TestCase {

  public void testStoreReturnsNullForNonexistantId() throws IOException {
    AttachmentStore store = newAttachmentStore();
    AttachmentId id = new AttachmentId("", "some_madeup_id");
    assertNull(store.getAttachment(id));
  }

  public void testStoreCanStoreData() throws Exception {
    String testData = "some file data";
    AttachmentId id = new AttachmentId("", "id_1");
    AttachmentStore store = makeStoreWithData(id, testData);

    AttachmentData data = store.getAttachment(id);
    assertEquals(testData, dataToString(data));
  }

  public void testContentLengthMatchesDataSize() throws Exception {
    String testData = "blah blah blah";
    AttachmentId id = new AttachmentId("", "id_2");
    AttachmentStore store = makeStoreWithData(id, testData);

    AttachmentData data = store.getAttachment(id);
    assertEquals(testData.length(), data.getSize());
  }

  public void testStoreCanDeleteData() throws Exception {
    String testData = "some day, I'm going to run out of test strings";
    AttachmentId id = new AttachmentId("", "id_3");
    AttachmentStore store = makeStoreWithData(id, testData);

    store.deleteAttachment(id);
    AttachmentData data = store.getAttachment(id);
    assertNull(data);
  }

  public void testAttachmentCanWriteToOutputStream() throws Exception {
    String testData = "maybe there's some easy way to generate test strings";
    AttachmentId id = new AttachmentId("", "id_4");
    AttachmentStore store = makeStoreWithData(id, testData);
    AttachmentData data = store.getAttachment(id);

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    InputStream io = data.getInputStream();
    try {
      AttachmentUtil.writeTo(io, stream);
      assertEquals(testData, stream.toString("UTF-8"));
    } finally {
      io.close();
    }
  }

  public void testAttachmentHasWorkingInputStream() throws Exception {
    String testData = "I suppose these strings don't actually need to be different";
    AttachmentId id = new AttachmentId("", "id_5");
    AttachmentStore store = makeStoreWithData(id, testData);
    AttachmentData data = store.getAttachment(id);

    BufferedReader reader = new BufferedReader(new InputStreamReader(data.getInputStream()));

    StringBuilder builder = new StringBuilder();
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        // This little snippet will discard any "\n" characters, but it shouldn't
        // matter.
        builder.append(line);
      }
    } finally {
      reader.close();
    }

    assertEquals(testData, builder.toString());
  }

  public void testGetStreamReturnsNewStream() throws Exception {
    String testData = "There's something quite peaceful about writing tests.";
    AttachmentId id = new AttachmentId("", "id_6");
    AttachmentStore store = makeStoreWithData(id, testData);
    AttachmentData data = store.getAttachment(id);

    InputStream is1 = data.getInputStream();
    InputStream is2 = data.getInputStream();
    InputStream is3 = null;
    try {
      assertNotSame(is1, is2);

      int firstByte = is1.read();
      assertSame(firstByte, is2.read());

      // Check that a new input stream created now still has the same first
      // byte.
      is3 = data.getInputStream();
      assertSame(firstByte, is3.read());
    } finally {
      is1.close();
      is2.close();
      if (is3 != null) {
        is3.close();
      }
    }
  }

  public void testOverwriteAttachmentThrowsException() throws Exception {
    String testData = "First.";
    AttachmentId id = new AttachmentId("", "id_7");
    AttachmentStore store = makeStoreWithData(id, testData);

    boolean exceptionThrown=false;
    try {
      // A second element added with the same ID should not write.
      writeStringDataToAttachmentStore(store, id, "Second");
    } catch (IOException ex) {
      exceptionThrown=true;
    }
    assertTrue(exceptionThrown);

    // Check that the database still contains the original entry
    assertEquals(testData, dataToString(store.getAttachment(id)));
  }

  // Helpers.
  /**
   * Create and return a new attachment store instance of the type being tested.
   * @return a new attachment store
   */
  protected abstract AttachmentStore newAttachmentStore();

  protected void writeStringDataToAttachmentStore(
      AttachmentStore store, AttachmentId id, String data) throws IOException {
    store.storeAttachment(id, new ByteArrayInputStream(data.getBytes("UTF-8")));
  }

  protected AttachmentStore makeStoreWithData(AttachmentId id, String data)
      throws Exception {
    AttachmentStore store = newAttachmentStore();
    writeStringDataToAttachmentStore(store, id, data);
    return store;
  }

  protected String dataToString(AttachmentData data) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    InputStream io = data.getInputStream();
    try {
      AttachmentUtil.writeTo(io, out);
    } finally {
      io.close();
    }
    return out.toString("UTF-8");
  }
}