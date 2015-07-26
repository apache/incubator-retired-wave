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

package org.waveprotocol.wave.model.wave.undo;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpCollector;

/**
 * A list of document operations.
 *
 */
abstract class DocOpList {

  /**
   * A singleton list.
   */
  static final class Singleton extends DocOpList {

    final DocOp docOp;

    /**
     * @param docOp The single operation in the list.
     */
    Singleton(DocOp docOp) {
      this.docOp = docOp;
    }

    @Override
    void collectInto(DocOpCollector collector) {
      collector.add(docOp);
    }

  }

  /**
   * A concatenation of two lists.
   */
  static private final class Node extends DocOpList {

    final DocOpList left;
    final DocOpList right;

    /**
     * @param left The first list to participate in the concatenation.
     * @param right The second list to participate in the concatenation.
     */
    Node(DocOpList left, DocOpList right) {
      this.left = left;
      this.right = right;
    }

    @Override
    void collectInto(DocOpCollector collector) {
      left.collectInto(collector);
      right.collectInto(collector);
    }

  }

  /**
   * Concatenates this list with a given list.
   *
   * @param other The list to concatenate with this list.
   * @return The concatenation of this list with the given list.
   */
  DocOpList concatenateWith(DocOpList other) {
    return new Node(this, other);
  }

  /**
   * Composes all the operations together and returns the result.
   *
   * @return The composition of all the operations in the list.
   */
  DocOp composeAll() {
    DocOpCollector collector = new DocOpCollector();
    collectInto(collector);
    return collector.composeAll();
  }

  /**
   * Collects operations into the given <code>DocOpCollector</code>.
   *
   * @param collector The collector into which to collect the document
   *        operations.
   */
  abstract void collectInto(DocOpCollector collector);

}
