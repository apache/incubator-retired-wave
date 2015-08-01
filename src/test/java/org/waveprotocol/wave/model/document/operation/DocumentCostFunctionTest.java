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

package org.waveprotocol.wave.model.document.operation;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;


/**
 * @author ohler@google.com (Christian Ohler)
 */
public class DocumentCostFunctionTest extends TestCase {

  private static final String S0  = "";
  private static final String S1A = "a";
  private static final String S1B = "\uD800\uDC00";  // a surrogate pair
  private static final String S2  = S1B + S1A;
  private static final String S10 = " 12345678*";

  // These constants have X_Y suffixes where X is the number of times the object
  // overhead is incurred, and Y is the total number of code points in strings.

  private static final Attributes ATTR0_0  = new AttributesImpl();
  private static final Attributes ATTR1_1  = new AttributesImpl(S1B, S0);
  private static final Attributes ATTR1_2A = new AttributesImpl(S1A, S1A);
  private static final Attributes ATTR1_2B = new AttributesImpl(S2, S0);
  private static final Attributes ATTR2_2  = new AttributesImpl(S1A, S0, S1B, S0);
  private static final Attributes ATTR2_3  = new AttributesImpl(S1A, S1B, S1B, S0);
  private static final Attributes ATTR2_12 = new AttributesImpl(S1A, S0, S1B, S10);

  private static final AnnotationBoundaryMap ANN0_0 = new AnnotationBoundaryMapBuilder().build();
  private static final AnnotationBoundaryMap ANN1_0A = new AnnotationBoundaryMapBuilder()
      .change(S0, S0, S0).build();
  private static final AnnotationBoundaryMap ANN1_0B = new AnnotationBoundaryMapBuilder()
      .end(S0).build();
  private static final AnnotationBoundaryMap ANN2_1 = new AnnotationBoundaryMapBuilder()
      .change(S0, null, S0).end(S1A).build();
  private static final AnnotationBoundaryMap ANN2_3 = new AnnotationBoundaryMapBuilder()
      .end(S1B).end(S2).build();
  private static final AnnotationBoundaryMap ANN2_13 = new AnnotationBoundaryMapBuilder()
      .change(S10, null, S0).change(S2, null, S1B).build();

  private final DocumentCostFunction f0 = DocumentCostFunction.withObjectOverhead(0);
  private final DocumentCostFunction f1 = DocumentCostFunction.withObjectOverhead(1);
  private final DocumentCostFunction f2 = DocumentCostFunction.withObjectOverhead(2);
  private final DocumentCostFunction f12 = DocumentCostFunction.withObjectOverhead(12);

  public void testComputeStringCost() {
    assertEquals(0, f1.computeCost(S0));
    assertEquals(1, f1.computeCost(S1A));
    assertEquals(1, f1.computeCost(S1B));
    assertEquals(2, f1.computeCost(S2));
    assertEquals(10, f1.computeCost(S10));
  }

  public void testComputeAttributesCost() {
    assertEquals( 0, f0.computeCost(ATTR0_0));
    assertEquals( 1, f0.computeCost(ATTR1_1));
    assertEquals( 2, f0.computeCost(ATTR1_2A));
    assertEquals( 2, f0.computeCost(ATTR1_2B));
    assertEquals( 2, f0.computeCost(ATTR2_2));
    assertEquals( 3, f0.computeCost(ATTR2_3));
    assertEquals(12, f0.computeCost(ATTR2_12));

    assertEquals( 0, f1.computeCost(ATTR0_0));
    assertEquals( 2, f1.computeCost(ATTR1_1));
    assertEquals( 3, f1.computeCost(ATTR1_2A));
    assertEquals( 3, f1.computeCost(ATTR1_2B));
    assertEquals( 4, f1.computeCost(ATTR2_2));
    assertEquals( 5, f1.computeCost(ATTR2_3));
    assertEquals(14, f1.computeCost(ATTR2_12));

    assertEquals( 0, f2.computeCost(ATTR0_0));
    assertEquals( 3, f2.computeCost(ATTR1_1));
    assertEquals( 4, f2.computeCost(ATTR1_2A));
    assertEquals( 4, f2.computeCost(ATTR1_2B));
    assertEquals( 6, f2.computeCost(ATTR2_2));
    assertEquals( 7, f2.computeCost(ATTR2_3));
    assertEquals(16, f2.computeCost(ATTR2_12));

    assertEquals( 0, f12.computeCost(ATTR0_0));
    assertEquals(13, f12.computeCost(ATTR1_1));
    assertEquals(14, f12.computeCost(ATTR1_2A));
    assertEquals(14, f12.computeCost(ATTR1_2B));
    assertEquals(26, f12.computeCost(ATTR2_2));
    assertEquals(27, f12.computeCost(ATTR2_3));
    assertEquals(36, f12.computeCost(ATTR2_12));
  }

