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


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.raw.RawDocumentProviderImpl;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;

/**
 * Tests logic of the rich text mutation builder.
 *
 */

public class RawElementStyleViewTest extends TestCase {
  public void testNoStyle() {
    RawElementStyleView view = parse("<p/>");
    assertEquals(null, view.getStylePropertyValue(view.getDocumentElement(), "foo"));
  }

  public void testEmptyStyle() {
    RawElementStyleView view = parse("<p style=\"\"/>");
    assertEquals(null, view.getStylePropertyValue(view.getDocumentElement(), "foo"));
  }

  public void testSingleStyle() {
    RawElementStyleView view = parse("<p style=\"foo: value1\"/>");
    assertEquals("value1", view.getStylePropertyValue(view.getDocumentElement(), "foo"));
    assertEquals(null, view.getStylePropertyValue(view.getDocumentElement(), "bar"));
  }

  public void testMultipleStyles() {
    RawElementStyleView view = parse("<p style=\"foo: value1; bar: value2\"/>");
    view.getDocumentElement();
    assertEquals("value1", view.getStylePropertyValue(view.getDocumentElement(), "foo"));
    assertEquals("value2", view.getStylePropertyValue(view.getDocumentElement(), "bar"));
    assertEquals(null   , view.getStylePropertyValue(view.getDocumentElement(), "baz"));
  }

  private RawElementStyleView parse(String xmlString) {
    return new RawElementStyleView(
        RawDocumentProviderImpl.create(RawDocumentImpl.BUILDER).parse(xmlString));
  }
}
