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

package org.waveprotocol.wave.client.doodad.attachment.testing;

import org.waveprotocol.wave.client.doodad.attachment.SimpleAttachmentManager;
import org.waveprotocol.wave.media.model.Attachment;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.util.StringMap;


/**
 * Super-simple Attachment manager for use with test harness
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
// TODO(nigeltao): Replace this Fake by a mockito Mock.
public class FakeAttachmentsManager implements SimpleAttachmentManager {
  private final StringMap<FakeAttachment> map = CollectionUtils.createStringMap();
  private final IdentitySet<Listener> listeners = CollectionUtils.createIdentitySet();

  public FakeAttachment createFakeAttachment(
      String url, int thumbnailWidth, int thumbnailHeight) {
    return createFakeAttachment(url, thumbnailWidth, thumbnailHeight, "application/octet-stream");
  }

  public FakeAttachment createFakeAttachment(
      String url, int thumbnailWidth, int thumbnailHeight, String mimeType) {
    FakeAttachment a = new FakeAttachment(url, thumbnailWidth, thumbnailHeight, mimeType);
    map.put(url, a);
    return a;
  }

  public FakeAttachmentsManager() {
  }

  @Override
  public void addListener(Listener l) {
    listeners.add(l);
  }

  @Override
  public Attachment getAttachment(String id) {
    return id != null ? map.get(id) : null;
  }

  @Override
  public void removeListener(Listener l) {
    listeners.remove(l);
  }

}
