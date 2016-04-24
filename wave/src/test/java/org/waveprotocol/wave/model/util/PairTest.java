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

package org.waveprotocol.wave.model.util;


import junit.framework.TestCase;


/**
 * Test that pair is a good little well behaved java object
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class PairTest extends TestCase {
  final Pair<String,String> p1 = new Pair<String,String>("Hi", "There");
  final Pair<String,String> p2 = new Pair<String,String>("Hi", "There");
  final Pair<String,String> p3 = new Pair<String,String>("Hi", "Ho");
  final Pair<String,String> p4 = new Pair<String,String>("Yo", "There");

  final Pair<String,String> p5 = new Pair<String,String>(null, "There");
  final Pair<String,String> p6 = new Pair<String,String>("Hi", null);
  final Pair<String,String> p7 = new Pair<String,String>(null, "Ho");
  final Pair<String,String> p8 = new Pair<String,String>("Yo", null);

  final Pair<String,String> p9 = new Pair<String,String>(null, "There");
  final Pair<String,String> p10 = new Pair<String,String>("Hi", null);

  final Pair<String,String> pn = new Pair<String,String>(null, null);
  final Pair<String,String> pn2 = new Pair<String,String>(null, null);

  public void testEquals() {
    // compare with null
    assertTrue(!p1.equals(null));
    assertTrue(!pn.equals(null));

    // identity
    assertEquals(p1, p1);

    // two normal fields
    assertEquals(p1, p2);
    assertTrue(!p1.equals(p3));
    assertTrue(!p1.equals(p4));

    // with one or both fields null
    assertTrue(!p1.equals(p5));
    assertTrue(!p1.equals(p6));
    assertTrue(!p1.equals(pn));

    // nulls with each other
    assertTrue(!p7.equals(p5));
    assertTrue(!p8.equals(p6));
    assertEquals(pn, pn);
    assertEquals(pn, pn2);

    // copy constructor
    assertEquals(p1, new Pair<String, String>(p1));
    assertEquals(new Pair<String, String>(p1), p1);
    assertEquals(p5, new Pair<String, String>(p5));
    assertEquals(p6, new Pair<String, String>(p6));
    assertEquals(pn, new Pair<String, String>(pn));
  }

  public void testHashCode() {
    assertEquals(p1.hashCode(), p2.hashCode());
    assertEquals(p5.hashCode(), p9.hashCode());
    assertEquals(p6.hashCode(), p10.hashCode());
    assertEquals(pn.hashCode(), pn2.hashCode());
  }

  public void testToString() {
    assertEquals("(Hi,There)", p1.toString());
    assertEquals("(null,There)", p5.toString());
    assertEquals("(Hi,null)", p6.toString());
    assertEquals("(null,null)", pn.toString());
  }

}
