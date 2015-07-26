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

package org.waveprotocol.box.expimp;

import java.util.Set;

import org.waveprotocol.box.server.util.testing.TestingConstants;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.federation.Proto;
import org.waveprotocol.wave.federation.Proto.ProtocolDocumentOperation.Component.ElementStart;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation.MutateDocument;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.image.ImageConstants;

import junit.framework.TestCase;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.model.version.HashedVersion;

/**
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class DeltaParserTest extends TestCase implements TestingConstants {

  private static final HashedVersion VERSION = HashedVersion.unsigned(111L);
  private static final String DOC_ID = "doc1";
  private static final String ATTACHMENT_ID = "attachment1";

  private static final MutateDocument document = MutateDocument.newBuilder()
      .setDocumentId(DOC_ID)
      .setDocumentOperation(Proto.ProtocolDocumentOperation.newBuilder()
        .addComponent(Proto.ProtocolDocumentOperation.Component.newBuilder()
          .setElementStart(ElementStart.newBuilder()
            .setType(ImageConstants.TAGNAME)
            .addAttribute(Proto.ProtocolDocumentOperation.Component.KeyValuePair.newBuilder()
              .setKey(ImageConstants.ATTACHMENT_ATTRIBUTE)
              .setValue(ATTACHMENT_ID))))).build();

  private static final ProtocolWaveletDelta DELTA_ADD_USER = ProtocolWaveletDelta.newBuilder()
    .setAuthor(USER)
    .setHashedVersion(CoreWaveletOperationSerializer.serialize(VERSION))
    .addOperation(ProtocolWaveletOperation.newBuilder().setMutateDocument(document)).build();

  @Override
  protected void setUp() throws Exception {
  }

  public void testGetAttachmentIds() {
    Set<AttachmentId> ids = DeltaParser.getAttachemntIds(DELTA_ADD_USER);
    assertEquals(ids.size(), 1);
    assertEquals(ids.iterator().next().getId(), ATTACHMENT_ID);
  }
}
