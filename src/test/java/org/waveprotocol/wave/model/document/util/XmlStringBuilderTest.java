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

import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema.PermittedCharacters;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Test for {@link XmlStringBuilder}
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class XmlStringBuilderTest extends TestCase {

  XmlStringBuilder a = XmlStringBuilder.createEmpty();
  XmlStringBuilder b = XmlStringBuilder.createEmpty();
  XmlStringBuilder c = XmlStringBuilder.createEmpty();
  XmlStringBuilder d = XmlStringBuilder.createEmpty();

  public void testTextIsJustText() {
    checkText("", a);
    a.appendText("");
    checkText("", a);
    a.appendText("blah");
    checkText("blah", a);
  }

  public void testEscapesText() {
    String text = "the <p> & \n <q/> tags", expected = "the &lt;p&gt; &amp; \n &lt;q/&gt; tags";
    a.appendText(text, PermittedCharacters.ANY);
    check(text.length(), expected, a);

    text = "the <p> & <q/> tags";
    expected = "the &lt;p&gt; &amp; &lt;q/&gt; tags";
    check(text.length(), expected, XmlStringBuilder.createText(text));
  }

  public void testTextLengthIsUnescapedStringLength() {
    check(5, "hello", a.appendText("hello"));
    check(9, "hello&lt;p&gt;&amp;", a.appendText("<p>&"));
  }

  /**
   * test whether createFromXmlString works by converting some fragments to
   * and from an XmlStringBuilder. We also check that invalid text doesn't
   * work.
   */
  public void testCreateFromXmlString() {
    String xmlText = "<p>Hello world</p><p>How are things</p>";
    XmlStringBuilder builder = XmlStringBuilder.createFromXmlString(xmlText);
    assertEquals(xmlText, builder.toString());
  }

  public void testWrapMakesLengthOne() {
    checkLength(2, a.wrap("abc"));
    checkLength(6, a.appendText("hi").wrap("Yo", "x", "y"));
    checkLength(9, b.appendText("<h&ello").wrap("xx"));
  }

  public void testLengthIsCumulative() {
    checkLength(9, a.appendText("blah").wrap("ab").appendText("abc"));
  }

  public void testWrapEscapesAttributes() {
    check(4, "<ab cd=\"e&apos;f&lt;\" gh=\"&amp;i&quot;&gt;j\" xy=\"zz\">kl</ab>",
        a.appendText("kl").wrap("ab", "cd", "e'f<", "gh", "&i\">j", "xy", "zz"));
  }

  // TODO(danilatos): Sort keys, or don't compare just strings
  public void testWrapWithAttributeMap() {
    StringMap<String> attribs = CollectionUtils.createStringMap();
    attribs.put("t", "li");
    attribs.put("i", "2");
    check(2, "<line t=\"li\" i=\"2\"></line>",
        XmlStringBuilder.createEmpty().wrap("line", attribs));
  }

  // TODO(danilatos): Sort keys, or don't compare just strings
  public void testWrapWithAttributeMapWithEmptyValues() {
    StringMap<String> attribs = CollectionUtils.createStringMap();
    attribs.put("a", "1");
    attribs.put("b", "");
    check(2, "<x b=\"\" a=\"1\"></x>",
        XmlStringBuilder.createEmpty().wrap("x", attribs));

    attribs = CollectionUtils.createStringMap();
    attribs.put("a", "");
    attribs.put("b", "");
    check(2, "<x b=\"\" a=\"\"></x>",
        XmlStringBuilder.createEmpty().wrap("x", attribs));

    attribs = CollectionUtils.createStringMap();
    attribs.put("a", "");
    attribs.put("b", "2");
    attribs.put("c", "");
    check(2, "<x b=\"2\" c=\"\" a=\"\"></x>",
        XmlStringBuilder.createEmpty().wrap("x", attribs));
  }

  public void testConstructsFromNodes() {
    checkConstruction("some text");
    checkConstruction("some text &gt; &amp; &lt;");
    checkConstruction("<p>blah</p>");
    checkConstruction("<p a=\"b\" c=\"d\">blah</p>");
    checkConstruction("<p a=\"b\" c=\"d&apos;e\" f=\"&gt;g\">blah</p>");
  }

  public void testEqualityCorrespondsToXmlBeingBuilt() {

    assertEquals(a, b);

    a.appendText("hello");
    b.appendText("hello");

    assertEquals(a, b);

    assertTrue(!a.appendText("x").equals(b));
    b.appendText("x");

    c.appendText("hi").wrap("x");
    d.appendText("hi").wrap("x");

    assertEquals(c, d);
    assertTrue(!c.wrap("y").equals(d));
    d.wrap("y");

    a.append(c);
    assertTrue(!a.equals(b));
    b.append(d);

    assertEquals(a, b);

    assertEquals(a.wrap("blah"), b.wrap("blah"));

    // TODO(danilatos): Test other methods
  }

  protected void checkText(String expected, XmlStringBuilder xml) {
    check(expected.length(), expected, xml);
  }

  protected void check(int length, String expected, XmlStringBuilder xml) {
    checkLength(length, xml);
    DocCompare.equivalent(DocCompare.STRUCTURE, expected, xml.getXmlString());
  }

  protected void checkLength(int length, XmlStringBuilder xml) {
    assertEquals(length, xml.getLength());
  }

  protected void checkConstruction(String xml) {
    try {
      String outerXml = "<DOC a=\"b\">" + xml + "</DOC>";

      RawDocumentImpl doc = RawDocumentImpl.PROVIDER.parse(outerXml);
      Element element = doc.getDocumentElement();

      int size = element.calculateSize();

      check(size - 2, xml, XmlStringBuilder.createChildren(doc, element));
      check(size, outerXml, XmlStringBuilder.createNode(doc, element));

    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }
}
