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


import org.waveprotocol.wave.client.wavepanel.view.dom.full.i18n.ReplyBoxMessages;
import junit.framework.TestCase;

import org.waveprotocol.wave.client.wavepanel.view.dom.full.ReplyBoxViewBuilder.Components;


/**
 * Test the Reply Box Ui Builder to ensure well formed HTML.
 */
public class ReplyBoxBuilderTest extends TestCase {
  private ReplyBoxViewBuilder.Css css;
  private ReplyBoxMessages messages;

  @Override
  protected void setUp() {
    css = UiBuilderTestHelper.mockCss(ReplyBoxViewBuilder.Css.class);
    messages = UiBuilderTestHelper.mockReplyBoxMessages(ReplyBoxMessages.class);
  }

  public void testInline() throws Exception {
    String id = "askljfalikwh4rlkhs";
    ReplyBoxViewBuilder builder = new ReplyBoxViewBuilder(css, messages, id);
    UiBuilderTestHelper.verifyHtml(builder, id, Components.values());
  }
}
