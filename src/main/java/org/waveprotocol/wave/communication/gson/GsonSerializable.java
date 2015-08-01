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

package org.waveprotocol.wave.communication.gson;

import org.waveprotocol.wave.communication.json.RawStringData;

import com.google.gson.Gson;
import com.google.gson.JsonElement;


/**
 * An interface adapter used to enable serialization via Gson.
 *
 */
public interface GsonSerializable  {

  /**
   * Serialize the current object using the provided context
   *
   * @param data The RawStringData object used to store the additional string
   *        needed for multistage parsing. If {@code null} is used,
   *        multistage output will not be generated.
   * @param gson Gson object used to convert the JsonElement into string. If {@code null} is used,
   *        multistage output will not be generated.
   * @return A tree of JsonElements
   */
  JsonElement toGson(RawStringData data, Gson gson);

  /**
   * Deserialize the provided JsonElements into the current object
   *
   * @param json The provided JSON tree
   * @param gson The gson used to parse string into JsonElement
   * @param data The RawStringData object that contains the additional data
   *        needed for multistage parsing. Can be {@code null} if multiple
   *        stage parsing is not needed.
   */
  void fromGson(JsonElement json, Gson gson, RawStringData data) throws GsonException;
}
