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

import java.util.Map;

/**
 * Saves and restores scope and timer of asynchronous call.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
@SuppressWarnings({"rawtypes"})
public class AsyncCallContext {
  final Map<Class, RequestScope.Value> values;
  final Timer timer;

  private AsyncCallContext(Map<Class, RequestScope.Value> values, Timer timer) {
    this.values = values;
    this.timer = timer;
  }

  /**
   * Clones scope values and creates new timer.
   *
   * @param name the name of new task.
   * @return context with new scope and timer.
   */
  public static AsyncCallContext start(String name) {
    Map<Class, RequestScope.Value> values = null;
    Timer timer = null;
    if (Timing.isEnabled()) {
      values = Timing.cloneScopeValues();
      ExecutionTree tree = (ExecutionTree)values.get(ExecutionTree.class);
      if (tree != null) {
        timer = tree.start(name);
      }
    }
    return new AsyncCallContext(values, timer);
  }

  /**
   * Enters to cloned scope and stop timer.
   */
  public void stop() {
    if (timer != null && timer.isActive()) {
      Timing.enterScope(values);
      Timing.stop(timer);
    }
  }
}
