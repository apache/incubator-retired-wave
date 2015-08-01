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

package org.waveprotocol.wave.model.document.operation.util;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.util.ImmutableUpdateMap.AttributeUpdate;

import java.util.Arrays;

/**
 * @author ohler@google.com (Christian Ohler)
 */
public class ImmutableUpdateMapTest extends TestCase {

  public void testCheckUpdatesSorted() {
    // see also the corresponding tests in ImmutableStateMapTest.
    ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(new AttributeUpdate[] {}));
    ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(new AttributeUpdate("a", null, "1")));
    ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(
        new AttributeUpdate("aa", "0", "1"),
        new AttributeUpdate("ab", null, null)));
    ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(
        new AttributeUpdate("a", "0", null),
        new AttributeUpdate("b", "p", "2"),
        new AttributeUpdate("c", "1", "1")));
    try {
      ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(
          new AttributeUpdate("asdfa", "a", "1"),
          new AttributeUpdate("asdfb", "2", null),
          new AttributeUpdate("asdfb", "2", "3")));
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
    try {
      ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(
          new AttributeUpdate("rar", null, "1"),
          new AttributeUpdate("rar", "2", null),
          new AttributeUpdate("rbr", "1", "2")));
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
    try {
      ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(
          null,
          new AttributeUpdate("a", "2", "1")));
      fail();
    } catch (NullPointerException e) {
      // ok
    }
    try {
      ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(
          new AttributeUpdate("a", "2", "a"),
          null));
      fail();
    } catch (NullPointerException e) {
      // ok
    }
    try {
      ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(
          new AttributeUpdate("a", "1", "j"),
          new AttributeUpdate("a", "1", "r")));
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
    try {
      ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(
          new AttributeUpdate("a", "1", "f"),
          null,
          new AttributeUpdate("c", "a", "1")));
      fail();
    } catch (NullPointerException e) {
      // ok
    }
    try {
      ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(
          null,
          new AttributeUpdate("a", "1", "o"),
          new AttributeUpdate("c", "1", "l")));
      fail();
    } catch (NullPointerException e) {
      // ok
    }
    try {
      ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(
          new AttributeUpdate("a", "1", "y"),
          new AttributeUpdate("c", "1", ";"),
          null));
      fail();
    } catch (NullPointerException e) {
      // ok
    }
    try {
      ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(
          new AttributeUpdate("ard", "1", "3"),
          new AttributeUpdate("ard", "1", "2"),
          null));
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
    try {
      ImmutableUpdateMap.checkUpdatesSorted(Arrays.asList(
          new AttributeUpdate("a", "1", null),
          new AttributeUpdate("c", "2", null),
          new AttributeUpdate("b", "3", null)));
      fail();
    } catch (IllegalArgumentException e) {
      // ok
    }
  }

}
