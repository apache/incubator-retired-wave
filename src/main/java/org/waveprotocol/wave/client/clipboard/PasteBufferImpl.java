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

package org.waveprotocol.wave.client.clipboard;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.common.util.QuirksConstants;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;

import org.waveprotocol.wave.model.document.util.Point;

/**
 * Provides an offscreen, HTML paste buffer for various browsers.
 *
 */
public class PasteBufferImpl {

  /**
   * The actual element that contains the paste.
   */
  protected Element element = null;

  private static final boolean SHOW_DEBUG_PASTEBUFFER = false;

  /**
   * Factory constructor, creates and attaches the buffer to the DOM.
   *
   * @return Browser specific implementation of a paste buffer.
   */
  static PasteBufferImpl create() {
    PasteBufferImpl pasteBuffer;

    if (UserAgent.isSafari() || QuirksConstants.FIREFOX_GREATER_THAN_VER_15) {
      pasteBuffer = new PasteBufferImplSafariAndNewFirefox();
    } else if (UserAgent.isFirefox() && !QuirksConstants.SANITIZES_PASTED_CONTENT) {
      // Older versions of firefox doesn't sanitize pasted content and requires the
      // paste buffer to be an iframe to prevent XSS.
      pasteBuffer = new PasteBufferImplOldFirefox();
    } else {
      pasteBuffer = new PasteBufferImpl();
    }

    pasteBuffer.setupDom();
    return pasteBuffer;
  }

  /**
   * Empty protected constructor. Use static create for instantiation.
   */
  protected PasteBufferImpl() {
  }

  /**
   * Clears and sets the content of the paste element.
   *
   * @param node The DOM to append.
   */
  void setContent(Node node) {
    element.setInnerHTML("");
    element.appendChild(node);
  }

  /**
   * Use this to get the root level container.
   *
   * @return The offscreen element.
   */
  public Element getContainer() {
    return element;
  }

  /**
   * Use this to get the paste contents (from innerHTML).
   *
   * @return The pasted contents.
   */
  public Element getPasteContainer() {
    return element;
  }

  /**
   * Prepare the buffer to accept a paste event by setting the selection and
   * focus to the container.
   */
  public void prepareForPaste() {
    element.setInnerHTML("");
    element.appendChild(Document.get().createTextNode(""));

    NativeSelectionUtil.setCaret(Point.inText(element.getFirstChild(), 0));
  }

  protected void positionPasteBuffer(Element element) {
    if (SHOW_DEBUG_PASTEBUFFER) {
      element.getStyle().setPosition(Position.ABSOLUTE);
      element.getStyle().setHeight(150, Unit.PX);
      element.getStyle().setLeft(1000, Unit.PX);
      element.getStyle().setTop(10, Unit.PX);
    } else {
      element.getStyle().setPosition(Position.ABSOLUTE);
      element.getStyle().setTop(-100, Unit.PX); // arbitrary numbers
      element.getStyle().setHeight(50, Unit.PX);
    }
  }

  /**
   * Sets up the PasteBuffer DOM.
   *
   * Implementations should call positionPasteBuffer
   */
  protected void setupDom() {
    element = Document.get().createDivElement();
    // For some reason setting display to none prevents this trick from working
    // instead, we move it away from view, so it's still "visible"
    // NOTE(user): We setwhitespace pre-wrap prevents the browser from
    // collapsing sequences of whitespace. This is important to ensure that the
    // spaces after a start tag, or before an end tag are preserved through copy/paste.
    // Also, we can't use DomHelper.setContentEditable as setting -webkit-user-modify
    // to read-write-plaintext-only will force the pasted content to plain text and
    // kill all formatting and semantic paste.
    // This trick doesn't work in Firefox, because the pre-wrap attribute is not
    // preserved through copy, to fix this in Firefox, we'll need to manually
    // replace spaces with &nbsp;
    element.setAttribute("contentEditable", "true");
    JsoView.as(element.getStyle()).setString("white-space", "pre-wrap");
    // DomHelper.setContentEditable(element, true, false);
    element.getStyle().setOverflow(Overflow.HIDDEN);

    positionPasteBuffer(element);
    Document.get().getBody().appendChild(element);
  }
}
