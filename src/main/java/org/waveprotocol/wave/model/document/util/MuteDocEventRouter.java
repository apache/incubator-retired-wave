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
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.util.AttributeListener;
import org.waveprotocol.wave.model.util.DeletionListener;
import org.waveprotocol.wave.model.util.ElementListener;

/**
 * A DocEventRouter which ignores listeners. It is intended for use in
 * situations where documents are not live, or the models being built upon them
 * are short-lived, such that listening to events is unnecessary. By not adding
 * listeners to the underlying document, the MuteDocEventRouter allows the
 * models built upon it to be garbage collected immediately, not when the
 * document is garbage collected.
 *
 *  Warning! If the document does change underneath, arbitrarily bad things can
 * happen.
 *
 *
 */
public class MuteDocEventRouter implements DocEventRouter {

  private static final ListenerRegistration NULL_REGISTRATION = new ListenerRegistration() {
    @Override
    public void detach() {
      // Do nothing
    }
  };

  private final ObservableDocument doc;

  private MuteDocEventRouter(ObservableDocument doc) {
    this.doc = doc;
  }

  public static MuteDocEventRouter create(ObservableDocument doc) {
    return new MuteDocEventRouter(doc);
  }

  @Override
  public ObservableDocument getDocument() {
    return doc;
  }

  @Override
  public ListenerRegistration addAttributeListener(
      Doc.E target, AttributeListener<Doc.E> listener) {
    return NULL_REGISTRATION;
  }

  @Override
  public ListenerRegistration addChildListener(Doc.E parent, ElementListener<Doc.E> listener) {
    return NULL_REGISTRATION;
  }

  @Override
  public ListenerRegistration addDeletionListener(Doc.E target, DeletionListener listener) {
    return NULL_REGISTRATION;
  }
}
