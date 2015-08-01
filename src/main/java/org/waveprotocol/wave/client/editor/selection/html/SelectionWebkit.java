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

/**
 * Webkit specific extensions to the Selection interface.
 *
 */
public class SelectionWebkit extends SelectionW3CNative {
  /**
   * Direction to move the selection.
   */
  public enum Direction {
    FORWARD("forward"),
    BACKWARD("backward"),
    LEFT("left"),
    RIGHT("right");

    String direction;
    Direction(String direction) {
      this.direction = direction;
    }
  }

  /**
   * Movement granularity.
   *
   * This class is low-level and DOM specific. The move units here directly
   * correspond to the DOM move units, LineContainer move units are higher level
   * abstractions.
   *
   * TODO(user): Should this be unified with SignalEvent's MoveUnit
   */
  public enum MoveUnit {
    CHARACTER("character"),
    WORD("word"),
    SENTENCE("sentence"),
    LINE("line"),
    PARAGRAPH("paragraph");

    String moveUnit;
    MoveUnit(String moveUnit) {
      this.moveUnit = moveUnit;
    }
  }

  /**
   * Gets the current selection object.
   */
  public static SelectionWebkit getSelection() {
    return SelectionW3CNative.getSelectionGuarded().cast();
  }

  protected SelectionWebkit() {
  }

  private final native void modify(String action, String direction, String moveUnit) /*-{
    this.modify(action, direction, moveUnit);
  }-*/;

  /**
   * Move the selection in the specified direction and move unit. Note, this
   * moves the copy of the selection object, but does not change the location of
   * the selection in the browser.
   *
   * @param direction
   * @param moveUnit
   */
  public final void move(Direction direction, MoveUnit moveUnit) {
    modify("move",  direction.toString(), moveUnit.toString());
  }

  /**
   * Extends the selection in the specified direction and move unit. Note, this
   * moves the copy of the selection object, but does not change the location of
   * the selection in the browser.
   *
   * @param direction
   * @param moveUnit
   */
  public final void extend(Direction direction, MoveUnit moveUnit) {
    modify("extend",  direction.toString(), moveUnit.toString());
  }
}
