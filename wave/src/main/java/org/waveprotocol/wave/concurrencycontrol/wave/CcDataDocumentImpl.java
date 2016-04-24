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

package org.waveprotocol.wave.concurrencycontrol.wave;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;

/**
 * Client's implementation of data documents. These documents are just
 * observable documents that are compatible with CC (i.e., they can be flushed).
 *
 */
public class CcDataDocumentImpl extends ObservablePluggableMutableDocument implements CcDocument {

  public CcDataDocumentImpl(DocumentSchema schema, DocInitialization content) {
    super(schema, content);
  }

  @Override
  public boolean flush(Runnable resume) {
    // Nothing to flush
    return true;
  }
}
