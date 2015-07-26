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

package com.google.wave.api.event;

import com.google.wave.api.Blip;
import com.google.wave.api.Wavelet;
import com.google.wave.api.impl.EventMessageBundle;

/**
 * Object describing a single event, that captures changes made to a
 * wavelet or blip in a wave.
 */
public interface Event {

  /**
   * Returns the type of the event.
   *
   * @return the type of the event.
   */
  EventType getType();

  /**
   * Returns the wavelet in which this event occurs.
   *
   * @return the wavelet in which this event occurs.
   */
  Wavelet getWavelet();

  /**
   * Returns the blip in which this event occurs, or the root blip for a wavelet
   * event.
   *
   * @return the blip in which this event occurs, or the root blip.
   */
  Blip getBlip();

  /**
   * Returns the id of the participant that triggered this event.
   *
   * @return the id of the participant that triggered this event.
   */
  String getModifiedBy();

  /**
   * Returns the timestamp when this event occurred on the server.
   *
   * @return the timestamp of the event.
   */
  long getTimestamp();

  /**
   * Returns the message bundle which this event belongs to.
   *
   * @return the message bundle object.
   */
  EventMessageBundle getBundle();
}
