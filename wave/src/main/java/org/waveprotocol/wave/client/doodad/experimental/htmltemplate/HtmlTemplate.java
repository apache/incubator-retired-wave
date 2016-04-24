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

package org.waveprotocol.wave.client.doodad.experimental.htmltemplate;

import static org.waveprotocol.wave.model.gadget.GadgetConstants.URL_ATTRIBUTE;

import com.google.gwt.user.client.ui.HTML;

import org.waveprotocol.wave.client.doodad.DoodadInstallers.GlobalInstaller;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.ChunkyElementHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.util.EditorDocHelper;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * Entry point for the HTML template doodad support. HTML template doodads are
 * created by adding an {@code <template>} tag to Wave XML. An example of
 * the XML would be:
 *
 * <p>
 *
 * <pre>
 *   <template url="http://example.com/stickynotes.html">
 *     <namevaluepair name="geometry_note0" value="37,24,142,97"/>
 *     <namevaluepair name="geometry_note1" value="68,59,95,66"/>
 *     <namevaluepair name="zorder" value="note_1,note_0"/>
 *     <namevaluepair name="borderstyle" value="thick"/>
 *     <part id="note_0">
 *       Remember to call Alice at 555-1212.
 *     </part>
 *     <part id="note_1">
 *       Shopping list: milk and eggs.
 *     </part>
 *   </template>
 * </pre>
 *
 * <p>
 * This XML would be for a hypothetical application that displays a bunch of
 * "sticky notes" in a wave, layed out in some idiomatic fashion. We have:
 *
 * <p>
 * * An {@code <template>} tag with a {@code url} attribute pointing to the
 * location of an HTML file that contains markup and JavaScript code
 * implementing the application.
 *
 * <p>
 * * A number of {@code <namevaluepair>} tags managed by the application to keep
 * track of its own internal state.
 *
 * <p>
 * * A number of {@code <part>} tags created by the application, each of which
 * contains arbitrary Wave content. (Only simple text is shown here for
 * clarity.)
 *
 * <p>
 * This application has chosen to create a {@code <part>} for each sticky note,
 * and to use {@code <namevaluepair>}s to remember the geometry of the layout.
 *
 * @author ihab@google.com (Ihab Awad)
 */
public final class HtmlTemplate {

  public static final String TEMPLATE_TAG = "template";
  public static final String LINE_TAG = "line";
  public static final String NAMEVALUEPAIR_TAG = "namevaluepair";
  public static final String PART_TAG = "part";

  public static final String TEMPLATE_URL_ATTR = "url";
  public static final String NAMEVALUEPAIR_NAME_ATTR = "name";
  public static final String NAMEVALUEPAIR_VALUE_ATTR = "value";
  public static final String PART_ID_ATTR = "id";

  /**
   * A binding from an <template> content element to the top-level viewer
   * widget.
   */
  public static final Property<HTML> TEMPLATE_WIDGET =
      Property.<HTML> immutable("template_widget");

  /**
   * A binding from an <template> content element to the plugin context that
   * permits communication with the third-part code.
   */
  public static final Property<PluginContext> TEMPLATE_PLUGIN_CONTEXT =
      Property.<PluginContext> immutable("template_plugin_context_impl");

  public static GlobalInstaller installer() {
    return new GlobalInstaller() {
      @Override
      public void install(Registries r) {
        ElementHandlerRegistry handlers = r.getElementHandlerRegistry();
        RenderingMutationHandler multiHandler = TemplateNodeMutationHandler.create();

        LineRendering.registerContainer(PART_TAG, handlers);
        handlers.registerRenderingMutationHandler(TEMPLATE_TAG, multiHandler);
        handlers.registerMutationHandler(NAMEVALUEPAIR_TAG, new NameValuePairNodeMutationHandler());
        handlers.registerEventHandler(TEMPLATE_TAG, ChunkyElementHandler.INSTANCE);
        handlers.registerEventHandler(PART_TAG, LineRendering.DEFAULT_PARAGRAPH_EVENT_HANDLER);
      }
    };
  }

  public static boolean isPartElement(ContentNode node) {
    return EditorDocHelper.isNamedElement(node, PART_TAG);
  }

  public static boolean isNameValuePairElement(ContentNode node) {
    return EditorDocHelper.isNamedElement(node, NAMEVALUEPAIR_TAG);
  }

  public static XmlStringBuilder createXml(String url) {
    XmlStringBuilder builder = XmlStringBuilder.createEmpty();
    builder.wrap(TEMPLATE_TAG, URL_ATTRIBUTE, url);
    return builder;
  }

  private HtmlTemplate() {
  }
}
