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

package org.waveprotocol.wave.model.document.operation;

import org.waveprotocol.wave.model.document.operation.util.UpdateMap;

import java.util.Collection;

/**
 * A mutation of an <code>Attributes</code> object. This is a map from attribute
 * names to attribute values, indicating the attributes whose values should be
 * changed and what values they should be assigned, where a null value indicates
 * that the corresponding attribute should be removed. The entries in the map
 * are sorted by the attribute names, so that the set obtained from entrySet()
 * will allow you to easily iterate through the entries in sorted order.
 *
 * Implementations must be immutable.
 */
public interface AttributesUpdate extends UpdateMap {
  public AttributesUpdate composeWith(AttributesUpdate mutation);
  public AttributesUpdate exclude(Collection<String> keys);
}
