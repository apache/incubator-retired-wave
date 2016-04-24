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
 * Tests for {@link HashedVersionSerializer}.
 *
 * @author anorth@google.com (Alex North)
 */
public class HashedVersionSerializerTest extends TestCase {

  private static final HashedVersion TYPICAL = HashedVersion.of(5678, new byte[] {
      -127, -45, 0, 45, 110, 127
  });
  private static final HashedVersion UNSIGNED = HashedVersion.unsigned(5678);

  public void testSerializeDeserialize() {
    assertEquals("5678:gdMALW5/", serialize(TYPICAL));
    assertEquals("5678:", serialize(UNSIGNED));

    assertEquals(TYPICAL, HashedVersionSerializer.INSTANCE.fromString(null, TYPICAL));

    assertEquals(null, deserialize(serialize(null)));
    assertEquals(TYPICAL, deserialize(serialize(TYPICAL)));
    assertEquals(UNSIGNED, deserialize(serialize(UNSIGNED)));
  }

  private static final String serialize(HashedVersion v) {
    return HashedVersionSerializer.INSTANCE.toString(v);
  }

  private static final HashedVersion deserialize(String s) {
    return HashedVersionSerializer.INSTANCE.fromString(s);
  }
}
