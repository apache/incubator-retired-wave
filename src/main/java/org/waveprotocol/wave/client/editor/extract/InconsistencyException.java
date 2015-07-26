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

package org.waveprotocol.wave.client.editor.extract;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import org.waveprotocol.wave.client.editor.content.ContentNode;

import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Thrown whenever we detect an inconsistency has come up, and we
 * need to repair it in some manner.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
@SuppressWarnings("serial")
public abstract class InconsistencyException extends Exception {
  private final Element where;

  /**
   * @param where The HTML element where or within something went awry
   */
  protected InconsistencyException(Element where) {
    this.where = where;
  }

  /**
   * @return The HTML element where or within something went awry
   */
  public Element getNode() {
    return where;
  }

  /**
   * HTML node(s) removed with respect to the content
   */
  public static class HtmlMissing extends InconsistencyException {
    ContentNode brokenNode;

    /**
     * @param brokenNode The content node that is missing its impl nodelet
     * @param where The HTML element where or within something went awry
     */
    public HtmlMissing(ContentNode brokenNode, Element where) {
      super(where);
      Preconditions.checkNotNull(brokenNode, "Broken node is unspecified");
      this.brokenNode = brokenNode;
    }

    /**
     * @return The content node that is missing its impl nodelet
     */
    public ContentNode getBrokenNode() {
      return brokenNode;
    }

    @Override
    public String toString() {
      return "HtmlMissing: " + brokenNode + " in " + getNode();
    }
  }

  /**
   * HTML node(s) inserted with respect to the content
   */
  public static class HtmlInserted extends InconsistencyException {
    final Point.El<ContentNode> contentPoint;
    final Point.El<Node> htmlPoint;

    /**
     * @param htmlPoint The place in the HTML right before where html was inserted
     * @param contentPoint The corresponding place in the content
     */
    public HtmlInserted(Point.El<ContentNode> contentPoint, Point.El<Node> htmlPoint) {
      super(htmlPoint.getContainer().<Element>cast());
      this.contentPoint = contentPoint;
      this.htmlPoint = htmlPoint;
    }

    /**
     * @return The place in the content where something was inserted
     */
    public Point.El<ContentNode> getContentPoint() {
      return contentPoint;
    }

    /**
     * @return The place in the html where something was inserted
     */
    public Point.El<Node> getHtmlPoint() {
      return htmlPoint;
    }

    @Override
    public String toString() {
      return "HtmlInserted: " + contentPoint;
    }
  }
}
