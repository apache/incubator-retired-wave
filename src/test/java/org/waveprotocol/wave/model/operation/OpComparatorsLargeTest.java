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

package org.waveprotocol.wave.model.operation;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.bootstrap.BootstrapDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.operation.OpComparators.OpEquator;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator;
import org.waveprotocol.wave.model.testing.RandomProviderImpl;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.Parameters;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.RandomProvider;


public class OpComparatorsLargeTest extends TestCase {


  // This file focuses on negative tests. TODO: Add positive tests.

  public void testNullable() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    assertTrue(eq.equalNullable((DocOp) null, null));
    assertFalse(eq.equalNullable(null, new DocOpBuffer().finish()));
    assertFalse(eq.equalNullable(new DocOpBuffer().finish(), null));

    try {
      eq.equal(new DocOpBuffer().finish(), null);
      fail();
    } catch (NullPointerException e) {
      // ok
    }

    try {
      eq.equal(null, new DocOpBuffer().finish());
      fail();
    } catch (NullPointerException e) {
      // ok
    }
  }

  public void testDocOp() {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    DocOpBuffer ba1 = new DocOpBuffer();
    ba1.characters("a");
    DocOp a1 = ba1.finish();

    DocOpBuffer ba2 = new DocOpBuffer();
    ba2.characters("a");
    DocOp a2 = ba2.finish();

    DocOpBuffer bb1 = new DocOpBuffer();
    bb1.deleteCharacters("a");
    DocOp b1 = bb1.finish();

    DocOpBuffer bb2 = new DocOpBuffer();
    bb2.deleteCharacters("a");
    DocOp b2 = bb1.finish();

    assertTrue(eq.equal(a1, a1));
    assertTrue(eq.equal(a1, a2));
    assertTrue(eq.equal(a2, a1));
    assertTrue(eq.equal(a2, a2));

    assertTrue(eq.equal(b1, b1));
    assertTrue(eq.equal(b1, b2));
    assertTrue(eq.equal(b2, b1));
    assertTrue(eq.equal(b2, b2));

    assertFalse(eq.equal(a1, b1));
    assertFalse(eq.equal(a1, b2));
    assertFalse(eq.equal(a2, b1));
    assertFalse(eq.equal(a2, b2));
  }

  /**
   * Tests a bugfix before which there was a possible ambiguity with annotation
   * keys containing spaces: Ending the single annotation 'x y' could not be
   * distinguished from ending the two annotations 'x' and 'y'. This case
   * verifies that the two cases are considered distinct.
   */
  public void testEqualHandlesSpacesInAnnotationKeys() {
    DocInitialization doc1 = new DocInitializationBuilder()
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .updateValues(
                "x", null, "1",
                "x y", null, "3",
                "y", null, "2").build())
        .characters("m")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x", "y").build())
        .characters("n")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x y").build())
        .build();

    DocInitialization doc2 = new DocInitializationBuilder()
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .updateValues(
                "x", null, "1",
                "x y", null, "3",
                "y", null, "2").build())
        .characters("m")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x y").build())
        .characters("n")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x", "y").build())
        .build();
    assertFalse("\ndoc1: " + doc1 + "\ndoc2: " + doc2,
        OpComparators.SYNTACTIC_IDENTITY.equal(doc1, doc2));
    assertFalse("\ndoc1: " + DocOpUtil.toXmlString(doc1) + "\ndoc2: " + DocOpUtil.toXmlString(doc2),
        OpComparators.equalDocuments(doc1, doc2));
  }

  /**
   * Tests that annotation keys can contain double quotes (") without causing
   * any ambiguity in equality checks.
   */
  public void testEqualHandlesQuotesInAnnotationKeys() {
    DocInitialization doc1 = new DocInitializationBuilder()
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .updateValues(
                "x", null, "1",
                "x\" \"y", null, "3",
                "y", null, "2").build())
        .characters("m")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x", "y").build())
        .characters("n")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x\" \"y").build())
        .build();

    DocInitialization doc2 = new DocInitializationBuilder()
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .updateValues(
                "x", null, "1",
                "x\" \"y", null, "3",
                "y", null, "2").build())
        .characters("m")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x\" \"y").build())
        .characters("n")
        .annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd("x", "y").build())
        .build();
    assertFalse("\ndoc1: " + doc1 + "\ndoc2: " + doc2,
        OpComparators.SYNTACTIC_IDENTITY.equal(doc1, doc2));
    assertFalse("\ndoc1: " + DocOpUtil.toXmlString(doc1) + "\ndoc2: " + DocOpUtil.toXmlString(doc2),
        OpComparators.equalDocuments(doc1, doc2));
  }

  public void testRandomDocOps() throws OperationException {
    OpEquator eq = OpComparators.SYNTACTIC_IDENTITY;

    Parameters p = new Parameters();
    for (int i = 0; i < 200; i++) {
      BootstrapDocument doc = new BootstrapDocument();
      for (int j = 0; j < 20; j++) {
        RandomProvider ra = RandomProviderImpl.ofSeed(i * 20 + j);
        RandomProvider rb = RandomProviderImpl.ofSeed(i * 20 + j + 1);
        DocOp a = RandomDocOpGenerator.generate(ra, p, doc);
        DocOp b = RandomDocOpGenerator.generate(rb, p, doc);
        doc.consume(a);
        assertTrue(eq.equal(a, a));
        // The combination of RandomProvider and RandomDocOpGenerator doesn't
        // really guarantee this property, but it happens to be true with the
        // random seeds that occur here.
        assertFalse(eq.equal(a, b));
      }
    }
  }

}
