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

import com.google.wave.api.Wavelet;
import com.google.wave.api.impl.EventMessageBundle;

/**
 * Event triggered when an error on the server occurred when processing
 * operations.
 */
public class OperationErrorEvent extends AbstractEvent {

  /** The id of the operation that caused the error. */
  private final String operationId;

  /** The error message. */
  private final String message;

  /**
   * Constructor.
   *
   * @param wavelet the wavelet where this event occurred.
   * @param bundle the message bundle this event belongs to.
   * @param modifiedBy the id of the participant that triggered this event.
   * @param timestamp the timestamp of this event.
   * @param operationId the id of the operation that caused the error.
   * @param message the error message.
   */
  public OperationErrorEvent(Wavelet wavelet, EventMessageBundle bundle, String modifiedBy,
      Long timestamp, String operationId, String message) {
    super(EventType.OPERATION_ERROR, wavelet, bundle, modifiedBy, timestamp, null);
    this.operationId = operationId;
    this.message = message;
  }

  /**
   * Constructor for deserialization.
   */
  OperationErrorEvent() {
    this.operationId = null;
    this.message = null;
  }

  /**
   * Returns the id of the operation that caused the error.
   *
   * @return the id of the operation that caused the error.
   */
  public String getOperationId() {
    return operationId;
  }

  /**
   * Returns the error message.
   *
   * @return the error message.
   */
  public String getMessage() {
    return message;
  }

  /**
   * Helper method for type conversion.
   *
   * @return the concrete type of this event, or {@code null} if it is of a
   *     different event type.
   */
  public static OperationErrorEvent as(Event event) {
    if (!(event instanceof OperationErrorEvent)) {
      return null;
    }
    return OperationErrorEvent.class.cast(event);
  }
}
