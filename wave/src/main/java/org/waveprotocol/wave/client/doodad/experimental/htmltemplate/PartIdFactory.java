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
 * Constructs a new identifier for a newly created <part> object. Responsible for ensuring that the
 * part IDs are unique and site-specific, so that, if a given wave is being viewed in two sites, and
 * a new part is created in each site at the same time, the part IDs are distinct.
 *
 * @author ihab@google.com (Ihab Awad)
 *
 */
public interface PartIdFactory {

  /**
   * Manufacture and return a new <part> identifier.
   *
   * @return an identifier.
   */
  String getNextPartId();
}
