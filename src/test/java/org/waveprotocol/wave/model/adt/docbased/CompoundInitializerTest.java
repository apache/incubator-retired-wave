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

package org.waveprotocol.wave.model.adt.docbased;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Collections;
import java.util.Map;

/**
 * Tests for the composing initializer.
 *
 * @author anorth@google.com (Alex North)
 */
public class CompoundInitializerTest extends TestCase {

  public void testEmptyCompoundInitialiserDoesNothing() {
    CompoundInitializer init = new CompoundInitializer();
    TestUtil.assertInitializerValues(Collections.<String, String> emptyMap(), init);
  }

  public void testCompoundInitialiserComposesAttributes() {
    CompoundInitializer init = new CompoundInitializer(new Initializer() {
      public void initialize(Map<String, String> target) {
        target.put("k1", "v1");
      }
    }, new Initializer() {
      public void initialize(Map<String, String> target) {
        target.put("k2", "v2");
      }
    });
    TestUtil.assertInitializerValues(CollectionUtils.immutableMap("k1", "v1", "k2", "v2"), init);
  }
}
