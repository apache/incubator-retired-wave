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

package org.waveprotocol.wave.model.version;

import junit.framework.TestCase;

/**
 * Tests for {@link HashedVersion}.
 *
 * @author anorth@google.com (Alex North)
 */
public class HashedVersionTest extends TestCase {

  private static final byte[] AAA = "AAA".getBytes();
  private static final byte[] BBB = "BBB".getBytes();

  public void testEquality() {
    assertHashedVersionsEqual(10, AAA, 10, AAA);

    assertHashedVersionsDiffer(10, AAA, 10, BBB);
    assertHashedVersionsDiffer(10, AAA, 12, AAA);
    assertHashedVersionsDiffer(10, AAA, 12, BBB);
  }

  public void testComparisons() {
    assertHashedVersionsCompare(10, AAA, 10, AAA, 0);

    // Different versions.
    assertHashedVersionsCompare(10, AAA, 12, AAA, -1);
    // Different hashes.
    assertHashedVersionsCompare(10, AAA, 10, BBB, -1);
    // Both different, version takes precedence.
    assertHashedVersionsCompare(10, BBB, 12, AAA, -1);
  }

  public void testUnsigned() {
    assertEquals(10L, HashedVersion.unsigned(10).getVersion());
    assertEquals(0, HashedVersion.unsigned(10).getHistoryHash().length);
  }

  public void testUnsignedComparisons() {
    assertEquals(0, HashedVersion.unsigned(10).compareTo(HashedVersion.unsigned(10)));
    // unsigned(10) is smaller than every other hashed version with version number 10
    assertEquals(-1, HashedVersion.unsigned(10).compareTo(HashedVersion.of(10, AAA)));
    // and, of course, bigger than hashed versions with smaller version number
    assertEquals(1, HashedVersion.unsigned(10).compareTo(HashedVersion.of(9, AAA)));
  }

  private static void assertHashedVersionsEqual(long v1, byte[] h1, long v2, byte[] h2) {
    assertEquals(HashedVersion.of(v1, h1), HashedVersion.of(v2, h2));
  }

  private static void assertHashedVersionsDiffer(long v1, byte[] h1, long v2, byte[] h2) {
    assertFalse(HashedVersion.of(v1, h1).equals(HashedVersion.of(v2, h2)));
  }

  private static void assertHashedVersionsCompare(long v1, byte[] h1, long v2, byte[] h2, int cmp) {
    assertEquals(cmp, HashedVersion.of(v1, h1).compareTo(HashedVersion.of(v2, h2)));
    assertEquals(-cmp, HashedVersion.of(v2, h2).compareTo(HashedVersion.of(v1, h1)));
  }
}
