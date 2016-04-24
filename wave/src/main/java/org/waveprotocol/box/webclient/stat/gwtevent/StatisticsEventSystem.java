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
package org.waveprotocol.box.webclient.stat.gwtevent;

import java.util.Iterator;

/**
 * The statistics event system is responsible for managing the statistics
 * {@link StatisticsEventListener event listeners} and dispatching the
 * {@link StatisticsEvent events}.
 */
public interface StatisticsEventSystem {

  /**
   * Register a listener to receive future {@link StatisticsEvent events}.
   *
   * @param replay if true, past (recorded) events will be replayed on the
   *               given listener.
   */
  public void addListener(StatisticsEventListener listener, boolean replay);

  /**
   * Removes a listener so it will no longer receive any
   * {@link StatisticsEvent events}.
   */
  public void removeListener(StatisticsEventListener listener);

  /**
   * Provides read-only access to all the recorded events.
   */
  public Iterator<StatisticsEvent> pastEvents();

  /**
   * Clears the event history.
   */
  public void clearEventHistory();
}
