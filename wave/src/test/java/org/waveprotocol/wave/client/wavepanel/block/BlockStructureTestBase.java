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

package org.waveprotocol.wave.client.wavepanel.block;


import junit.framework.TestCase;

import org.waveprotocol.wave.client.render.ConversationRenderer;
import org.waveprotocol.wave.client.render.RendererHelper;
import org.waveprotocol.wave.client.wavepanel.block.BlockStructure.Node;
import org.waveprotocol.wave.client.wavepanel.block.BlockStructure.NodeType;
import org.waveprotocol.wave.client.wavepanel.view.ModelIdMapperImpl;
import org.waveprotocol.wave.client.wavepanel.view.ViewIdMapper;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.conversation.testing.FakeConversationView;

/**
 * Basic tests for a block structure.  Subclasses provude the structure to test.
 *
 */
public abstract class BlockStructureTestBase extends TestCase {
  private ConversationView model;
  private BlockStructure bs;
  private ViewIdMapper viewIdMapper;

  @Override
  public void setUp() {
    model = createSample();
    viewIdMapper = new ViewIdMapper(ModelIdMapperImpl.create(model, "empty"));
    bs = create(model);
  }

  protected abstract BlockStructure create(ConversationView model);

  private static ConversationView createSample() {
    ConversationView v = FakeConversationView.builder().build();
    Conversation c = v.createRoot();
    ConversationThread root = c.getRootThread();
    sampleReply(root.appendBlip());
    root.appendBlip();
    root.appendBlip();
    biggerSampleReply(root.appendBlip());
    root.appendBlip();
    root.appendBlip();
    biggestSampleReply(root.appendBlip());
    root.appendBlip();
    biggerSampleReply(root.appendBlip());
    sampleReply(root.appendBlip());
    return v;
  }

  private static void sampleReply(ConversationBlip blip) {
    ConversationThread thread = blip.addReplyThread();
    thread.appendBlip();
    thread.appendBlip();
  }

  private static void biggerSampleReply(ConversationBlip blip) {
    ConversationThread thread = blip.addReplyThread();
    sampleReply(thread.appendBlip());
    sampleReply(thread.appendBlip());
    thread.appendBlip();
  }

  private static void biggestSampleReply(ConversationBlip blip) {
    ConversationThread thread = blip.addReplyThread();
    biggerSampleReply(thread.appendBlip());
    biggerSampleReply(thread.appendBlip());
    thread.appendBlip();
    thread.appendBlip();
  }


  public void testRootHasNoParent() {
    assertNull(bs.getRoot().getParent());
  }

  public void testIdMapping() {
    traverse(bs.getRoot(), new Visitor() {
      @Override
      public void visit(Node n) {
        assertSame(n, bs.getNode(n.getId()));
      }
    });
  }

  public void testStructureIsSelfConsistent() {
    traverse(bs.getRoot(), new Visitor() {
      @Override
      public void visit(Node n) {
        assertFirstChildSymmetry(n);
        assertLastChildSymmetry(n);
        assertNextSiblingSymmetry(n);
        assertPreviousSiblingSymmetry(n);
      }

    });
  }

  private static void assertFirstChildSymmetry(Node n) {
    Node c = n.getFirstChild();
    if (c != null) {
      assertSame(c.getParent(), n);
    }
  }

  private static void assertLastChildSymmetry(Node n) {
    Node c = n.getLastChild();
    if (c != null) {
      assertSame(c.getParent(), n);
    }
  }

  private static void assertNextSiblingSymmetry(Node n) {
    Node s = n.getNextSibling();
    if (s != null) {
      assertSame(s.getPreviousSibling(), n);
    }
  }

  private static void assertPreviousSiblingSymmetry(Node n) {
    Node s = n.getPreviousSibling();
    if (s != null) {
      assertSame(s.getNextSibling(), n);
    }
  }

  /**
   * Tests commutative nature of rendering, which produces homomorphic
   * structure. In layman's terms, tests that the rendering of X is composed of
   * the renderings of the components of X.
   */
  public void testNodeStructureReflectsRendering() {
    ConversationRenderer.renderWith(new NodeChecker(bs.getRoot()), model);
  }

  //
  //
  //

  /** Node visitor. */
  protected interface Visitor {
    void visit(Node n);
  }

  /** Notifies a visitor of every node in a subtree. */
  protected static void traverse(Node n, Visitor v) {
    if (n != null) {
      v.visit(n);
    }
    for (Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
      traverse(c, v);
    }
  }

  /**
   * Verifies that a block structure corresponds to a conversation rendering.
   */
  private class NodeChecker implements RendererHelper {
    private Node current;

    public NodeChecker(Node start) {
      current = start;
    }

    private void next() {
      assertNotNull(current);
      Node next;
      if ((next = current.getFirstChild()) != null) {
        current = next;
      } else {
        while (current != null && (next = current.getNextSibling()) == null) {
          current = current.getParent();
        }
        current = next;
      }
    }

    private void expect(NodeType type, String id) {
      next();
      assertNotNull(current);
      NodeType actualType = current.getType();
      String actualId = current.getId();
      assertEquals("unexpected type", type, actualType);
      assertEquals("unexpected id", id, actualId);
    }

    @Override
    public void startView(ConversationView view) {
      assertEquals(current.getType(), NodeType.ROOT);
    }

    @Override
    public void startConversation(Conversation conv) {
      expect(NodeType.CONVERSATION, viewIdMapper.conversationOf(conv));
      expect(NodeType.PARTICIPANTS, viewIdMapper.participantsOf(conv));
    }

    @Override
    public void startThread(ConversationThread thread) {
      expect(NodeType.THREAD, viewIdMapper.threadOf(thread));
    }

    @Override
    public void startInlineThread(ConversationThread thread) {
      expect(NodeType.THREAD, viewIdMapper.threadOf(thread));
    }

    @Override
    public void startBlip(ConversationBlip blip) {
      expect(NodeType.BLIP, viewIdMapper.blipOf(blip));
      expect(NodeType.META, viewIdMapper.metaOf(blip));
    }

    @Override
    public void endBlip(ConversationBlip blip) {
    }

    @Override
    public void endConversation(Conversation conv) {
    }

    @Override
    public void endInlineThread(ConversationThread thread) {
    }

    @Override
    public void endThread(ConversationThread thread) {
    }

    @Override
    public void endView(ConversationView view) {
      next();
      assertNull(current);
    }
  }
}
