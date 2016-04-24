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

package com.google.wave.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A thread represents a group of blips in a wave.
 */
public class BlipThread implements Serializable {

  /** The id of the thread. */
  private final String id;

  /** The offset of the parent blip where this thread is inlined. */
  private int location;

  /** A list of ids of all blips that are in this thread. */
  private final List<String> blipIds;

  /** A map of blips of the wavelet to which this thread belongs to. */
  @NonJsonSerializable private final Map<String, Blip> blips;

  /**
   * Constructor.
   *
   * @param id the id of the thread.
   * @param location the location or offset of this thread in the containing
   *     blip. This should be {@code -1} if this is not an inline thread.
   * @param blipIds the ids of the blips that are in this thread.
   * @param blips a map of blips of the wavelet to which this thread belongs to.
   */
  public BlipThread(String id, int location, List<String> blipIds, Map<String, Blip> blips) {
    this.id = id;
    this.location = location;
    this.blipIds = blipIds;
    this.blips = blips;
  }

  /**
   * @return the id of the thread.
   */
  public String getId() {
    return id;
  }

  /**
   * @return the location or offset of this thread in the containing or parent
   *     blip. This method will return {@code -1} if this {@link BlipThread} is not
   *     an inline thread.
   */
  public int getLocation() {
    return location;
  }

  /**
   * Sets the location of the thread.
   *
   * @param location the new location.
   */
  public void setLocation(int location) {
    this.location = location;
  }

  /**
   * @return a list of all blip ids that are in this thread.
   */
  public List<String> getBlipIds() {
    return blipIds;
  }

  /**
   * @return all available blips that are in this thread.
   */
  public List<Blip> getBlips() {
    List<Blip> result = new ArrayList<Blip>(blipIds.size());
    for (String blipId : blipIds) {
      Blip blip = blips.get(blipId);
      if (blip != null) {
        result.add(blips.get(blipId));
      }
    }
    return result;
  }

  /**
   * Appends a blip to the end of this thread.
   *
   * @param blip the blip to append.
   */
  void appendBlip(Blip blip) {
    blipIds.add(blip.getBlipId());
  }

  /**
   * Removes a blip from this thread.
   *
   * @param blip the blip to remove.
   * @return {@code true} if this thread contained the given id, and removal was
   *     successful.
   */
  boolean removeBlip(Blip blip) {
    return blipIds.remove(blip.getBlipId());
  }

  /**
   * @return {@code true} if this thread has no blips.
   */
  boolean isEmpty() {
    return blipIds.isEmpty();
  }
}
