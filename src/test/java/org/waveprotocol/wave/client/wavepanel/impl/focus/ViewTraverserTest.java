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

package org.waveprotocol.wave.client.wavepanel.impl.focus;


import junit.framework.TestCase;

import org.waveprotocol.wave.client.wavepanel.view.AnchorView;
import org.waveprotocol.wave.client.wavepanel.view.BlipMetaView;
import org.waveprotocol.wave.client.wavepanel.view.BlipView;
import org.waveprotocol.wave.client.wavepanel.view.ConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineConversationView;
import org.waveprotocol.wave.client.wavepanel.view.InlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.ThreadView;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeAnchor;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeBlipView;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeConversationView;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeInlineThreadView;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeRenderer;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeThreadView;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeTopConversationView;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Tests the traversal ordering.
 *
 */

public class ViewTraverserTest extends TestCase {

  /**
   * Knows how to populate the structure of, and verify the travesral order of,
   * threads in a blip.
   */
  class BlipBuilder {
    private final ThreadBuilder[] anchored;
    private final ThreadBuilder[] unanchored;
    private final ConversationBuilder[] privates;

    BlipBuilder(ThreadBuilder[] anchored, ThreadBuilder[] unanchored,
        ConversationBuilder[] privates) {
      this.anchored = anchored;
      this.unanchored = unanchored;
      this.privates = privates;
    }

    void populate(ConversationBlip blip, FakeBlipView blipUi) {
      for (ThreadBuilder threadBuilder : unanchored) {
        ConversationThread thread = blip.addReplyThread();
        threadBuilder.populate(thread, blipUi.insertDefaultAnchorBefore(null, thread).getThread());
      }
      for (ThreadBuilder threadBuilder : anchored) {
        ConversationThread thread = blip.addReplyThread(blip.getContent().size() - 1);
        FakeAnchor anchor = blipUi.insertDefaultAnchorBefore(null, thread);
        FakeInlineThreadView threadUi = anchor.getThread();
        threadBuilder.populate(thread, threadUi);
        anchor.detach(threadUi);
        blipUi.getMeta().createInlineAnchorBefore(null, thread).attach(threadUi);
      }
      for (ConversationBuilder conversationBuilder : privates) {
        Conversation conversation = wave.createConversation();
        conversation.setAnchor(blip.getConversation().createAnchor(blip));
        assert conversation.hasAnchor();
        conversationBuilder.populate(
            conversation, blipUi.insertConversationBefore(null, conversation));
     }
    }

    void verify(Queue<BlipView> blips, BlipView blip) {
      assertEquals(blips.poll(), blip);
      BlipMetaView meta = blip.getMeta();

      AnchorView a = meta.getInlineAnchorAfter(null);
      for (ThreadBuilder threadBuilder : anchored) {
        assertNotNull(a);
        threadBuilder.verify(blips, a.getThread());
        a = meta.getInlineAnchorAfter(a);
      }
      assertNull(a);

      int anchoredDefaults = 0;  // empty default anchors.
      a = blip.getDefaultAnchorAfter(null);
      for (ThreadBuilder threadBuilder : unanchored) {
        assertNotNull(a);
        InlineThreadView thread = a.getThread();
        while (thread == null) {
          a = blip.getDefaultAnchorAfter(a);
          thread = a.getThread();
          assertNotNull(a);
          anchoredDefaults++;
        }
        threadBuilder.verify(blips, thread);
        a = blip.getDefaultAnchorAfter(a);
      }
      while (a != null) {
        assertNull(a.getThread());
        a = blip.getDefaultAnchorAfter(a);
        anchoredDefaults++;
      }
      assertNull(a);
      assertEquals(anchored.length, anchoredDefaults);

      InlineConversationView c = blip.getConversationAfter(null);
      for (ConversationBuilder conversationBuilder : privates) {
        assertNotNull(c);
        conversationBuilder.verify(blips, c);
        c = blip.getConversationAfter(c);
      }
      assertNull(c);
    }
  }

  /**
   * Knows how to populate the structure of, and verify the travesral order of,
   * blips in a thread.
   */
  class ThreadBuilder {
    private final BlipBuilder[] blipBuilders;

    ThreadBuilder(BlipBuilder... blipBuilders) {
      this.blipBuilders = blipBuilders;
    }

    void populate(ConversationThread thread, FakeThreadView threadUi) {
      for (BlipBuilder blipBuilder : blipBuilders) {
        ConversationBlip blip = thread.appendBlip();
        blip.getContent().insertText(blip.getContent().size() - 1, "Blip " + blipCount++);
        blipBuilder.populate(blip, threadUi.insertBlipBefore(null, blip));
      }
    }

    void verify(Queue<BlipView> blips, ThreadView thread) {
      BlipView blip = thread.getBlipAfter(null);
      for (BlipBuilder blipBuilder : blipBuilders) {
        assertNotNull(blip);
        blipBuilder.verify(blips, blip);
        blip = thread.getBlipAfter(blip);
      }
      assertNull(blip);
    }
  }

  /**
   * Knows how to populate the structure of, and verify the travesral order of,
   * an inline conversation.
   */
  class ConversationBuilder {
    private final ThreadBuilder rootBuilder;

    ConversationBuilder(ThreadBuilder rootBuilder) {
      this.rootBuilder = rootBuilder;
    }

    void populate(Conversation conversation, FakeConversationView conversationUi) {
      rootBuilder.populate(conversation.getRootThread(), conversationUi.getRootThread());
    }

    void verify(Queue<BlipView> blips, ConversationView conversation) {
      rootBuilder.verify(blips, conversation.getRootThread());
    }
  }

