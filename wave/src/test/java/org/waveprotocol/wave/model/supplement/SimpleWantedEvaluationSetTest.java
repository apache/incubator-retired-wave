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

import com.google.common.collect.ImmutableSet;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.WaveletId;

/**
 * Tests for {@link SimpleWantedEvaluationSet}
 *
 */

public class SimpleWantedEvaluationSetTest extends TestCase {

  private static final WaveletId TEST_ID;
  private static final SimpleWantedEvaluation EVALUATION_1;
  private static final SimpleWantedEvaluation EVALUATION_2;
  private static final SimpleWantedEvaluation EVALUATION_3;
  private static final String ADDER = "badhorse@evilleagueofevil.com";

  static {
    TEST_ID = WaveletId.of("google.com", "wavelet1");
    EVALUATION_1 =
      new SimpleWantedEvaluation(TEST_ID, ADDER, true, 0.2f, 1000, "agent", false, "");
    EVALUATION_2 =
      new SimpleWantedEvaluation(TEST_ID, ADDER, false, 0.2f, 1010, "test2", false, "");
    EVALUATION_3 =
      new SimpleWantedEvaluation(TEST_ID, ADDER, true, 0.3f, 1010, "agent", true, "");
  }

  public void testBasic() {

    // New evaluation set should have just default evaluation.
    WantedEvaluationSet evalSet = new SimpleWantedEvaluationSet(TEST_ID);
    assertEquals(TEST_ID, evalSet.getWaveletId());

    assertEquals(true, evalSet.isWanted());
    WantedEvaluation eval = evalSet.getMostCertain();
    assertEquals(true, eval.isWanted());
    assertTrue(0.01 > eval.getCertainty());
    assertEquals(0, evalSet.getEvaluations().size());

    // Add two new evaluations with same certainty. Higher timestamp should win
    evalSet = new SimpleWantedEvaluationSet(TEST_ID, EVALUATION_1, EVALUATION_2);
    assertEquals(EVALUATION_2, evalSet.getMostCertain());
    assertFalse(evalSet.isWanted());
    assertFalse(evalSet.isIgnored());

    // Add a third, with higher certainty, and that should be the most certain
    evalSet =
        new SimpleWantedEvaluationSet(TEST_ID, EVALUATION_1, EVALUATION_2, EVALUATION_3);
    assertEquals(EVALUATION_3, evalSet.getMostCertain());
    assertTrue(evalSet.isWanted());
    assertTrue(evalSet.isIgnored());

    // All three evaluations should be present in data
    assertEquals(ImmutableSet.of(EVALUATION_1, EVALUATION_2, EVALUATION_3),
        evalSet.getEvaluations());
  }
}
