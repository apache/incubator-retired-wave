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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.MutableDocumentImpl;
import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Implementation of Document in terms of generic inner objects
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class DocumentImpl extends MutableDocumentImpl<Doc.N, Doc.E, Doc.T> implements Document {

  /**
   * @see MutableDocumentImpl#MutableDocumentImpl(OperationSequencer, ReadableWDocument)
   */
  // Unfortunately, java does not permit <N extends N, E extends N & Doc.E, ...>
  // which would be required to make this typesafe.
  @SuppressWarnings({"unchecked", "rawtypes"})
  public DocumentImpl(OperationSequencer<Nindo> sequencer,
      ReadableWDocument document) {
    super(sequencer, document);

    Preconditions.checkArgument(document.getDocumentElement() instanceof Doc.E,
        "Document and sequencer must be for nodes of the Doc.* variety");
  }
}
