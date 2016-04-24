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
 * The listener interface for the atmosphere client wrapper.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface AtmosphereConnectionListener {

  /**
   * Called when a successful connection with server is performed
   */
  void onConnect();

  /**
   * Called when connection is closed properly or by error
   */
  void onDisconnect();

  /**
   * Called when a new message is received from the server
   * @param message
   */
  void onMessage(String message);



}
