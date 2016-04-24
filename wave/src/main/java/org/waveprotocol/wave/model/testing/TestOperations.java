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

package org.waveprotocol.wave.model.testing;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;

public class TestOperations {

  // The test case for the message-based implementation should also use this.
  public static DocOp getBasicTestOp() {
    DocOpBuffer b = new DocOpBuffer();

    // The operation starts with characters/deleteCharacters of various lengths
    // and case, mixed with some retains and nested element start/end with
    // different mixes of attributes.
    b.characters("hello");
    b.characters("z");
    b.retain(1);
    b.deleteCharacters("ab");
    b.characters("world");
    b.retain(2);
    b.deleteCharacters("cd");
    b.elementStart("a", Attributes.EMPTY_MAP);
    b.characters("hEllo");
    b.elementStart("b", new AttributesImpl("a", "1"));
    b.characters("world");
    b.elementStart("B", new AttributesImpl("A", "1", "b", "abc12"));
    b.elementEnd();
    // A non-ASCII Unicode character.
    b.characters("\u2603");
    b.elementEnd();
    b.elementEnd();
    b.deleteElementStart("a", new AttributesImpl("a", "2", "c", ""));
    b.deleteCharacters("asdf");
    b.deleteElementEnd();

    // Now some replaceAttributes with different size and case.
    b.replaceAttributes(new AttributesImpl("a", "b"), new AttributesImpl("b", "c", "c", "d"));
    b.replaceAttributes(Attributes.EMPTY_MAP, new AttributesImpl("Aa", "aA"));
    b.replaceAttributes(new AttributesImpl("B", "A"), new AttributesImpl());
    // Try both a fresh empty AttributesImpl() instance and the preallocated
    // EMPTY_MAP.
    b.replaceAttributes(new AttributesImpl(), Attributes.EMPTY_MAP);
    // Now we add similar cases for annotation boundaries.  Since consecutive annotation
    // boundaries would make the operation ill-formed, we interleave them with further
    // updateAttributes tests.
    b.annotationBoundary(AnnotationBoundaryMapImpl.builder().build());
    b.updateAttributes(new AttributesUpdateImpl());
    b.annotationBoundary(AnnotationBoundaryMapImpl.builder()
        .updateValues("b", "XZ", "yz", "f-", null, null,
            "g-", "a", null, "k-", "b", "", "r", "", "2")
        .build());
    b.updateAttributes(new AttributesUpdateImpl("a", null, "1"));
    b.annotationBoundary(AnnotationBoundaryMapImpl.builder()
        .initializationEnd("b", "g-", "k-", "r")
        .updateValues("e", "166", null, "f-", null, null)
        .build());
    b.updateAttributes(new AttributesUpdateImpl("P", null, "", ":wq", "ZZ", null));
    b.annotationBoundary(AnnotationBoundaryMapImpl.builder()
        .initializationEnd("e", "f-")
        .build());

    return b.finish();
  }
}
