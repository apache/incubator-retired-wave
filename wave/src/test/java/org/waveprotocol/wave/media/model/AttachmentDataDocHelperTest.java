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

import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

/**
 * Attachment data document helper tests.
 *
 */

public class AttachmentDataDocHelperTest extends TestCase  {

  private static final String DATA_DOC_CONTENT = "<test/>";
  private static final XmlStringBuilder DATA_DOC_XML =
      XmlStringBuilder.createFromXmlString(DATA_DOC_CONTENT);
  private static final String EMPTY_DATA_DOC = "";

  private FakeWaveView waveView;
  private OpBasedWavelet wavelet;

  @Override
  public void setUp() {
    waveView = BasicFactories.fakeWaveViewBuilder().build();
    wavelet = waveView.create();
  }

  private void createAttachmentDataDoc(String attachmentId) {
    String attachmentDataDocId = AttachmentDataDocHelper.dataDocIdFromAttachmentId(attachmentId);
    wavelet.getDocument(attachmentDataDocId).appendXml(DATA_DOC_XML);
  }

  /**
   * Primary success case with a valid id containing a domain component.
   *
   * @throws InvalidIdException
   */
  public void testIdWithDomain() throws InvalidIdException {
    String attachmentId = "domain.com/some-id";
    createAttachmentDataDoc(attachmentId);
    ObservableDocument attachmentDataDoc =
        AttachmentDataDocHelper.getAttachmentDataDoc(wavelet, attachmentId);
    assertEquals(DATA_DOC_CONTENT, attachmentDataDoc.toXmlString());
  }

  /**
   * Test case where the domain component is stripped off but a data doc with a
   * matching id component is found.
   *
   * @throws InvalidIdException
   */
  public void testIdWithDifferentDomain() throws InvalidIdException {
    String attachmentId = "domain.com/some-id";
    String attachmentIdDifferentDomain = "something-else/some-id";
    createAttachmentDataDoc(attachmentIdDifferentDomain);
    ObservableDocument attachmentDataDoc =
        AttachmentDataDocHelper.getAttachmentDataDoc(wavelet, attachmentId);
    assertEquals(DATA_DOC_CONTENT, attachmentDataDoc.toXmlString());
  }

  /**
   * Test case where the data doc for a non-migrated id with no domain
   * component is found.
   *
   * @throws InvalidIdException
   */
  public void testIdWithNoDomain() throws InvalidIdException {
    String attachmentId = "some-id";
    createAttachmentDataDoc(attachmentId);
    ObservableDocument attachmentDataDoc =
        AttachmentDataDocHelper.getAttachmentDataDoc(wavelet, attachmentId);
    assertEquals(DATA_DOC_CONTENT, attachmentDataDoc.toXmlString());
  }

  /**
   * Test case where no match can be found.
   *
   * @throws InvalidIdException
   */
  public void testIdNotFound() throws InvalidIdException {
    String attachmentId = "domain.com/some-id";
    ObservableDocument attachmentDataDoc =
        AttachmentDataDocHelper.getAttachmentDataDoc(wavelet, attachmentId);
    assertEquals(EMPTY_DATA_DOC, attachmentDataDoc.toXmlString());
  }

  /**
   * Test case where a null attachment id is requested.
   *
   * @throws InvalidIdException
   */
  public void testNullId() throws InvalidIdException {
    try {
      ObservableDocument attachmentDataDoc =
          AttachmentDataDocHelper.getAttachmentDataDoc(wavelet, null);
      fail("NullPointerException was expected but not thrown.");
    } catch (NullPointerException expected) {
      // empty
    }
  }

  /**
   * Test case where an attachment id contains too many components.
   */
  public void testInvalidIdInput() {
    try {
      String attachmentId = "domain.com/blah/some-id";
      ObservableDocument attachmentDataDoc =
          AttachmentDataDocHelper.getAttachmentDataDoc(wavelet, attachmentId);
      fail("InvalidIdException was expected but not thrown.");
    } catch (InvalidIdException expected) {
      // empty
    }
  }

  /**
   * Test case where another data doc with an invalid id is encountered during
   * the exhaustive search for the attachment id.
   */
  public void testInvalidAttachmentDataDocId() {
    try {
      String attachmentId = "domain.com/some-id";
      String invalidAttachmentId = "domain.com/blah/some-id";
      createAttachmentDataDoc(invalidAttachmentId);
      ObservableDocument attachmentDataDoc =
          AttachmentDataDocHelper.getAttachmentDataDoc(wavelet, attachmentId);
      fail("InvalidIdException was expected but not thrown.");
    } catch (InvalidIdException expected) {
      // empty
    }
  }
}
