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

package org.waveprotocol.wave.client.wavepanel.render;

import static org.mockito.Mockito.mock;

import com.google.common.collect.Iterables;

import junit.framework.TestCase;

import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.impl.ProfileManagerImpl;
import org.waveprotocol.wave.client.scheduler.testing.FakeTimerService;
import org.waveprotocol.wave.client.state.ThreadReadStateMonitor;
import org.waveprotocol.wave.client.wavepanel.view.View;
import org.waveprotocol.wave.client.wavepanel.view.dom.ModelAsViewProvider;
import org.waveprotocol.wave.client.wavepanel.view.fake.FakeRenderer;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.testing.FakeConversationView;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;

/**
 * Tests commutativity between static rendering and dynamic rendering (i.e.,
 * render then update produces the same rendering as update then render).
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class WaveRendererTest extends TestCase {
  private FakeTimerService timer;
  private ObservableConversationView wave;

  private View rendering;

  @Override
  protected void setUp() {
    ShallowBlipRenderer populator = mock(ShallowBlipRenderer.class);
    ObservableSupplementedWave supplement = mock(ObservableSupplementedWave.class);
    ProfileManager profiles = new ProfileManagerImpl();
    ThreadReadStateMonitor readMonitor = mock(ThreadReadStateMonitor.class);

    // Create a wave
    wave = createWave();

    // Render it.
    FakeRenderer renderer = FakeRenderer.create(wave);
    ModelAsViewProvider views = renderer;
    rendering = renderer.render(wave);

    // Keep it live.
    ReplyManager replyHandler = new ReplyManager(views);
    timer = new FakeTimerService();
    LiveConversationViewRenderer.create(
        timer, wave, views, populator, replyHandler, readMonitor, profiles, supplement).init();
  }

  private FakeConversationView createWave() {
    FakeConversationView wave = FakeConversationView.builder().build();
    Conversation c = wave.createRoot();
    ConversationBlip b1 = c.getRootThread().appendBlip();
    ConversationBlip b2 = c.getRootThread().appendBlip();
    write(b1, "First blip");
    write(b2, "Second blip");
    ConversationThread b1t1 = b1.addReplyThread(5);
    write(b1t1.appendBlip(), "First reply");
    ConversationThread b1t2 = b1.addReplyThread();
    write(b1t2.appendBlip(), "Second reply");

    return wave;
  }

  private static void write(ConversationBlip blip, String msg) {
    org.waveprotocol.wave.model.document.Document d = blip.getContent();
    d.emptyElement(d.getDocumentElement());
    d.appendXml(XmlStringBuilder.createFromXmlString("<body><line></line>" + msg + "</body>"));
  }

  /** Completely re-renders the wave, producing a new view. */
  private View render() {
    View view = FakeRenderer.create(wave).render(wave);
    timer.tick(1); // Flush profile rendering.
    return view;
  }

  public void testInitialRendering() {
    // Just check that nothing crashes during setUp.
  }

  public void testLiveRenderingCommutesOnBlipAppend() {
    ConversationThread root = wave.getRoot().getRootThread();
    root.appendBlip();
    assertEquals(render().toString(), rendering.toString());
  }

  public void testLiveRenderingCommutesOnBlipInsert() {
    ConversationThread root = wave.getRoot().getRootThread();
    root.insertBlip(Iterables.get(root.getBlips(), 1));

    assertEquals(render().toString(), rendering.toString());
  }

  public void testLiveRenderingCommutesOnBlipDelete() {
    ConversationThread root = wave.getRoot().getRootThread();
    root.getFirstBlip().delete();

    assertEquals(render().toString(), rendering.toString());
  }

}
