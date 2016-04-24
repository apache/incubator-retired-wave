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

package org.waveprotocol.wave.model.adt.docbased;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.MutableDocument.Method;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.ObservableMutableDocument.Action;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.DefaultDocumentEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.testing.BasicFactories;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test cases for {@link DocumentBasedBasicMap}.
 *
 */

public class DocumentBasedBooleanTest extends TestCase {

  private static final String ENTRY_TAG = "muted";
  private static final String VALUE_ATTR = "muted";

  //
  // To keep the tests succinct, the document state is described as just a List
  // of Booleans. Null means no attribute value.
  //

  private ObservableMutableDocument<?, ?, ?> doc;
  private DocumentBasedBoolean<?> target;

  //
  // Test-specific setup helpers.
  //

  @Override
  protected void setUp() throws Exception {
    createTargetOn(Collections.<Boolean>emptyList());
  }

  /**
   * Creates a document-based boolean.
   */
  private void createTargetOn(final List<Boolean> state) {
    doc = BasicFactories.observableDocumentProvider().create("blort", Attributes.EMPTY_MAP);
    for (Boolean b : state) {
      addEntry(b);
    }

    doc.with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(ObservableMutableDocument<N, E, T> doc) {
        target = DocumentBasedBoolean.create(DefaultDocumentEventRouter.create(doc),
            doc.getDocumentElement(), ENTRY_TAG, VALUE_ATTR);
      }
    });
  }

  /**
   * Adds an entry to the target map's underlying state. This simulates a
   * concurrent modification by some other agent.
   */
  private void addEntry(final Boolean state) {
    doc.with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(ObservableMutableDocument<N, E, T> doc) {
        E container = doc.getDocumentElement();

        // Insert entries
        Attributes attrs =
            state != null ? new AttributesImpl(VALUE_ATTR, state.toString()) : Attributes.EMPTY_MAP;
        doc.createChildElement(container, ENTRY_TAG, attrs);
      }
    });
  }

  /**
   * Checks the document state.
   *
   * @param state the document state specified in terms of entries
   */
  private void assertSubstrate(final List<Boolean> state) {
    doc.with(new Action() {
      @Override
      public <N, E extends N, T extends N> void exec(ObservableMutableDocument<N, E, T> doc) {
        E container = doc.getDocumentElement();

        E entry = DocHelper.getFirstChildElement(doc, container);
        // Skip over anything we don't care about
        while (entry != null && !ENTRY_TAG.equals(doc.getTagName(entry))) {
          entry = DocHelper.getNextSiblingElement(doc, entry);
        }

        for (Boolean b : state) {
          assertNotNull(entry);
          Attributes attrs =
              b != null ? new AttributesImpl(VALUE_ATTR, b.toString()) : Attributes.EMPTY_MAP;
          assertEquals(attrs, doc.getAttributes(entry));

          entry = DocHelper.getNextSiblingElement(doc, entry);
          // Skip over anything we don't care about
          while (entry != null && !ENTRY_TAG.equals(doc.getTagName(entry))) {
            entry = DocHelper.getNextSiblingElement(doc, entry);
          }
        }
        assertNull("Unexpected element in subtrate: " + entry, entry);
      }
    });
  }

  public void testInitialStateIsNull() {
    assertNull(target.get());
  }

  public void testSetTrueOnInitialStateMakesTrue() {
    target.set(true);
    assertTrue(target.get());
  }

  public void testSetFalseOnInitialStateMakesFalse() {
    target.set(false);
    assertFalse(target.get());
  }

  public void testBackwardsCompatibleWithTrue() {
    createTargetOn(Arrays.asList(true));
    assertTrue(target.get());
  }

  public void testBackwardsCompatibleWithCorruptTrue() {
    createTargetOn(Arrays.asList(true, true));
    assertTrue(target.get());
  }

  public void testBackwardsCompatibleWithFalse() {
    createTargetOn(Arrays.asList(false));
    assertFalse(target.get());
  }

  public void testBackwardsCompatibleWithCorruptFalse() {
    createTargetOn(Arrays.asList(false, false));
    assertFalse(target.get());
  }

  public void testWriteTrueCleansUp() {
    createTargetOn(Arrays.asList(false, false));
    target.set(true);
    assertSubstrate(Arrays.asList(true));
  }

  public void testWriteFalseCleansUp() {
    createTargetOn(Arrays.asList(false, true, null));
    target.set(false);
    assertSubstrate(Arrays.asList(false));
  }

  public void testWriteNullCleansUp() {
    createTargetOn(Arrays.asList(false, true, null));
    target.set(null);
    assertSubstrate(Arrays.<Boolean>asList());
  }

  public void testWriteDoesNotCleanupForeignElements() {
    createTargetOn(Arrays.asList(false, true));

    // Add some random element.
    final Object foreign = doc.with(new Method<Object>() {
      @Override
      public <N, E extends N, T extends N> Object exec(MutableDocument<N, E, T> doc) {
        return doc.createChildElement(doc.getDocumentElement(), "foreign", Attributes.EMPTY_MAP);
      }
    });

    target.set(null);

    // Verify that the random element remains.
    doc.with(new MutableDocument.Action() {
      @Override
      public <N, E extends N, T extends N> void exec(MutableDocument<N, E, T> doc) {
        E top = DocHelper.getFirstChildElement(doc, doc.getDocumentElement());
        assertEquals(foreign, top);
        assertNull(doc.getNextSibling(top));
      }
    });
  }
}
