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

package org.waveprotocol.wave.client.editor.content.paragraph;


import org.waveprotocol.wave.client.editor.content.paragraph.OrderedListRenumberer.LevelNumbers;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class RenumbererTest extends RenumbererTestBase {

  public void testLevelNumbers() {
    LevelNumbers numbers = new LevelNumbers(0, 1);

    numbers.setLevel(0);
    assertEquals(1, numbers.getNumberAndIncrement());
    assertEquals(2, numbers.getNumberAndIncrement());
    assertEquals(3, numbers.getNumberAndIncrement());
    numbers.setLevel(1);
    assertEquals(1, numbers.getNumberAndIncrement());
    assertEquals(2, numbers.getNumberAndIncrement());
    numbers.setLevel(0);
    assertEquals(4, numbers.getNumberAndIncrement());
    assertEquals(5, numbers.getNumberAndIncrement());
    numbers.setLevel(2);
    assertEquals(1, numbers.getNumberAndIncrement());
    assertEquals(2, numbers.getNumberAndIncrement());
    numbers.setLevel(1);
    assertEquals(1, numbers.getNumberAndIncrement());
    assertEquals(2, numbers.getNumberAndIncrement());
    numbers.setNumber(2);
    assertEquals(2, numbers.getNumberAndIncrement());
    assertEquals(3, numbers.getNumberAndIncrement());
  }


  public void testSimple0() {
    update(0, Type.DECIMAL, 0);
    update(1, Type.DECIMAL, 0);
    update(2, Type.DECIMAL, 0);
    check();

    update(0, Type.HEADING, 0);
    check();
  }

  public void testSimple1() {
    update(0, Type.DECIMAL, 0);
    update(1, Type.DECIMAL, 0);
    update(2, Type.DECIMAL, 0);
    check();

    update(1, Type.DECIMAL, 1);
    check();
  }

  public void testSimple2() {
    update(0, Type.DECIMAL, 0);
    update(1, Type.DECIMAL, 0);
    update(2, Type.DECIMAL, 0);
    update(3, Type.DECIMAL, 0);
    update(4, Type.DECIMAL, 0);
    check();
    update(2, Type.DECIMAL, 1);
    update(3, Type.DECIMAL, 1);
    check();
  }

  public void testSimple3() {
    update(0, Type.DECIMAL, 0);
    update(1, Type.DECIMAL, 0);
    update(2, Type.DECIMAL, 0);
    update(3, Type.DECIMAL, 0);
    update(4, Type.DECIMAL, 0);
    check();
    update(1, Type.DECIMAL, 3);
    update(2, Type.DECIMAL, 1);
    update(3, Type.DECIMAL, 1);
    check();
  }

  public void testSimpleAdds1() {
    update(0, Type.DECIMAL, 0);
    check();
    create(1, Type.DECIMAL, 0, true);
    check();
  }

  public void testSimpleAdds2() {
    update(0, Type.DECIMAL, 0);
    check();
    create(1, Type.DECIMAL, 0, false);
    check();
  }

  public void testSimpleAdds3() {
    update(0, Type.DECIMAL, 0);
    update(1, Type.DECIMAL, 0);
    update(2, Type.DECIMAL, 0);
    check();
    create(1, Type.NONE, 0, true);
    check();
  }

  public void testSimpleDeletes1() {
    update(0, Type.DECIMAL, 0);
    update(1, Type.DECIMAL, 0);
    update(2, Type.DECIMAL, 0);
    check();

    delete(1);
    check();
  }

  public void testSimpleDeletes2() {
    update(0, Type.DECIMAL, 0);
    update(1, Type.DECIMAL, 1);
    update(2, Type.NONE, 0);
    update(3, Type.DECIMAL, 1);
    update(4, Type.DECIMAL, 0);
    check();

    delete(2);
    check();
  }

  public void testSimpleDeletes3() {
    update(0, Type.DECIMAL, 0);
    update(1, Type.DECIMAL, 1);
    update(2, Type.NONE, 0);
    update(3, Type.NONE, 1);
    update(4, Type.DECIMAL, 1);
    update(5, Type.DECIMAL, 0);
    check();

    delete(2);
    delete(3);
    check();
  }

  public void testSimple4() {
    update(0, Type.DECIMAL, 2);
    update(1, Type.DECIMAL, 2);
    update(2, Type.DECIMAL, 0);
    update(3, Type.DECIMAL, 1);
    update(4, Type.DECIMAL, 3);
    update(5, Type.DECIMAL, 2);
    update(6, Type.DECIMAL, 0);
    check();

    update(1, Type.DECIMAL, 2, false);
    update(2, Type.DECIMAL, 3, true);
    delete(2);
    check();
  }

  public void testSimple5() {
    update(0, Type.DECIMAL, 0);
    update(1, Type.DECIMAL, 0);
    update(2, Type.DECIMAL, 0);
    check();

    doc.setElementAttribute(getLineElement(1), "i", "1");
    check();
    printInfo(null, null);

    doc.setElementAttribute(getLineElement(1), "i", null);
    check();
    printInfo(null, null);
  }

  public void testQuickRandom() {
    doRandomTest(80, 0);
  }
}
