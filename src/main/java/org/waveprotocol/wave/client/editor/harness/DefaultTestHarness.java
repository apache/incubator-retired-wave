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

package org.waveprotocol.wave.client.editor.harness;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RootPanel;

import org.waveprotocol.wave.client.doodad.attachment.ImageThumbnail;
import org.waveprotocol.wave.client.doodad.attachment.ImageThumbnail.ThumbnailActionHandler;
import org.waveprotocol.wave.client.doodad.attachment.render.ImageThumbnailWrapper;
import org.waveprotocol.wave.client.doodad.attachment.testing.FakeAttachmentsManager;
import org.waveprotocol.wave.client.doodad.diff.DiffDeleteRenderer;
import org.waveprotocol.wave.client.doodad.form.FormDoodads;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.doodad.suggestion.Suggestion;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.testtools.TestConstants;

import java.util.Map;

/**
 * An EntryPoint class for the Editor Test Harness
 *
 * Contains a small test harness for signals as well
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class DefaultTestHarness implements EntryPoint {

  /**
   * {@inheritDoc}
   */
  public void onModuleLoad() {
    final EditorHarness editorHarness = new EditorHarness() {
      @Override
      public void extend(Registries registries) {

        FormDoodads.register(registries.getElementHandlerRegistry());

        // We'll need an attachment manager
        FakeAttachmentsManager attachmentManager = new FakeAttachmentsManager();
        // Create a few attachments
        attachmentManager.createFakeAttachment("pics/Snow.jpg", 120, 80);
        attachmentManager.createFakeAttachment("pics/yosemite.jpg", 120, 80);
        attachmentManager.createFakeAttachment("pics/hills.jpg", 120, 74);
        attachmentManager.createFakeAttachment("pics/wave.gif", 120, 74);

        ImageThumbnail.register(registries.getElementHandlerRegistry(), attachmentManager,
            new ThumbnailActionHandler() {
          @Override
          public boolean onClick(ImageThumbnailWrapper thumbnail) {
            ContentElement e = thumbnail.getElement();
            String newId = Window.prompt("New attachment id, or 'remove' to remove the attribute",
                e.getAttribute(ImageThumbnail.ATTACHMENT_ATTR));

            if (newId == null) {
              // They hit escape
              return true;
            }

            if ("remove".equals(newId)) {
              newId = null;
            }

            e.getMutableDoc().setElementAttribute(e, ImageThumbnail.ATTACHMENT_ATTR, newId);
            return true;
          }
        });


        LinkAnnotationHandler.register(registries,
            new LinkAttributeAugmenter() {
              @Override
              public Map<String, String> augment(Map<String, Object> annotations, boolean isEditing,
                  Map<String, String> current) {
                return current;
              }
            });
        Suggestion.register(registries.getElementHandlerRegistry());
        DiffDeleteRenderer.register(registries.getElementHandlerRegistry());

      }

      @Override
      public String[] extendSampleContent() {
        String hills = "pics/hills.jpg";
        String yosemite = "pics/yosemite.jpg";

        return new String[] {
            "abcd" +
            ImageThumbnail.constructXml(yosemite, "Yosemite").getXmlString() +
            ImageThumbnail.constructXml(hills, "Hills").getXmlString() +
            ImageThumbnail.constructXml(hills, true, "Hills").getXmlString(),

            ImageThumbnail.constructXml(yosemite, "Yosemite").getXmlString() +
            ImageThumbnail.constructXml(hills, "Hills").getXmlString() +
            "<line t=\"li\"/>Some stuff"
        };
      }
    };
    final SignalEventHarness eventHarness = new SignalEventHarness();

    // Poor man's tab panel. Use a tab panel once OOPHM is fixed to not break with it,
    // in the same way as the oracle box.
    FlowPanel topLevel = new FlowPanel();
    final ListBox selector = new ListBox();
    selector.addItem("edit");
    selector.addItem("event");
    selector.getElement().setId(TestConstants.PAGE_MODE_SELECTOR);
    selector.addChangeHandler(new ChangeHandler() {
      public void onChange(ChangeEvent e) {
        if (selector.isItemSelected(0)) {
          editorHarness.getElement().getStyle().setDisplay(Display.BLOCK);
          eventHarness.getElement().getStyle().setDisplay(Display.NONE);
        } else {
          editorHarness.getElement().getStyle().setDisplay(Display.NONE);
          eventHarness.getElement().getStyle().setDisplay(Display.BLOCK);
        }
      }
    });
    topLevel.add(selector);
    topLevel.add(editorHarness);
    topLevel.add(eventHarness);

    RootPanel.get().add(topLevel);

    editorHarness.getEditor1().focus(true);
  }
}
