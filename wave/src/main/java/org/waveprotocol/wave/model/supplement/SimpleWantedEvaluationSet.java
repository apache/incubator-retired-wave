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

import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Canonical implementation of a {@link WantedEvaluationSet}.
 *
 */
public class SimpleWantedEvaluationSet implements WantedEvaluationSet {

  private static final String DEFAULT_AGENT = "default";
  private static final String DEFAULT_COMMENT = "default rule";
  private static final String DEFAULT_ADDER = "default address";
  private final Set<WantedEvaluation> evaluations;
  private final WaveletId waveletId;

  /**
   * Default Constructor. Creates a new, empty WantedEvaluationSet.
   *
   * @param waveletId The wavelet that this is for.
   *
   * @param evaluations A bunch of wanted evaluations for the aforementioned
   *        wavelet. This set should not be modified during the lifetime of the
   *        SimpleWantedEvaluationSet.
   */
  public SimpleWantedEvaluationSet(WaveletId waveletId, Collection<WantedEvaluation> evaluations) {
    this.waveletId = waveletId;
    this.evaluations = CollectionUtils.immutableSet(evaluations);
  }

  /**
   * Convenience constructor for testing.
   */
  SimpleWantedEvaluationSet(WaveletId waveletId, WantedEvaluation... evaluations) {
    this(waveletId, CollectionUtils.newHashSet(evaluations));
  }

  private WantedEvaluation createDefaultEvaluation() {
    return new SimpleWantedEvaluation(
        waveletId, DEFAULT_ADDER, true, 0.0f, 0, DEFAULT_AGENT, false, DEFAULT_COMMENT);
  }

  @Override
  public Set<WantedEvaluation> getEvaluations() {
    return evaluations;
  }

  @Override
  public WantedEvaluation getMostCertain() {
    WantedEvaluation result = null;
    for (WantedEvaluation evaluation : evaluations) {
      if ((result == null) || (result.compareTo(evaluation) < 0)) {
        result = evaluation;
      }
    }
    return result == null ? createDefaultEvaluation() : result;
  }

  @Override
  public WantedEvaluation getMostRecent() {
    WantedEvaluation result = null;
    for (WantedEvaluation evaluation : evaluations) {
      if ((result == null) || (evaluation.getTimestamp() > result.getTimestamp())) {
        result = evaluation;
      }
    }
    return result == null ? createDefaultEvaluation() : result;
  }

  @Override
  public WaveletId getWaveletId() {
    return waveletId;
  }

  @Override
  public boolean isIgnored() {
    return getMostRecent().isIgnored();
  }

  @Override
  public boolean isWanted() {
    return getMostCertain().isWanted();
  }

  @Override
  public String toString() {
    List<WantedEvaluation> toDisplay = CollectionUtils.newArrayList(evaluations);
    Collections.sort(toDisplay, WantedEvaluation.COMPARATOR);
    return "SimpleWantedEvaluationSet(" + waveletId + ", " + toDisplay + ")";
  }
}
