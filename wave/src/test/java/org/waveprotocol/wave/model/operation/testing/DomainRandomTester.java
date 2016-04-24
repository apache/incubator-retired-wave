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

package org.waveprotocol.wave.model.operation.testing;

import org.waveprotocol.wave.model.operation.Domain;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author ohler@google.com (Christian Ohler)
 */
public class DomainRandomTester<D, O> {

  public static class FailureException extends RuntimeException {

  }

  interface Log {
    void info(String ... lines);
    void inconsistent(String ... lines);
    void fatal(Throwable exception, String ... lines);
  }

  private final int INITIAL_MUTATION_COUNT = 3;

  private final int FEATURE_ITERATION_COUNT = 20;

  private final Domain<D, O> domain;

  private final RandomOpGenerator<D, O> generator;

  private final Log log;

  public DomainRandomTester(Log log, Domain<D, O> domain, RandomOpGenerator<D, O> generator) {
    this.domain = domain;
    this.generator = generator;
    this.log = log;
  }

  /**
   * Test that applying data as an operation to get back a copy of the data
   * works correctly.
   *
   * The other tests assume that this test always passes.
   *
   * @param numIterations
   */
  public void testDataOperationEquivalence(int numIterations) {
    log.info("TESTING testDataOperationEquivalence");
    Random r = new Random(0);

    for (int iteration = 0; iteration < numIterations; iteration++) {
      log.info("Iteration: " + iteration);

      D d1 = domain.initialState();

      try {
        for (int i = 0; i < INITIAL_MUTATION_COUNT; i++) {
          O op = generator.randomOperation(d1, r);
          domain.apply(op, d1);

          D d2 = domain.initialState();
          domain.apply(domain.asOperation(d1), d2);

          if (!domain.equivalent(d1, d2)) {
            log.inconsistent(
                "DATA-AS-OPERATION BUG",
                "Subiteration: " + i,
                "Op from data: " + domain.asOperation(d1),
                "Data: " + d1,
                "Result of op on fresh state: " + d2
                );
          }
        }
      } catch (OperationException e) {
        logException("DATA-AS-OPERATION BUG? Operation exception", e);
      } catch (RuntimeException e) {
        logException("DATA-AS-OPERATION BUG? Runtime exception", e);
      }
    }
  }

  public void testOperationInversion(int numIterations) {
    log.info("TESTING testOperationInversion");
    Random r = new Random(0);

    for (int iteration = 0; iteration < numIterations; iteration++) {
      log.info("Iteration: " + iteration);

      D d1 = domain.initialState();

      try {
        for (int i = 0; i < INITIAL_MUTATION_COUNT; i++) {
          O op = generator.randomOperation(d1, r);
          domain.apply(op, d1);
        }

        for (int i = 0; i < FEATURE_ITERATION_COUNT; i++) {
          log.info("X " + i);
          D backup = copy(d1);

          O op = generator.randomOperation(d1, r);
          domain.apply(op, d1);

          D afterOp = copy(d1);

          O reverse = domain.invert(op);
          domain.apply(reverse, d1);

          if (!domain.equivalent(d1, backup)) {
            log.inconsistent(
                "INVERSION BUG",
                "Subiteration: " + i,
                "Op: " + op,
                "Reverse: " + reverse,
                "Initial state: " + backup,
                "State after op: " + afterOp,
                "State after inverse: " + d1
                );
          }
        }
      } catch (OperationException e) {
        logException("COMPOSE BUG? Operation exception", e);
      } catch (RuntimeException e) {
        logException("COMPOSE BUG? Runtime exception", e);
      }
    }
  }

  public void testCompositionOnInitialState(int numIterations) {
    log.info("TESTING testCompositionOnInitialState");
    testSimpleComposition(numIterations, true);
  }

  /**
   * Test that (a.b).c = a.(b.c)
   *
   *
   */
  public void testCompositionAssociativity(int numIterations) {
    log.info("TESTING testCompositionAssociativity");
    Random r = new Random(0);

    for (int iteration = 0; iteration < numIterations; iteration++) {
      log.info("Iteration: " + iteration);

      try {
        D d1 = domain.initialState();

        for (int i = 0; i < INITIAL_MUTATION_COUNT; i++) {
          O op = generator.randomOperation(d1, r);
          domain.apply(op, d1);
        }

        D d2 = copy(d1);
        D d3 = copy(d1);

        for (int i = 0; i < FEATURE_ITERATION_COUNT; i++) {
          D backup = copy(d1);

          O op1 = generator.randomOperation(d1, r);
          domain.apply(op1, d1);
          O op2 = generator.randomOperation(d1, r);
          domain.apply(op2, d1);
          O op3 = generator.randomOperation(d1, r);
          domain.apply(op3, d1);

          O op12 = domain.compose(op2, op1);
          O op23 = domain.compose(op3, op2);

          domain.apply(op1, d2);
          domain.apply(op23, d2);

          domain.apply(op12, d3);
          domain.apply(op3, d3);

          if (!domain.equivalent(d2, d3)) {
            log.inconsistent(
                "COMPOSE ASSOCIATIVITY BUG",
                "Subiteration: " + i,
                "Op1: " + op1,
                "Op2: " + op2,
                "Op3: " + op2,
                "Op2.Op1: " + op12,
                "Op3.Op2: " + op23,
                "Initial state: " + backup,
                "State after Op3.(Op2.Op1): " + d3,
                "State after (Op3.Op2).Op1: " + d2
                );
          }
        }
      } catch (OperationException e) {
        logException("COMPOSE BUG? Operation exception", e);
      } catch (RuntimeException e) {
        logException("COMPOSE BUG? Runtime exception", e);
      }
    }
  }

