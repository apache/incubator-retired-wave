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

package org.waveprotocol.wave.model.document;

/**
 * Injects a document listener into an observable document, dispatching
 * events to a (document-free) document listener.
 *
 */
public final class MutableDocumentListenerAdapter implements DocHandler {
  private final MutableDocumentListener listener;
  private final ObservableDocument doc;

  public MutableDocumentListenerAdapter(MutableDocumentListener listener,
      ObservableDocument doc) {
    this.doc = doc;
    this.listener = listener;
  }

  public static <N> void observe(MutableDocumentListener listener,
      ObservableDocument doc) {
    doc.addListener(new MutableDocumentListenerAdapter(listener, doc));
  }

  @Override
  public void onDocumentEvents(DocHandler.EventBundle<Doc.N, Doc.E, Doc.T> event) {
    listener.onDocumentEvents(new MutableDocumentEvent(doc, event));
  }
}
