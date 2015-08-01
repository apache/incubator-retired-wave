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
 * Data describing a thread in a conversation manifest. A thread is a list of
 * blips, has an identifier, unique within the manifest, and a flag specifying
 * whether the thread should display inline.
 *
 * @author zdwang@google.com (David Wang)
 */
public interface ManifestThread {
  /**
   * @return the id of the thread
   */
  String getId();

  /**
   * @return true if this reply thread is an inline reply
   */
  boolean isInline();

  /**
   * Appends a new blip to the end of this thread.
   *
   * @param id id for the new blip
   * @return the newly created blip
   */
  ManifestBlip appendBlip(String id);

  /**
   * Insert a new blip into this thread.
   *
   * @param index location at which to insert the new blip
   * @param id id for the new blip
   * @return the newly created blip
   */
  ManifestBlip insertBlip(int index, String id);

  /**
   * @return the blip at the given index
   */
  ManifestBlip getBlip(int index);

  /**
   * @return an iterator over the blip in this thread
   */
  Iterable<? extends ManifestBlip> getBlips();

  /**
   * @return the index of a blip in this thread
   */
  int indexOf(ManifestBlip blip);

  /**
   * Removes a blip from this thread.
   */
  boolean removeBlip(ManifestBlip blip);

  /**
   * @return the number of blips in this thread
   */
  int numBlips();
}
