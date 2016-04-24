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



/**
 * Bridge class to take care of some of the more annoying type conversion
 * related to documents.
 *
 * NOTE(user): This two-step unravelling of re-quantifying the
 * type-parameters into type variables is only required in order to coax
 * javac's dumb generic-inference to realise that the call is type-safe.
 * Eclipse's compiler is smarter, and does not require this extra work.
 *
 */
public final class DocumentUnwrapper {

  /** Helper interface that encapsulates an action */
  public interface Action<R> {

    /** Method to make use of the revised type */
    <N, E extends N, T extends N> R handle(MutableDocument<N, E, T> doc);
  }

  /**
   * Invoke the supplied action on an appropriately typed document
   *
   * @param action The action object to invoke
   * @param doc The document to apply the action to
   * @return The result of performing the action.
   */
  public static <R, N> R invoke(Action<R> action, MutableDocument<N, ?, ?> doc) {
    return action.handle(doc);
  }

  /** private constructor */
  private DocumentUnwrapper() {
  }
}
