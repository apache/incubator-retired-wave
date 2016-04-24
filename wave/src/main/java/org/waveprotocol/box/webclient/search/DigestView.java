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

import org.waveprotocol.wave.client.account.Profile;

/**
 * View interface for a digest.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public interface DigestView {
  /** Removes this view from the UI. */
  void remove();

  void setAvatars(Iterable<Profile> urls);

  void setTimestamp(String time);

  // Note: setTitle is a widget method.
  void setTitleText(String text);

  void setSnippet(String snippet);

  void setMessageCounts(int unread, int total);

  /** Renders this view in the selected state. */
  void select();

  /** Renders this view in the de-selected state. */
  void deselect();
}
