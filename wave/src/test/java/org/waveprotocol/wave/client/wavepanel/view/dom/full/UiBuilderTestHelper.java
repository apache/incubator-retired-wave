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

package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import org.waveprotocol.wave.client.wavepanel.view.dom.full.i18n.ReplyBoxMessages;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.i18n.BlipMessages;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import com.google.gwt.resources.client.CssResource;

import org.apache.xerces.jaxp.DocumentBuilderFactoryImpl;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Document;
import org.waveprotocol.wave.client.common.safehtml.SafeHtml;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.uibuilder.BuilderHelper.Component;
import org.waveprotocol.wave.client.uibuilder.HtmlClosure;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Utility function for testing ui builder implementation.
 *
 */
public class UiBuilderTestHelper {

  // Re-use DocumentBuilderFactory across tests, because it's expensive?
  private static final DocumentBuilderFactory factory = new DocumentBuilderFactoryImpl();

  /** Error handler that treats errors and fatals as failure. */
  private static final class ErrorHandler extends DefaultHandler {
    private final List<SAXParseException> failures = new ArrayList<SAXParseException>();

    @Override
    public void error(SAXParseException e) {
      failures.add(e);
    }

    @Override
    public void fatalError(SAXParseException e) {
      failures.add(e);
    }

    void throwIfErrors() throws SAXException {
      if (!failures.isEmpty()) {
        throw new SAXException(buildExceptionMessage());
      }
    }

    private String buildExceptionMessage() {
      StringBuilder sb = new StringBuilder();
      for (SAXParseException e : failures) {
        sb.append(e.toString()).append('\n');
      }
      return sb.toString();
    }
  }

  private UiBuilderTestHelper() {
  }

  /**
   * Returns a DOM object representing parsed xml.
   *
   * @param xml The xml to parse
   * @return Parsed XML document
   */
  private static Document parse(String xml)
      throws IOException, ParserConfigurationException, SAXException {
    ErrorHandler errors = new ErrorHandler();
    try {
      synchronized (factory) {
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(errors);
        return builder.parse(new InputSource(new StringReader(xml)));
      }
    } catch (SAXException se) {
      // Prefer parse errors over general errors.
      errors.throwIfErrors();
      throw se;
    }
  }

  /**
   * Renders an HTML closure to HTML.
   */
  public static String render(HtmlClosure html) {
    SafeHtmlBuilder output = new SafeHtmlBuilder();
    html.outputHtml(output);
    return output.toSafeHtml().asString();
  }

  /**
   * Verifies that an HTML closure produces well formed HTML, and that it
   * includes all its components.
   *
   * @param builder builder to test
   * @param id DOM id for the builder
   * @param components components of the builder
   */
  public static void verifyHtml(UiBuilder builder, String id, Component [] components)
      throws IOException, ParserConfigurationException, SAXException {
    String html = render(builder);

    // make sure the html is wellformed
    Document dom = parse("<xml>" + html + "</xml>");
    assertFalse("It is not valid html to have '/>'", html.contains("/>"));

    String[] domIds = new String[components.length];

    for (int i = 0; i < components.length; i++) {
      domIds[i] = components[i].getDomId(id);
    }

    // make sure all the expected elements are there exactly once
    assertDomContainsUniqueIds(html, dom, domIds);
  }

  /**
   * @param str
   * @param pattern
   * @return the
   */
  public static int countOccurrences(String str, String pattern) {
    int num = 0;
    int index = -1;
    do {
      index = str.indexOf(pattern, index + 1);
      if (index >= 0) {
        num++;
      }
    } while (index >= 0);
    return num;
  }

  private static void assertDomContainsUniqueIds(String html, Document document, String[] ids) {
    Set<String> idsExamine = new HashSet<String>();
    for (String id : ids) {
      if (idsExamine.contains(id)) {
        fail("Duplicated ids");
      }
      idsExamine.add(id);

      // for each id, it must be unique in the string and has an element with
      // the given id.
      assertEquals(
          "Cannot find id " + id, 1, UiBuilderTestHelper.countOccurrences(html, "id='" + id + "'"));
    }
  }

  /**
   * Convert a string to safehtml string, only used for testing.
   *
   * @param str
   */
  public static SafeHtml toSafeHtml(String str) {
    return new SafeHtmlBuilder().appendPlainText(str).toSafeHtml();
  }

  /** @return a mock CSS resouces */
  public static <T extends CssResource> T mockCss(Class<T> cssClass) {
    return Mockito.mock(cssClass, Mockito.withSettings().defaultAnswer( // \u2620
    new Answer<String>() {
      int i;

      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return "asdf" + i++;
      }
    }));
  }

  /** @return a mock constants */
  public static <T extends BlipMessages> T mockBlipMessages(Class<T> constantsClass) {
    return Mockito.mock(constantsClass, Mockito.withSettings().defaultAnswer( // \u2620
    new Answer<String>() {
      int i;

      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return "asdf" + i++;
      }
    }));
  }

  /** @return a mock constants */
  public static <T extends ReplyBoxMessages> T mockReplyBoxMessages(Class<T> constantsClass) {
    return Mockito.mock(constantsClass, Mockito.withSettings().defaultAnswer( // \u2620
    new Answer<String>() {
      int i;

      @Override
      public String answer(InvocationOnMock invocation) throws Throwable {
        return "asdf" + i++;
      }
    }));
  }
}
