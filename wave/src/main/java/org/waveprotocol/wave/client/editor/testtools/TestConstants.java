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

package org.waveprotocol.wave.client.editor.testtools;

/**
 * Common constants to share between code and webdriver tests
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class TestConstants {

  /** id for the top level selector between editor or event harness */
  public static final String PAGE_MODE_SELECTOR = "page-mode-selector";
  /** prefix for event info spans */

  public enum EventInfo {
    /** event info span classname suffix for raw event type */
    TYPE,
    /** event info span classname suffix for key signal type */
    KEYSIGNAL,
    /** event info span classname suffix for keycode */
    KEYCODE,
    GETSHIFT,
    GETALT,
    GETCTRL,
    GETMETA,
    GETCOMMAND;

    private static final String EVENT_INFO_PREFIX = "event-info-";

    public String className() {
      return EVENT_INFO_PREFIX + toString().toLowerCase();
    }
  }
  /** button to clear the event log */
  public static final String CLEAR_EVENT_LOG = "clear-event-log";
  /** log where signal information goes */
  public static final String EVENT_SIGNAL_LOG = "event-signal-log";
  /** textarea where events should be sent */
  public static final String EVENT_INPUT = "event-input";


  private TestConstants() {}
}
