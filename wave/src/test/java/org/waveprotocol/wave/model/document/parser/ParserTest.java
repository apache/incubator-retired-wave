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

package org.waveprotocol.wave.model.document.parser;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Tests for parser package
 *
 */
public class ParserTest extends TestCase {
  public void testParseCharData() {
    {
      try {
        StreamingXmlParser parser = new StreamingXmlParser("abcde");
        Item text = parser.getTextChunk();
        assertEquals("abcde", text.data);
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    {
      try {
        StreamingXmlParser parser = new StreamingXmlParser("ab^cde<");
        Item text = parser.getTextChunk();
        assertEquals("ab^cde", text.data);
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    // non decimal digits
    try {
      StreamingXmlParser parser = new StreamingXmlParser("&#abc;");
      Item text = parser.getTextChunk();
      fail("Exception should've been thrown");
    } catch (XmlParseException e) {
    }

    // Invalid codepoint
    try {
      StreamingXmlParser parser = new StreamingXmlParser("&#x20ffff;");
      Item text = parser.getTextChunk();
      fail("Exception should've been thrown");
    } catch (XmlParseException e) {
    }

    // Low surrogate followed by high surrogate
    try {
      StreamingXmlParser parser = new StreamingXmlParser("&#xdc00;&#xd800;");
      Item text = parser.getTextChunk();
      fail("Exception should've been thrown");
    } catch (XmlParseException e) {
    }
  }

  public void testParseAttribute() {
    {
      try {
        StreamingXmlParser parser = new StreamingXmlParser("abcm='hello'");
        Pair<String, String> attr = parser.attr();
        assertEquals("abcm", attr.first);
        assertEquals("hello", attr.second);
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    {
      try {
        StreamingXmlParser parser = new StreamingXmlParser("abcm='hello&amp;'");
        Pair<String, String> attr = parser.attr();
        assertEquals("abcm", attr.first);
        assertEquals("hello&", attr.second);
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    {
      try {
        StreamingXmlParser parser = new StreamingXmlParser("abcm='hello&#32;world'");
        Pair<String, String> attr = parser.attr();
        assertEquals("abcm", attr.first);
        assertEquals("hello world", attr.second);
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    {
      try {
        StreamingXmlParser parser = new StreamingXmlParser("abcm='hello&#32;world&gt;'");
        Pair<String, String> attr = parser.attr();
        assertEquals("abcm", attr.first);
        assertEquals("hello world>", attr.second);
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }
  }

  public void testParseRepeatedAttribute() {
    try {
      StreamingXmlParser parser = new StreamingXmlParser(" abc='def' hello=\"world\" ");
      StringMap<String> attrList = parser.attrList();
      assertEquals("def", attrList.get("abc"));
      assertEquals("world", attrList.get("hello"));
    } catch (XmlParseException e) {
      fail(e.getMessage());
    }
  }

  public void testParseElement() {
    {
      try {
        StreamingXmlParser parser = new StreamingXmlParser("<doc abc='def' hello=\"world\"/>");
        Item element = parser.startTag();
        assertEquals("doc", element.name);
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    {
      try {
        StreamingXmlParser parser = new StreamingXmlParser("<doc abc='def' hello=\"world\">asdf</doc>");
        Item start = parser.startTag();
        Item text = parser.getTextChunk();
        Item end = parser.endTag();
        assertEquals("doc", start.name);
        assertEquals("asdf", text.data);
        assertEquals("doc", end.name);
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }
  }

  public void testPI() {
    {
      try {
        StreamingXmlParser parser = new StreamingXmlParser("<?hello abcdef?>");
        Item pi;
        pi = parser.processingInstruction();
        assertEquals("hello", pi.name);
        assertEquals("abcdef", pi.data);
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    try {
      StreamingXmlParser parser = new StreamingXmlParser("<?ab    cd?>");
      Item pi;
      pi = parser.processingInstruction();
      assertEquals("ab", pi.name);
      assertEquals("cd", pi.data);
    } catch (XmlParseException e) {
      fail(e.getMessage());
    }

    {
      try {
        StreamingXmlParser parser = new StreamingXmlParser("<?x <??>");
        Item pi;
        pi = parser.processingInstruction();
        assertEquals("x", pi.name);
        assertEquals("<?", pi.data);
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    {
      try {
        StreamingXmlParser parser = new StreamingXmlParser("<?a <body><ab?>");
        Item pi;
        pi = parser.processingInstruction();
        assertEquals("a", pi.name);
        assertEquals("<body><ab", pi.data);
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    // test invalid start char for PITarget
    {
      try {
        StreamingXmlParser parser = new StreamingXmlParser("<?<doc> <bodyabc?>");
        Item pi = parser.processingInstruction();
        fail("should've thrown exception");
      } catch (XmlParseException e) {
      }
    }

    try {
      StreamingXmlParser parser = new StreamingXmlParser("<?abc?>");
      Item pi;
      pi = parser.processingInstruction();
      assertEquals("abc", pi.name);
      assertEquals("", pi.data);
    } catch (XmlParseException e) {
      fail(e.getMessage());
    }

    try {
      StreamingXmlParser parser = new StreamingXmlParser("<?abc ?>");
      Item pi;
      pi = parser.processingInstruction();
      assertEquals("abc", pi.name);
      assertEquals("", pi.data);
    } catch (XmlParseException e) {
      fail(e.getMessage());
    }
  }

  public void testGetItem() {
    {
      try {
        StreamingXmlParser parser =
        new StreamingXmlParser("<doc abc='def' hello=\"world\"><x></x>asdf<z></z></doc>");
        Item item;

        item = parser.getItem();
        assertTrue(item.type == ItemType.START_ELEMENT);
        assertEquals("doc", item.name);

        item = parser.getItem();
        assertTrue(item.type == ItemType.START_ELEMENT);
        assertEquals("x", item.name);

        item = parser.getItem();
        assertTrue(item.type == ItemType.END_ELEMENT);
        assertEquals("x", item.name);

        item = parser.getItem();
        assertTrue(item.type == ItemType.TEXT);
        assertEquals("asdf", item.data);

        item = parser.getItem();
        assertTrue(item.type == ItemType.START_ELEMENT);
        assertEquals("z", item.name);

        item = parser.getItem();
        assertTrue(item.type == ItemType.END_ELEMENT);
        assertEquals("z", item.name);


        item = parser.getItem();
        assertTrue(item.type == ItemType.END_ELEMENT);
        assertEquals("doc", item.name);
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }
  }

  public void testUTF16Surrogate() {
    // Test valid surrogate
    {
      try {
        XmlPullParser parser = new StreamingXmlParser("<doc>\uD800\uDC00def</doc>");
        assertTrue(parser.next() == ItemType.START_ELEMENT);
        assertTrue(parser.next() == ItemType.TEXT);
        assertEquals("\uD800\uDC00def", parser.getText());
        assertTrue(parser.next() == ItemType.END_ELEMENT);
        assertFalse(parser.hasNext());
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    // Test invalid surrogate throws exception
    {
      try {
        XmlPullParser parser = new StreamingXmlParser("<doc>\uD800\uD800def</doc>");
        assertTrue(parser.next() == ItemType.START_ELEMENT);
        assertTrue(parser.next() == ItemType.TEXT);
        fail("Invalid surrogate should thrown parse exeption");
      } catch (XmlParseException e) {
        // good, exception thrown
      }
    }
  }

  public void testXmlPullParserInterface() {
    xmlPullParserInterfaceTestHelper(true);
    xmlPullParserInterfaceTestHelper(false);
  }

  public void xmlPullParserInterfaceTestHelper(boolean useBufferedParser) {
    {
      try {
        XmlPullParser parser =
          getParser("", useBufferedParser);
        assertFalse(parser.hasNext());
      } catch (XmlParseException e) {
        fail (e.getMessage());
      }

      try {
        XmlPullParser parser =
          getParser("<doc/>", useBufferedParser);
        assertTrue(parser.hasNext());
        assertTrue(parser.next() == ItemType.START_ELEMENT);
        assertTrue(parser.next() == ItemType.END_ELEMENT);
        assertTrue(!parser.hasNext());
      } catch (XmlParseException e) {
        fail (e.getMessage());
      }

      try {
        XmlPullParser parser =
          getParser("<doc abc='def' hello=\"world\"><x></x>asdf<z></z></doc>", useBufferedParser);
        assertTrue(parser.hasNext());
        assertEquals(ItemType.START_ELEMENT, parser.next());
        assertEquals(ItemType.START_ELEMENT, parser.next());
        assertEquals(ItemType.END_ELEMENT, parser.next());
        assertEquals(ItemType.TEXT, parser.next());

        assertEquals(ItemType.START_ELEMENT, parser.next());
        assertEquals(ItemType.END_ELEMENT, parser.next());
        assertEquals(ItemType.END_ELEMENT, parser.next());
        assertFalse(parser.hasNext());
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    {
      try {
        XmlPullParser parser =
          getParser("some text &gt; &amp; &lt;", useBufferedParser);
        assertTrue(parser.hasNext());
        assertEquals(ItemType.TEXT, parser.next());
        assertEquals("some text > & <", parser.getText());
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    {
      try {
        XmlPullParser parser =
            getParser(
                "<body><?a 'conv/title'='' 'lang'='unknown'?><line/>spelly test<?a 'lang'?></body>",
                useBufferedParser);
        assertTrue(parser.hasNext());
        assertEquals(ItemType.START_ELEMENT, parser.next());
        assertEquals("body", parser.getTagName());
        assertEquals(ItemType.PROCESSING_INSTRUCTION, parser.next());
        assertEquals("a", parser.getProcessingInstructionName());
        assertEquals("'conv/title'='' 'lang'='unknown'", parser.getProcessingInstructionValue());
        assertEquals(ItemType.START_ELEMENT, parser.next());
        assertEquals(ItemType.END_ELEMENT, parser.next());
        assertEquals(ItemType.TEXT, parser.next());
        assertEquals("spelly test", parser.getText());
        assertEquals(ItemType.PROCESSING_INSTRUCTION, parser.next());
        assertEquals("a", parser.getProcessingInstructionName());
        assertEquals("'lang'", parser.getProcessingInstructionValue());
        assertEquals(ItemType.END_ELEMENT, parser.next());
        assertEquals("body", parser.getTagName());
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }

    {
      try {
        XmlPullParser parser =
            getParser(
                "<body></body>hello<a/>",
                useBufferedParser);
        assertTrue(parser.hasNext());
        assertEquals(ItemType.START_ELEMENT, parser.next());
        assertEquals("body", parser.getTagName());
        assertEquals(ItemType.END_ELEMENT, parser.next());
        assertEquals("body", parser.getTagName());

        assertEquals(ItemType.TEXT, parser.next());
        assertEquals("hello", parser.getText());

        assertEquals(ItemType.START_ELEMENT, parser.next());
        assertEquals("a", parser.getTagName());
        assertEquals(ItemType.END_ELEMENT, parser.next());
        assertEquals("a", parser.getTagName());
      } catch (XmlParseException e) {
        fail(e.getMessage());
      }
    }
  }

  public void testIllFormedXml() {
    assertParserThrowsException("<? hello <doc>hello</doc>");
    assertParserThrowsException("<doc src='hi>hello</doc>");
    assertParserThrowsException("<doc hello </doc>");
    assertParserThrowsException("<doc> hello <\\doc>");
    assertParserThrowsException("<doc> hello");
    assertParserThrowsException("<a><b>hello</a></b>");
    assertParserThrowsException("<doc src='hi\">hello</doc>");
    assertParserThrowsException("<doc>hello<? ></doc>");
    assertParserThrowsException("<doc>hello<? foo></doc>");
  }

  public void testDocProviderParse() {
    String testStr =
        "<body><?a \"conv/title\"=\"\" \"lang\"=\"unknown\"?><line/>spelly test<?a \"conv/title\" \"lang\"?></body>";
    IndexedDocument<Node, Element, Text> parse = DocProviders.POJO.parse(testStr);
    assertEquals(testStr, parse.toXmlString());
  }

  private void assertParserThrowsException(String input) {
    try {
      XmlPullParser parser = getParser(input, false);
      while (parser.hasNext()) {
        parser.next();
      }
    } catch (XmlParseException e) {
      return;
    } catch (RuntimeXmlParseException e){
      return;
    }
    fail("Parsing ill-formed expression did not throw exception.");
  }

  private XmlPullParser getParser(String input, boolean buffered) throws XmlParseException {
    if (buffered) {
      return XmlParserFactory.buffered(input);
    } else {
      return XmlParserFactory.unbuffered(input);
    }
  }

//  public void testNewXmlPullParserSpeed() {
//    String s = "<doc abc='def' hello=\"world\"><x></x>asdf<z></z></doc>";
//    StringBuilder b = new StringBuilder();
//
//    for (int i = 0; i < 500000; ++i) {
//      b.append(s);
//    }
//
//    try {
//      StreamingXmlParser parser = new StreamingXmlParser(b.toString());
//      while (parser.hasNext()) {
//        parser.next();
//        parser.getCurrentItem();
//      }
//    } catch (XmlParseException e) {
//      fail(e.getMessage());
//    }
//  }
//
//  public void testOldXmlPullParserSpeed() {
//    String s = "<doc abc='def' hello=\"world\"><x></x>asdf<z></z></doc>";
//    StringBuilder b = new StringBuilder();
//
//    for (int i = 0; i < 50000; ++i) {
//      b.append(s);
//    }
//
//    XmlPullParser parser = new SimpleXmlParser(b.toString());
//    while (parser.hasNext()) {
//      parser.next();
//    }
//  }
}
