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

package org.waveprotocol.wave.client.editor.testtools;

import org.waveprotocol.wave.model.document.util.Range;

/**
 * Parses strings like <p>ab|cd</p> into content <p>abcd</p> and
 * caret location 4 (note that the string represents the children
 * of the document element, and thus that the position before the
 * first start tag has location 1), or strings like <p>a[bc]d</p>
 * into the same content string and selection range 3-5.
 */
public class ContentWithSelection {

  /**
   * The content string w/o selection
   */
  public String content;

  /**
   * The selection start point or caret or -1 if no selection
   */
  public Range selection = null;

  /**
   * Constructor
   *
   * @param content String potentially containing selection, e.g.,
   *    <p>ab|cd</p> or <p>a[bc]d</p>
   */
  public ContentWithSelection(String content) {
    boolean hasSelection =
        (content.contains("|") ||
         content.contains("["));
    if (hasSelection) {
      parseSelection(content);
      this.content = stripSelection(content);
    } else {
      this.content = content;
    }
  }

  /**
   * @param content
   * @return string with selection chars [] and | stripped
   */
  private static String stripSelection(String content) {
    return content.replaceAll("[|\\[\\]]", "");
  }

  /**
   * Parses selection from a content string
   *
   * @param content
   */
  private void parseSelection(String content) {
    for (int i = 0, loc = 0, start = -1; i < content.length(); i++, loc++) {
      switch (content.charAt(i)) {
        case '<':
          int slash = content.indexOf('/', i);
          i = content.indexOf('>', i);
          assert i > 0 : "Invalid content";
          if (slash == i - 1) {
            loc++;
          }
          break;
        case '|':
          selection = new Range(loc);
          return;
        case '[':
          start = loc--;
          break;
        case ']':
          selection = new Range(start, loc);
          return;
      }
    }
  }

}
