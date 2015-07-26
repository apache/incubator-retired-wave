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
 * A service that "cajoles" an HTML page referenced via a URL.
 */
public interface CajoleService {

  /** The response from the cajoer. */
  public interface CajolerResponse {
    // TODO: Consider using SafeHtml.
    String getHtml();
    String getJs();
  }

  /**
   * Requests that the content reference by a URL be cajoled.
   */
  void cajole(String url, Callback<? super CajolerResponse> callback);
}