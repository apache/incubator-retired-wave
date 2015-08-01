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

package com.google.wave.api;

import org.waveprotocol.wave.model.id.DualIdSerialiser;
import org.waveprotocol.wave.model.id.IdSerialiser;

/**
 * Serializer for ids transmitted as part of the API.
 *
 * @author anorth@google.com (Alex North)
 */
public final class ApiIdSerializer {

  public static IdSerialiser instance() {
    return DualIdSerialiser.MODERN;
  }

  private ApiIdSerializer() {
  }
}