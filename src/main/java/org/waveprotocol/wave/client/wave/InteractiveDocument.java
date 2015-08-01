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


package org.waveprotocol.wave.client.wave;

import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.Registries;

/**
 * A document implementation that is suitable for use in an interactive client.
 * An interactive document has three primary concerns:
 * <ul>
 * <li>it reveals a {@link ContentDocument}, for sophisticated document
 * rendering;</li>
 * <li>it exposes rendering control, to start and stop live rendering; and</li>
 * <li>it exposes reading control (diff highlighting).</li>
 * </ul>
 *
 * @author hearnden@google.com (David Hearnden)
 */
public interface InteractiveDocument {
  /**
   * @return the document implementation.
   */
  ContentDocument getDocument();

  /**
   * Renders this document, keeping the rendering live until
   * {@link #stopRendering() stopped}.
   *
   * @param registries rendering definitions and event handlers for doodads.
   * @param panel parent for adoption of GWT widgets that may occur in doodads.
   */
  void startRendering(Registries registries, LogicalPanel panel);

  /**
   * Stops rendering this document.
   */
  void stopRendering();

  /**
   * Enters a suppression scope. Diffs are cleared, and diffs are not shown
   * until {@link #stopDiffSuppression() exited}. This scope may be entered
   * while in a retention scope.
   * <p>
   * More concretely, a suppression scope corresponds to an editing session.
   *
   * @throws IllegalStateException if currently in a suppression scope.
   */
  void startDiffSuppression();

  /**
   * Leaves the suppression scope.
   *
   * @throws IllegalStateException if not in a suppression scope.
   */
  void stopDiffSuppression();

  /**
   * Enters a retention scope. Calls to {@link #clearDiffs} will have no
   * effect until {@link #stopDiffRetention() exited}.
   * <p>
   * More concretely, a retention scope corresponds to a reading session.
   *
   * @throws IllegalStateException if currently in a retention scope.
   */
  void startDiffRetention();

  /**
   * Leaves the retention scope, undoing the effect of the last call to
   * {@link #startDiffRetention}.
   *
   * @throws IllegalStateException if not in a retention scope.
   */
  void stopDiffRetention();

  /** Collapses any diff state. */
  void clearDiffs();

  /** @return true if this document has no state other than diff state. */
  boolean isCompleteDiff();
}
