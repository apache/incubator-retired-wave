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

package org.waveprotocol.wave.model.conversation;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.List;

/**
 * Tests for {@link TagsDocument}.
 *
 */

public class TagsDocumentTest extends TestCase {

  /**
   * A simple test Listener which accumulates additions/removals of tags.
   */
  public static class TestListener implements TagsDocument.Listener {
    public List<String> addedTags = CollectionUtils.newArrayList();
    public List<Integer> removedTags = CollectionUtils.newArrayList();
    @Override
    public void onAdd(String tagName) {
      addedTags.add(tagName);
    }

    @Override
    public void onRemove(int tagPosition) {
      removedTags.add(tagPosition);
    }
  }

  private TestListener listener;
  private TagsDocument<?,?,?> doc;

  @Override
  protected void setUp() throws Exception {
    listener = new TestListener();
    doc = createDocument("");
    doc.addListener(listener);
  }

  private TagsDocument<?,?,?> createDocument(String xmlContent) {
    MutableDocumentImpl<Node, Element, Text> baseDocument = DocProviders.MOJO.parse(xmlContent);
    return new TagsDocument<Node, Element, Text>(baseDocument);
  }

  public void testAddTag() throws Exception {
    doc.addTag("new-tag");
    doc.processInitialState();
    assertTrue(listener.addedTags.contains("new-tag"));
  }

  public void testDeleteTag() throws Exception {
    doc.addTag("new-tag");
    doc.deleteTag(0);
    doc.processInitialState();
    assertTrue(listener.addedTags.isEmpty());
  }

  public void testDeleteTagByName() throws Exception {
    doc.addTag("new-tag");
    doc.deleteTag("new-tag");
    doc.processInitialState();
    assertTrue(listener.addedTags.isEmpty());
  }

  public void testAddTagPosition() throws Exception {
    doc.addTag("new-tag");
    doc.addTag("second-tag", 0);
    doc.processInitialState();
    assertEquals(CollectionUtils.newArrayList("second-tag", "new-tag"), listener.addedTags);
  }
}
