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

package org.waveprotocol.wave.client.editor.debug;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import org.waveprotocol.wave.client.common.util.OffsetPosition;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;

/**
 * Visual indication of selection in terms of offsetTop, offsetLeft for debug
 * purposes.
 *
 *
 */
class CursorDisplay implements EditorUpdateEvent.EditorUpdateListener {
  private final DivElement DEBUG_CURSOR = Document.get().createDivElement();
  private boolean enabled = false;
  private final EditorImpl editorImpl;

  CursorDisplay(EditorImpl editorImpl) {
    this.editorImpl = editorImpl;
    DEBUG_CURSOR.setInnerText("o");
    DEBUG_CURSOR.getStyle().setZIndex(1000);
    DEBUG_CURSOR.getStyle().setPosition(Position.ABSOLUTE);
  }

  /**
   * @return true iff the cursor is enabled.
   */
  boolean getEnabled() {
    return enabled ;
  }

  /**
   * Turn on/off the cursor display.
   * @param enabled
   */
  void setEnabled(boolean enabled) {
    if (enabled) {
      editorImpl.addUpdateListener(this);
    } else {
      editorImpl.removeUpdateListener(this);
      DEBUG_CURSOR.removeFromParent();
    }
    this.enabled = enabled;
  }

  @Override
  public void onUpdate(EditorUpdateEvent event) {
    OffsetPosition p = NativeSelectionUtil.slowGetPosition();
    if (p != null) {
      EditorStaticDeps.logger.trace().log("x: " + p.left + "y: " + p.top);

      if (p.offsetParent == null) {
        // If offsetParent is null, interpret offsetX and offsetY as
        // absolute positions
        if (DEBUG_CURSOR.getParentElement() != Document.get().getBody()) {
          Document.get().getBody().appendChild(DEBUG_CURSOR);
        }
        DEBUG_CURSOR.getStyle().setTop(p.top - Document.get().getBody().getAbsoluteTop(), Unit.PX);
        DEBUG_CURSOR.getStyle().setLeft(p.left - Document.get().getBody().getAbsoluteLeft(),
                                        Unit.PX);
      } else {
        if (editorImpl.getElement() != null
            && DEBUG_CURSOR.getParentElement() != editorImpl.getElement()) {
          editorImpl.getElement().appendChild(DEBUG_CURSOR);
        }
        DEBUG_CURSOR.getStyle().setTop(p.top, Unit.PX);
        DEBUG_CURSOR.getStyle().setLeft(p.left, Unit.PX);
      }
    }
  }
}
