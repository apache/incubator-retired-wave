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

package org.waveprotocol.wave.client.editor.content;

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.editor.extract.SelectionMatcher;
import org.waveprotocol.wave.model.document.util.ReadableDocumentView;

/**
 * Interface provides nice html rendering for paste or export purpose.
 *
 */
public interface NiceHtmlRenderer {
  /**
   * Renders a sequence of nodes.
   *
   * This will render a sequence of nodes, and append the result to dest.
   *
   * The ability to render a sequence of nodes is important. For example, it is
   * ideal to render a sequence of paragraph with the li attribute, as a
   * single bulleted list under the same ul.
   *
   * @return the last node in the sequence rendered by this method.
   * @param view
   * @param firstItem  the start of the sequence
   * @param stopAt  the last node in the sequence
   * @param dest  the destination  where this sequence will be appended
   * @param selectionMatcher this records corresponding selection in the destination dom
   */
  ContentNode renderSequence(
      ReadableDocumentView<ContentNode, ContentElement, ContentTextNode> view,
      ContentNode firstItem,
      ContentNode stopAt,
      Element dest,
      SelectionMatcher selectionMatcher);
}
