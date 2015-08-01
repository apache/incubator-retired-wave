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

package org.waveprotocol.wave.model.id;

import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * An immutable filter representing a (possibly infinite) subset of all possible
 * WaveletIds.
 *
 * A filter comprises a (possibly empty) set of ids to explicitly accept, plus a
 * (possibly empty) set of id prefixes to accept.
 *
 * Matches against WaveletIds must be exact, but when matching a prefix, only
 * the {@link WaveletId#getId() local id} part of the WaveletId is considered.
 * Thus a WaveletId is accepted by an IdFilter if it exactly equals one of the
 * specified WaveletIds, or if its local id part starts with one of the
 * specified prefixes.
 *
 * An IdFilter with prefix "" accepts everything. An IdFilter with no prefix
 * and no id accepts nothing.
 *
 * IdFilter is a value class. Use {@link #accepts(IdFilter, WaveletId)} to
 * determine if a filter accepts an id.
 *
 * @author anorth@google.com (Alex North)
 */
public final class IdFilter {

  /** Creates a filter accepting the specified nonempty list of ids. */
  public static IdFilter ofIds(WaveletId firstId, WaveletId... otherIds) {
    List<WaveletId> waveletIds = new ArrayList<WaveletId>(1 + otherIds.length);
    waveletIds.add(firstId);
    waveletIds.addAll(Arrays.asList(otherIds));
    return of(waveletIds, Collections.<String>emptyList());
  }

  /** Creates a filter accepting the specified nonempty list of prefixes. */
  public static IdFilter ofPrefixes(String firstPrefix, String... otherPrefixes) {
    List<String> prefixes = new ArrayList<String>(1 + otherPrefixes.length);
    prefixes.add(firstPrefix);
    prefixes.addAll(Arrays.asList(otherPrefixes));
    return of(Collections.<WaveletId>emptyList(), prefixes);
  }

  /**
   * Constructs an IdFilter that accepts the specified ids and prefixes.
   *
   * @throws NullPointerException if ids or prefixes is null
   */
  public static IdFilter of(Collection<WaveletId> ids, Collection<String> prefixes) {
    return new IdFilter(ids, prefixes);
  }

  /**
   * Checks whether a wavelet id is accepted by filter.
   */
  public static boolean accepts(IdFilter filter, WaveletId id) {
    boolean match = filter.ids.contains(id);
    Iterator<String> itr = filter.prefixes.iterator();
    while (itr.hasNext() && !match) {
      match = id.getId().startsWith(itr.next());
    }
    return match;
  }

  private final Set<WaveletId> ids;
  private final Set<String> prefixes;

  /**
   * Constructs an IdFilter that accepts the specified ids and prefixes.
   */
  private IdFilter(Collection<WaveletId> ids, Collection<String> prefixes) {
    Preconditions.checkNotNull(ids, "null ids");
    Preconditions.checkNotNull(prefixes, "null prefixes");
    this.ids = Collections.unmodifiableSet(new HashSet<WaveletId>(ids));
    this.prefixes = Collections.unmodifiableSet(new HashSet<String>(prefixes));
  }

  /**
   * Gets an unmodifiable view on the WaveletIds covered by this filter.
   */
  public Set<WaveletId> getIds() {
    return ids;
  }

  /**
   * Gets an unmodifiable view on the id prefixes covered by this filter.
   */
  public Set<String> getPrefixes() {
    return prefixes;
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof IdFilter) {
      IdFilter that = (IdFilter) obj;
      return this.ids.equals(that.ids) && this.prefixes.equals(that.prefixes);
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return (17 + 31 * ids.hashCode()) * 31 + prefixes.hashCode();
  }

  @Override
  public final String toString() {
    StringBuilder resultBuilder = new StringBuilder("IdFilter[");
    // Distinguish no prefixes from a single empty prefix in String representation
    if (!prefixes.isEmpty()) {
      resultBuilder.append("prefixes=").append(prefixes);
    }
    if (!ids.isEmpty()) {
      if (!prefixes.isEmpty()) {
        resultBuilder.append(", ");
      }
      resultBuilder.append("ids=").append(ids);
    }
    return resultBuilder.append("]").toString();
  }
}
