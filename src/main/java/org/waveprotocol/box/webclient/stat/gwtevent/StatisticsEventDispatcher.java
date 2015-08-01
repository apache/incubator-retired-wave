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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Helps with creating and dispatching the events for the debug panel.
 */
public interface StatisticsEventDispatcher {

  /**
   * Returns {@code true} if there is somebody listening to events on the other
   * side.
   */
  public boolean enabled();

  /**
   * Creates a new event object to be dispatched.

   * @see StatisticsEvent
   */
  public StatisticsEvent newEvent(String system, String group, double millis, String type);

  /**
   * Sets the given extra parameter's value as a JavaScriptObject.
   *
   * @param event the event to set the parameter on. This has to be an instance
   *        returned by @{link {@link #newEvent(String, String, double, String)}.
   */
  public void setExtraParameter(StatisticsEvent event, String name, JavaScriptObject value);

  /**
   * Sets the given extra parameter's value as a String.
   *
   * @param event the event to set the parameter on. This has to be an instance
   *        returned by @{link {@link #newEvent(String, String, double, String)}.
   */
  public void setExtraParameter(StatisticsEvent event, String name, String value);

  /**
   * Dispatches the given event.
   *
   * @param event the event to dispatch. This has to be an instance
   *        returned by @{link {@link #newEvent(String, String, double, String)}.
   */
  public void dispatch(StatisticsEvent event);
}
