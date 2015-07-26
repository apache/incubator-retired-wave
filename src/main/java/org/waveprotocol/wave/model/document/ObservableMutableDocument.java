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

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.document.indexed.DocumentHandler;
import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * A mutable document to which event handlers can be attached.
 *
 */
public interface ObservableMutableDocument<N, E extends N, T extends N> extends
    MutableDocument<N, E, T>, SourcesEvents<DocumentHandler<N, E, T>> {

  public interface Action {
    /**
     * Runs the action with the given document.
     *
     * The document may only be assumed to be valid only during the method's
     * execution.
     */
    <N, E extends N, T extends N> void exec(ObservableMutableDocument<N, E, T> doc);
  }

  public interface Method<V> {
    /**
     * Runs the method with the given document.
     *
     * The document may only be assumed to be valid only during the method's
     * execution.
     */
    <N, E extends N, T extends N> V exec(ObservableMutableDocument<N, E, T> doc);
  }

  // Overload with() to perform actions on observable documents.

  public void with(Action actionToRunWithObservableMutableDocument);
  public <V> V with(Method<V> methodToRunWithMutableDocument);
}
