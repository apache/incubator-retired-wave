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

package org.waveprotocol.box.webclient.search;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

/**
 * A wave digest.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public interface Digest {

  /** @return the wave represented by this digest. Never null. */
  WaveId getWaveId();

  /** @return the identity of the wave's author. May be null. */
  ParticipantId getAuthor();

  /** @return the identities of some other participants on this wave. Never null. */
  List<ParticipantId> getParticipantsSnippet();

  /** @return the title of this wave. May be null. */
  String getTitle();

  /** @return a snippet for this wave. May be null. */
  String getSnippet();

  /** @return the number of messages in this wave. */
  int getBlipCount();

  /** @return the number of unread messages in this wave. */
  int getUnreadCount();

  /** @return the epoch time of the last modification of this wave. */
  double getLastModifiedTime();
}
