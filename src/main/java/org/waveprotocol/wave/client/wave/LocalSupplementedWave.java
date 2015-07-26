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


package org.waveprotocol.wave.client.wave;

import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.supplement.ObservableSupplementedWave;

/**
 * Optimistic implementation of a wave supplement.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public interface LocalSupplementedWave extends ObservableSupplementedWave {

  /**
   * Starts reading a blip.
   *
   * The blip is marked as read immediately, and also periodically, until
   * {@link #stopReading} is called. Calls must be balanced, but not nested,
   * with calls to {@link #stopReading}.
   */
  void startReading(ConversationBlip blip);

  /**
   * Stops reading a blip. The blip is marked as read immediately. A future edit
   * on this blip caused by someone else will make it become unread again. Calls
   * must be balanced, but not nested, with calls to {@link #startReading}.
   */
  void stopReading(ConversationBlip blip);
}
