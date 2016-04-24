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


import org.waveprotocol.wave.client.wavepanel.view.dom.full.i18n.BlipMessages;
import junit.framework.TestCase;

import org.waveprotocol.wave.client.common.safehtml.EscapeUtils;
import org.waveprotocol.wave.client.uibuilder.UiBuilder;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipViewBuilder.Components;


public class BlipViewBuilderTest extends TestCase {
  private BlipViewBuilder.Css css;
  BlipMessages constants;

  private static final String content = "<div>yyy</div>";
  private String blipDomId;
  private BlipViewBuilder blipUi;
  private BlipMetaViewBuilder metaUi;

  @Override
  protected void setUp() {
    css = UiBuilderTestHelper.mockCss(BlipViewBuilder.Css.class);
    constants = UiBuilderTestHelper.mockBlipMessages(BlipMessages.class);
    String blipId = "askljfalikwh4rlkhs";
    String metaDomId = blipId = "M";
    blipDomId = blipId + "B";

    UiBuilder fakeContent = UiBuilder.Constant.of(EscapeUtils.fromSafeConstant(content));
    metaUi = new BlipMetaViewBuilder(css, constants, metaDomId, fakeContent);
    blipUi = new BlipViewBuilder(blipDomId, metaUi, UiBuilder.EMPTY, UiBuilder.EMPTY, css);
  }

  public void testBasicContentAvailable() throws Exception {
    String time = "234khdlga";
    String metaline = "45o32958uoig";
    String avatar = "http://foo.com/image.png";

    metaUi.setTime(time);
    metaUi.setMetaline(metaline);
    metaUi.setAvatar(avatar);

    UiBuilderTestHelper.verifyHtml(blipUi, blipDomId, Components.values());
    String html = UiBuilderTestHelper.render(blipUi);

    // make sure all the data is in the output
    assertEquals(1, UiBuilderTestHelper.countOccurrences(html, time));
    assertEquals(1, UiBuilderTestHelper.countOccurrences(html, content));
    assertEquals(1, UiBuilderTestHelper.countOccurrences(html, metaline));
  }

  public void testEvilHtmlEscaped() {
    String evilTime = "123<abc></abc>";
    String evilLine = "123<abc></abc>";
    String evilAvatar = "http://\" src=evil";

    metaUi.setTime(evilTime);
    metaUi.setMetaline(evilLine);
    metaUi.setAvatar(evilAvatar);

    String html = UiBuilderTestHelper.render(blipUi);

    assertFalse("Time string not escaped", html.contains(evilTime));
    assertFalse("Metaline string not escaped", html.contains(evilLine));
    assertFalse("Avatar url not escaped", html.contains(evilAvatar));
  }

}
