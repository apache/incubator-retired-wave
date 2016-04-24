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

package org.waveprotocol.box.webclient.client.state;

import junit.framework.TestCase;

import org.waveprotocol.wave.client.state.ThreadReadStateMonitor;
import org.waveprotocol.wave.client.state.ThreadReadStateMonitorImpl;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.supplement.LiveSupplementedWaveImpl;
import org.waveprotocol.wave.model.supplement.ObservablePrimitiveSupplement;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;
import org.waveprotocol.wave.model.supplement.SupplementedWaveImpl.DefaultFollow;
import org.waveprotocol.wave.model.supplement.WaveletBasedSupplement;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;


/**
 * Test the thread state monitor class to ensure proper counting of total, read
 * and unread blips in the root thread as well as ensuring that counts are
 * properly aggregated when nested threads are present.
 *
 * @author Michael MacFadden
 */
public class ThreadReadStateMonitorTest extends TestCase {

  private static final ParticipantId viewer = new ParticipantId("nobody@nowhere.com");
  private static final IdGenerator idgen = FakeIdGenerator.create();

  private ObservableSupplementedWave supplementedWave;
  private ConversationThread rootThread;
  private ThreadReadStateMonitor monitor;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    FakeWaveView view = BasicFactories.fakeWaveViewBuilder().with(idgen).build();
    Wavelet userDataWavelet = view.createUserData();
    ObservablePrimitiveSupplement primitiveSupplement =
        WaveletBasedSupplement.create(userDataWavelet);
    WaveBasedConversationView conversationView = WaveBasedConversationView.create(view, idgen);
    WaveletBasedConversation rootConversation = conversationView.createRoot();

