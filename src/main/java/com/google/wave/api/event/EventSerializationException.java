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

import com.google.gson.JsonObject;

/**
 * Checked exception for signaling problem serializing or deserializing event
 * JSON.
 */
public class EventSerializationException extends Exception {

  /** The event JSON object that couldn't be deserialized. */
  private final JsonObject eventJson;

  /**
   * Constructor.
   *
   * @param message the exception message.
   * @param eventJson the event JSON.
   */
  public EventSerializationException(String message, JsonObject eventJson) {
    super(message);
    this.eventJson = eventJson;
  }

  /**
   * Constructor.
   *
   * @param message the exception message.
   */
  public EventSerializationException(String message) {
    super(message);
    this.eventJson = null;
  }

  /**
   * Returns the event JSON.
   *
   * @return the event JSON.
   */
  public JsonObject getEventJson() {
    return eventJson;
  }
}
