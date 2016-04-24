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

package org.waveprotocol.wave.model.document.util;


import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.RangedAnnotationIterableTestBase;
import org.waveprotocol.wave.model.document.indexed.AnnotationTree;
import org.waveprotocol.wave.model.document.indexed.RawAnnotationSet;
import org.waveprotocol.wave.model.util.ReadableStringSet;

/**
 * Test for GenericRangedAnnotationIterable.
 *
 * @author ohler@google.com (Christian Ohler)
 */

public class GenericRangedAnnotationIterableTest extends RangedAnnotationIterableTestBase {
  @Override
  protected Iterable<? extends RangedAnnotation<Object>> getIterable(RawAnnotationSet<Object> set,
      int start, int end, ReadableStringSet keys) {
    return new GenericRangedAnnotationIterable<Object>(set, start, end, keys);
  }

  @Override
  protected AnnotationTree<Object> getNewSet() {
    return new AnnotationTree<Object>("a", "b", null);
  }
}
