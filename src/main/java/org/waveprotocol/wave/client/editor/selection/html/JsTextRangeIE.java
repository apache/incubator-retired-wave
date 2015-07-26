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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

/**
 * Represents an IE TextRange
 *
 * All the interface methods directly map to those of the IE TextRange,
 * except some originally void methods return the range for convenience
 *
 * See the MS docos for details:
 *
 * http://msdn2.microsoft.com/en-us/library/ms533042(VS.85).aspx
 *
 * Add more methods as needed.
 *
 * TODO(danilatos): Put this somewhere common.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * NOTE NOTE NOTE(lars): use with care. See comments in
 * patches/com/google/gwt/core/client/JavaScriptObject#toString
 */
public class JsTextRangeIE extends JavaScriptObject {

  /**
   * Units to move, used by certain methods.
   *
   * Do not change the spelling or capitalisation of these, because
   * their name() method depends on it.
   */
  public enum MoveUnit {
    /** Move by character */
    character,
    /** Move by word */
    word,
    /** Move by sentence */
    sentence,
    /** Move to start or end of original range */
    textedit,
    ;
  }

  /**
   * Units to move, used by certain methods.
   *
   * Do not change the spelling or capitalisation of these, because
   * their name() method depends on it.
   */
  public enum CompareMode {
    /** */
    StartToStart,
    /** Move by word */
    StartToEnd,
    /** Move by sentence */
    EndToStart,
    /** Move to start or end of original range */
    EndToEnd,
    ;
  }

  protected JsTextRangeIE() { }

  /**
   * @return a new text range on $doc's body
   */
  public static native JsTextRangeIE create() /*-{
    return $doc.body.createTextRange();
  }-*/;

  /**
   * Set range to encompass given element
   * @param element
   * @return this
   */
  public final native JsTextRangeIE moveToElementText(Element element) /*-{
    this.moveToElementText(element);
    return this;
  }-*/;

  /**
   * Collapse the range
   * @param toStart If true, to the start point. Otherwise, to the end point.
   * @return this
   */
  public final native JsTextRangeIE collapse(boolean toStart) /*-{
    this.collapse(toStart);
    return this;
  }-*/;

  /**
   * Collapse and move by given amount
   * @param unit Unit of movement, e.g. "character"
   * @param amount
   * @return this
   */
  public final native int move(MoveUnit unit, int amount) /*-{
    return this.move(unit.@java.lang.Enum::name()(), amount);
  }-*/;

  /**
   * Move end of range by given amount
   * @param unit
   * @param amount
   * @return this
   */
  public final native int moveEnd(MoveUnit unit, int amount) /*-{
    return this.moveEnd(unit.@java.lang.Enum::name()(), amount);
  }-*/;

  /**
   * Set our specified end point to other text range's specified end point.
   * @param mode Which end points to match, e.g. "StartToStart" or "EndToStart", etc.
   * @param other Other text range
   * @return this
   */
  public final native JsTextRangeIE setEndPoint(
      CompareMode mode, JsTextRangeIE other) /*-{
    this.setEndPoint(mode.@java.lang.Enum::name()(), other);
    return this;
  }-*/;

  /**
   *
   * @param mode Which end points to compare
   * @param other
   * @return standard comparison style int
   */
  public final native int compareEndPoints(
      CompareMode mode, JsTextRangeIE other) /*-{
    return this.compareEndPoints(mode.@java.lang.Enum::name()(), other);
  }-*/;

  /**
   * HTML of the range as a valid HTML fragment
   * @return value of the htmlText property
   */
  public final native String getHtmlText() /*-{
    return this.htmlText;
  }-*/;

  /**
   * @return .text property of underlying js object
   */
  public final native String getText() /*-{
    return this.text;
  }-*/;

  /**
   * Set the user's selection to this text range
   * @return this
   */
  public final native JsTextRangeIE select() /*-{
    this.select();
    return this;
  }-*/;

  /**
   * Pastes an HTML string into range
   *
   * @param html
   * @return this
   */
  public final native JsTextRangeIE pasteHTML(String html) /*-{
    this.pasteHTML(html);
    return this;
  }-*/;

  /**
   * @return A copy
   */
  public final native JsTextRangeIE duplicate() /*-{
    return this.duplicate();
  }-*/;

  /**
   * @return The parentElement() for the TextRange
   */
  public final native Element parentElement() /*-{
    return this.parentElement();
  }-*/;

  /**
   * Executes command on the range.
   * @param command the command to execute.
   * @return this
   */
  public final native JsTextRangeIE execCommand(String command) /*-{
    this.execCommand(command);
    return this;
  }-*/;

  /**
   * @param range
   * @return true if this equals range
   */
  public final native boolean isEqual(JsTextRangeIE range) /*-{
    return this.isEqual(range);
  }-*/;

  /**
   * The calculated left position of the object relative to its offset parent
   * @return offset left position in px;
   */
  public final native int getOffsetLeft() /*-{
    return this.offsetLeft;
  }-*/;

  /**
   * The calculated top position of the object relative to its offset parent
   * @return offset top position in px;
   */
  public final native int getOffsetTop() /*-{
    return this.offsetTop;
  }-*/;

  /**
   * Test if two text ranges are equivalent
   * @param a Range 1
   * @param b Range 2
   * @return true if they are equivalent
   */
  public static boolean equivalent(JsTextRangeIE a, JsTextRangeIE b) {
    return a.compareEndPoints(CompareMode.StartToStart, b) == 0
        && a.compareEndPoints(CompareMode.EndToEnd, b) == 0;
  }

  /**
   * @return the opaque bookmark representing this range
   */
  public final native String getBookmark() /*-{
    return this.getBookmark();
  }-*/;

  /**
   * Set the range to the saved bookmark
   * @param bookmark
   */
  public final native void moveToBookmark(String bookmark) /*-{
    this.moveToBookmark(bookmark);
  }-*/;
}
