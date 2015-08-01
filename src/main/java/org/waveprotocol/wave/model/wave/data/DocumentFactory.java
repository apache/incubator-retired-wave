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

package org.waveprotocol.wave.model.wave.data;

import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.id.WaveletId;

/**
 * Factory interface for creating new documents within a wave view.
 *
 * @author anorth@google.com (Alex North)
 */
public interface DocumentFactory<D extends DocumentOperationSink> {
  /**
   * Creates a new document with the given content.
   * The document's identity within the wave view is provided such that an
   * implementation of this interface may keep track of the documents within
   * a wave view, providing domain-specific behavior for them.
   *
   * @param waveletId   wavelet in which the new document is being created
   * @param docId       id of the new document
   * @param content     content for the new document
   * @return a new document
   */
  D create(WaveletId waveletId, String docId, DocInitialization content);
}
