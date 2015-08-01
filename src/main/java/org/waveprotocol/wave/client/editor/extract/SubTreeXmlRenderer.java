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

import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema.PermittedCharacters;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * Extracts a subtree from the document that contains all nodes where the
 * start/end tags or characters lie within a given range.
 *
 *
 * @param <N>
 * @param <E>
 * @param <T>
 */
public class SubTreeXmlRenderer<N, E extends N, T extends N> {
  private final ReadableWDocument<N, E, T> doc;

  public SubTreeXmlRenderer(ReadableWDocument<N, E, T> doc) {
    this.doc = doc;
  }

  /**
   * Renders the range between the given points as an xml string.
   *
   * @param start
   * @param end
   */
  public XmlStringBuilder renderRange(Point<N> start, Point<N> end) {
    N nearestCommonAncestor =
        DocHelper.nearestCommonAncestor(doc, start.getCanonicalNode(), end.getCanonicalNode());

    Range inclusion = new Range(doc.getLocation(start), doc.getLocation(end));

    XmlStringBuilder builder =
      XmlStringBuilder.createEmptyWithCharConstraints(PermittedCharacters.BLIP_TEXT);

    E asElement = doc.asElement(nearestCommonAncestor);
    if (asElement != null) {
      for (N child = doc.getFirstChild(asElement);
           child != null; child = doc.getNextSibling(child)) {
        builder.append(augmentBuilder(child, inclusion));
      }
      if (asElement != doc.getDocumentElement()
          && shouldInclude(inclusion, getNodeRange(asElement))) {
        builder.wrap(doc.getTagName(asElement),
            CollectionUtils.adaptStringMap(doc.getAttributes(asElement)));
      }
    } else {
      T asText = doc.asText(nearestCommonAncestor);
      int tStart = doc.getLocation(asText);
      String substring =
          doc.getData(asText).substring(inclusion.getStart() - tStart, inclusion.getEnd() - tStart);

      builder.appendText(substring);
    }
    return builder;
  }

  private XmlStringBuilder augmentBuilder(N node, Range inclusion) {
    Range nodeRange = getNodeRange(node);
    XmlStringBuilder builder = XmlStringBuilder.createEmpty();

    if (!shouldInclude(inclusion, nodeRange)) {
      return builder;
    }

    E asElement = doc.asElement(node);
    if (doc.asElement(node) != null) {
      for (N child = doc.getFirstChild(node); child != null; child = doc.getNextSibling(child)) {
        builder.append(augmentBuilder(child, inclusion));
      }
      builder.wrap(doc.getTagName(asElement),
          CollectionUtils.adaptStringMap(doc.getAttributes(asElement)));
    } else {
      T asText = doc.asText(node);
      int tStart = doc.getLocation(asText);
      String data = doc.getData(asText);
      int start = Math.max(0, inclusion.getStart() - tStart);
      int end = Math.min(data.length(), inclusion.getEnd() - tStart);

      builder.appendText(data.substring(start, end));
    }

    return builder;
  }

  private boolean shouldInclude(Range inclusion, Range nodeRange) {
    if (nodeRange.getStart() < inclusion.getStart() && nodeRange.getEnd() > inclusion.getEnd()) {
      return false;
    }

    if (nodeRange.getEnd() <= inclusion.getStart() || nodeRange.getStart() >= inclusion.getEnd()) {
      return false;
    }
    return true;
  }

  private int getNodeEnd(N node) {
    int end;
    N nextSibling = doc.getNextSibling(node);

    if (nextSibling != null) {
      return doc.getLocation(nextSibling);
    } else {
      E parent = doc.getParentElement(node);
      return parent == doc.getDocumentElement() ? doc.size() : getNodeEnd(parent) - 1;
    }
  }

  private Range getNodeRange(N node) {
    assert node != null && node != doc.getDocumentElement() :
      "Node cannot be null or the document element";
    Range r = new Range(doc.getLocation(node), getNodeEnd(node));
    return r;
  }
}
