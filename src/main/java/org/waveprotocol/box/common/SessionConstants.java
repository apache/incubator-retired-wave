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

package org.waveprotocol.box.common;

/**
 * Session constants for FedOne clients.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public interface SessionConstants {

  /**
   * The domain the wave server serves waves for.
   */
  public final static String DOMAIN = "domain";

  /**
   * The user's logged in address.
   */
  public final static String ADDRESS = "address";

  /**
   * A globally unique id that the client can use to seed unique ids of the session.
   * It has no relationship with the application session or http session or authentication, and
   * is not guaranteed to be cryptographically strong.
   */
  public final static String ID_SEED = "id";
}
