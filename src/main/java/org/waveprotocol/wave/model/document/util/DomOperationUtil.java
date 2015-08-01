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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;

/**
 * Utilities for converting between DOM trees and operations.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public final class DomOperationUtil {
  /** Static methods, no constructor. */
  private DomOperationUtil() {}

  /**
   * Writes an entire subtree worth of operations to an initialization cursor.
   * NOTE(patcoleman): does not include annotations.
   *
   * @param doc Document the node resides within.
   * @param node Root of the subtree.
   * @param cursor Cursor to write results out to.
   */
  public static <N, E extends N, T extends N> void buildDomInitializationFromSubtree(
      ReadableDocument<N, E, T> doc, N node, DocInitializationCursor cursor) {
    T text = doc.asText(node);
    if (text == null) {
      buildDomInitializationFromElement(doc, doc.asElement(node), cursor, true);
    } else {
      buildDomInitializationFromTextNode(doc, text, cursor);
    }
  }

  /**
   * Writes a text node's information out to an initialization cursor.
   * NOTE(patcoleman): does not include annotations.
   *
   * @param doc Document the node resides within.
   * @param textNode Text node containing information to be written
   * @param cursor Cursor to write results out to.
   */
  public static <N, E extends N, T extends N> void buildDomInitializationFromTextNode(
      ReadableDocument<N, E, T> doc, T textNode, DocInitializationCursor cursor) {
    cursor.characters(doc.getData(textNode));
  }


  /**
   * Writes an element's information out to an initialization cursor, optionally recursing
   * to do likewise for its children.
   *
   * @param doc Document the node resides within.
   * @param element Element containing information to be written.
   * @param cursor Cursor to write results out to.
   * @param recurse Whether or not to write children to the operation.
   */
  public static <N, E extends N, T extends N> void buildDomInitializationFromElement(
      ReadableDocument<N, E, T> doc, E element, DocInitializationCursor cursor, boolean recurse) {
    cursor.elementStart(doc.getTagName(element), new AttributesImpl(doc.getAttributes(element)));
    if (recurse) {
      for (N child = doc.getFirstChild(element); child != null; child = doc.getNextSibling(child)) {
        buildDomInitializationFromSubtree(doc, child, cursor);
      }
    }
    cursor.elementEnd();
  }
}
