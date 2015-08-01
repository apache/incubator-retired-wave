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

package org.waveprotocol.wave.model.supplement;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.schema.SchemaCollection;
import org.waveprotocol.wave.model.schema.account.AccountSchemas;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.schema.supplement.UserDataSchemas;
import org.waveprotocol.wave.model.supplement.SupplementedWaveImpl.DefaultFollow;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.List;
import java.util.Map;

/**
 * Tests abuse portions of the {@link LiveSupplementedWaveImpl} class.
 *
 */

public class LiveSupplementedWaveImplAbuseTest extends TestCase {

  /** Type funging adapter method to keep command-line JDK compiler happy. */
  public static <N> void fungeInsertInvalidEvaluation(
      MutableDocument<N, ?, ?> doc) {
    LiveSupplementedWaveImplAbuseTest.insertInvalidEvaluation(doc);
  }

  /** Insert bad WantedEvaluation data. */
  private static <N, E extends N, T extends N> void insertInvalidEvaluation(
      MutableDocument<N, E, T> doc) {

    Map<String, String> attributes = CollectionUtils.newHashMap();
    attributes.put(DocumentBasedAbuseStore.AGENT_ATTR, "invalid agent");
    attributes.put(DocumentBasedAbuseStore.CERTAINTY_ATTR, "0.0");
    attributes.put(DocumentBasedAbuseStore.TIMESTAMP_ATTR, "1234");
    attributes.put(DocumentBasedAbuseStore.COMMENT_ATTR, "no comment");
    attributes.put(DocumentBasedAbuseStore.WANTED_ATTR, "true");

    doc.createChildElement(doc.getDocumentElement(),
        DocumentBasedAbuseStore.WANTED_EVAL_TAG, attributes);
  }

  private static final SchemaCollection schemas = new SchemaCollection();
  static {
    schemas.add(new AccountSchemas());
    schemas.add(new ConversationSchemas());
    schemas.add(new UserDataSchemas());
  }

  private WantedEvaluation eval1;
  private WantedEvaluation eval2;
  private WantedEvaluation eval3;
  private WaveletId otherId;
  private Wavelet otherWavelet;
  private ObservablePrimitiveSupplement primitiveSupplement;
  private WaveletId rootId;
  private Wavelet rootWavelet;
  private ObservableSupplementedWave supplementedWave;

  private Wavelet userDataWavelet;

  private FakeWaveView view;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    ParticipantId viewer = new ParticipantId("nobody@nowhere.com");
    String adder = "evilbob@evil.com";
    IdGenerator idgen = FakeIdGenerator.create();
    view = BasicFactories.fakeWaveViewBuilder().with(idgen).build();
    userDataWavelet = view.createUserData();
    rootWavelet = view.createRoot();
    rootId = rootWavelet.getId();
    otherWavelet = view.createWavelet();
    otherId = otherWavelet.getId();
    eval1 =
      new SimpleWantedEvaluation(rootId, adder, true, 0.1f, 1234L, "agent0", true, "no comment");
    eval2 =
      new SimpleWantedEvaluation(otherId, adder, true, 0.8f, 1256L, "agent2", false, "no comment");
    eval3 =
      new SimpleWantedEvaluation(rootId, adder, false, 0.5f, 1278L, "agent1", true, "no comment");

    primitiveSupplement = WaveletBasedSupplement.create(userDataWavelet);
    ObservableConversationView conversation = WaveBasedConversationView.create(view, idgen);
    supplementedWave =
        new LiveSupplementedWaveImpl(primitiveSupplement, view, viewer, DefaultFollow.ALWAYS,
            conversation);

  }

  public void testEmptyEvaluationSet() {
    WantedEvaluationSet evaluationSet = supplementedWave.getWantedEvaluationSet(rootWavelet);
    assertTrue(evaluationSet.isWanted());
    assertTrue(evaluationSet.getEvaluations().isEmpty());

    WantedEvaluation defaultEvaluation = evaluationSet.getMostCertain();

    assertTrue(0.01 > defaultEvaluation.getCertainty());
    assertTrue(defaultEvaluation.isWanted());
  }

  public void testInvalidEvalauationsIgnored() {
    // Insert an evaluation missing a wavelet id. Must do it via XML because
    // regular machinery does not allow null values
    ObservableMutableDocument<?, ?, ?> document = userDataWavelet
        .getDocument(WaveletBasedSupplement.ABUSE_DOCUMENT);
    LiveSupplementedWaveImplAbuseTest.fungeInsertInvalidEvaluation(document);

    supplementedWave.addWantedEvaluation(eval2);

    WantedEvaluationSet set = supplementedWave.getWantedEvaluationSet(otherWavelet);
    assertEquals(CollectionUtils.immutableSet(eval2), set.getEvaluations());
  }

  public void testListening() {
    supplementedWave.addWantedEvaluation(eval1);

    final List<WaveletId> waveletIds = CollectionUtils.newArrayList();
    supplementedWave.addListener(new ObservableSupplementedWave.ListenerImpl() {
      @Override
      public void onWantedEvaluationsChanged(WaveletId waveletId) {
        waveletIds.add(waveletId);
      }
    });

    supplementedWave.addWantedEvaluation(eval2);
    supplementedWave.addWantedEvaluation(eval3);

    // Should have notifications for adding eval2 and eval3, but not eval1
    assertEquals(CollectionUtils.newArrayList(otherId, rootId), waveletIds);
  }

  public void testMultipleEvaluationSets() {
    supplementedWave.addWantedEvaluation(eval1);
    supplementedWave.addWantedEvaluation(eval2);
    supplementedWave.addWantedEvaluation(eval3);

    // Check that evaluations split into sets correctly.
    WantedEvaluationSet set1 = supplementedWave.getWantedEvaluationSet(rootWavelet);
    assertEquals(CollectionUtils.immutableSet(eval1, eval3), set1.getEvaluations());

    WantedEvaluationSet set2 = supplementedWave.getWantedEvaluationSet(otherWavelet);
    assertEquals(CollectionUtils.immutableSet(eval2), set2.getEvaluations());
  }
}