  /**
   * This test is not strictly required, but provides good redundancy.
   *
   *
   *
   * @param numIterations
   */
  public void testSimpleComposition(int numIterations) {
    log.info("TESTING testSimpleComposition");
    testSimpleComposition(numIterations, false);
  }

  private void testSimpleComposition(int numIterations, boolean emptyInitialState) {
    Random r = new Random(0);

    for (int iteration = 0; iteration < numIterations; iteration++) {
      log.info("Iteration: " + iteration);

      D d1 = domain.initialState();

      try {
        if (!emptyInitialState) {
          for (int i = 0; i < INITIAL_MUTATION_COUNT; i++) {
            O op = generator.randomOperation(d1, r);
            domain.apply(op, d1);
          }
        }

        D d2 = copy(d1);

        for (int i = 0; i < FEATURE_ITERATION_COUNT; i++) {
          D backup = copy(d1);

          O op1 = generator.randomOperation(d1, r);
          domain.apply(op1, d1);
          D after1 = copy(d1);
          O op2 = generator.randomOperation(d1, r);
          domain.apply(op2, d1);

          O composedOp = domain.compose(op2, op1);
          domain.apply(composedOp, d2);

          if (!domain.equivalent(d1, d2)) {
            log.inconsistent(
                "COMPOSE ASSOCIATIVITY BUG",
                "Subiteration: " + i,
                "Op1: " + op1,
                "Op2: " + op2,
                "Composed: " + composedOp,
                "Initial state: " + backup,
                "State after first: " + after1,
                "State after first then second: " + d1,
                "State after composed: " + d2
                );
          }
        }
      } catch (OperationException e) {
        logException("COMPOSE BUG? Operation exception", e);
      } catch (RuntimeException e) {
        logException("COMPOSE BUG? Runtime exception", e);
      }
    }
  }

  public void testTransformDiamondProperty(int numIterations) {
    log.info("TESTING testTransformDiamondProperty");
    Random r = new Random(0);

    for (int iteration = 0; iteration < numIterations; iteration++) {
      log.info("Iteration: " + iteration);

      D d1 = domain.initialState();

      try {
        for (int i = 0; i < INITIAL_MUTATION_COUNT; i++) {
          O op = generator.randomOperation(d1, r);
          domain.apply(op, d1);
        }

        D d2 = copy(d1);

        for (int i = 0; i < FEATURE_ITERATION_COUNT; i++) {
          D original = copy(d1);

          O op1 = generator.randomOperation(original, r);
          O op2 = generator.randomOperation(original, r);

          domain.apply(op1, d1);
          domain.apply(op2, d2);

          D client = copy(d1);
          D server = copy(d2);

          OperationPair<O> pair = domain.transform(op1, op2);

          domain.apply(pair.serverOp(), d1);
          domain.apply(pair.clientOp(), d2);

          if (!domain.equivalent(d1, d2)) {
            log.inconsistent(
                "TRANSFORM BUG",
                "Subiteration: " + i,
                "Client: " + op1,
                "Server: " + op2,
                "Client': " + pair.clientOp(),
                "Server': " + pair.serverOp(),
                "Initial state: " + original,
                "Client state 1:" + client,
                "Client state 2:" + d1,
                "Server state 1:" + server,
                "Server state 2:" + d2
                );
          }
        }

      } catch (OperationException e) {
        logException("TRANSFORM BUG? Operation exception", e);
      } catch (TransformException e) {
        logException("TRANSFORM BUG? Transform exception", e);
      } catch (RuntimeException e) {
        logException("TRANSFORM BUG? Runtime exception", e);
      }
    }
  }

