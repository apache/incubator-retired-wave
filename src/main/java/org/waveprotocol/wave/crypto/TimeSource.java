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

package org.waveprotocol.wave.crypto;

import java.util.Date;

/**
 * Interface for obtaining the current time. We extract this interface for
 * easier testing. There should be no need to replace the default implementation
 * of this interface, except for testing.
 */
public interface TimeSource {

  /**
   * Returns a {@link Date} representing the current time.
   */
  public Date now();

  /**
   * Returns the current time in milliseconds sice the epoch UTC
   */
  public long currentTimeMillis();
}
