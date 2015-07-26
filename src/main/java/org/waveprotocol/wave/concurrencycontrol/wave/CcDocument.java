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

package org.waveprotocol.wave.concurrencycontrol.wave;

import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;

/**
 * A document that can be flushed.
 *
 * NOTE(user): the flush mechanism, which is the source of the requirement
 *   that Cc-controlled documents be flushable, is a bit artificial, and may
 *   disappear in the future.
 *
 */
public interface CcDocument extends DocumentOperationSink {
  /**
   * Brings this document a consistent state.
   *
   * "Consistent" means that the document state includes the effects
   * of all local actions before now, and that that state is consistent with
   * client operations that have been received by CC (i.e., no operations are
   * buffered).
   *
   * This should be called immediately prior to
   * {@link #consume(org.waveprotocol.wave.model.document.operation.DocOp)}.
   *
   * @param resume  if the document is not in a consistent state,
   *                a callback to fire as soon as consistency is reached.
   * @return true if the document is in a consistent state, false
   *         otherwise (note: {@code resume} is not called if this method
   *         returns true).
   */
  boolean flush(Runnable resume);
}
