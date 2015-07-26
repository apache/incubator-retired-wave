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
import static org.mockito.Mockito.when;

import com.google.wave.api.Attachment;
import com.google.wave.api.Element;
import com.google.wave.api.FormElement;
import com.google.wave.api.Image;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.Map;

/**
 * Unit tests {@link ElementSerializer}.
 *
 */

public class ElementSerializerTest extends TestCase {

  private static final String ATTACHMENT_MIMETYPE = "image/jpeg";
  private static final String ATTACHMENT_URL =
    "/attachment/CIMG0217.JPG?id=xJFILMDj3&key=AH0qf5zWBJNcPtb7BUmo3YGrLJ0EVojhjQ";

  @Override
  public void setUp() {
    Blips.init();
  }

  public void testLabelSerialization() {
    convertBackAndForth("<label for=\"something\">label</label>");
  }

  public void testInputSerialization() {
    convertBackAndForth("<input name=\"inputbox\">value</input>");
  }

  public void testPasswordSerialization() {
    convertBackAndForth("<password name=\"lbl\" value=\"something\"></password>");
  }

  public void testTextareaSerialization() {
    convertBackAndForth(
        "<textarea name=\"lbl\"><line></line>line 1<line></line>line 2</textarea>");
  }

  public void testButtonSerialization() {
    convertBackAndForth(
        "<button name=\"button\"><caption>button</caption><events></events></button>");
  }

  public void testRadiogroupSerialization() {
    convertBackAndForth("<radiogroup name=\"rgroup\"></radiogroup>");
  }

  public void testRadioSerialization() {
    convertBackAndForth("<radio name=\"radio\" group=\"group\"></radio>");
  }

  public void testCheckboxSerialization() {
    String xml = "<check name=\"include\" submit=\"true\" value=\"false\"></check>";
    FormElement element = (FormElement) createApiElementFromXml(xml);
    assertEquals("include", element.getName());
    assertEquals("false", element.getValue());
    assertEquals("true", element.getDefaultValue());
    assertEquals(xml, ElementSerializer.apiElementToXml(element).getXmlString());
  }

  public void testGadgetSerialization() throws Exception {
    convertBackAndForth("<gadget url=\"http://www.example.com/gadget.xml\"></gadget>");
    convertBackAndForth("<gadget url=\"http://www.example.com/gadget.xml\">"
        + "<pref value=\"value\"></pref>" + "</gadget>");
    convertBackAndForth("<gadget url=\"http://www.example.com/gadget.xml\">"
        + "<state name=\"key\" value=\"value\"></state>" + "</gadget>");
  }

  public void testImgSerialization() {
    convertBackAndForth("<img src=\"http://www.example.com/image.png\"></img>");
    convertBackAndForth(
        "<img src=\"http://www.example.com/image.png\" width=\"100\" height=\"20\"></img>");
    Element element =
        createApiElementFromXml("<img src=\"http://www.example.com/image.png\"></img>");
    assertTrue(element instanceof Image);
    assertEquals("http://www.example.com/image.png", ((Image) element).getUrl());
  }

  public void testAttachmentSerialization() {
    convertBackAndForth("<image attachment=\"id\"></image>");
    convertBackAndForth("<image attachment=\"id\"><caption>caption</caption></image>");
    Element element = createApiElementFromXml(
        "<image attachment=\"id\"><gadge>something</gadge>" +
            "<caption>caption</caption><fake>fake</fake></image>", createWavelet("id"));
    assertTrue(element instanceof Attachment);
    assertEquals("caption", ((Attachment) element).getCaption());
    assertEquals("id", ((Attachment) element).getAttachmentId());
    assertEquals(ATTACHMENT_MIMETYPE, ((Attachment) element).getMimeType());
    assertEquals(ATTACHMENT_URL, ((Attachment) element).getAttachmentUrl());
  }

  public void testLineSerialization() {
    convertBackAndForth("<line i=\"2\" d=\"r\"></line>");
  }

  /**
   * Test that the passed xml string deserializes into exactly two elements and
   * that the second element is the one we're after and serialized back to the
   * passed xml.
   */
  private void convertBackAndForth(String xml) {
    Element element = createApiElementFromXml(xml);
    String resultXml = ElementSerializer.apiElementToXml(element).getXmlString();
    assertEquals(xml, resultXml);
  }

  private static Element createApiElementFromXml(String xml, Wavelet wavelet) {
    Document document = BasicFactories.documentProvider().parse(
        LineContainers.debugContainerWrap(xml));
    Map<Integer, Element> elements = ElementSerializer.serialize(document, wavelet);
    assertEquals(2, elements.size());
    return elements.get(1);
  }

  private static Element createApiElementFromXml(String xml) {
    return createApiElementFromXml(xml, mock(Wavelet.class));
  }

  private static Wavelet createWavelet(String attachmentId) {
    String xmlString =
        "<node key=\"download_token\" value=\"rk_S_RuHB01g\"></node>" +
        "<node key=\"image_height\" value=\"2736\"></node>" +
        "<node key=\"image_width\" value=\"3648\"></node>" +
        "<node key=\"thumbnail_height\" value=\"90\"></node>" +
        "<node key=\"thumbnail_width\" value=\"120\"></node>" +
        "<node key=\"mime_type\" value=\"" + ATTACHMENT_MIMETYPE + "\"></node>" +
        "<node key=\"filename\" value=\"CIMG0217.JPG\"></node>" +
        "<node key=\"thumbnail_url\" value=\"/thumbnail/CIMG0217_thumb.jpg?id=xJFILMDj3&amp;" +
            "key=AH0qf5woLURO3-CemfDebYFaWA9fD3PMuA\"></node>" +
        "<node key=\"attachment_url\" value=\"" + ATTACHMENT_URL + "\"></node>";
    ObservableDocument document = mock(ObservableDocument.class);
    when(document.toXmlString()).thenReturn(xmlString);

    Wavelet wavelet = mock(Wavelet.class);
    when(wavelet.getDocument("attach+" + attachmentId)).thenReturn(document);
    return wavelet;
  }
}
