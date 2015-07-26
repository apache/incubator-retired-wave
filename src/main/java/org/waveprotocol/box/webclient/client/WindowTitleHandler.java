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

package org.waveprotocol.box.webclient.client;

import com.google.gwt.user.client.Window;

import org.waveprotocol.box.webclient.search.WaveStore;
import org.waveprotocol.box.webclient.widget.frame.FramedPanel;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.document.WaveContext;

/**
 * Sets the browser window title to the wave title.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public final class WindowTitleHandler implements WaveStore.Listener {

  private static final String APP_NAME = "WIAB";

  private static final String DEFAULT_TITLE = "Communicate and collaborate in real-time";

  private final WaveStore waveStore;
  private final FramedPanel waveFrame;

  public static WindowTitleHandler install(WaveStore waveStore, FramedPanel waveFrame) {
    return new WindowTitleHandler(waveStore, waveFrame);
  }

  private WindowTitleHandler(WaveStore waveStore, FramedPanel waveFrame) {
    this.waveStore = waveStore;
    this.waveFrame = waveFrame;
    init();
  }

  private void init() {
    waveStore.addListener(this);
  }

  @Override
  public void onOpened(WaveContext wave) {
    String waveTitle = TitleHelper.getTitle(wave);
    String windowTitle = formatTitle(waveTitle);
    if (waveTitle == null || waveTitle.isEmpty()) {
      windowTitle = DEFAULT_TITLE;
    }
    Window.setTitle(windowTitle);
    waveFrame.setTitleText(waveTitle);
  }

  @Override
  public void onClosed(WaveContext wave) {
    Window.setTitle(DEFAULT_TITLE);
  }

  private String formatTitle(String title) {
    return  title + " - " + Session.get().getAddress() + " - " + APP_NAME;
  }
}
