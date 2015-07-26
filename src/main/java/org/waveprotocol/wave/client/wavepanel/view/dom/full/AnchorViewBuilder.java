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

package org.waveprotocol.wave.client.wavepanel.view.dom.full;

import static org.waveprotocol.wave.client.uibuilder.BuilderHelper.nonNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.uibuilder.OutputHelper;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.View.Type;

/**
 * UI builder for an inline thread.
 *
 */
public final class AnchorViewBuilder implements UiBuilder {

  /**
   * A unique id for this builder.
   */
  private final String id;

  //
  // Structural components.
  //

  private final UiBuilder thread;

  /**
   * Creates an anchor view.
   *
   * @param id HTML-escaped value of the anchor's DOM id
   * @param thread UI of the thread positioned at this anchor
   */
  public static AnchorViewBuilder create(String id, UiBuilder thread) {
    // Cheap semi-reliable check of escapedness.
    Preconditions.checkArgument(id == null || !id.contains("\'"));
    return new AnchorViewBuilder(id, nonNull(thread));
  }

  @VisibleForTesting
  AnchorViewBuilder(String id, UiBuilder thread) {
    this.id = id;
    this.thread = thread;
  }

  @Override
  public void outputHtml(SafeHtmlBuilder output) {
    OutputHelper.openSpan(output, id, null, TypeCodes.kind(Type.ANCHOR));
    thread.outputHtml(output);
    OutputHelper.closeSpan(output);
  }

}
