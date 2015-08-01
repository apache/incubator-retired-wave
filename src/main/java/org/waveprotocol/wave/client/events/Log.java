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

package org.waveprotocol.wave.client.events;

import org.waveprotocol.wave.client.events.DebugMessageEvent.Severity;

/**
 * Created by IntelliJ IDEA. User: arb Date: May 13, 2010 Time: 9:35:04 PM To
 * change this template use File | Settings | File Templates.
 */
public class Log {
  public Log(Class<? extends Object> clazz) {
  }

  public static Log get(Class<? extends Object> clazz) {
    return new Log(clazz);
  }

  public void info(String message) {
    ClientEvents.get().fireEvent(
        new DebugMessageEvent(Severity.INFO, message, null));
  }

  public void severe(String message) {
    ClientEvents.get().fireEvent(
        new DebugMessageEvent(Severity.SEVERE, message, null));
  }

  public void severe(String message, Throwable t) {
    ClientEvents.get().fireEvent(
        new DebugMessageEvent(Severity.SEVERE, message, t));
  }
}
