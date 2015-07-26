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

import static org.waveprotocol.wave.model.document.util.DocCompare.ATTRS;
import static org.waveprotocol.wave.model.document.util.DocCompare.ATTR_VALUES;
import static org.waveprotocol.wave.model.document.util.DocCompare.STRUCTURE;
import static org.waveprotocol.wave.model.document.util.DocCompare.TEXT;
import static org.waveprotocol.wave.model.document.util.DocCompare.TYPES;
import static org.waveprotocol.wave.model.document.util.DocCompare.equivalent;
import static org.waveprotocol.wave.model.document.util.DocCompare.structureEquivalent;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class DocCompareTest extends TestCase {

  public void testCompare() {
    same(STRUCTURE, "abc", "abc");
    diff(STRUCTURE, "abc", "def");
    same(STRUCTURE - TEXT, "abc", "def");
    diff(STRUCTURE - TEXT, "abc", "abcd");

    same(STRUCTURE - TYPES, "xyz<x>abc</x>def", "xyz<y>abc</y>def");
    same(STRUCTURE - TYPES - ATTRS, "xyz<x>abc</x>def", "xyz<y>abc</y>def");
    diff(STRUCTURE, "xyz<x>abc</x>def", "xyz<y>abc</y>def");
    same(STRUCTURE - TYPES - TEXT, "xyz<x>abc</x>def", "abc<y>xyz</y>def");
    diff(STRUCTURE - TYPES, "xyz<x>abc</x>def", "abc<y>xyz</y>def");

    same(STRUCTURE, "xyz<x a=\"b\">abc</x>def", "xyz<x a=\"b\">abc</x>def");
    diff(STRUCTURE, "xyz<x a=\"b\">abc</x>def", "xyz<x a=\"c\">abc</x>def");
    diff(STRUCTURE, "xyz<x a=\"b\">abc</x>def", "xyz<x a=\"b\" c=\"d\">abc</x>def");
    same(STRUCTURE - ATTR_VALUES, "xyz<x a=\"b\">abc</x>def", "xyz<x a=\"c\">abc</x>def");
    diff(STRUCTURE - ATTR_VALUES, "xyz<x a=\"b\">abc</x>def", "xyz<x a=\"b\" c=\"d\">abc</x>def");
    same(STRUCTURE - ATTRS, "xyz<x a=\"b\">abc</x>def", "xyz<x a=\"c\">abc</x>def");
    same(STRUCTURE - ATTRS, "xyz<x a=\"b\">abc</x>def", "xyz<x a=\"b\" c=\"d\">abc</x>def");
  }

  public void testAttributeOrder() {
    ReadableDocument<Node, Element, Text> doc1 = new IdentityView<Node, Element, Text>(
        DocProviders.POJO.parse(
            "<x a=\"1\" b=\"2\" c=\"3\" d=\"4\" e=\"5\" f=\"6\" g=\"7\"/>")) {
      @Override
      public Map<String, String> getAttributes(Element element) {
        return new TreeMap<String, String>(super.getAttributes(element));
      }
    };

    ReadableDocument<Node, Element, Text> doc2 = new IdentityView<Node, Element, Text>(doc1) {
      @Override
      public Map<String, String> getAttributes(Element element) {
        Map<String, String> attributes = super.getAttributes(element);
        Map<String, String> reversed = new LinkedHashMap<String, String>();
        List<String> keys = new ArrayList<String>(attributes.keySet());
        for (int i = keys.size() - 1; i >= 0; i--) {
          reversed.put(keys.get(i), attributes.get(keys.get(i)));
        }
        return reversed;
      }
    };

    assertTrue(structureEquivalent(STRUCTURE, doc1, doc2));
    assertTrue(structureEquivalent(STRUCTURE - ATTR_VALUES, doc1, doc2));
  }

  private void same(int flags, String a, String b) {
    assertTrue(equivalent(flags, a, DocProviders.POJO.parse(b)));
    assertTrue(equivalent(flags, DocProviders.POJO.parse(a), DocProviders.POJO.parse(b)));
  }

  private void diff(int flags, String a, String b) {
    assertFalse(equivalent(flags, a, DocProviders.POJO.parse(b)));
    assertFalse(equivalent(flags, DocProviders.POJO.parse(a), DocProviders.POJO.parse(b)));
  }
}
