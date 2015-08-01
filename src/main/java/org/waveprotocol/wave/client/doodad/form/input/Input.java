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

package org.waveprotocol.wave.client.doodad.form.input;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.client.editor.content.misc.LinoTextEventHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.DefaultParagraphHtmlRenderer;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.content.paragraph.ParagraphRenderer;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

public class Input extends LinoTextEventHandler {
  public interface Resources extends ClientBundle {
    @Source("Input.css")
    Css css();
  }

  public interface Css extends CssResource {
    String input();
    String textarea();
  }

  public static final String INPUT_TAGNAME = "input";
  public static final String TEXTAREA_TAGNAME = "textarea";

  private static final String NAME = ContentElement.NAME;
  private static final String SUBMIT = ContentElement.SUBMIT;

  /** The singleton instance of our CSS resources. */
  private static final Css css = GWT.<Resources>create(Resources.class).css();

  /**
   * Registers subclass with ContentElement
   */
  public static void register(final ElementHandlerRegistry registry) {
    Editor.TAB_TARGETS.add(INPUT_TAGNAME);
    Editor.TAB_TARGETS.add(TEXTAREA_TAGNAME);

    // Also register text area.
    // TODO(danilatos): Do proper text area doodad, needs a non-paragraph
    // implementation as it does not contain text directly, it is more
    // akin to a top-level container element.
    Paragraph.register(INPUT_TAGNAME, registry);
    registry.registerEventHandler(INPUT_TAGNAME, new Input());
    registry.registerRenderer(INPUT_TAGNAME, new ParagraphRenderer(
        new DefaultParagraphHtmlRenderer() {
          @Override
          protected Element createNodelet(Renderable element) {
            Element e = DomHelper.setContentEditable(
                Document.get().createElement("span"), true, true);
            e.setClassName(css.input());
            return e;
          }
        }));

    LineRendering.registerContainer(TEXTAREA_TAGNAME, registry);
    registry.registerRenderer(TEXTAREA_TAGNAME, new Renderer() {

      @Override
      public Element createDomImpl(Renderable element) {
        Element e = DomHelper.setContentEditable(
            Document.get().createElement(TEXTAREA_TAGNAME), true, true);
        e.setClassName(css.textarea());
        return element.setAutoAppendContainer(e);
      }
    });
  }

  /**
   * Register schema + inject stylesheet
   */
  static {
    // For unit testing using mockito all Gwt.Create() returns mocks.
    // The mock for Resources.class returns null css by default.
    if (css != null) {
      StyleInjector.inject(css.getText(), true);
    }
  }

  /**
   * @param value
   * @param name
   * @return A content xml string containing an input field
   */
  public static XmlStringBuilder constructXml(XmlStringBuilder value, String name) {
    return value.wrap(INPUT_TAGNAME, NAME, name);
  }

  /**
   * @param value
   * @param name
   * @return A content xml string containing an input field
   */
  public static XmlStringBuilder constructXml(
        XmlStringBuilder value, String name, String submit) {
    return value.wrap(INPUT_TAGNAME, NAME, name, SUBMIT, submit);
  }

}
