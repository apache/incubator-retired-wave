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

import com.google.gwt.http.client.Request;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.ValueUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

/**
 * Interface that exposes search services to the client.
 *
 * @author hearnden@google.com (David Hearnden)
 * @author vega113@gmail.com (Yuri Z.)
 */
public interface SearchService {

  public interface Callback {
    void onFailure(String message);

    /**
     * Notifies this callback of a successful search response.
     *
     * @param total total number of results in the search (this is greater or
     *        equal to the size of {@code snapshots}, since not necessarily all
     *        results are returned)
     * @param snapshots some digest snapshots
     */
    void onSuccess(int total, List<DigestSnapshot> snapshots);
  }

  /**
   * An immutable digest.
   */
  public final static class DigestSnapshot implements Digest {
    private final String title;
    private final String snippet;
    private final WaveId waveId;
    private final double lastModified;
    private final int unreadCount;
    private final int blipCount;
    private final ParticipantId author;
    private final List<ParticipantId> participants;

    public DigestSnapshot(String title, String snippet, WaveId waveId, ParticipantId author,
        List<ParticipantId> participants, double lastModified, int unreadCount, int blipCount) {
      this.title = title;
      this.snippet = snippet;
      this.waveId = waveId;
      this.author = author;
      this.participants = participants;
      this.lastModified = lastModified;
      this.unreadCount = unreadCount;
      this.blipCount = blipCount;
    }

    @Override
    public String getTitle() {
      return title;
    }

    @Override
    public String getSnippet() {
      return snippet;
    }

    @Override
    public WaveId getWaveId() {
      return waveId;
    }

    @Override
    public ParticipantId getAuthor() {
      return author;
    }

    @Override
    public List<ParticipantId> getParticipantsSnippet() {
      return participants;
    }

    @Override
    public double getLastModifiedTime() {
      return lastModified;
    }

    @Override
    public int getUnreadCount() {
      return unreadCount;
    }

    @Override
    public int getBlipCount() {
      return blipCount;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((author == null) ? 0 : author.hashCode());
      result = prime * result + blipCount;
      result = prime * result + (int) lastModified;
      result = prime * result + participants.hashCode();
      result = prime * result + ((snippet == null) ? 0 : snippet.hashCode());
      result = prime * result + ((title == null) ? 0 : title.hashCode());
      result = prime * result + unreadCount;
      result = prime * result + waveId.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      DigestSnapshot other = (DigestSnapshot) obj;
      return waveId.equals(other.waveId) //
          && ValueUtils.equal(author, other.author) //
          && participants.equals(other.participants) //
          && ValueUtils.equal(title, other.title) //
          && ValueUtils.equal(snippet, other.snippet) //
          && blipCount == other.blipCount //
          && unreadCount == other.unreadCount //
          && lastModified == other.lastModified;
    }
  }

  /**
   * Symbolic constant to indicate that the total size of the search result is
   * unknown.
   */
  int UNKNOWN_SIZE = -1;

  /**
   * Performs a search.
   *
   * @param query the query to execute.
   * @param index the index from which to return results.
   * @param numResults the maximum number of results to return.
   * @param callback callback through which the search query results are
   *        returned.
   */
  Request search(String query, int index, int numResults, Callback callback);
}
