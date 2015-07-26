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

package org.waveprotocol.wave.client.editor.util;

import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.util.DocHelper;

public class PaddingBundle {
  /** The new version of the padded text. */
  final String text;

  /** Whether padding was added to the start and end of the text. */
  final boolean start;
  final boolean end;

  /**
   * Utility that adds spaces to the start/end of text as required.
   *
   * @param doc The document the text will be inserted into
   * @param text The new text to insert
   * @param start The start of where the text being replaced is
   * @param end The end of where the text being replaced is
   * @return A bundle containing all the padding calculated information
   */
  public static PaddingBundle applyPadding(ReadableWDocument<?, ?, ?> doc,
      String text, int start, int end) {
    // NOTE(patcoleman): assumes text is non-null and non-empty.

    // TODO(patcoleman): come up with a better API that gets the next/previous text character
    // from a given location in the document.
    int buffer = 6;
    int before = Math.max(start - buffer, 0);
    int after = Math.min(end + buffer, doc.size() - 1);

    String textBefore = DocHelper.getText(doc, before, start);
    String textAfter = DocHelper.getText(doc, end, after);

    boolean addSpaceBefore = textBefore.length() == 0 ? true :
      textBefore.charAt(textBefore.length() - 1) != ' ';
    if (textBefore.length() == 0 || text.charAt(0) == ' ') {
      addSpaceBefore = false; // don't pad if already padded or there's no text before
    }

    boolean addSpaceAfter = textAfter.length() == 0 ? true :
      textAfter.charAt(0) != ' ';
    if (text.charAt(text.length() - 1) == ' ') {
      addSpaceAfter = false; // as above.
    }

    // build the new string, as well as the bundle to return.
    StringBuilder formatted = new StringBuilder();
    if (addSpaceBefore) {
      formatted.append(' ');
    }
    formatted.append(text);
    if (addSpaceAfter) {
      formatted.append(' ');
    }
    return new PaddingBundle(formatted.toString(), addSpaceBefore, addSpaceAfter);
  }

  /** Constructs the bundle by setting all internal members. */
  public PaddingBundle(String postText, boolean addedStart, boolean addedEnd) {
    text = postText;
    start = addedStart;
    end = addedEnd;
  }

  /**
   * @return The text to insert, with padding applied where necessary
   */
  public String getText() {
    return this.text;
  }

  /**
   * @return Whether padding was added to the start
   */
  public boolean isAddedStart() {
    return this.start;
  }

  /**
   * @return Whether padding was added to the end
   */
  public boolean isAddedEnd() {
    return this.end;
  }
}
