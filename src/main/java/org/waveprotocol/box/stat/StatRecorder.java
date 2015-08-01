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

import com.google.gwt.core.shared.GWT;

import java.util.HashMap;
import java.util.Map;

/**
 * Collects global and request-based statistic.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
class StatRecorder {

  private final StatStore globalStore = new StatStore();
  private final Map<String, StatStore> sessionsStore = new HashMap<>();

  StatRecorder() {
  }

  /**
   * Gets global statistic.
   */
  StatStore getGlobalStore() {
    return globalStore;
  }

  synchronized StatStore getSessionStore() {
    if (getSessionContext() != null) {
      StatStore store = sessionsStore.get(getSessionContext().getSessionKey());
      if (store == null) {
        sessionsStore.put(getSessionContext().getSessionKey(), store = new StatStore());
      }
      return store;
    }
    return null;
  }

  /**
   * Records a single incident of measure and duration in millis with threshold.
   */
  void record(String name, String module, int duration, int threshold) {
    if (!GWT.isClient() && getSessionContext() != null && getSessionContext().isAuthenticated()) {
      getSessionStore().recordMeasurement(name, module, duration, threshold);
    }
    globalStore.recordMeasurement(name, module, duration, threshold);
  }

  /**
   * Records an http request call tree.
   */
  void recordRequest(ExecutionNode node) {
    if (!GWT.isClient() && getSessionContext() != null && getSessionContext().isAuthenticated()) {
      getSessionStore().storeRequest(node);
    }
    globalStore.storeRequest(node);
  }

  private SessionContext getSessionContext() {
    return Timing.getScopeValue(SessionContext.class);
  }
}
