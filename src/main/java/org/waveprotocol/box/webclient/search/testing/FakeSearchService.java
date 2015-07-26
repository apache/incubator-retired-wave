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

package org.waveprotocol.box.webclient.search.testing;

import com.google.common.base.Joiner;
import com.google.gwt.http.client.Request;

import org.waveprotocol.box.webclient.search.SearchService;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Serves up fake search results.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class FakeSearchService implements SearchService {
  private static final int FAKE_DELAY_MS = 300;
  private final TimerService timer;
  private final List<DigestSnapshot> canned;

  public static class Factory {
    private final static int FAKE_DIGEST_COUNT = 50;
    /** A bunch of random words. */
    private final static String[] FAKE_WORDS =
        {"alien", "bellies", "cherry", "dustiest", "effected", "family", "milkweed", "sniffs",
            "tunnel", "unofficially", "virtually", "withdrawal", "x-ray", "zippy"};

    public static List<DigestSnapshot> createCanned(ParticipantId me) {
      String domain = me.getDomain();
      Random r = new Random();
      double now = SchedulerInstance.getLowPriorityTimer().currentTimeMillis();
      double week = 7 * 24 * 60 * 60 * 1000;
      List<DigestSnapshot> digests = CollectionUtils.newArrayList();
      List<ParticipantId> allParticipants =
          Arrays.asList(me, ParticipantId.ofUnsafe("john@" + domain),
              ParticipantId.ofUnsafe("jane@" + domain), ParticipantId.ofUnsafe("jill@" + domain));
      for (int i = 0; i < FAKE_DIGEST_COUNT; i++) {
        String title = randomSentence(r, 4);
        String snippet = randomSentence(r, 15);
        int msgs = Math.min(5, r.nextInt(20));
        int unread = Math.max(0, r.nextInt(msgs + 5) - 5);
        ParticipantId author = allParticipants.get(r.nextInt(allParticipants.size()));
        List<ParticipantId> participants = randomSubsequence(r, allParticipants);
        participants.remove(author);
        WaveId wid = WaveId.of(domain, "fake" + i);
        double lmt = now - r.nextDouble() * week;
        digests.add(new DigestSnapshot(title, snippet, wid, author, participants, lmt, unread, msgs));
      }
      return digests;
    }

    private static String randomSentence(Random r, int wordCount) {
      String[] words = new String[wordCount];
      for (int j = 0; j < words.length; j++) {
        words[j] = FAKE_WORDS[r.nextInt(FAKE_WORDS.length)];
      }
      return Joiner.on(' ').join(words);
    }

    private static <T> List<T> randomSubsequence(Random r, List<T> items) {
      List<T> sub = CollectionUtils.newArrayList();
      int length = r.nextInt(items.size());
      for (int i = 0; i < length; i++) {
        sub.add(items.get(r.nextInt(items.size())));
      }
      return sub;
    }

    public static FakeSearchService create(ParticipantId me) {
      return new FakeSearchService(SchedulerInstance.getLowPriorityTimer(), createCanned(me));
    }
  }

  public FakeSearchService(TimerService timer, List<DigestSnapshot> canned) {
    this.timer = timer;
    this.canned = canned;
  }

  @Override
  public Request search(String query, final int index, final int numResults, final Callback callback) {
    timer.scheduleDelayed(new Task() {
      @Override
      public void execute() {
        int from = Math.min(index, canned.size() - 1);
        int to = Math.max(index + numResults, canned.size());
        callback.onSuccess(canned.size(), canned.subList(from, to));
      }
    }, FAKE_DELAY_MS);
    return null;
  }
}
