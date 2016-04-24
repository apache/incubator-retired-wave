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


/**
 * Data describing a blip in a conversation manifest. A blip contains reply
 * threads, some of which may be inline.
 *
 * @author zdwang@google.com (David Wang)
 */
public interface ManifestBlip {

  /**
   * @return The id of the blip content document
   */
  String getId();

  /**
   * Appends a new thread to this blip.
   *
   * @param id the new thread's id
   * @param inline whether the thread is inline
   * @return the newly created thread
   */
  ManifestThread appendReply(String id, boolean inline);

  /**
   * Inserts a new thread in this blip.
   *
   * @param index location at which to insert the new thread
   * @param id the new thread's id
   * @param inline whether the thread is inline
   * @return the newly created thread
   */
  ManifestThread insertReply(int index, String id, boolean inline);

  /**
   * @return the thread at a given index in this blip
   */
  ManifestThread getReply(int index);

  /**
   * @return an iterator over the threads in this blip
   */
  Iterable<? extends ManifestThread> getReplies();

  /**
   * @return the index of a thread in this blip
   */
  int indexOf(ManifestThread reply);

  /**
   * Removes a thread from this blip.
   */
  boolean removeReply(ManifestThread reply);

  /**
   * @return the number of reply threads in this blip
   */
  int numReplies();
}
