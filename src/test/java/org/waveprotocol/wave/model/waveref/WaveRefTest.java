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

package org.waveprotocol.wave.model.waveref;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

/**
 * Unit tests for {@link WaveRef}
 *
 * @author meade@google.com <Edwina Mead>
 */
public class WaveRefTest extends TestCase {

  public void testBasicEquals() {
    WaveRef first = WaveRef.of(WaveId.of("example.com", "w+1234abcd"));
    WaveRef second = WaveRef.of(WaveId.of("example.com", "w+1234abcd"));
    WaveRef different = WaveRef.of(WaveId.of("test.com", "w+1234abcd"));

    assertFalse(first.equals(null));
    assertTrue(first.equals(first));
    assertTrue(first.equals(second));
    assertFalse(first.equals(different));
  }

  public void testEqualsWithSameWaveIdDifferentOtherFields() {
    WaveRef first = WaveRef.of(WaveId.of("example.com", "w+1234abcd"));
    WaveRef second = WaveRef.of(WaveId.of("example.com", "w+1234abcd"),
        WaveletId.of("example.com", "conv+root"));
    WaveRef third = WaveRef.of(WaveId.of("example.com", "w+1234abcd"),
        WaveletId.of("example.com", "conv+root"),
        "b+12345");

    assertTrue(second.equals(second));
    assertTrue(third.equals(third));

    assertFalse(first.equals(second));
    assertFalse(first.equals(third));
    assertFalse(second.equals(third));
  }

  public void testEqualsWithDifferentWaveIdSameOtherFields() {
    WaveRef first = WaveRef.of(WaveId.of("test.com", "w+1234"),
        WaveletId.of("example.com", "conv+root"),
        "b+12345");
    WaveRef second = WaveRef.of(WaveId.of("example.com", "w+1234"),
        WaveletId.of("example.com", "conv+root"),
        "b+12345");

    assertFalse(first.equals(second));
  }

  public void testHashCode() {
    WaveRef first = WaveRef.of(WaveId.of("example.com", "w+1234"));
    WaveRef second = WaveRef.of(WaveId.of("example.com", "w+1234"),
        WaveletId.of("example.com", "conv+root"));
    WaveRef third = WaveRef.of(WaveId.of("example.com", "w+1234"),
        WaveletId.of("example.com", "conv+root"), "b+12345");

    WaveRef sameAsFirst = WaveRef.of(WaveId.of("example.com", "w+1234"));
    WaveRef sameAsThird = WaveRef.of(WaveId.of("example.com", "w+1234"),
        WaveletId.of("example.com", "conv+root"), "b+12345");

    assertEquals(first.hashCode(), sameAsFirst.hashCode());
    assertEquals(third.hashCode(), sameAsThird.hashCode());

    assertFalse(first.hashCode() == second.hashCode());
    assertFalse(first.hashCode() == third.hashCode());
    assertFalse(second.hashCode() == third.hashCode());
  }
}
