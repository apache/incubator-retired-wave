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
import com.google.gwt.dom.client.Node;

/**
 * Wrapper for w3c Range.
 *
 * http://www.w3.org/TR/DOM-Level-2-Traversal-Range/ranges.html
 *
 */
public final class JsRange extends JavaScriptObject {

  protected JsRange() {
  }

  /**
   * Creates a JsRange object
   */
  public static native JsRange create() /*-{
    return $doc.createRange();
  }-*/;

  /**
   * Set start of range before given node.
   * @param node
   */
  public final native void setStartBefore(Node node) /*-{
    this.setStartBefore(node);
  }-*/;

  /**
   * Set start of range after given node.
   * @param node
   */
  public final native void setStartAfter(Node node) /*-{
    this.setStartAfter(node);
  }-*/;

  /**
   * Set end of range before given node.
   * @param node
   */
  public final native void setEndBefore(Node node) /*-{
    this.setEndBefore(node);
  }-*/;

  /**
   * Set end of range after given node.
   * @param node
   */
  public final native void setEndAfter(Node node) /*-{
    this.setEndAfter(node);
  }-*/;

  /**
   * Collapse the range
   * @param toStart  if true, collapse to start, otherwise collapse to end.
   */
  public final native void collapse(boolean toStart) /*-{
    this.collapse(toStart);
  }-*/;

  /**
   * Set start of range inside parent with given offset.
   * @param parent
   * @param offset
   */
  public final native void setStart(Node parent, int offset) /*-{
    this.setStart(parent, offset);
  }-*/;

  /**
   * Set end of range inside parent with given offset.
   * @param parent
   * @param offset
   */
  public final native void setEnd(Node parent, int offset) /*-{
    this.setEnd(parent, offset);
  }-*/;

  /**
   * Returns the start container.
   */
  public final native Node startContainer() /*-{
    return this.startContainer;
  }-*/;

  /**
   * Returns the start offset.
   */
  public final native int startOffset() /*-{
    return this.startOffset;
  }-*/;

  /**
   * Returns the end container.
   */
  public final native Node endContainer() /*-{
    return this.endContainer;
  }-*/;

  /**
   * Returns the end offset.
   */
  public final native int endOffset() /*-{
    return this.endOffset;
  }-*/;
}
