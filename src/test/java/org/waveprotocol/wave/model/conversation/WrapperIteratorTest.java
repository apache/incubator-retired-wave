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

package org.waveprotocol.wave.model.conversation;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 */

public class WrapperIteratorTest extends TestCase {

  private static class TestManifest implements HasId {

    private final String id;

    public TestManifest(String id) {
      this.id = id;
    }

    @Override
    public String getId() {
      return id;
    }

  }

  private static List<String> doIteration(List<String> ids, String... wrapMap) {
    StringMap<String> elms = CollectionUtils.createStringMap();
    for (int i = 0; i < wrapMap.length; i += 2) {
      elms.put(wrapMap[i], wrapMap[i + 1]);
    }
    List<TestManifest> manifests = new ArrayList<TestManifest>();
    for (String id : ids) {
      manifests.add(new TestManifest(id));
    }
    List<String> result = new ArrayList<String>();
    WrapperIterator<TestManifest, String> iter = WrapperIterator.create(manifests.iterator(), elms);
    while (iter.hasNext()) {
      result.add(iter.next());
    }
    return result;
  }

  private static void doTest(List<String> expected, List<String> ids, String... wrapMap) {
    assertEquals(expected, doIteration(ids, wrapMap));
  }

  public void testIteration() {
    doTest(Arrays.asList("a", "b", "c"), Arrays.asList("1", "2", "3"),
        "1", "a", "2", "b", "3", "c");
    doTest(Arrays.asList("a", "c"), Arrays.asList("1", "2", "3"),
        "1", "a", "3", "c");
    doTest(Arrays.asList("c"), Arrays.asList("1", "2", "3"), "2", "c");
    doTest(Collections.<String>emptyList(), Collections.<String>emptyList());
    doTest(Arrays.asList("a", "b"), Arrays.asList("1", "2"),
        "1", "a", "2", "b", "3", "c");
  }

}
