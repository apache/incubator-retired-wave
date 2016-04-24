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

package org.waveprotocol.wave.client.editor.annotation;

import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.Parameters;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.RandomProvider;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.Parameters.AnnotationOption;
import org.waveprotocol.wave.model.testing.RandomNindoGenerator;

import java.util.Arrays;
import java.util.Collections;

public abstract class AnnotationPainterRandomTestBase extends AnnotationPainterTestBase {
  /**
   * Configuration on number of mutations to run.
   */
  public static final class TestConfiguration {
    final int numInitialMutations;
    final int numAnnotationMutations;
    final int numGeneralMutations;
    final int numRuns;
    final int lotsOfTime;

    public TestConfiguration(int numInitialMutations, int numAnnotationMutations,
        int numGeneralMutations, int numRuns, int lotsOfTime) {
      this.numInitialMutations = numInitialMutations;
      this.numAnnotationMutations = numAnnotationMutations;
      this.numGeneralMutations = numGeneralMutations;
      this.numRuns = numRuns;
      this.lotsOfTime = lotsOfTime;
    }
  }

  public static final TestConfiguration JUNIT_TEST_CONFIG =
    new TestConfiguration(15, 20, 20, 2500, 100);

  private final TestConfiguration testConfiguration;

  protected final RandomProvider random;

  public AnnotationPainterRandomTestBase(TestConfiguration testConfiguration,
      RandomProvider random) {
    this.testConfiguration = testConfiguration;
    this.random = random;
  }

  static final Parameters STRUCT_PARAMS = new Parameters().setMaxOpeningComponents(15);
  static {
    STRUCT_PARAMS.setAnnotationOptions(Collections.<AnnotationOption>emptyList());
  }

  static final Parameters ANNO_PARAMS = new Parameters().setMaxOpeningComponents(31);
  static {
    ANNO_PARAMS.setElementTypes(Collections.<String>emptyList());
    setAnnotationOptions(ANNO_PARAMS);
  }

  static final Parameters GENERAL_PARAMS = new Parameters().setMaxOpeningComponents(31);
  static {
    setAnnotationOptions(GENERAL_PARAMS);
  }

  private static void setAnnotationOptions(Parameters params) {
    params.setAnnotationOptions(Arrays.asList(new AnnotationOption[] {
        new AnnotationOption("a", Arrays.asList(new String[] {"1", "2", "3", "4"})),
        new AnnotationOption("b", Arrays.asList(new String[] {"1", "2", "3", "4"})),
        new AnnotationOption("c", Arrays.asList(new String[] {"1", "2", "3", "4"})),
        new AnnotationOption("d", Arrays.asList(new String[] {"1", "2", "3", "4"})),
        new AnnotationOption("w", Arrays.asList(new String[] {"1", "2", "3", "4"})),
        new AnnotationOption("x", Arrays.asList(new String[] {"1", "2", "3", "4"})),
        new AnnotationOption("y", Arrays.asList(new String[] {"1", "2", "3", "4"})),
        new AnnotationOption("z", Arrays.asList(new String[] {"1", "2", "3", "4"}))
    }));
  }

  /**
   * Runs a group of random mutations against the annotation painter and verifies that
   * it does not crash.
   *
   * TODO(danilatos): Verify that the rendering is accurate.
   *
   * @param <N>
   * @param <E>
   * @param <T>
   * @param random
   * @param cxt
   * @param indexedDoc
   */
  protected <N, E extends N, T extends N> void runMutations(
      DocumentContext<N, E, T> cxt,
      IndexedDocument<N, E, T> indexedDoc) {

    try {
      IndexedDocument<Node, Element, Text> copy = DocProviders.POJO.build(indexedDoc.asOperation(),
          DocumentSchema.NO_SCHEMA_CONSTRAINTS);
      copy.consume(indexedDoc.asOperation());

      for (int i = 0; i < testConfiguration.numInitialMutations; i++) {
        Nindo nindo = RandomNindoGenerator.generate(random, STRUCT_PARAMS,
            ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS, copy);
        DocOp op = indexedDoc.consumeAndReturnInvertible(nindo);
        copy.consume(op);
      }

      for (int i = 0; i < testConfiguration.numAnnotationMutations; i++) {
        try {
          Nindo nindo = RandomNindoGenerator.generate(random, ANNO_PARAMS,
              ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS, copy);
          DocOp op = indexedDoc.consumeAndReturnInvertible(nindo);
//          System.out.println("\n\n");
//          System.out.println(nindo);
//          System.out.println(op);
//          System.out.println(copy);
//          System.out.println(CollectionUtils.join('\n', "INLINE: ",
//              DocOpUtil.visualiseOpWithDocument(copy.asOperation(), op)));
          copy.consume(op);
          checkRender(cxt);
        } catch (RuntimeException e) {
          System.out.println("During annotation mutations: " + i + " " + e);
          throw e;
        }
      }

      for (int i = 0; i < testConfiguration.numGeneralMutations; i++) {
        try {
          Nindo nindo = RandomNindoGenerator.generate(random, GENERAL_PARAMS,
              ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS, copy);
          DocOp op = indexedDoc.consumeAndReturnInvertible(nindo);
          copy.consume(op);
          checkRender(cxt);
        } catch (RuntimeException e) {
          System.out.println("During structural+annotation mutations: " + i + " " + e);
          System.out.println(DocOpUtil.toXmlString(indexedDoc.asOperation()));
          throw e;
        }
      }
    } catch (OperationException e) {
      throw new OperationRuntimeException("Test Failure", e);
    }
  }

  <N, E extends N, T extends N> void checkRender(DocumentContext<N, E, T> cxt) {
    timerService.tick(testConfiguration.lotsOfTime);
  }

  /**
   * Implement this by calling through to
   * {@link #runMutations(RandomProvider, DocumentContext, IndexedDocument)} with a document
   * context
   *
   * @param randomProvider
   */
  protected abstract void callRunMutationsWithContext();

  /***/
  public void testPainter() {
    for (int i = 0; i < testConfiguration.numRuns; i++) {
      try {
        System.out.println(i);
        callRunMutationsWithContext();
      } catch (RuntimeException e) {
        System.out.println("RUN: " + i);
        throw e;
      }
    }
  }

  /**
   * Runs a single iteration of the painter random test.
   */
  public void singleIterationPainterTest() {
    callRunMutationsWithContext();
  }
}
