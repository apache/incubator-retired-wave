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


package org.waveprotocol.wave.client.uibuilder;

import org.waveprotocol.wave.client.common.safehtml.SafeHtml;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;

/**
 * Outputs some HTML when invoked. An {@code HTMLClosure} guarantees that the
 * HTML that it outputs is well-formed and balanced (every open tag has a
 * corresponding closing tag).
 * <p>
 * The use of closures, rather than string concatenation, allows HTML structures
 * to be built up modularly in linear time rather than quadratic time.
 *
 */
public interface HtmlClosure {

  /**
   * Outputs HTML to a builder.
   *
   * @param out builder to collect the HTML.
   */
  void outputHtml(SafeHtmlBuilder out);

  /** An empty closure that outputs nothing. */
  HtmlClosure EMPTY = new HtmlClosure() {
    @Override
    public void outputHtml(SafeHtmlBuilder out) {
    }
  };

  /**
   * An HTML closure that outputs a constant string.
   */
  final class Constant {

    // Utility class
    private Constant() {
    }

    public static HtmlClosure of(final SafeHtml html) {
      return new HtmlClosure() {
        @Override
        public void outputHtml(SafeHtmlBuilder out) {
          out.append(html);
        }
      };
    }
  }
}
