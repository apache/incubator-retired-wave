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

package org.waveprotocol.wave.client.editor.examples.img;

import com.google.gwt.core.client.EntryPoint;

import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.harness.EditorHarness;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas.DefaultDocumentSchema;

/**
 * Test entry point module for an example doodad
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class TestModule implements EntryPoint {

  @Override
  public void onModuleLoad() {
    final EditorHarness harness = new EditorHarness() {

      @Override
      public void extend(Registries registries) {
        MyDoodad.register(registries.getElementHandlerRegistry());
      }

      @Override
      public String[] extendSampleContent() {
        return new String[] {
          "<mydoodad ref='pics/wave.gif'/>",
          "<mydoodad ref='pics/yosemite-sm.jpg'/>",
          "<mydoodad ref='pics/hills-sm.jpg'><mycaption>Howdy</mycaption></mydoodad>",
        };
      }

      /**
       * Extend the schema with our experimental new doodad.
       *
       * Note that this is only necessary for new element types that are not
       * already in the main document schema.
       */
      @Override
      public DocumentSchema getSchema() {
        return new DefaultDocumentSchema() {
          {
            // Permit our doodad to appear inside a <body> element
            addChildren("body", MyDoodad.TAGNAME);

            // Permit a 'ref' attribute on the <mydoodad> element.
            // e.g. permit content like <mydoodad ref='pics/wave.gif'/>
            addAttrs(MyDoodad.TAGNAME, MyDoodad.REF_ATTR);

            // Permit our caption element to appear inside our doodad's main
            // element, e.g.
            // <mydoodad>
            //   <mycaption>text permitted here</mycaption>
            // </mydoodad>
            addChildren(MyDoodad.TAGNAME, MyDoodad.CAPTION_TAGNAME);
            containsBlipText(MyDoodad.CAPTION_TAGNAME);
          }
        };
      }
    };

    harness.run();
  }
}
