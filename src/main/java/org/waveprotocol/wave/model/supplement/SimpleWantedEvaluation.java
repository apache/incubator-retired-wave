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

/**
 * The result of a single evaluation of the wanted state of a wavelet. This is
 * an immutable value object.
 *
 */
public class SimpleWantedEvaluation implements WantedEvaluation {

  private final String agentIdentity;
  private final String adderAddress;
  private final double certainty;
  private final String comment;
  private final long timestamp;
  private final boolean wanted;
  private final boolean ignored;
  private final WaveletId waveletId;

  /**
   * Constructor.
   *
   * @param waveletId The wavelet to which this evaluation pertains. A null
   *        indicates that this WantedEvaluation can be ignored.
   * @param adderAddress address of the account that added the user
   * @param wanted Whether the wave is wanted.
   * @param certainty How certain we are about the wanted state.
   * @param timestamp Java timestamp of the evaluation.
   * @param agentIdentity Identifies the agent for debugging purposes.
   * @param ignored whether this wave has been ignored by the user
   * @param comment Comment, for debugging purposes.
   */
  public SimpleWantedEvaluation(WaveletId waveletId, String adderAddress, boolean wanted,
      double certainty, long timestamp, String agentIdentity, boolean ignored, String comment) {
    this.waveletId = waveletId;
    this.adderAddress = adderAddress;
    this.wanted = wanted;
    this.certainty = certainty;
    this.timestamp = timestamp;
    this.agentIdentity = agentIdentity;
    this.ignored = ignored;
    this.comment = comment;
  }

  @Override
  public int compareTo(WantedEvaluation other) {
    return WantedEvaluation.COMPARATOR.compare(this, other);
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof WantedEvaluation) {
      return WantedEvaluation.ImplementationHelper.calculateEqual(this, (WantedEvaluation) other);
    } else {
      return false;
    }
  }

  @Override
  public String getAdderAddress() {
    return adderAddress;
  }

  @Override
  public String getAgentIdentity() {
    return agentIdentity;
  }

  @Override
  public double getCertainty() {
    return certainty;
  }

  @Override
  public String getComment() {
    return comment;
  }

  @Override
  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String getTypeName() {
    return "SimpleWantedEvaluation";
  }

  @Override
  public WaveletId getWaveletId() {
    return waveletId;
  }

  @Override
  public int hashCode() {
    return WantedEvaluation.ImplementationHelper.calculateHashCode(this);
  }

  @Override
  public boolean isIgnored() {
    return ignored;
  }

  @Override
  public boolean isWanted() {
    return wanted;
  }

  @Override
  public String toString() {
    return WantedEvaluation.ImplementationHelper.calculateToString(this);
  }
}
