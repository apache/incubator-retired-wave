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

package org.waveprotocol.wave.client.doodad.experimental.htmltemplate;


/**
 * Singleton implementation of {@code PartIdFactory} which establishes
 * uniqueness based on static state and the Wave server-generated session ID.
 *
 * @author ihab@google.com (Ihab Awad)
 */
final class SessionPartIdFactory implements PartIdFactory {
  private static int counter = 0;

  private static final PartIdFactory instance = new SessionPartIdFactory();

  private SessionPartIdFactory() {
  }

  public static PartIdFactory get() {
    return instance;
  }

  @Override
  public String getNextPartId() {
    return "seed" + counter++;
  }
}
