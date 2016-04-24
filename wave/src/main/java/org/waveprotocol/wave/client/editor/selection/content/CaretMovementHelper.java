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

package org.waveprotocol.wave.client.editor.selection.content;

import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Interface for caret navigation.
 *
 * Finds position of caret movement relative to the current selection.
 * TODO(user): add methods for finding line/sentence boundaries.
 *
 */
public interface CaretMovementHelper {
  /**
   * Finds the next word boundary.
   *
   * @param forward If true, find the next word boundary from selection,
   *        otherwise find the previous word boundary.
   */
  Point<ContentNode> getWordBoundary(boolean forward);
}
