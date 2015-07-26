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

package org.waveprotocol.wave.client.editor.extract;

import org.waveprotocol.wave.model.document.util.DocProviders;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * SubTreeXmlRenderer test.
 *
 */

public class SubTreeXmlRendererTest extends TestCase {

  public void testRenderXml() {
    testHelper("hello", 2, 4, "ll");
    testHelper("hello<x></x>", 0, 6, "hello<x></x>");
    testHelper("hello<x></x>", 5, 7, "<x></x>");
    testHelper("<x></x>hello", 0, 2, "<x></x>");
    testHelper("<x></x>hello", 0, 3, "<x></x>h");
    testHelper("<x></x>hello", 0, 7, "<x></x>hello");
    testHelper("<x></x>hello", 1, 1, "");

    testHelper("<x><y></y><z></z></x>", 0, 1, "<x></x>");
    testHelper("<x><y></y><z></z></x>", 0, 2, "<x><y></y></x>");
    testHelper("<x><y></y><z></z></x>", 1, 1, "");
    testHelper("<x><y></y><z></z></x>", 1, 2, "<y></y>");
    testHelper("<x><y></y><z></z></x>", 1, 3, "<y></y>");
    testHelper("<x><y></y><z></z></x>", 1, 4, "<y></y><z></z>");
    testHelper("<x><y></y><z></z></x>", 1, 5, "<y></y><z></z>");
    testHelper("<x><y></y><z></z></x>", 1, 6, "<x><y></y><z></z></x>");

    testHelper("<x><y></y><z></z></x>", 2, 2, "");
    testHelper("<x><y></y><z></z></x>", 2, 3, "<y></y>");
    testHelper("<x><y></y><z></z></x>", 2, 4, "<y></y><z></z>");
    testHelper("<x><y></y><z></z></x>", 2, 5, "<y></y><z></z>");
    testHelper("<x><y></y><z></z></x>", 2, 6, "<x><y></y><z></z></x>");

    testHelper("<x><y><a></a></y><z></z></x>", 3, 6, "<y><a></a></y><z></z>");
    testHelper("<x><y><a></a></y><z><b></b></z></x>", 3, 6, "<y><a></a></y><z></z>");
    testHelper("<x><y><a></a></y><z><b></b></z></x>", 3, 8, "<y><a></a></y><z><b></b></z>");
    testHelper("<x><y/></x>", 1, 4, "<x><y></y></x>");

    testHelper("hello<x><y/></x>", 0, 9, "hello<x><y></y></x>");
    testHelper("hello<x><y/></x>", 1, 9, "ello<x><y></y></x>");
    testHelper("hello<x><y>world</y></x>", 1, 7, "ello<x><y></y></x>");
    testHelper("hello<x><y>world</y></x>", 1, 8, "ello<x><y>w</y></x>");
    testHelper("hello<x><y>world</y></x>", 1, 9, "ello<x><y>wo</y></x>");
    testHelper("hello<x><y>world</y></x>", 1, 12, "ello<x><y>world</y></x>");

    testHelper("hello<x><y>world</y></x><b></b>", 1, 13, "ello<x><y>world</y></x>");
    testHelper("hello<x><y>world</y></x><b></b>", 3, 15, "lo<x><y>world</y></x><b></b>");
    testHelper("hello<x><y>world</y></x><b></b>", 5, 10, "<x><y>wor</y></x>");
    testHelper("hello<x><y>world</y></x><b></b>", 5, 15, "<x><y>world</y></x><b></b>");
    testHelper("hello<x><y>world</y></x><b></b>", 6, 15, "<x><y>world</y></x><b></b>");
    testHelper("hello<x><y>world</y></x><b></b>", 7, 15, "<x><y>world</y></x><b></b>");
    testHelper("hello<x><y>world</y></x><b></b>", 8, 15, "<x><y>orld</y></x><b></b>");
    testHelper("hello<x><y>world</y></x><b></b>", 12, 15, "<x><y></y></x><b></b>");
  }

  private void testHelper(String initialContent, int start, int end, String expectedContent) {
    IndexedDocument<Node, Element, Text> parse = DocProviders.POJO.parse(initialContent);
    SubTreeXmlRenderer<Node, Element, Text> renderer =
      new SubTreeXmlRenderer<Node, Element, Text>(parse);

    Element nearestCommonAncestor = parse.getDocumentElement();
    XmlStringBuilder rendered = renderer.renderRange(parse.locate(start), parse.locate(end));

    assertEquals(expectedContent, rendered.toString());
  }
}