  public void testComputeAnnotationBoundaryMapCost() {
    assertEquals( 0, f0.computeCost(ANN0_0));
    assertEquals( 0, f0.computeCost(ANN1_0A));
    assertEquals( 0, f0.computeCost(ANN1_0B));
    assertEquals( 1, f0.computeCost(ANN2_1));
    assertEquals( 3, f0.computeCost(ANN2_3));
    assertEquals(13, f0.computeCost(ANN2_13));

    assertEquals( 0, f1.computeCost(ANN0_0));
    assertEquals( 1, f1.computeCost(ANN1_0A));
    assertEquals( 1, f1.computeCost(ANN1_0B));
    assertEquals( 3, f1.computeCost(ANN2_1));
    assertEquals( 5, f1.computeCost(ANN2_3));
    assertEquals(15, f1.computeCost(ANN2_13));

    assertEquals( 0, f2.computeCost(ANN0_0));
    assertEquals( 2, f2.computeCost(ANN1_0A));
    assertEquals( 2, f2.computeCost(ANN1_0B));
    assertEquals( 5, f2.computeCost(ANN2_1));
    assertEquals( 7, f2.computeCost(ANN2_3));
    assertEquals(17, f2.computeCost(ANN2_13));

    assertEquals( 0, f12.computeCost(ANN0_0));
    assertEquals(12, f12.computeCost(ANN1_0A));
    assertEquals(12, f12.computeCost(ANN1_0B));
    assertEquals(25, f12.computeCost(ANN2_1));
    assertEquals(27, f12.computeCost(ANN2_3));
    assertEquals(37, f12.computeCost(ANN2_13));
  }

  private static final DocInitialization DOC0_0 = new DocInitializationBuilder().build();
  private static final DocInitialization DOC1_0 = new DocInitializationBuilder()
      .annotationBoundary(ANN0_0).build();
  private static final DocInitialization DOC1_2 = new DocInitializationBuilder()
      .characters(S1A + S1A).build();
  private static final DocInitialization DOC2_2 = new DocInitializationBuilder().characters(S1A).
      characters(S1A).build();
  private static final DocInitialization DOC3_2 = new DocInitializationBuilder()
      .elementStart(S1B, ATTR1_1).elementEnd().build();
  private static final DocInitialization DOC4_14 = new DocInitializationBuilder()
      .elementStart(S2, ATTR2_12).elementEnd().build();
  private static final DocInitialization DOC5_15 = new DocInitializationBuilder()
      .elementStart(S2, ATTR2_12).characters(S1A).elementEnd().build();
  private static final DocInitialization DOC6_15 = new DocInitializationBuilder()
      .annotationBoundary(ANN0_0).elementStart(S2, ATTR2_12).characters(S1A).elementEnd().build();
  private static final DocInitialization DOC7_15 = new DocInitializationBuilder()
      .annotationBoundary(ANN0_0).elementStart(S2, ATTR2_12).annotationBoundary(ANN0_0)
      .characters(S1A).elementEnd().build();
  private static final DocInitialization DOC9_15 = new DocInitializationBuilder()
      .annotationBoundary(ANN1_0A).elementStart(S2, ATTR2_12).annotationBoundary(ANN1_0B)
      .characters(S1A).elementEnd().build();
  private static final DocInitialization DOC12_17 = new DocInitializationBuilder()
      .annotationBoundary(ANN1_0A).elementStart(S2, ATTR2_12).annotationBoundary(ANN1_0B)
      .characters(S1A).elementStart(S1A, ATTR1_1).elementEnd().elementEnd().build();


