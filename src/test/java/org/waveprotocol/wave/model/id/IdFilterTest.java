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


import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Tests for IdFilter
 *
 * @author anorth@google.com (Alex North)
 */

public class IdFilterTest extends TestCase {
  /**
   * Tests that the ids and prefixes provided at construction are accurately
   * reflected in accessors.
   */
  public void testConstructionCopiesFilters() {
    List<WaveletId> ids = Arrays.asList(wid("foo"), wid("bar"));
    List<String> prefixes = Arrays.asList("baz", "42");

    IdFilter filter = IdFilter.of(ids, prefixes);
    assertCollectionEquals(ids, filter.getIds());
    assertCollectionEquals(prefixes, filter.getPrefixes());
  }

  public void testAllIds() {
    assertEquals(Collections.<WaveletId>emptySet(), IdFilters.ALL_IDS.getIds());
    assertEquals(Collections.singleton(""), IdFilters.ALL_IDS.getPrefixes());
  }

  public void testNoIds() {
    assertEquals(Collections.<WaveletId>emptySet(), IdFilters.NO_IDS.getIds());
    assertEquals(Collections.<String>emptySet(), IdFilters.NO_IDS.getPrefixes());
  }

  /**
   * Tests that ofIds() creates a filter matching specified ids.
   */
  public void testFilterOfIdsMatchesIds() {
    WaveletId id1 = wid("id1");
    WaveletId id2 = wid("id2");
    IdFilter filter = IdFilter.ofIds(id1, id2);
    assertCollectionEquals(Arrays.asList(id1, id2), filter.getIds());
    assertCollectionEquals(Arrays.<String> asList(), filter.getPrefixes());
  }

  /**
   * Tests that ofPrefixes() creates a filter matching specified filters.
   */
  public void testFilterOfPrefixesMatchesPrefixes() {
    IdFilter filter = IdFilter.ofPrefixes("pf1", "pf2");
    assertCollectionEquals(Arrays.asList("pf1", "pf2"), filter.getPrefixes());
    assertCollectionEquals(Arrays.<WaveletId> asList(), filter.getIds());
  }

  /**
   * Tests that the filter of all ids matches some string.
   */
  public void testAllFilterMatchesAnything() {
    assertTrue(IdFilter.accepts(IdFilters.ALL_IDS, wid("anystring")));
  }

  /**
   * Tests that the filter of no ids doesn't match some string.
   */
  public void testNoFilterDoesntMatchSomething() {
    assertFalse(IdFilter.accepts(IdFilters.NO_IDS, wid("anystring")));
  }

  /**
   * Tests matches against a single id filter.
   */
  public void testSingleIdMatch() {
    IdFilter filter = IdFilter.ofIds(wid("match-id"));
    assertTrue(IdFilter.accepts(filter, wid("match-id")));
    assertFalse(IdFilter.accepts(filter, wid("match-id-longer")));
    assertFalse(IdFilter.accepts(filter, wid("anystring")));
  }

  /**
   * Tests matches against a multiple id filter.
   */
  public void testMultipleIdMatch() {
    IdFilter filter = IdFilter.ofIds(wid("match-id-1"),
      wid("match-id-2"));
    assertTrue(IdFilter.accepts(filter, wid("match-id-1")));
    assertTrue(IdFilter.accepts(filter, wid("match-id-2")));
    assertFalse(IdFilter.accepts(filter, wid("anystring")));
  }

  /**
   * Tests matches against a single prefix filter.
   */
  public void testSinglePrefixMatch() {
    IdFilter filter = IdFilter.ofPrefixes("match-id");
    assertTrue(IdFilter.accepts(filter, wid("match-id")));
    assertTrue(IdFilter.accepts(filter, wid("match-id-n")));
    assertFalse(IdFilter.accepts(filter, wid("match-")));
    assertFalse(IdFilter.accepts(filter, wid("anystring")));
  }

  /**
   * Tests matches against a multiple prefix filter.
   */
  public void testMultiplePrefixMatch() {
    IdFilter filter = IdFilter.ofPrefixes("match-", "match-id", "other-match");
    assertTrue(IdFilter.accepts(filter, wid("match-")));
    assertTrue(IdFilter.accepts(filter, wid("match-id-n")));
    assertTrue(IdFilter.accepts(filter, wid("other-match-n")));
    assertFalse(IdFilter.accepts(filter, wid("other")));
  }

  /**
   * Tests matches against a filter with multiple ids and prefixes.
   */
  public void testMutilpleIdAndFilterMatch() {
    List<WaveletId> ids = Arrays.asList(wid("id-1"), wid("id-2"));
    List<String> prefixes = Arrays.asList("first", "second");
    IdFilter filter = IdFilter.of(ids, prefixes);

    assertTrue(IdFilter.accepts(filter, wid("id-1")));
    assertTrue(IdFilter.accepts(filter, wid("first-id1")));
    assertTrue(IdFilter.accepts(filter, wid("second-id2")));
    assertFalse(IdFilter.accepts(filter, wid("id-3")));
    assertFalse(IdFilter.accepts(filter, wid("fi")));
  }

  public WaveletId wid(String waveletId) {
    return WaveletId.of("example.com", waveletId);
  }

  /**
   * Asserts that two collections contain equivalent sets of elements.
   */
  private static <T> void assertCollectionEquals(Collection<T> c1, Collection<T> c2) {
    assertTrue(c1.containsAll(c2));
    assertTrue(c2.containsAll(c1));
  }
}
