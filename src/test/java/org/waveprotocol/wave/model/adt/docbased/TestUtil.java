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

import static junit.framework.Assert.assertEquals;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Map;

/**
 * Utilities for testing document-based value types.
 *
 * @author anorth@google.com (Alex North)
 */
final class TestUtil {
  private TestUtil() {}

  /**
   * A context in which a value is used. Specifically, a document
   * and an element within that document in which value state is embedded.
   */
  static final class ValueContext<N, E extends N> {
    public final ObservableMutableDocument<N, E, ?> doc;
    public final E container;

    ValueContext(ObservableMutableDocument<N, E, ?> doc, E container) {
      this.doc = doc;
      this.container = container;
    }

    /**
     * Deletes the container element.
     */
    void delete() {
      doc.deleteNode(container);
    }
  }

  /**
   * Creates a {@link Factory} for integers backed by string representations as
   * an attribute value.
   *
   * @param attributeName name of the backing attribute
   */
  static Factory<Doc.E, Integer, String> newIntegerFactory(final String attributeName) {
    return new Factory<E, Integer, String>() {
      @Override
      public Integer adapt(DocumentEventRouter<? super E, E, ?> router, E element) {
        String valueStr = router.getDocument().getAttribute(element, attributeName);
        return Integer.parseInt(valueStr);
      }

      @Override
      public Initializer createInitializer(final String initialState) {
        return new Initializer() {
          @Override
          public void initialize(Map<String, String> target) {
            target.put(attributeName, initialState);
          }
        };
      }};
  }

  /**
   * Asserts that an {@link Initializer} produces expected values.
   */
  static void assertInitializerValues(Map<String, String> expected, Initializer init) {
    Map<String, String> attrs = CollectionUtils.newHashMap();
    init.initialize(attrs);
    assertEquals(expected, attrs);
  }
}
