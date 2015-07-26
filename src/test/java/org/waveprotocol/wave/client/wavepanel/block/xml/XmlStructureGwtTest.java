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

package org.waveprotocol.wave.client.wavepanel.block.xml;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;

import org.waveprotocol.wave.client.wavepanel.block.BlockStructureTestBase;
import org.waveprotocol.wave.client.wavepanel.view.ModelIdMapperImpl;
import org.waveprotocol.wave.client.wavepanel.view.ViewIdMapper;
import org.waveprotocol.wave.model.conversation.ConversationView;

/**
 * Wraps an {@link BlockStructureTestBase}, and runs it against an
 * {@link XmlStructure}.
 *
 */

public class XmlStructureGwtTest extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.wavepanel.block.Tests";
  }

  /**
   * Creates an xml block structure from a model, and injects it into the page.
   */
  private XmlStructure createXml(ConversationView model) {
    String xml =
        XmlRenderer.render(new ViewIdMapper(ModelIdMapperImpl.create(model, "empty")), model);
    Element dom = Document.get().createElement("xml");
    dom.setInnerHTML(xml);
    Document.get().getBody().appendChild(dom);
    return XmlStructure.create(XmlRenderer.ROOT_ID);
  }

 /** @return a block test on a block tree that is attached to the page. */
  private BlockStructureTestBase getAttachedTest() {
    BlockStructureTestBase x = new BlockStructureTestBase() {
      @Override
      protected XmlStructure create(ConversationView model) {
        return createXml(model);
      }
    };
    x.setUp();
    return x;
  }

  /** @return a block test on a block tree that is detached from the page. */
  private BlockStructureTestBase getTest() {
    BlockStructureTestBase x = new BlockStructureTestBase() {
      @Override
      protected XmlStructure create(ConversationView model) {
        XmlStructure x = createXml(model);
        x.detach();
        return x;
      }
    };
    x.setUp();
    return x;
  }

  //
  // Forwarding methods for each test method on the base test.
  //

  public void testIdMapping() {
    getAttachedTest().testIdMapping();
  }

  public void testRootHasNoParent() {
    getTest().testRootHasNoParent();
  }

  public void testNodeStructureReflectsRendering() {
    getTest().testNodeStructureReflectsRendering();
  }

  public void testStructureIsSelfConsistent() {
    getTest().testStructureIsSelfConsistent();
  }
}
