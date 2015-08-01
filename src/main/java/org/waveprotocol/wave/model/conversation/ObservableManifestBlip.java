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

package org.waveprotocol.wave.model.conversation;

import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * Extends {@link ManifestBlip} to provide events that can be listened to.
 *
 * @author zdwang@google.com (David Wang)
 */
interface ObservableManifestBlip extends ManifestBlip,
    SourcesEvents<ObservableManifestBlip.Listener>, HasId {
  /**
   * Receives events on a {@link ManifestBlip}.
   */
  public interface Listener {
    /**
     * Called when a reply is added to the blip.
     */
    void onReplyAdded(ObservableManifestThread thread);

    /**
     * Called when a reply is removed from the blip.
     */
    void onReplyRemoved(ObservableManifestThread thread);
  }

  /**
   * Removes all listeners from this blip.
   */
  void detachListeners();

  // Covariant specialisations.

  @Override
  ObservableManifestThread appendReply(String id, boolean inline);

  @Override
  ObservableManifestThread insertReply(int index, String id, boolean inline);

  @Override
  ObservableManifestThread getReply(int index);

  @Override
  Iterable<? extends ObservableManifestThread> getReplies();
}
