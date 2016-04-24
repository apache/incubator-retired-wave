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

package org.waveprotocol.wave.model.util;

@Deprecated
public class TitleExtractor {

  /**
   * Extracts a title from an XML string.
   * The string may be either an entire document (that contains a title
   * element) or the inner XML of a title element).
   *
   * @param rich  an XML string
   * @return a title extracted from {@code rich}.  The string is element-free,
   *         escaped XML.
   */
  public static String extractTitle(String rich) {
    return firstPhrase(rich);
  }

  /**
   * Tests if an XML string contains only a title.
   * Essentially, this just tests if the text view of the XML string contains
   * only a single sentence.
   *
   * @param rich  an XML string
   * @return true if {@code rich} contains just a single sentence.
   */
  public static boolean isOnlyTitle(String rich) {
    String titleIsh = stripWhite(processForFirstPhrase(rich));
    String title = extractTitle(rich);
    return titleIsh.equals(title);
  }

  private static String firstPhrase(String rich) {
    String processed = processForFirstPhrase(rich);
    int stop = processed.indexOf('\n');
    return stripWhite(stop != -1 ? processed.substring(0, stop) : processed);
  }

  private static String processForFirstPhrase(String rich) {
    // Place a \n at first <br>
    String text = rich.replaceFirst("<br.*?>", "\n")
      // Place a \n at first closing p
      .replaceFirst("</p>", "\n")
      // Place a \n at first stop char, followed by whitespace
      .replaceFirst("((\\.|\\?|!)+(\\s))", "$1\n");

    return stripTags(text);
  }

  /**
   *  Remove opening and closing tags.
   */
  private static String stripTags(String rich) {
    // This was done via a rich.replaceAll("<(.|\\n)+?>", "");
    // which would cause a StackOverflowError in the indexer when a root blip contained a large
    // tag e.g. a gadget.

    StringBuilder b = new StringBuilder();

    int start = 0;
    int open = rich.indexOf('<');
    if (open < 0) {
      return rich;
    }
    while(start >= 0 && start < rich.length()) {
      if (open < 0) {
        // Append all the rest.
        b.append(rich.substring(start));
        break;
      }
      if (open > start) {
        // Append the chars between the start and the open.
        b.append(rich.substring(start, open));
      }
      // jump to the next
      start = rich.indexOf('>', open);
      if (start > 0) {
        // skip the '>'
        ++start;
      }
      open = rich.indexOf('<', start);
    }
    return b.toString();
  }

  /**
   * Strips leading and trailing whitespace.
   */
  private static String stripWhite(String text) {
    return text.replaceAll("^(\\s|\u00a0)+|\\s+$", "");
  }
}
