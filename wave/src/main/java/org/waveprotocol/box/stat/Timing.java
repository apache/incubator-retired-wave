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
import java.util.Map;

/**
 * Request-scoped timing.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
@SuppressWarnings({"rawtypes"})
public class Timing {
  static private final StatRecorder statsRecorder = new StatRecorder();
  static private final StatRenderer renderer = new StatRenderer();
  static private RequestScope scope;

  static private boolean enabled = false;

  /**
   * Gets recorder of statistic.
   */
  public static StatRecorder getStatsRecorder() {
    return statsRecorder;
  }

  /**
   * Enables/disables statistic.
   */
  static public void setEnabled(boolean enabled) {
    Timing.enabled = enabled;
  }

  /**
   * Gets enable state.
   */
  static public boolean isEnabled() {
    return enabled;
  }

  /**
   * Initializes scope.
   */
  static public void enterScope() {
    if (enabled && scope != null) {
      scope.enter();
    }
  }

  /**
   * Initializes scope with specified data.
   */
  static public void enterScope(Map<Class, RequestScope.Value> scopeValues) {
    if (enabled && scope != null && scopeValues != null) {
      scope.enter(scopeValues);
    }
  }

  /**
   * Clears scope.
   */
  static public void exitScope() {
    if (scope != null) {
      scope.exit();
    }
  }

  /**
   * Sets scope.
   */
  static public void setScope(RequestScope scope) {
    Timing.scope = scope;
  }

  /**
   * Gets scope value.
   */
  static public <T extends RequestScope.Value> T getScopeValue(Class<T> clazz) {
    if (scope != null) {
      enterIfOutOfScope();
      return scope.get(clazz);
    }
    return null;
  }

  /**
   * Sets scope value.
   */
  static public <T extends RequestScope.Value> void setScopeValue(Class<T> clazz, T value) {
    if (scope != null) {
      enterIfOutOfScope();
      scope.set(clazz, value);
    }
  }

  /**
   * Clones scope values.
   */
  static public Map<Class, RequestScope.Value> cloneScopeValues() {
    if (scope != null) {
      enterIfOutOfScope();
      return scope.cloneValues();
    }
    return null;
  }

  /**
   * Creates and starts the timer with specified name.
   */
  static public Timer start(String name) {
    if (enabled) {
      return getExecutionTree().start(name);
    }
    return null;
  }

  /**
   * Creates and starts the timer with specified name and threshold.
   */
  static public Timer start(String name, int threshold) {
    if (enabled) {
      return getExecutionTree().start(name, threshold);
    }
    return null;
  }

  /**
   * Creates and starts the timer for request with specified name.
   */
  static public Timer startRequest(String name) {
    if (enabled) {
      return getExecutionTree().startRequest(name);
    }
    return null;
  }

  /**
   * Creates and starts the timer for request with specified name and threshold.
   */
  static public Timer startRequest(String name, int threshold) {
    if (enabled) {
      return getExecutionTree().startRequest(name, threshold);
    }
    return null;
  }

  /**
   * Stops the timer.
   */
  static public void stop(Timer timer) {
    if (timer != null && timer.isActive()) {
      timer.stop(getExecutionTree());
    }
  }

  /**
   * Stops the timer on specified time.
   */
  static public void stop(Timer timer, long stopTime) {
    if (timer != null) {
      timer.stop(getExecutionTree(), stopTime);
    }
  }

  /**
   * Records statistics for specified name and interval.
   */
  static public void record(String name, int interval) {
    if (enabled) {
      getExecutionTree().record(name, interval);
    }
  }

  /**
   * Records statistics for specified name, interval and threshold.
   */
  static public void record(String name, int interval, int threshold) {
    if (enabled) {
      getExecutionTree().record(name, interval, threshold);
    }
  }

  /**
   * Records statistics for specified request name and threshold.
   */
  static public void recordRequest(String name, int interval) {
    if (enabled) {
      getExecutionTree().recordRequest(name, interval);
    }
  }

  /**
   * Records statistics for specified request name, interval and threshold.
   */
  static public void recordRequest(String name, int interval, int threshold) {
    if (enabled) {
      getExecutionTree().recordRequest(name, interval, threshold);
    }
  }

  /**
   * Clears the statistics.
   */
  static public void clearStatistics() {
    if (GWT.isClient()) {
      statsRecorder.getGlobalStore().clear();
    } else {
      statsRecorder.getSessionStore().clear();
    }
  }

  /**
   * Renders statistics for all program.
   */
  static public String renderGlobalStatistics() {
    return renderer.renderHtml(
            statsRecorder.getGlobalStore().getMeasurements(),
            statsRecorder.getGlobalStore().getProfiledRequests());
  }

  /**
   * Renders statistics for current session. Server only.
   */
  static public String renderSessionStatistics() {
    return renderer.renderHtml(
            statsRecorder.getSessionStore().getMeasurements(),
            statsRecorder.getSessionStore().getProfiledRequests());
  }

  /**
   * Renders tracked statistic.
   */
  static public String renderStats() {
    return renderer.renderHtml(Statistic.getStats());
  }

  /**
   * Renders title.
   */
  static public String renderTitle(String title, int level) {
    return renderer.renderTitle(title, level);
  }

  /**
   * Gets execution tree.
   */
  static private ExecutionTree getExecutionTree() {
     if (scope != null) {
      enterIfOutOfScope();
      return scope.get(ExecutionTree.class);
     }
     return null;
  }

  static private void enterIfOutOfScope() {
    if (!scope.isEntered()) {
      scope.enter();
    }
  }
}
