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
 * A closure that outputs HTML rooted at a single element.
 *
 */
public interface UiBuilder extends HtmlClosure {

  /**
   * {@inheritDoc}
   *
   * The output HTML has a single top-level element.
   */
  @Override
  void outputHtml(SafeHtmlBuilder out);

  /** An empty UI builder that outputs nothing. */
  UiBuilder EMPTY = new UiBuilder() {
    @Override
    public void outputHtml(SafeHtmlBuilder out) {
    }
  };

  /**
   * A UI builder that outputs a constant html string.
   */
  final class Constant {

    // Utility class
    private Constant() {
    }

    public static UiBuilder of(final SafeHtml html) {
      return new UiBuilder() {
        @Override
        public void outputHtml(SafeHtmlBuilder out) {
          out.append(html);
        }
      };
    }
  }
}
