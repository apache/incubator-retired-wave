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

package org.waveprotocol.wave.client.wavepanel.view.fake;

import org.waveprotocol.wave.client.wavepanel.view.ReplyBoxView;
import org.waveprotocol.wave.client.wavepanel.view.RootThreadView;

/**
 * Fake, pojo implementation of a reply box view.
 *
 */
public class FakeReplyBoxView implements ReplyBoxView {

  private final FakeRootThreadView threadView;
  private boolean enabled;
  private String avatarImageUrl;

  FakeReplyBoxView(FakeRootThreadView threadView) {
    this.threadView = threadView;
  }

  @Override
  public Type getType() {
    return Type.REPLY_BOX;
  }

  @Override
  public void remove() {
    // no-op
  }

  @Override
  public String getId() {
    return "fakeId";
  }

  @Override
  public void enable() {
    this.enabled = true;
  }

  @Override
  public void disable() {
    this.enabled = false;
  }

  protected boolean isEnabled() {
    return this.enabled;
  }

  @Override
  public void setAvatarImageUrl(String imageUrl) {
    this.avatarImageUrl = imageUrl;
  }

  protected String getAvatarImageUrl() {
    return this.avatarImageUrl;
  }

  @Override
  public RootThreadView getParent() {
    return this.threadView;
  }
}