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

import com.google.protobuf.InvalidProtocolBufferException;
import org.waveprotocol.wave.federation.Proto.ProtocolDocumentOperation.Component;
import org.waveprotocol.wave.federation.Proto.ProtocolDocumentOperation.Component.ElementStart;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation.MutateDocument;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.image.ImageConstants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Raw deltas parser.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class DeltaParser {

  private DeltaParser() {
  }

  public static List<ProtocolWaveletDelta> parseDeltas(List<byte[]> rawDeltas)
      throws InvalidProtocolBufferException {
    List<ProtocolWaveletDelta> deltas = new ArrayList<ProtocolWaveletDelta>();
    for (byte[] delta : rawDeltas) {
      deltas.add(ProtocolWaveletDelta.parseFrom(delta));
    }
    return deltas;
  }

  /**
   * Extract attachment ids from operations.
   */
  public static Set<AttachmentId> getAttachemntIds(ProtocolWaveletDelta delta) {
    Set<AttachmentId> ids = new HashSet<AttachmentId>();
    for (int i=0; i < delta.getOperationCount(); i++) {
      ProtocolWaveletOperation op = delta.getOperation(i);
      if (op.hasMutateDocument()) {
        MutateDocument doc = op.getMutateDocument();
        for (int c = 0; c < doc.getDocumentOperation().getComponentCount(); c++) {
          Component comp = doc.getDocumentOperation().getComponent(c);
          ElementStart start = comp.getElementStart();
          if (ImageConstants.TAGNAME.equals(start.getType())) {
            for (int a=0; a < start.getAttributeCount(); a++) {
              Component.KeyValuePair attr = start.getAttribute(a);
              if (ImageConstants.ATTACHMENT_ATTRIBUTE.equals(attr.getKey())) {
                try {
                  ids.add(AttachmentId.deserialise(attr.getValue()));
                } catch (InvalidIdException ex) {
                  Console.error("Invalid attachment Id " + attr.getValue(), ex);
                }
              }
            }
          }
        }
      }
    }
    return ids;
  }
}
