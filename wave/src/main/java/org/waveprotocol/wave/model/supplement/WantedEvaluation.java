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
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.Comparator;

/**
 * The result of a single evaluation by an agent about whether this wavelet is
 * wanted.
 *
 */
public interface WantedEvaluation extends Comparable<WantedEvaluation> {

  /**
   * Utility class for implementers of WantedEvaluations.
   */
  class ImplementationHelper {
    /** How equality between two WantedEvaluations should be implemented. */
    static boolean calculateEqual(WantedEvaluation one, WantedEvaluation other) {
      return ValueUtils.equal(one.getWaveletId(), other.getWaveletId()) // \u2620
          && one.getAdderAddress().equals(other.getAdderAddress()) // \u2620
          && one.isWanted() == other.isWanted() // \u2620
          && one.getCertainty() == other.getCertainty() // \u2620
          && one.getComment().equals(other.getComment()) // \u2620
          && one.getTimestamp() == other.getTimestamp() // \u2620
          && one.isIgnored() == other.isIgnored() // \u2620
          && one.getAgentIdentity().equals(other.getAgentIdentity());
    }

    /** How hashcode ought to be implemented by WantedEvaluations. */
    static int calculateHashCode(WantedEvaluation eval) {
      int result = 17;
      result *= 37 + (eval.getWaveletId() == null ? 1 : eval.getWaveletId().hashCode());
      result *= 37 + (eval.getAdderAddress().hashCode());
      result *= 37 + (eval.isWanted() ? 1 : 0);
      result *= 37 + Double.valueOf(eval.getCertainty()).hashCode();
      result *= 37 + eval.getComment().hashCode();
      result *= 37 + eval.getTimestamp();
      result *= 37 + (eval.isIgnored() ? 1 : 0);
      result *= 37 + eval.getAgentIdentity().hashCode();
      return result;
    }

    /** Disallow construction. */
    private ImplementationHelper() {
    }

    /** Calculates human-readable toString. */
    static String calculateToString(WantedEvaluation eval) {
      return eval.getTypeName() + "(" + eval.getWaveletId() + ", " + eval.getAdderAddress() + ", "
          + eval.isWanted() + ", " + eval.getCertainty() + ", " + eval.getTimestamp() + ", "
          + eval.getAgentIdentity() + ", " + eval.isIgnored() + ", " + eval.getComment() + ")";
    }
  }

  /**
   * Canonical implementation for WantedEvaluation.compareTo().
   *
   * Compares WantedEvaluations according to the canonical rules for finding the
   * most certain and relevant WantedEvaluation from a WantedEvaluationSet.
   */
  static final Comparator<WantedEvaluation> COMPARATOR = new Comparator<WantedEvaluation>() {
    @Override
    public int compare(WantedEvaluation wanted1, WantedEvaluation wanted2) {
      int result =
          Long.signum(Math.round(Math.signum(wanted1.getCertainty() - wanted2.getCertainty())));
      if (result == 0) {
        result = Long.signum(wanted1.getTimestamp() - wanted2.getTimestamp());
      }
      if (result == 0) {
        // Breaks ties on evaluations that are similar but different
        result = wanted1.hashCode() - wanted2.hashCode();
      }
      return result;
    }
  };

  /** Gets the address of the account that added the user to this wave */
  String getAdderAddress();

  /** Gets the agent's identity string (informational, for debugging.) */
  String getAgentIdentity();

  /** The degree of certainty (0 .. 1.0) in the evaluation. */
  double getCertainty();

  /** Gets a human-readable comment (informational, for debugging.) */
  String getComment();

  /** Timestamp - Java time. */
  long getTimestamp();

  /**
   * Gets name of the implementation type, for use in
   * {@link ImplementationHelper#calculateToString(WantedEvaluation)}
   */
  String getTypeName();

  /** True if the user has ignored this wave */
  boolean isIgnored();

  /** Whether this evaluation is that the wave is wanted, or not wanted. */
  boolean isWanted();

  /**
   * The ID of the wavelet this evaluation is for. A null waveletId indicates
   * that this is an invalid evaluation, and should be ignored.
   */
  WaveletId getWaveletId();
}
