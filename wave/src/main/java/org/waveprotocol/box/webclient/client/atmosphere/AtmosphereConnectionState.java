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

package org.waveprotocol.box.webclient.client.atmosphere;

/**
 * Connection state definition inspired by former SocketIO/WebSocket
 * implementation
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public enum AtmosphereConnectionState {

  MESSAGE_RECEIVED("messageReceived"),
 MESSAGE_PUBLISHED("messagePublished"), OPENED("opened"),
  CLOSED("closed"), UNKNOWN("unknown");

    private String value;
    private AtmosphereConnectionState(String v) { this.value = v; }
    public String value() { return value; }

    public static AtmosphereConnectionState fromString(String val) {

    if (val.equals("messageReceived")) {
      return MESSAGE_RECEIVED;
    } else if (val.equals("messagePublished")) {
      return MESSAGE_PUBLISHED;
    } else if (val.equals("opened")) {
      return OPENED;
    } else if (val.equals("closed")) {
      return CLOSED;
    }

        return UNKNOWN;
    }
}