  public void testComputeDocumentCost() {
    // Empty document has a cost of zero.  This is important as, conceptually,
    // documents with all possible names exist but are initially empty.
    assertEquals(0, f0.computeCost(DOC0_0));
    assertEquals(0, f1.computeCost(DOC0_0));
    assertEquals(0, f2.computeCost(DOC0_0));
    assertEquals(0, f12.computeCost(DOC0_0));

    // Non-normalized documents are more expensive than normalized ones.
    assertEquals(0, f0.computeCost(DOC1_0));
    assertEquals(2, f0.computeCost(DOC1_2));
    assertEquals(2, f0.computeCost(DOC2_2));

    assertEquals(1, f1.computeCost(DOC1_0));
    assertEquals(3, f1.computeCost(DOC1_2));
    assertEquals(4, f1.computeCost(DOC2_2));

    assertEquals(2, f2.computeCost(DOC1_0));
    assertEquals(4, f2.computeCost(DOC1_2));
    assertEquals(6, f2.computeCost(DOC2_2));

    assertEquals(12, f12.computeCost(DOC1_0));
    assertEquals(14, f12.computeCost(DOC1_2));
    assertEquals(26, f12.computeCost(DOC2_2));

    // Some bigger ops.
    assertEquals( 2, f0.computeCost(DOC3_2));
    assertEquals(14, f0.computeCost(DOC4_14));
    assertEquals(15, f0.computeCost(DOC5_15));
    assertEquals(15, f0.computeCost(DOC6_15));
    assertEquals(15, f0.computeCost(DOC7_15));
    assertEquals(15, f0.computeCost(DOC9_15));
    assertEquals(17, f0.computeCost(DOC12_17));

    assertEquals( 5, f1.computeCost(DOC3_2));
    assertEquals(18, f1.computeCost(DOC4_14));
    assertEquals(20, f1.computeCost(DOC5_15));
    assertEquals(21, f1.computeCost(DOC6_15));
    assertEquals(22, f1.computeCost(DOC7_15));
    assertEquals(24, f1.computeCost(DOC9_15));
    assertEquals(29, f1.computeCost(DOC12_17));

    assertEquals(2 *  3 +  2, f2.computeCost(DOC3_2));
    assertEquals(2 *  4 + 14, f2.computeCost(DOC4_14));
    assertEquals(2 *  5 + 15, f2.computeCost(DOC5_15));
    assertEquals(2 *  6 + 15, f2.computeCost(DOC6_15));
    assertEquals(2 *  7 + 15, f2.computeCost(DOC7_15));
    assertEquals(2 *  9 + 15, f2.computeCost(DOC9_15));
    assertEquals(2 * 12 + 17, f2.computeCost(DOC12_17));

    assertEquals(12 *  3 +  2, f12.computeCost(DOC3_2));
    assertEquals(12 *  4 + 14, f12.computeCost(DOC4_14));
    assertEquals(12 *  5 + 15, f12.computeCost(DOC5_15));
    assertEquals(12 *  6 + 15, f12.computeCost(DOC6_15));
    assertEquals(12 *  7 + 15, f12.computeCost(DOC7_15));
    assertEquals(12 *  9 + 15, f12.computeCost(DOC9_15));
    assertEquals(12 * 12 + 17, f12.computeCost(DOC12_17));
  }

}
