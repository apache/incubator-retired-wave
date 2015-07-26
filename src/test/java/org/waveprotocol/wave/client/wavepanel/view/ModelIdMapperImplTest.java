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

package org.waveprotocol.wave.client.wavepanel.view;


import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;


public class ModelIdMapperImplTest extends TestCase {

  public void testMapping() throws Exception {
    ModelIdMapperImpl impl = ModelIdMapperImpl.create(null, "empty");
    verifyMapper(impl);
  }

  private void verifyMapper(ModelIdMapperImpl impl) {
    HashSet<String> shortIdSet = new HashSet<String>();
    HashSet<String> longIdSet = new HashSet<String>();
    ArrayList<String> shortIds = new ArrayList<String>();
    ArrayList<String> longIds = new ArrayList<String>();

    Random random = new Random();

    // ensure unique
    while (longIdSet.size() < 100) {
      String longId = Integer.toBinaryString(random.nextInt());
      while (longIdSet.contains(longId)) {
        longId = Integer.toBinaryString(random.nextInt());
      }
      longIdSet.add(longId);

      String shortId = impl.shorten(longId);
      assertFalse("Short Id is already in the set " + shortId + " set " + shortIdSet, shortIdSet
          .contains(shortId));
      shortIdSet.add(shortId);

      shortIds.add(shortId);
      longIds.add(longId);
    }

    for (int i = 0; i < shortIds.size(); i++) {
      assertEquals("The shortId returned for repeated longId is not the same", shortIds.get(i),
          impl.shorten(longIds.get(i)));
      assertEquals("Restoring of the shortIds is not the same as long id", longIds.get(i), impl
          .restoreId(shortIds.get(i)));
    }
  }

  public void testCorrectForm() {
    ModelIdMapperImpl impl = new ModelIdMapperImpl(null, "empty", 42);

    assertEquals("Next id not of expected form", "empty42", impl.shorten("1234"));
    verifyMapper(impl);
  }
}
