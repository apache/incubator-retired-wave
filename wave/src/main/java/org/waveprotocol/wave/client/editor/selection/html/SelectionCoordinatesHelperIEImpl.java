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

package org.waveprotocol.wave.client.editor.selection.html;

import org.waveprotocol.wave.client.common.util.OffsetPosition;
import org.waveprotocol.wave.model.util.IntRange;

/**
 * IE implementation of SelectionCoordinatesHelper
 *
 */
public class SelectionCoordinatesHelperIEImpl implements SelectionCoordinatesHelper {

  @Override
  public OffsetPosition getNearestElementPosition() {
    final JsTextRangeIE textRange = JsSelectionIE.get().createRange();
    return new OffsetPosition(textRange.parentElement().getOffsetLeft(), textRange.parentElement()
        .getOffsetTop(), textRange.parentElement() == null ? null : textRange.parentElement()
        .getOffsetParent());
  }


  @Override
  public OffsetPosition getFocusPosition() {
    final JsTextRangeIE textRange = JsSelectionIE.get().createRange();
    if (textRange == null) {
      return null;
    } else {
      return new OffsetPosition(textRange.getOffsetLeft(), textRange.getOffsetTop(), null);
    }
  }

  @Override
  public OffsetPosition getAnchorPosition() {
    // Returning the focus for the anchor will make it look like the selection
    // is collapsed which should be a fairly safe fallback behavior when there
    // is no good way to determine the actual focus.
    return getFocusPosition();
  }

  @Override
  public IntRange getFocusBounds() {
    // NOTE(user): Consider returning the bottom here as well in the range.
    return new IntRange(getFocusPosition().top);
  }
}
