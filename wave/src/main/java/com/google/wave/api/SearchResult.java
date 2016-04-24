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

import java.util.ArrayList;
import java.util.List;

/**
 * SearchResult is returned from a search request.
 *
 */
public class SearchResult {
  
  /**
   * Digest contains the digest information for one 'hit' in the result.
   */
  public static class Digest {
    private final String title;
    private final String snippet;
    private final String waveId;
    private final long lastModified;
    private final long created;
    private final int unreadCount;
    private final int blipCount;
    private final List<String> participants;

    public Digest(String title, String snippet, String waveId, List<String> participants,
                  long lastModified, long created, int unreadCount, int blipCount) {
      this.title = title;
      this.snippet = snippet;
      this.waveId = waveId;
      this.participants = new ArrayList<String>(participants);
      this.lastModified = lastModified;
      this.created = created;
      this.unreadCount = unreadCount;
      this.blipCount = blipCount;
    }

    public String getTitle() {
      return title;
    }

    public String getSnippet() {
      return snippet;
    }

    public String getWaveId() {
      return waveId;
    }

    public List<String> getParticipants() {
      return participants;
    }

    public long getLastModified() {
      return lastModified;
    }
    
    public long getCreated() {
      return created;
    }

    public int getUnreadCount() {
      return unreadCount;
    }

    public int getBlipCount() {
      return blipCount;
    }
  }

  private final String query;
  private int numResults;
  private final List<Digest> digests = new ArrayList<Digest>(10);

  public SearchResult(String query) {
    this.query = query;
    this.numResults = 0;
  }

  /**
   * Add a result to the set
   * @param digest to add
   */
  public void addDigest(Digest digest) {
    numResults += 1;
    digests.add(digest);
  }

  /**
   * @returns the query associated with this result
   */
  public String getQuery() {
    return query;
  }

  /**
   * @returns the number of results
   */
  public int getNumResults() {
    return numResults;
  }

  /**
   * @returns the digests for the result
   */
  public List<Digest> getDigests() {
    return digests;
  }
}
