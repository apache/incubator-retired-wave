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

package com.google.wave.api.data;

import static org.mockito.Mockito.mock;

import com.google.wave.api.Element;
import com.google.wave.api.ElementType;
import com.google.wave.api.FormElement;
import com.google.wave.api.Gadget;
import com.google.wave.api.Image;
import com.google.wave.api.data.ApiView.ElementInfo;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.List;

/**
 * Unit tests {@link ApiView}.
 *
 */

public class ApiViewTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Blips.init();
  }

  public void testStringAppend() {
    Document document =
        BasicFactories.documentProvider().parse(LineContainers.debugContainerWrap(""));
    ApiView api = new ApiView(document, mock(Wavelet.class));
    api.insert(1, "world");
    assertEquals("\nworld", api.apiContents());
    assertInSync(document, api);
    api.insert(1, "hello ");
    assertEquals("\nhello world", api.apiContents());
    assertInSync(document, api);
  }

  public void testStringReplace() {
    Document document =
        BasicFactories.documentProvider().parse(LineContainers.debugContainerWrap(""));
    ApiView api = new ApiView(document, mock(Wavelet.class));
    api.insert(1, "22 a b 22 c d 22 e f");
    while (true) {
      String contents = api.apiContents();
      int p = contents.indexOf("22");
      if (p < 0) {
        break;
      }
      api.insert(p, "q");
      assertInSync(document, api);
      api.delete(p + 1, p + 3);
      assertInSync(document, api);
    }
    assertEquals("\nq a b q c d q e f", api.apiContents());
    assertInSync(document, api);
  }

  public void testShift() {
    Document document =
        BasicFactories.documentProvider().parse(LineContainers.debugContainerWrap(""));
    ApiView api = new ApiView(document, mock(Wavelet.class));
    api.insert(1, "0123456789");
    api.insert(4, new Image("id1", "caption"));
    api.insert(8, new Image("id1", "caption"));
    assertInSync(document, api);
    api.delete(2, 3);
    List<ElementInfo> elements = api.getElements();
    assertEquals(3, elements.size());
    api.delete(elements.get(1).apiPosition, elements.get(0).apiPosition + 1);
    assertInSync(document, api);
    api.delete(api.getElements().get(1).apiPosition, elements.get(0).apiPosition + 1);
    assertInSync(document, api);
  }

  public void testComposeDocUsingAppend() {
    Document document =
        BasicFactories.documentProvider().parse(LineContainers.debugContainerWrap(""));
    ApiView api = new ApiView(document, mock(Wavelet.class));
    api.insert(1, "hello");
    api.insert(1, new Image("id", "caption"));
    assertEquals(2, api.getElements().size());
    assertEquals("\n hello", api.apiContents());
    assertInSync(document, api);
    api.insert(api.apiContents().length(), " world");
    assertEquals("\n hello world", api.apiContents());
    assertInSync(document, api);
    api.insert(api.apiContents().length(), new Image("id", "caption"));
    assertInSync(document, api);
    api.insert(api.apiContents().length(), "!");
    assertEquals("\n hello world !", api.apiContents());
    assertInSync(document, api);
  }

  public void testDeleteString() {
    Document document =
        BasicFactories.documentProvider().parse(LineContainers.debugContainerWrap(""));
    ApiView api = new ApiView(document, mock(Wavelet.class));
    api.insert(1, "helllo");
    api.delete(3, 4);
    assertEquals("\nhello", api.apiContents());
    assertInSync(document, api);
    api.delete(0, 2);
    assertEquals("ello", api.apiContents());
    assertInSync(document, api);
  }

  public void testInsertAndDeleteElementInString() {
    Document document =
        BasicFactories.documentProvider().parse(LineContainers.debugContainerWrap(""));
    ApiView api = new ApiView(document, mock(Wavelet.class));
    api.insert(1, "hello");
    api.insert(3, new Image("id", "caption"));
    assertEquals("\nhe llo", api.apiContents());
    assertInSync(document, api);
    api.delete(3, 5);
    assertEquals("\nhelo", api.apiContents());
    assertInSync(document, api);
  }

  public void testInsertTextWithNewline() {
    Document document =
        BasicFactories.documentProvider().parse(LineContainers.debugContainerWrap(""));
    ApiView api = new ApiView(document, mock(Wavelet.class));
    api.insert(1, "hello\nworld");
    assertEquals("\nhello\nworld", api.apiContents());
    assertInSync(document, api);
    List<ElementInfo> elements = api.getElements();
    assertEquals(2, elements.size());
    assertEquals(ElementType.LINE, elements.get(0).element.getType());
    assertEquals(ElementType.LINE, elements.get(1).element.getType());
  }

  public void testInsertBeforeElementThenDeleteElement() {
    Document document =
        BasicFactories.documentProvider().parse(LineContainers.debugContainerWrap(""));
    ApiView api = new ApiView(document, mock(Wavelet.class));
    api.insert(1, "0123456789");
    api.insert(4, new Image("id1", "caption"));
    api.insert(4, "4");
    assertInSync(document, api);
    api.delete(5, 6);
    assertEquals(1, api.getElements().size());
    assertEquals("\n01243456789", api.apiContents());
    assertInSync(document, api);
  }

  public void testTransformToXmlOffset() {
    Document document = BasicFactories.documentProvider().parse(
        LineContainers.debugContainerWrap("some text<gadget></gadget>"));
    ApiView api = new ApiView(document, mock(Wavelet.class));
    api.insert(3, new Image("id", "caption"));
    List<ElementInfo> apiElements = api.getElements();
    for (int i = 0; i < apiElements.size(); i++) {
      ElementInfo info = apiElements.get(i);
      assertEquals(info.xmlPosition, api.transformToXmlOffset(info.apiPosition));
    }
    assertInSync(document, api);
  }

  public void testLocateElement() {
    Document document = BasicFactories.documentProvider().parse(
        LineContainers.debugContainerWrap("01234567890123456789"));
    ApiView api = new ApiView(document, mock(Wavelet.class));
    api.insert(3, new FormElement(ElementType.BUTTON, "buttonName"));
    FormElement button1 = new FormElement(ElementType.BUTTON, "buttonName");
    assertEquals(3, api.locateElement(button1));

    FormElement button2 = new FormElement(ElementType.BUTTON, "notInDocument");
    assertEquals(-1, api.locateElement(button2));

    api.insert(4, new Gadget("http://test.com"));
    Gadget gadget1 = new Gadget("http://test.com");
    assertEquals(4, api.locateElement(gadget1));

    Gadget gadget2 = new Gadget("http://test.com/something");
    assertEquals(-1, api.locateElement(gadget2));
    assertInSync(document, api);

    Element inlineBlip = new Element(ElementType.INLINE_BLIP);
    inlineBlip.setProperty("id", "b+1234");
    api.insert(5, inlineBlip);
    assertEquals(5, api.locateElement(inlineBlip));
  }

  public void testTransformToTextOffset() {
    Document document = BasicFactories.documentProvider().parse(
        LineContainers.debugContainerWrap("123<gadget><state>foo</state></gadget>456"));
    ApiView api = new ApiView(document, mock(Wavelet.class));

    // Assert the text offsets of <body>, <line> and </line>.
    assertEquals(0, api.transformToTextOffset(0));
    assertEquals(0, api.transformToTextOffset(1));
    assertEquals(0, api.transformToTextOffset(2));

    // Assert the text offsets of 123.
    assertEquals(1, api.transformToTextOffset(3));
    assertEquals(2, api.transformToTextOffset(4));
    assertEquals(3, api.transformToTextOffset(5));

    // Assert the text offsets of <gadget><state>foo</state></gadget>.
    assertEquals(4, api.transformToTextOffset(6));
    assertEquals(4, api.transformToTextOffset(7));
    assertEquals(4, api.transformToTextOffset(8));
    assertEquals(4, api.transformToTextOffset(9));
    assertEquals(4, api.transformToTextOffset(10));
    assertEquals(4, api.transformToTextOffset(11));
    assertEquals(4, api.transformToTextOffset(12));

    // Assert the text offsets of 456.
    assertEquals(5, api.transformToTextOffset(13));
    assertEquals(6, api.transformToTextOffset(14));
    assertEquals(7, api.transformToTextOffset(15));

    // Assert the text offset of </body> and after </body>.
    assertEquals(8, api.transformToTextOffset(16));
    assertEquals(8, api.transformToTextOffset(17));
  }

  /**
   * Assert that the api view and document are still in sync.
   */
  private void assertInSync(Document document, ApiView api) {
    ApiView alt = new ApiView(document, mock(Wavelet.class));
    assertEquals(alt.apiContents(), api.apiContents());
    List<ElementInfo> apiElements = api.getElements();
    List<ElementInfo> altElements = alt.getElements();
    assertEquals(altElements.size(), apiElements.size());
    for (int i = 0; i < apiElements.size(); i++) {
      ElementInfo altInfo = altElements.get(i);
      ElementInfo apiInfo = apiElements.get(i);
      assertEquals(altInfo.element.getType(), apiInfo.element.getType());
      assertEquals(altInfo.apiPosition, apiInfo.apiPosition);
      assertEquals(altInfo.xmlPosition, apiInfo.xmlPosition);
    }
  }
}
