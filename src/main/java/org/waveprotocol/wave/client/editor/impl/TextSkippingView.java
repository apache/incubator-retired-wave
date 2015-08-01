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
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.ContentView;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.util.FilteredView;

/**
 * A view that skips all text nodes
 * TODO(danilatos): Parametrise by node type if needed
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class TextSkippingView<N, E extends N, T extends N> extends FilteredView<N, E, T> {

  /**
   * Convenience HTML implementation
   */
  public static class Html extends TextSkippingView<Node, Element, Text> implements HtmlView {
    /**
     * Simple view which will simply present only the elements in a document
     * @param documentElement
     */
    public Html(Element documentElement) {
      super(new HtmlViewImpl(documentElement));
    }

    /**
     * Compose with another view
     * @param rawView
     */
    public Html(ReadableDocument<Node, Element, Text> rawView) {
      super(rawView);
    }
  }

  /**
   * Convenience Content implementation
   */
  public static class Content extends TextSkippingView<
      ContentNode, ContentElement, ContentTextNode>
      implements ContentView {

    /**
     * Compose with another view
     * @param rawView
     */
    public Content(ReadableDocument<ContentNode, ContentElement, ContentTextNode> rawView) {
      super(rawView);
    }
  }

  /**
   * Compose the view with another
   * @param rawView
   */
  public TextSkippingView(ReadableDocument<N, E, T> rawView) {
    super(rawView);
  }

  @Override
  protected Skip getSkipLevel(N node) {
    // TODO(danilatos): Detect and repair new elements. Currently we just ignore them.
    if (asText(node) != null) {
      return Skip.DEEP;
    } else {
      return Skip.NONE;
    }
  }
}
