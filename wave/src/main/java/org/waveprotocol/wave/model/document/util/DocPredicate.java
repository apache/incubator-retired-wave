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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;

/**
 * A predicate on document nodes, reusable independently
 * of the specific node implementations
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface DocPredicate {

  /**
   * @param doc document for the given node
   * @param node node the predicate is to be applied on
   * @return the result of this predicate on the node
   */
  <N, E extends N, T extends N> boolean apply(ReadableDocument<N, E, T> doc, N node);
}
