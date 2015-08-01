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

package com.google.wave.api;

import junit.framework.TestCase;

/*
 * Test cases for {@link Annotations}.
 */
public class AnnotationRobotTest extends TestCase {

  public void testShift() throws Exception {
    Annotation annotation = new Annotation("key", "value", 5 , 10);
    annotation.shift(4, 2);
    assertEquals(7, annotation.getRange().getStart());
    assertEquals(12, annotation.getRange().getEnd());

    annotation = new Annotation("key", "value", 5 , 10);
    annotation.shift(5, 3);
    assertEquals(8, annotation.getRange().getStart());
    assertEquals(13, annotation.getRange().getEnd());

    annotation = new Annotation("key", "value", 5 , 10);
    annotation.shift(9, 2);
    assertEquals(5, annotation.getRange().getStart());
    assertEquals(12, annotation.getRange().getEnd());

    annotation = new Annotation("key", "value", 5 , 10);
    annotation.shift(10, 2);
    assertEquals(5, annotation.getRange().getStart());
    assertEquals(12, annotation.getRange().getEnd());
  }
}
