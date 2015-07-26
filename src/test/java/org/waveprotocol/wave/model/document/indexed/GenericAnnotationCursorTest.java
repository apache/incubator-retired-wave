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

package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.AnnotationCursorTestBase;
import org.waveprotocol.wave.model.document.util.GenericAnnotationCursor;

/**
 * Tests the SimpleAnnotationSet data structure
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class GenericAnnotationCursorTest extends AnnotationCursorTestBase {

  @Override
  protected RawAnnotationSet<Object> getNewSet() {
    SimpleAnnotationSet set = new SimpleAnnotationSet(null);
    // Just to make sure we are in fact testing the generic annotation cursor
    assertTrue(set.annotationCursor(0, 0, strs()) instanceof GenericAnnotationCursor);
    return set;
  }

}
