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

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.operation.OperationException;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * A class that collects document operations together and composes them in an
 * efficient manner.
 */
public final class DocOpCollector {

  private final List<DocOp> operations = new ArrayList<DocOp>();

  public void add(DocOp operation) {
    //
    // The algorithm below ensures that the compose() calls for a sequence of
    // operations are performed in a tree-like manner. In an operation sequence,
    // it is generally faster to compose two adjacent ops than it is to compose
    // an op with the composition of all ops before it, so this distribution is
    // expected to be faster than sequential composition.
    //
    // The time complexity for composing N ops of total size n is O(n log N),
    // rather than O(n^2) for sequential composition.
    //
    // Illustration of first 8 steps, demonstrating the adjacent composition:
    // [a]
    // [0, (a;b)]
    // [c, (a;b)]
    // [0, 0, (a;b);(c;d)]
    // [e, 0, (a;b);(c;d)]
    // [0, (e;f), (a;b);(c;d)]
    // [g, (e;f), (a;b);(c;d)]
    // [0, 0, 0, ((a;b);(c;d));((e;f);(g;h)))]
    //
    // See http://groups.google.com/group/wave-protocol/msg/eb1e7293b26ff61b
    //
    ListIterator<DocOp> iterator = operations.listIterator();
    while (iterator.hasNext()) {
      DocOp nextOperation = iterator.next();
      if (nextOperation == null) {
        iterator.set(operation);
        return;
      }
      iterator.set(null);
      operation = compose(nextOperation, operation);
    }
    operations.add(operation);
  }

  public DocOp composeAll() {
    DocOp result = null;
    for (DocOp operation : operations) {
      if (operation != null) {
        result = (result != null) ? compose(operation, result) : operation;
      }
    }
    operations.clear();
    return result;
  }

  private DocOp compose(DocOp op1, DocOp op2) {
    try {
      return Composer.compose(op1, op2);
    } catch (OperationException e) {
      throw new IllegalArgumentException(e);
    }
  }

  public boolean isEmpty() {
    return operations.isEmpty();
  }

}
