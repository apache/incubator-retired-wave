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

import org.waveprotocol.wave.model.util.Pair;

/**
 * An anchor represented as a conversation id and blip id. Either string may be
 * null.
 *
 * @author anorth@google.com (Alex North)
 */
public final class AnchorData {
  final Pair<String, String> anchor;

  public AnchorData(String conversationId, String blipId) {
    this.anchor = Pair.of(conversationId, blipId);
  }

  public String getConversationId() {
    return anchor.getFirst();
  }

  public String getBlipId() {
    return anchor.getSecond();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AnchorData)) {
      return false;
    }
    AnchorData other = (AnchorData) obj;
    return anchor.equals(other.anchor);
  }

  @Override
  public int hashCode() {
    return anchor.hashCode();
  }
}