  /** Traverser to test. */
  private ViewTraverser traverser;

  /** View to build and traverse. */
  private FakeRenderer renderer;
  private ObservableConversationView wave;
  private FakeTopConversationView c;
  private int blipCount;

  private ConversationBuilder createSimpleSample() {
    return conversation(
        thread(
            leafBlip(),
            blip(
                anchored(
                    thread(leafBlip())
                ),
                unanchored(
                    thread(leafBlip(), leafBlip()),
                    thread(leafBlip())
                ),
                privates(
                    conversation(thread(leafBlip())),
                    conversation(thread(leafBlip()))
                )
            )
        )
    );
  }

  private ConversationBuilder createEmptyThreadSample() {
    return conversation(
        thread(
            leafBlip(),
            blip(
                anchored(thread()),
                unanchored(
                    thread(
                        leafBlip(),
                        leafBlip()
                    ),
                    thread()
                ),
                privates(
                    conversation((thread(leafBlip()))),
                    conversation((thread())),
                    conversation((thread(leafBlip())))
                )
            )
        )
    );
  }

  private ConversationBuilder createComplexSample() {
    return conversation(
        thread(
            leafBlip(),
            blip(
                anchored(
                    thread()
                ),
                anchored(
                    thread(
                        leafBlip(),
                        leafBlip()
                    ),
                    thread(
                        leafBlip()
                    )
                )
            ),
            blip(
                anchored(
                    thread(
                        leafBlip(),
                        leafBlip()
                    ),
                    thread(
                        leafBlip()
                    )
                ),
                unanchored(),
                privates(
                    conversation(
                        thread(
                            leafBlip(),
                            blip(
                                anchored(
                                    thread()
                                ),
                                anchored(
                                    thread(
                                        leafBlip(),
                                        leafBlip()
                                    ),
                                    thread(
                                        leafBlip()
                                    )
                                )
                            ),
                            blip(
                                anchored(),
                                unanchored(),
                                privates(
                                    conversation(
                                        thread(
                                            leafBlip(),
                                            leafBlip()
                                        )
                                    )
                                )
                            ),
                            leafBlip()
                        )
                    ),
                    conversation(
                        thread(
                            leafBlip(),
                            leafBlip()
                        )
                    )
                )
            ),
            blip(
                anchored(),
                anchored(
                    thread(
                        blip(
                            anchored(),
                            unanchored(
                                thread(
                                    leafBlip(),
                                    leafBlip()
                                )
                            )
                        )
                    )
                )
            ),
            leafBlip()
        )
    );
  }

  BlipBuilder leafBlip() {
    return blip(anchored(), unanchored(), privates());
  }

  ThreadBuilder [] anchored(ThreadBuilder ... threads) {
    return threads;
  }

  ThreadBuilder [] unanchored(ThreadBuilder ... threads) {
    return threads;
  }

  ConversationBuilder [] privates(ConversationBuilder ... convos) {
    return convos;
  }

  BlipBuilder blip(ThreadBuilder[] anchored, ThreadBuilder[] unanchored,
      ConversationBuilder ... privates) {
    return new BlipBuilder(anchored, unanchored, privates);
  }

  ThreadBuilder thread(BlipBuilder ... blips) {
    return new ThreadBuilder(blips);
  }

  ConversationBuilder conversation(ThreadBuilder root) {
    return new ConversationBuilder(root);
  }

  @Override
  protected void setUp() {
    traverser = new ViewTraverser();
    wave = org.waveprotocol.wave.model.conversation.testing.FakeConversationView.builder().build();
    Conversation main = wave.createRoot();
    renderer = FakeRenderer.create(wave);
    c = (FakeTopConversationView) renderer.render(main);
  }

  public void testSimpleForward() {
    ConversationBuilder sample = createSimpleSample();
    sample.populate(wave.getRoot(), c);
    sample.verify(getOrderViaForward(c), c);
  }

  public void testSimpleReverse() {
    ConversationBuilder sample = createSimpleSample();
    sample.populate(wave.getRoot(), c);
    sample.verify(getOrderViaReverse(c), c);
  }

  public void testSomeEmptyForward() {
    ConversationBuilder sample = createEmptyThreadSample();
    sample.populate(wave.getRoot(), c);
    sample.verify(getOrderViaForward(c), c);
  }

  public void testSomeEmptyReverse() {
    ConversationBuilder sample = createEmptyThreadSample();
    sample.populate(wave.getRoot(), c);
    sample.verify(getOrderViaReverse(c), c);
  }

  public void testComplexForward() {
    ConversationBuilder sample = createComplexSample();
    sample.populate(wave.getRoot(), c);
    sample.verify(getOrderViaForward(c), c);
  }

  public void testReverseTraversalOrder() {
    ConversationBuilder sample = createComplexSample();
    sample.populate(wave.getRoot(), c);
    sample.verify(getOrderViaReverse(c), c);
  }

  private LinkedList<BlipView> getOrderViaForward(ConversationView c) {
    LinkedList<BlipView> list = new LinkedList<BlipView>();
    for (BlipView b = traverser.getFirst(c); b != null; b = traverser.getNext(b)) {
      list.add(b);
    }
    return list;
  }

  private LinkedList<BlipView> getOrderViaReverse(ConversationView c) {
    LinkedList<BlipView> list = new LinkedList<BlipView>();
    for (BlipView b = traverser.getLast(c); b != null; b = traverser.getPrevious(b)) {
      list.add(b);
    }
    Collections.reverse(list);
    return list;
  }
}
