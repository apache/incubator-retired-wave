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

package org.waveprotocol.wave.model.supplement;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.supplement.ObservablePrimitiveSupplement.Listener;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;

/**
 * Test case for {@link GadgetStateCollection}.
 *
 */

public class GadgetStateCollectionTest extends TestCase {

  private static final String GADGET1 = "1";
  private static final String GADGET2 = "2";

  private Listener listener;
  private GadgetStateCollection<Doc.E> states;

  public void testSetGadgetState() {
    setupDoc();
    assertEquals(0, states.getGadgetState(GADGET1).countEntries());
    assertEquals(0, states.getGadgetState(GADGET2).countEntries());
    states.setGadgetState(GADGET1, "key1", "value1 in 1");
    assertEquals(1, states.getGadgetState(GADGET1).countEntries());
    assertEquals(0, states.getGadgetState(GADGET2).countEntries());
    assertEquals("value1 in 1", states.getGadgetState(GADGET1).get("key1"));
    states.setGadgetState(GADGET2, "key1", "value1 in 2");
    assertEquals(1, states.getGadgetState(GADGET1).countEntries());
    assertEquals(1, states.getGadgetState(GADGET2).countEntries());
    assertEquals("value1 in 1", states.getGadgetState(GADGET1).get("key1"));
    assertEquals("value1 in 2", states.getGadgetState(GADGET2).get("key1"));
    states.setGadgetState(GADGET1, "key1", null);
    states.setGadgetState(GADGET2, "key2", "value2 in 2");
    assertEquals(0, states.getGadgetState(GADGET1).countEntries());
    assertEquals(2, states.getGadgetState(GADGET2).countEntries());
    assertEquals("value1 in 2", states.getGadgetState(GADGET2).get("key1"));
    assertEquals("value2 in 2", states.getGadgetState(GADGET2).get("key2"));
  }

  public void testGadgetStateEvents() {
    setupDoc();
    never();
    states.setGadgetState(GADGET1, "key1", "value1 in 1");
    verify(listener).onGadgetStateChanged(GADGET1, "key1", null, "value1 in 1");
    states.setGadgetState(GADGET2, "key1", "value1 in 2");
    verify(listener).onGadgetStateChanged(GADGET2, "key1", null, "value1 in 2");
    states.setGadgetState(GADGET1, "key1", "value2 in 1");
    verify(listener).onGadgetStateChanged(GADGET1, "key1", "value1 in 1", "value2 in 1");
    states.setGadgetState(GADGET2, "key1", null);
    verify(listener).onGadgetStateChanged(GADGET2, "key1", "value1 in 2", null);
  }

  private void setupDoc() {
    ObservablePluggableMutableDocument doc =
        new ObservablePluggableMutableDocument(
            DocumentSchema.NO_SCHEMA_CONSTRAINTS, DocProviders.POJO.parse("").asOperation());
    doc.init(SilentOperationSink.VOID);
    listener = mock(Listener.class);
    states = GadgetStateCollection.create(DefaultDocumentEventRouter.create(doc),
        doc.getDocumentElement(), listener);
  }
}
