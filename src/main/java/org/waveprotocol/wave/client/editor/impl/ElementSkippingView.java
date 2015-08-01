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

package org.waveprotocol.wave.client.editor.impl;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.util.FilteredView;

/**
 * A view that skips all elements except the document element
 * TODO(danilatos): Parametrise by node type if needed
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class ElementSkippingView extends FilteredView<Node, Element, Text>
  implements HtmlView {

  /**
   * Simple view which will simply present all the text nodes under an element
   * as siblings
   * @param documentElement
   */
  public ElementSkippingView(Element documentElement) {
    super(new HtmlViewImpl(documentElement));
  }

  /**
   * Compose the view with another
   * @param rawView
   */
  public ElementSkippingView(ReadableDocument<Node, Element, Text> rawView) {
    super(rawView);
  }

  @Override
  protected Skip getSkipLevel(Node node) {
    // TODO(danilatos): Detect and repair new elements. Currently we just ignore them.
    if (DomHelper.isTextNode(node)) {
      return Skip.NONE;
    } else {
      // We can't skip the document element by definition
      return node == getDocumentElement() ? Skip.NONE : Skip.SHALLOW;
    }
  }
}
