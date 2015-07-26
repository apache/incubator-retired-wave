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
package org.waveprotocol.box.stat;

/**
 * Represents sampling of measurements.
 *
 * @author David Byttow
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
class Measurement {
  private int average;
  private int high;
  private int low;
  private int total;
  private int numSamples;
  private int threshold;

  Measurement() {
    this.low = Integer.MAX_VALUE;
    this.high = 0;
  }

  /**
   * Samples with a new delta.
   *
   * @param delta the duration of the current sample.
   */
  synchronized void sample(int delta) {
    ++numSamples;
    low = Math.min(delta, low);
    high = Math.max(delta, high);
    total += delta;
    average = total / numSamples;
  }

  int getAverage() {
    return average;
  }

  int getHigh() {
    return high;
  }

  int getLow() {
    return low;
  }

  int getTotal() {
    return total;
  }

  int getNumSamples() {
    return numSamples;
  }

  /**
   * @return threshold time considered "too slow"
   */
  int getThreshold() {
    return threshold;
  }

  void setThreshold(int threshold) {
    this.threshold = threshold;
  }

  @Override
  public String toString() {
    if (numSamples == 0) {
      return "<td></td> <td></td> <td></td> <td></td> <td></td>";
    } else if (numSamples == 1) {
      return new StringBuilder().append("<td></td> <td></td> <td></td> <td></td> <td>").
              append(formatMillis(total)).append("</td>").toString();
    } else {
      return new StringBuilder().append("<td>").append(numSamples).append("</td>").
              append(" <td>").append(formatMillis(average)).append("</td>").
              append(" <td>").append(formatMillis(low)).append("</td>").
              append(" <td>").append(formatMillis(high)).append("</td>").
              append(" <td>").append(formatMillis(total)).append("</td>").toString();
    }
  }

  private static String formatMillis(long millis) {
    long sec = millis / 1000;
    long ms = millis % 1000;
    StringBuilder sb = new StringBuilder();
    sb.append(sec).append(".");
    if (ms < 10) {
      sb.append("00");
    } else if (ms < 100) {
      sb.append('0');
    }
    sb.append(ms).append("s");
    return sb.toString();
  }
}
