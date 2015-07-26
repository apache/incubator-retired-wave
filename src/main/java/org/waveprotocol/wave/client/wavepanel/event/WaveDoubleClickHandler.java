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


package org.waveprotocol.wave.client.wavepanel.event;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.DoubleClickEvent;

/**
 * A handler of a double-click event.
 *
 */
public interface WaveDoubleClickHandler {

  /**
   * Handles a double-click event.
   *
   * @param event The event object from the browser.
   * @param context The target element whose kind is handled by this handler
   * @return true if the event is handled and should not continue to bubble,
   *         false otherwise.
   */
  boolean onDoubleClick(DoubleClickEvent event, Element context);
}