    supplementedWave =
        new LiveSupplementedWaveImpl(primitiveSupplement, view, viewer, DefaultFollow.ALWAYS,
            conversationView);
    monitor = ThreadReadStateMonitorImpl.create(supplementedWave, conversationView);
    rootThread = rootConversation.getRootThread();
  }

  /**
   * This test verifies the total counts of blips in the root thread.
   */
  public void testEmptyRootThread() throws Exception {
    assertEquals(0, monitor.getTotalCount(rootThread));
  }

  /**
   * This test verifies the total counts of blips in the root thread.
   */
  public void testRootThreadBlips() throws Exception {
    rootThread.appendBlip();
    rootThread.appendBlip();
    rootThread.appendBlip();
    assertEquals(3, monitor.getTotalCount(rootThread));
  }

  /**
   * This test verifies the count in an empty root thread.
   */
  public void testRootThreadDeleteBlip() throws Exception {
    rootThread.appendBlip();
    rootThread.appendBlip();
    rootThread.appendBlip().delete();
    assertEquals(2, monitor.getTotalCount(rootThread));
  }

  /**
   * This test verifies the count in an empty inline thread.
   */
  public void testEmptyInlineThread() throws Exception {
    ConversationBlip rb1 = rootThread.appendBlip();
    ConversationThread t1 = rb1.addReplyThread(0);
    assertEquals(1, monitor.getTotalCount(rootThread));
    assertEquals(0, monitor.getTotalCount(t1));
  }

  /**
   * This test ensures that the total blip counts aggregate properly up through
   * nested inline threads.
   */
  public void testNestedThreadBlips() throws Exception {
    ConversationBlip rb1 = rootThread.appendBlip();
    ConversationThread t1 = rb1.addReplyThread(0);
    t1.appendBlip();
    t1.appendBlip();

    ConversationBlip rb2 = rootThread.appendBlip();
    ConversationThread t2 = rb2.addReplyThread(0);
    t2.appendBlip();
    ConversationBlip t2b2 = t2.appendBlip();

    ConversationThread t3 = t2b2.addReplyThread();
    t3.appendBlip();

    assertEquals(2, monitor.getTotalCount(t1));
    assertEquals(3, monitor.getTotalCount(t2));
    assertEquals(1, monitor.getTotalCount(t3));
    assertEquals(7, monitor.getTotalCount(rootThread));
  }

  /**
   * This test verifies that blips counts are aggregated properly when a blip is
   * deleted which contains an inline thread.
   */
  public void testNestedThreadDeleteBlip() throws Exception {
    ConversationBlip rb1 = rootThread.appendBlip();
    ConversationThread t1 = rb1.addReplyThread(0);
    t1.appendBlip();
    t1.appendBlip();

    ConversationBlip rb2 = rootThread.appendBlip();
    ConversationThread t2 = rb2.addReplyThread(0);
    t2.appendBlip();
    ConversationBlip t2b2 = t2.appendBlip();

    ConversationThread t3 = t2b2.addReplyThread();
    t3.appendBlip();

    t2b2.delete();

    assertEquals(2, monitor.getTotalCount(t1));
    assertEquals(1, monitor.getTotalCount(t2));
    assertEquals(0, monitor.getTotalCount(t3));
    assertEquals(5, monitor.getTotalCount(rootThread));
  }

  /**
   * This test verifies read / unread blips are counted correctly in the root
   * thread.
   */
  public void testRootThreadReading() throws Exception {
    rootThread.appendBlip();
    rootThread.appendBlip();
    ConversationBlip blip = rootThread.appendBlip();

    supplementedWave.markAsRead(blip);

    assertEquals(1, monitor.getReadCount(rootThread));
    assertEquals(2, monitor.getUnreadCount(rootThread));
    assertEquals(3, monitor.getTotalCount(rootThread));

    assertEquals(monitor.getTotalCount(rootThread),
        monitor.getUnreadCount(rootThread) + monitor.getReadCount(rootThread));
  }

  /**
   * This test verifies the read and unread state in both the root thread as
   * well as an inline thread.
   */
  public void testNestedThreadReading() throws Exception {
    ConversationBlip rb1 = rootThread.appendBlip();
    ConversationThread t1 = rb1.addReplyThread(0);
    t1.appendBlip();
    t1.appendBlip();

    ConversationBlip rb2 = rootThread.appendBlip();
    ConversationThread t2 = rb2.addReplyThread(0);
    t2.appendBlip();
    ConversationBlip t2b2 = t2.appendBlip();

    ConversationThread t3 = t2b2.addReplyThread();
    ConversationBlip t3b1 = t3.appendBlip();

    supplementedWave.markAsRead(t3b1);

    assertEquals(0, monitor.getReadCount(t1));
    assertEquals(2, monitor.getUnreadCount(t1));

    assertEquals(1, monitor.getReadCount(t2));
    assertEquals(2, monitor.getUnreadCount(t2));

    assertEquals(1, monitor.getReadCount(t3));
    assertEquals(0, monitor.getUnreadCount(t3));

    assertEquals(1, monitor.getReadCount(rootThread));
    assertEquals(6, monitor.getUnreadCount(rootThread));
  }

  /**
   * This test verifies the read and unread state when a thread with read and
   * unread blips is deleted.
   */
  public void testNestedThreadWithDeleteReading() throws Exception {
    ConversationBlip rb1 = rootThread.appendBlip();
    ConversationThread t1 = rb1.addReplyThread(0);
    t1.appendBlip();
    ConversationBlip t1b2 = t1.appendBlip();

    ConversationBlip rb2 = rootThread.appendBlip();
    ConversationThread t2 = rb2.addReplyThread(0);
    t2.appendBlip();
    ConversationBlip t2b2 = t2.appendBlip();

    ConversationThread t3 = t2b2.addReplyThread();
    t3.appendBlip();
    ConversationBlip t3b1 = t3.appendBlip();

    supplementedWave.markAsRead(t3b1);
    supplementedWave.markAsRead(t1b2);
    t2b2.delete();

    assertEquals(1, monitor.getReadCount(t1));
    assertEquals(1, monitor.getUnreadCount(t1));

    assertEquals(0, monitor.getReadCount(t2));
    assertEquals(1, monitor.getUnreadCount(t2));

    assertEquals(0, monitor.getReadCount(t3));
    assertEquals(0, monitor.getUnreadCount(t3));

    assertEquals(1, monitor.getReadCount(rootThread));
    assertEquals(4, monitor.getUnreadCount(rootThread));
  }
}
