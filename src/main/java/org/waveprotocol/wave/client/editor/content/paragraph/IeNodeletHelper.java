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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Helper for some IE-specific quirks
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class IeNodeletHelper {

  /**
   * Some IE element classes require their html nodelets be attached to
   * somewhere under the body of html document in order for implementation to
   * work. See, e.g., {@link ParagraphHelperIE#onEmpty(Element)}
   */
  public static Point<Node> beforeImplementation(Element nodelet) {
    // Get point after nodelet
    Point<Node> point = nodelet.getParentNode() != null ?
        Point.inElement(nodelet.getParentNode(), nodelet.getNextSibling()) : null;
    // Attach nodelet to document body
    Document.get().getBody().appendChild(nodelet);
    return point;
  }

  /**
   * Complement to {@link #beforeImplementation(Element)}. Pass in the point
   * returned by that method.
   */
  public static void afterImplementation(Element nodelet, Point<Node> point) {
    // Place nodelet back where it came from
    if (point != null) {
      point.getContainer().insertBefore(nodelet, point.getNodeAfter());
    }
  }
}
