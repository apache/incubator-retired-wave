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


import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.bootstrap.BootstrapDocument;
import org.waveprotocol.wave.model.document.operation.Automatons;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.ModifiableDocument;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.DocOp.IsDocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.automaton.AutomatonDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.operation.impl.DocOpValidator;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator;
import org.waveprotocol.wave.model.testing.RandomNindoGenerator;
import org.waveprotocol.wave.model.testing.RandomProviderImpl;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.RandomProvider;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * Randomized test cases for IndexedDocumentImpl.
 *
 * @author ohler@google.com (Christian Ohler)
 */

public class IndexedDocumentImplRandomLargeTest extends TestCase {

  static final int NUM_INITIAL_MUTATIONS = 10;
  static final int NUM_REVERSED_MUTATIONS_PER_RUN = 20;
  static final int NUM_RUNS = 300; // NOTE: increase if you test changes to IndexedDocument
  static final RandomDocOpGenerator.Parameters[] PARAM_SETS = {
    // Default parameters.
    new RandomDocOpGenerator.Parameters(),
  };

  public void consumeMethodsTestOneRun(RandomProvider random,
      RandomDocOpGenerator.Parameters params) throws OperationException {
    IndexedDocument<Node, Element, Text> doc =
      new IndexedDocumentImpl<Node, Element, Text, Void>(RawDocumentImpl.PROVIDER.parse("<a></a>"),
          new AnnotationTree<Object>("a", "b", null), DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    AutomatonDocument autoDoc = Automatons.fromReadable(doc);

    ModifiableDocument checkDoc;
    IsDocOp checkDocOpProvider;
    AutomatonDocument checkAuto;

    // Set to false for faster test runs
    boolean checkAgainstBootstrapDocument = true;
    if (checkAgainstBootstrapDocument) {
      BootstrapDocument bootstrapDoc = new BootstrapDocument();
      checkDocOpProvider = bootstrapDoc;
      checkDoc = bootstrapDoc;
      checkAuto = bootstrapDoc;
    } else {
      IndexedDocument<Node, Element, Text> indexedDoc = DocProviders.POJO.parse("");
      checkDocOpProvider = indexedDoc;
      checkDoc = indexedDoc;
      checkAuto = Automatons.fromReadable(indexedDoc);
    }

    for (int i = 0; i < NUM_INITIAL_MUTATIONS; i++) {
      DocOp op = RandomDocOpGenerator.generate(random, params,
          // FIXME(ohler): Add back schema constraints
          // DocumentOperationValidator.DEFAULT_BLIP_SCHEMA_CONSTRAINTS,
          autoDoc);
      doc.consume(op);
      checkDoc.consume(op);
    }

    String originalXml = DocOpUtil.toXmlString(doc.asOperation());

    for (int i = 0; i < NUM_REVERSED_MUTATIONS_PER_RUN; i++) {

      // Apply random mutation and revert it.
      DocOp op = RandomDocOpGenerator.generate(random, params,
          // FIXME(ohler): Add back schema constraints
          // DocumentOperationValidator.DEFAULT_BLIP_SCHEMA_CONSTRAINTS,
          autoDoc);

      Nindo nindo = null;
      String finalXml = null;
      DocOp docOpCopy = null;
      DocInitialization docAsOp = null;

      try {

        //System.out.println("  " + i);
        //System.out.println("\n===" + iteration + "." + i + "===============================");

        docAsOp = doc.asOperation();
        validate(DocOpAutomaton.EMPTY_DOCUMENT, docAsOp);
        IndexedDocument<Node, Element, Text> copy = DocProviders.POJO.build(docAsOp,
            DocumentSchema.NO_SCHEMA_CONSTRAINTS);
        BootstrapDocument copy2 = new BootstrapDocument();
        copy2.consume(docAsOp);

        // CONSUME
        doc.consume(op);
        checkDoc.consume(op);

        finalXml = DocOpUtil.toXmlString(checkDocOpProvider.asOperation());

        assertEquals(finalXml, DocOpUtil.toXmlString(doc.asOperation()));

        // UNDO
        DocOp inverted1 = DocOpInverter.invert(op);
        validate(autoDoc, inverted1);
        doc.consume(inverted1);
        assertEquals(originalXml, DocOpUtil.toXmlString(doc.asOperation()));

        // CONSUME NINDO
        // TODO(danilatos): Both remove and don't remove the trailing skip, randomly
        nindo = Nindo.fromDocOp(op, true);
        docOpCopy = doc.consumeAndReturnInvertible(nindo);
        assertEquals(finalXml, DocOpUtil.toXmlString(doc.asOperation()));

        validate(Automatons.fromReadable(copy), docOpCopy);
        validate(copy2, docOpCopy);
        copy2.consume(docOpCopy);
        assertEquals(finalXml, DocOpUtil.toXmlString(copy2.asOperation()));

        // UNDO NINDO
        DocOp inverted2 = DocOpInverter.invert(docOpCopy);
        validate(checkAuto, inverted2);
        validate(autoDoc, inverted2);
        doc.consume(inverted2);
        assertEquals(originalXml, DocOpUtil.toXmlString(doc.asOperation()));
        checkDoc.consume(inverted2);

        // GENERATE NINDO
        nindo = RandomNindoGenerator.generate(random, params,
            DocumentSchema.NO_SCHEMA_CONSTRAINTS, doc);
        docAsOp = doc.asOperation();
        docOpCopy = doc.consumeAndReturnInvertible(nindo);
        validate(checkAuto, docOpCopy);
        doc.consume(DocOpInverter.invert(docOpCopy));
//        System.err.println(CollectionUtils.join('\n', "INLINE: ",
//            DocOpUtil.visualiseOpWithDocument(docAsOp, docOpCopy)));

      } catch (Throwable e) {
        if (e instanceof RuntimeException ||
            e instanceof AssertionError ||
            e instanceof AssertionFailedError) {
          System.err.println(originalXml);
          System.err.println(finalXml);
          System.err.println("ORIGINAL THEN COPY: ");
          System.err.println(op);
          System.err.println(docOpCopy);
          System.err.println(nindo);
          System.err.println("NORMALISED: ");
          System.err.println(DocOpUtil.normalize(op));
          if (docOpCopy != null) {
            System.err.println(DocOpUtil.normalize(docOpCopy));
          }

          System.err.println("NORMALISED INVERTED: ");
          System.err.println(DocOpUtil.normalize(DocOpInverter.invert(op)));
          if (docOpCopy != null) {
            System.err.println(DocOpUtil.normalize(DocOpInverter.invert(docOpCopy)));
          }
          if (docOpCopy != null) {
            System.err.println(CollectionUtils.join('\n', "INLINE: ",
                DocOpUtil.visualiseOpWithDocument(docAsOp, docOpCopy)));
          }

        }
        if (e instanceof Error) {
          throw (Error) e;
        } else if (e instanceof OperationException) {
          throw (OperationException) e;
        } else {
          throw (RuntimeException) e;
        }
      }

//      ViolationAccu v = DocumentOperationValidator.validate(doc, reverse,
//          DocumentOperationValidator.DEFAULT_BLIP_SCHEMA_CONSTRAINTS);
//      try {
//        assertTrue(v.isValid());
//      } catch (AssertionFailedError e) {
//        System.err.println("reverse op invalid:");
//        v.printDescriptions(System.err);
//        System.err.print(DocumentOperationCodePrinter.getCode(m, "m"));
//        System.err.print(DocumentOperationCodePrinter.getCode(reverse, "reverse"));
//        throw e;
//      }

//      reverse.apply(doc);

//      String finalXml = OperationXmlifier.xmlify(doc);
//
//      try {
//        assertEquals(originalXml, finalXml);
//      } catch (AssertionFailedError e) {
//        System.err.println("reverse op did not revert properly:");
//        System.err.print(DocumentOperationCodePrinter.getCode(m, "m"));
//        System.err.print(DocumentOperationCodePrinter.getCode(reverse, "reverse"));
//        throw e;
//      }
    }
  }

  private void validate(AutomatonDocument doc, DocOp op) {
    ViolationCollector v = new ViolationCollector();
    if (!DocOpValidator.validate(v, null, doc, op).isValid()) {
      v.printDescriptions(System.err);
      throw new AssertionFailedError("Validation failed: " + v);
    }
  }

  private int iteration;

  public void testConsumeMethods() throws OperationException {
    IndexedDocumentImpl.performValidation = false;
    try {
      RandomProvider r = new RandomProviderImpl(1);
      for (RandomDocOpGenerator.Parameters params : PARAM_SETS) {
        for (iteration = 0; iteration < NUM_RUNS; iteration++) {
          System.out.println(iteration);
          consumeMethodsTestOneRun(r, params);
        }
      }
    } finally {
      IndexedDocumentImpl.performValidation = true;
    }
  }

  /** For performance testing
   * @throws OperationException */
  public static void main(String[] argv) throws OperationException {
    RandomProvider random = new RandomProviderImpl(1);
    RandomDocOpGenerator.Parameters params = PARAM_SETS[0];
    DocumentSchema constraints = DocumentSchema.NO_SCHEMA_CONSTRAINTS;
    IndexedDocument<Node, Element, Text> doc =
      new IndexedDocumentImpl<Node, Element, Text, Void>(RawDocumentImpl.PROVIDER.parse("<a></a>"),
          new AnnotationTree<Object>("a", "b", null), constraints);
    AutomatonDocument autoDoc = Automatons.fromReadable(doc);

    final boolean testAsOperation = true;
    final boolean testNindos = true;
    final int NUM_OPS = 200;

    System.out.print("Pre-generating ops....");

    DocOp[] docOps = new DocOp[NUM_OPS * 2];
    Nindo[] nindos = new Nindo[NUM_OPS * 2];
    IsDocOp[] docs = new IsDocOp[NUM_OPS];

    for (int i = 0; i < NUM_OPS; i++) {
      System.out.print(".");
      if (testAsOperation) {
        Nindo nindo = RandomNindoGenerator.generate(random, params, constraints, doc);
        DocOp docOp = doc.consumeAndReturnInvertible(nindo);
        docs[i] = DocProviders.POJO.build(doc.asOperation(), constraints);
      } else if (testNindos) {
        Nindo nindo = RandomNindoGenerator.generate(random, params, constraints, doc);
        DocOp docOp = doc.consumeAndReturnInvertible(nindo);
        nindos[i] = nindo;
        nindos[NUM_OPS * 2 - i - 1] = Nindo.fromDocOp(DocOpInverter.invert(docOp), true);
      } else {
        DocOp docOp = RandomDocOpGenerator.generate(random, params, //constraints,
            Automatons.fromReadable(doc));
        doc.consume(docOp);
        docOps[i] = docOp;
        docOps[NUM_OPS * 2 - i - 1] =DocOpInverter.invert(docOp);
      }
    }

    System.out.println("\nDone pre-generating ops");

    int iteration = 0;

    IndexedDocument<Node, Element, Text> original = doc;

    doc = new IndexedDocumentImpl<Node, Element, Text, Void>(
        RawDocumentImpl.PROVIDER.parse("<a></a>"),
        new AnnotationTree<Object>("a", "b", null), constraints);

    final int numToAverage = 50;
    int[] times = new int[numToAverage];
    while (true) {
      System.out.print((testAsOperation ? "asOperation"
          : (testNindos ? "Nindo " : "DocOp ")) + ++iteration);
      long start = System.currentTimeMillis();
      if (testAsOperation) {
        for (IsDocOp d : docs) {
          d.asOperation().apply(new DocOpBuffer());
        }
      } else if (testNindos) {
        for (Nindo nindo : nindos) {
          doc.consumeAndReturnInvertible(nindo);
        }
      } else {
        for (DocOp docOp : docOps) {
          doc.consume(docOp);
        }
      }
      int duration = (int) (System.currentTimeMillis() - start);
      times[iteration % numToAverage] = duration;
      int sum = 0;
      for (int time : times) {
        sum += time;
      }
      System.out.println(" - took: " + duration + "ms, "
          + "average last " + numToAverage + ": " + (sum / numToAverage) + "ms");
    }
  }
}
