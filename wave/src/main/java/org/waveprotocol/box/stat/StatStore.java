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

import java.util.HashMap;
import java.util.LinkedList;

import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.waveprotocol.wave.model.util.Pair;
import com.google.common.collect.ImmutableList;

/**
 * Store for profiling statistic.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
class StatStore {

  private static int MAX_REQUESTS = 100;

  private volatile int queueSize = 0;
  private final Queue<ExecutionNode> profiledRequests = new LinkedList<>();
  private final Map<String, Measurement> measurements = new HashMap<>();

  StatStore() {
  }

  synchronized void recordMeasurement(String name, String module, int duration, int threshold) {
    Measurement m = measurements.get(name);
    if (m == null) {
      measurements.put(name, m = new Measurement());
    }
    m.sample(duration);
    m.setThreshold(threshold);
  }

  synchronized void storeRequest(ExecutionNode node) {
    if (profiledRequests.offer(node)) {
      queueSize++;
    }

    // Remove items from the queue while there are items to remove.
    if (queueSize > MAX_REQUESTS) {
      profiledRequests.poll();
      queueSize--;
    }
  }

  synchronized List<Pair<String, Measurement>> getMeasurements() {
    LinkedList<Pair<String, Measurement>> list = new LinkedList<>();
    for (Map.Entry<String, Measurement> entry : measurements.entrySet()) {
      list.add(Pair.of(entry.getKey(), entry.getValue()));
    }
    return list;
  }

  synchronized List<ExecutionNode> getProfiledRequests() {
    return ImmutableList.copyOf(profiledRequests);
  }

  synchronized void clear() {
    queueSize = 0;
    profiledRequests.clear();
    measurements.clear();
  }
}
