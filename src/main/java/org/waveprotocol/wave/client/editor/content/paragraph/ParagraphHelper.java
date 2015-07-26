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

package org.waveprotocol.wave.client.editor.content.paragraph;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;

import org.waveprotocol.wave.common.logging.LoggerBundle;

/**
 * Helps {@link Paragraph}. Deferred binding adds per-browser code.
 *
 * Browsers do not render empty <p> elements with enough space
 * for a caret, nor with margins/padding, etc. Therefore, we need
 * to add various type of 'spacers' that works with the browsers
 * native editors.
 *
 * Unfortunately, the browsers have rather different idiosyncrasies
 * in this area, thus the deferred binding for this class.
 *
 */
public class ParagraphHelper {
  /**
   * Deferred binding helper.
   */
  public static final ParagraphHelper INSTANCE = UserAgent.isIE() ? new ParagraphHelperIE()
      : (UserAgent.isWebkit() ? new ParagraphHelperWebkit() : new ParagraphHelperAlwaysBr());

  /**
   * Borrow editor's logger
   */
  public static LoggerBundle logger = EditorStaticDeps.logger;

  public static ParagraphHelper create() {
    return UserAgent.isIE() ? new ParagraphHelperIE() : (UserAgent.isWebkit()
        ? new ParagraphHelperWebkit() : new ParagraphHelperAlwaysBr());
  }

  public Node getEndingNodelet(Element paragraph) {
    return null;
  }

  /**
   * Handles the paragraph being emptied
   *
   * @param paragraph
   */
  public void onEmpty(Element paragraph) {}

  /**
   * Handles a child being added to the paragraph
   *
   * @param child
   * @param paragraph
   */
  public void onChildAdded(Node child, Element paragraph) {}

  /**
   * Handles a child being removed to the paragraph
   *
   * @param child
   * @param paragraph
   */
  public void onRemovingChild(Node child, Element paragraph) {}

  /**
   * Handles the paragraph repairing itself
   * @param paragraph
   */
  public void onRepair(ContentElement paragraph) {}

  /**
   * See @link {@link ContentNode#debugAssertHealthy()}
   * @param paragraph
   */
  public void assertHealthy(ContentElement paragraph) {}
}
