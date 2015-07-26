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
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Firefox old implementation of the paste buffer. We cannot use a standard div set
 * to contentEditable because pasting any javascript will automatically
 * execute it. Instead, use an offscreen iframe whose document is set to
 * "designMode". This is roughly equivalent to contentEditable with the
 * exception of being more sandbox-like and prevents script execution.
 *
 * NOTE(user): There's actually a lot of blackmagic in this class. In setupDom,
 * the real contentDocument of the iframe is actually created asynchronously. In
 * this code are actually using a transient element inside a transient
 * contentDocument, that just happens to work.
 *
 * Why don't we get contentDocument().getBody() after a delay then?
 *
 * If we get the use the real element, i.e. assign element from
 * iframe.getContentDocument().getBody() asynchronously we'll suffers from paste
 * self-xss. This seems strange, because we're using designMode in an IFrame,
 * which is supposed to guard against javascript execution. If we make this
 * iframe visible and paste in html that executes js directly, we are protected
 * from js execution. However, if we are doing it programmatically inside a copy
 * event, we are not protected.
 *
 * Tested on: Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.5; en-US; rv:1.9.2)
 * Gecko/20100115 Firefox/3.6 GTB7.0 < 15.0
 *
 */
class PasteBufferImplOldFirefox extends PasteBufferImpl {

  private final IFrameElement iframe;

  /**
   * Protected empty constructor. Will be created by factory constructor in
   * PasteBufferImpl.
   */
  protected PasteBufferImplOldFirefox() {
    iframe = Document.get().createIFrameElement();
  }

  @Override
  protected void setupDom() {
    Document.get().getBody().appendChild(iframe);

    positionPasteBuffer(iframe);
    element = iframe.getContentDocument().getBody();
    setDesignMode(iframe.getContentDocument());
  }

  private static native void setDesignMode(Document doc) /*-{
    doc.designMode = "on";
  }-*/;

  @Override
  public void prepareForPaste() {
    super.prepareForPaste();
    // N.B.(davidbyttow): In FF3, focus is not implicitly set by setting the
    // selection when appending a DOM element dynamically. So we must explicitly
    // set the focus.
    DomHelper.focus(iframe);
    NativeSelectionUtil.setCaret(Point.<Node>end(element));
  }
}
