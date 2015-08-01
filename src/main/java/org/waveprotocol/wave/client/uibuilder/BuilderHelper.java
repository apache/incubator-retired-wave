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

import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;

/**
 * Helper methods for UI builders.
 *
 */
public final class BuilderHelper {

  public static final String KIND_ATTRIBUTE = "kind";

  public interface Component {
    /**
     * @param baseId
     * @return the dom id to use for the given component.
     */
    String getDomId(String baseId);
  }

  // No instances - this is a container of static helpers.
  private BuilderHelper() {
  }

  /** @return a non-null equivalent of a nullable ui. */
  public static UiBuilder nonNull(UiBuilder ui) {
    return ui != null ? ui : UiBuilder.EMPTY;
  }

  /** @return a non-null equivalent of a nullable ui. */
  public static HtmlClosure nonNull(HtmlClosure ui) {
    return ui != null ? ui : HtmlClosureCollection.EMPTY;
  }

  /** @return a builder composed of a sequence of builders. */
  public static HtmlClosure compose(final UiBuilder... uis) {
    return new HtmlClosure() {
      @Override
      public void outputHtml(SafeHtmlBuilder out) {
        for (UiBuilder ui : uis) {
          nonNull(ui).outputHtml(out);
        }
      }
    };
  }
}