  /**
   * Tests that transformation and composition are compatible
   *
   * Assumes the diamond property of transformation
   *
   * NOTE: This should be rewritten to do proper comparison of operations.
   *
   * @param numIterations
   */
  public void testTransformationCompositionCompatible(int numIterations) {
    log.info("TESTING testTransformationCompositionCompatible");
    Random r = new Random(0);

    for (int iteration = 0; iteration < numIterations; iteration++) {
      log.info("Iteration: " + iteration);

      D server = domain.initialState();

      try {
        for (int i = 0; i < INITIAL_MUTATION_COUNT; i++) {
          O op = generator.randomOperation(server, r);
          domain.apply(op, server);
        }

        D client = copy(server);

        for (int i = 0; i < FEATURE_ITERATION_COUNT; i++) {
          D original = copy(server);
          if (!domain.equivalent(client, server)) {
            log.inconsistent("Sanity check failed: client and server not the same at start of test");
          }

          // Client is on the left for the first pass, but this
          // is reversed in the second pass (the meaning of the
          // variables "client" and "server" also reverses).
          //
          //        original (o)
          //          / \
          // client  a   b  server
          //        / \ /
          //       c   d
          //        \ /
          //        end (e)
          //

          O oa = generator.randomOperation(client, r);
          O ob = generator.randomOperation(server, r);

          domain.apply(oa, client);
          domain.apply(ob, server);

          D a = copy(client);
          D b = copy(server);

          OperationPair<O> pair1 = domain.transform(oa, ob);
          O bd = pair1.clientOp();
          O ad = pair1.serverOp();

          O ac = generator.randomOperation(a, r);
          domain.apply(ac, client);
          D c = copy(client);

          domain.apply(bd, server);
          D d = copy(server);

          D test = copy(a);
          domain.apply(ad, test);

          OperationPair<O> pair2 = domain.transform(ac, ad);
          O ce = pair2.serverOp();
          O de = pair2.clientOp();

          domain.apply(de, server);
          domain.apply(ce, client);
          D end = copy(client);

          O oc = domain.compose(ac, oa);
          O be = domain.compose(de, bd);

          // The property we want to test is that ce = ce2 and be = be2
          //
          OperationPair<O> pair3 = domain.transform(oc, ob);
          O ce2 = pair3.serverOp();
          O be2 = pair3.clientOp();

          D d1 = copy(c);
          domain.apply(ce2, d1);

          D d2 = copy(b);
          domain.apply(be2, d2);

          boolean ceOK = domain.equivalent(end, d1);
          boolean beOK = domain.equivalent(end, d2);
          if (!ceOK || !beOK) {
            log.inconsistent(
                "TRANSFORM AND COMPOSITION NOT COMPATIBLE",
                "Subiteration: " + i,
                ceOK ? "GOOD:" : "BAD:",
                "ce: " + ce,
                "ce2: " + ce2,
                beOK ? "GOOD:" : "BAD:",
                "be: " + be,
                "be2: " + be2,
                "-- States without compose: ",
                "        original (o)",
                "          / \\",
                " client  a   b  server",
                "        / \\ /",
                "       c   d",
                "        \\ /",
                "        end (e)",
                "o: " + original,
                "a: " + a,
                "b: " + b,
                "c: " + c,
                "d: " + d,
                "e: " + end
                );
          }
        }

      } catch (OperationException e) {
        logException("TRANSFORM BUG? Operation exception", e);
      } catch (TransformException e) {
        logException("TRANSFORM BUG? Transform exception", e);
      } catch (RuntimeException e) {
        logException("TRANSFORM BUG? Runtime exception", e);
      }
    }
  }

  private void logException(String message, Exception e) {
    if (e instanceof FailureException) {
      throw (FailureException) e;
    }
    // TODO: use record exception stuff below
    log.fatal(e, message, e + "");
  }

  private D copy(D state) throws OperationException {
    D copy = domain.initialState();
    domain.apply(domain.asOperation(state), copy);
    return copy;
  }

  final List<Throwable> exceptions = new ArrayList<Throwable>();

  String exceptionToStringForComparison(Throwable e) {
    StringWriter w = new StringWriter();
    e.printStackTrace(new PrintWriter(w));
    String s = w.toString();
    // Remove the explanatory string of the exception to eliminate any
    // distinction of different instances of the same problem.
    int firstLineEnd = s.indexOf('\n');
    assert firstLineEnd != -1;
    return e.getClass().getName() + '\n' + s.substring(firstLineEnd + 1);
  }

  boolean exceptionsEqual(Throwable e, Throwable known) {
    return exceptionToStringForComparison(e).equals(exceptionToStringForComparison(known));
  }

  boolean isExceptionKnown(Throwable e) {
    for (Throwable known : exceptions) {
      if (exceptionsEqual(e, known)) {
        return true;
      }
    }
    return false;
  }

  void recordException(Throwable e) {
    if (!isExceptionKnown(e)) {
      exceptions.add(e);
      System.err.println("new exception " + exceptions.size() + ":");
      e.printStackTrace(System.err);
      System.err.println("*** exceptions so far: " + exceptions.size());
    } else {
      System.err.println("*** exceptions so far: still " + exceptions.size());
    }
  }
}
