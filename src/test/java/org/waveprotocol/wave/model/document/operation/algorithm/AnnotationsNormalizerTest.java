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

package org.waveprotocol.wave.model.document.operation.algorithm;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.OpComparators;


public class AnnotationsNormalizerTest extends TestCase {

  private static final AnnotationBoundaryMap ANNOTATIONS1 = AnnotationBoundaryMapImpl.builder()
      .updateValues("x", "m", "a", "y", "n", "b")
      .build();
  private static final AnnotationBoundaryMap ANNOTATIONS2 = AnnotationBoundaryMapImpl.builder()
      .updateValues("x", "m", "c")
      .initializationEnd("y")
      .build();
  private static final AnnotationBoundaryMap ANNOTATIONS3 = AnnotationBoundaryMapImpl.builder()
      .updateValues("y", "n", "f")
      .initializationEnd("x")
      .build();
  private static final AnnotationBoundaryMap ANNOTATIONS4 = AnnotationBoundaryMapImpl.builder()
      .initializationEnd("y")
      .build();
  private static final AnnotationBoundaryMap ANNOTATIONS12 = AnnotationBoundaryMapImpl.builder()
      .updateValues("x", "m", "c")
      .build();
  private static final AnnotationBoundaryMap ANNOTATIONS23 = AnnotationBoundaryMapImpl.builder()
      .updateValues("y", "n", "f")
      .initializationEnd("x")
      .build();
  private static final AnnotationBoundaryMap ANNOTATIONS123 = AnnotationBoundaryMapImpl.builder()
      .updateValues("y", "n", "f")
      .build();

  public void testAnnotationNormalization1() {
    AnnotationsNormalizer<DocOp> normalizer =
        new AnnotationsNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS1);
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS2);
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS3);
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS4);
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(1)
        .annotationBoundary(ANNOTATIONS1)
        .retain(1)
        .annotationBoundary(ANNOTATIONS2)
        .retain(1)
        .annotationBoundary(ANNOTATIONS3)
        .retain(1)
        .annotationBoundary(ANNOTATIONS4)
        .retain(1)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

  public void testAnnotationNormalization2() {
    AnnotationsNormalizer<DocOp> normalizer =
        new AnnotationsNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS1);
    normalizer.annotationBoundary(ANNOTATIONS2);
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS3);
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS4);
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(1)
        .annotationBoundary(ANNOTATIONS12)
        .retain(1)
        .annotationBoundary(ANNOTATIONS3)
        .retain(1)
        .annotationBoundary(ANNOTATIONS4)
        .retain(1)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

  public void testAnnotationNormalization3() {
    AnnotationsNormalizer<DocOp> normalizer =
        new AnnotationsNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS1);
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS2);
    normalizer.annotationBoundary(ANNOTATIONS3);
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS4);
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(1)
        .annotationBoundary(ANNOTATIONS1)
        .retain(1)
        .annotationBoundary(ANNOTATIONS23)
        .retain(1)
        .annotationBoundary(ANNOTATIONS4)
        .retain(1)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

  public void testAnnotationNormalization4() {
    AnnotationsNormalizer<DocOp> normalizer =
        new AnnotationsNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS1);
    normalizer.annotationBoundary(ANNOTATIONS2);
    normalizer.annotationBoundary(ANNOTATIONS3);
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS4);
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(1)
        .annotationBoundary(ANNOTATIONS123)
        .retain(1)
        .annotationBoundary(ANNOTATIONS4)
        .retain(1)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

  public void testEmptyRetainNormalization() {
    AnnotationsNormalizer<DocOp> normalizer =
        new AnnotationsNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS1);
    normalizer.annotationBoundary(ANNOTATIONS2);
    normalizer.retain(0);
    normalizer.annotationBoundary(ANNOTATIONS3);
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS4);
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(1)
        .annotationBoundary(ANNOTATIONS123)
        .retain(1)
        .annotationBoundary(ANNOTATIONS4)
        .retain(1)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

  public void testEmptyCharactersNormalization() {
    AnnotationsNormalizer<DocOp> normalizer =
        new AnnotationsNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS1);
    normalizer.annotationBoundary(ANNOTATIONS2);
    normalizer.characters("");
    normalizer.annotationBoundary(ANNOTATIONS3);
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS4);
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(1)
        .annotationBoundary(ANNOTATIONS123)
        .retain(1)
        .annotationBoundary(ANNOTATIONS4)
        .retain(1)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

  public void testEmptyDeleteCharactersNormalization() {
    AnnotationsNormalizer<DocOp> normalizer =
        new AnnotationsNormalizer<DocOp>(new DocOpBuffer());
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS1);
    normalizer.annotationBoundary(ANNOTATIONS2);
    normalizer.deleteCharacters("");
    normalizer.annotationBoundary(ANNOTATIONS3);
    normalizer.retain(1);
    normalizer.annotationBoundary(ANNOTATIONS4);
    normalizer.retain(1);
    DocOp docOp = normalizer.finish();
    DocOp expected = new DocOpBuilder()
        .retain(1)
        .annotationBoundary(ANNOTATIONS123)
        .retain(1)
        .annotationBoundary(ANNOTATIONS4)
        .retain(1)
        .build();
    assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, docOp));
  }

}
