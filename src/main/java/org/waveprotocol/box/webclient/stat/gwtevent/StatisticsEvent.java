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
 * Java "Overlay" object of the JavaScript event objects fired by the stats
 * system.
 */
public interface StatisticsEvent {

  /**
   * Answers with the name of the module that caused this event.
   */
  public String getModuleName();

  /**
   * Answers with the name of the sub system (rpc, boot strap, etc..) that
   * caused this event.
   */
  public String getSubSystem();

  /**
   * Answers with a key unique to the group of events that this event belongs
   * to. Along with the module and sub system this forms a unique key for
   * timing related events. Note that each group requires at least two events:
   * one to signal the start of the measured period and one to signal the end.
   */
  public String getEventGroupKey();

  /**
   * Answers with the time stamp (millis since the epoch) at which this event
   * occurred. Using double, since long is not natively supported in
   * JavaScript (see {@link com.google.gwt.core.client.Duration}).
   */
  public double getMillis();

  /**
   * Answers with a read-only iterator over the names of any extra parameters
   * associated with this event.
   */
  public Iterator<String> getExtraParameterNames();

  /**
   * Answers with the given named extra parameter. Since most events are fired
   * from within JavaScript or another module, the returned value is either a
   * String, Double, Boolean or a JavaScriptObject.
   */
  public Object getExtraParameter(String name);
}
