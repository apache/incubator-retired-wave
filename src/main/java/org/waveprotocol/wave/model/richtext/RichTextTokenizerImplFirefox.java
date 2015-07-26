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

package org.waveprotocol.wave.model.richtext;

import org.waveprotocol.wave.model.document.util.ElementStyleView;

/**
 * Firefox specific implementation of the RichTextTokenizer. This is necessary
 * to remove whitespace and newlines from elements. Also, when pasting into
 * an element that does not have focus, it is necessary to explicitly call
 * focus on the element rather than just moving the selection.
 *
 * TODO(user): Rather than traversing the DOM prior to processing,
 * we can override processTextNode from the parent and do the modification then.
 * Only reason I haven't done this yet is because of the leftSibling logic.
 *
 */
public class RichTextTokenizerImplFirefox<N, E extends N, T extends N>
    extends RichTextTokenizerImpl<N, E, T> {

  public RichTextTokenizerImplFirefox(ElementStyleView<N, E, T> doc) {
    super(doc);
  }

  /**
   * When pasting HTML from and to FF, the extra whitespace and newlines
   * are being kept around, causing subsequent edits to leave to catastrophic
   * DOM mutations (such as deleting entire paragraph blocks when deleting
   * DOM nodes). I am not really sure why that is happening, but this
   * code is trying to prevent it. Ideally there will be a better solution
   * down the road.
   */
  @Override
  protected void processTextNodeInner(T textNode, N leftSibling) {
    // Keep the first space in a sequence of spaces. This is quite lame and we
    // shouldn't have to do this, but at least it's somewhat consistent with
    // other browsers.
    String data = document.getData(textNode).replace('\n', ' ');

    // Special logic: If this text node borders an Element, we can allow trailing
    // whitespace (e.g., "Click <a>here</a>").
    N rightSibling = document.getNextSibling(textNode);
    data = trimWhitespace(data,
        leftSibling == null || document.asText(leftSibling) != null,
        rightSibling == null || document.asText(rightSibling) != null);

    boolean remove = false;
    if (!data.isEmpty()) {
      StringBuilder b = new StringBuilder(data.length());
      boolean wasSpace = false;
      for (int i = 0; i < data.length(); ++i) {
        char ch = data.charAt(i);
        if (ch == ' ') {
          if (wasSpace) {
            continue;
          }
          wasSpace = true;
        } else {
          wasSpace = false;
        }
        b.append(ch);
      }
      data = b.toString();
    }

    // If this is an empty or single space text node, just remove it.
    if (data.isEmpty() || (data.length() == 1 && data.charAt(0) == ' ')) {
      // Ignored.
    } else {
      addTextToken(data);
    }
  }

  /**
   * Implementation of trim that handles either leading or trailing separately.
   *
   * @param str the string to trim.
   * @param trimLeading True if the leading spaces should be trimmed.
   * @param trimTrailing True if the trailing spaces should be trimmed.
   */
  private static String trimWhitespace(String str, boolean trimLeading,
      boolean trimTrailing) {
    if (str.isEmpty()) {
      return str;
    }
    int first = 0;
    if (trimLeading) {
      for (; first < str.length(); ++first) {
        if (str.charAt(first) != ' ') {
          break;
        }
      }
    }
    int last = str.length();
    if (trimTrailing) {
      for (last = last - 1; last >= first; --last) {
        if (str.charAt(last) != ' ') {
          last = last + 1;
          break;
        }
      }
    }
    if (first >= last) {
      return "";
    }
    return str.substring(first, last);
  }
}
