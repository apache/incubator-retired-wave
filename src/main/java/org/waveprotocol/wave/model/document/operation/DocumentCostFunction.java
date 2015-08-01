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

import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Map;

/**
 * Computes the cost of documents.
 *
 * The cost is a metric that, roughly speaking, counts the number of components
 * and how big each comopnent is.  We could call it "size" but IndexedDocument
 * has a method size() already (it returns the length).
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class DocumentCostFunction {

  private final int objectOverhead;

  /**
   * The objectOverhead is a parameter that affects how expensive elements and
   * annotation boundaries are relative to characters.
   */
  public static DocumentCostFunction withObjectOverhead(int objectOverhead) {
    return new DocumentCostFunction(objectOverhead);
  }

  private DocumentCostFunction(int objectOverhead) {
    Preconditions.checkArgument(objectOverhead >= 0, "Negative objectOverhead: %s", objectOverhead);
    this.objectOverhead = objectOverhead;
  }

  // In every loop, we add objectOverhead for every item, in addition to the
  // cost of the item itself.  This makes sure that even empty strings and other
  // empty stuff have a nonzero cost, and it is uniform and easy to predict.

  public int computeCost(String s) {
    return s.codePointCount(0, s.length());
  }

  public int computeCostNullable(String s) {
    return s == null ? 0 : computeCost(s);
  }

  public int computeCost(Attributes attrs) {
    int accu = 0;
    for (Map.Entry<String, String> attr : attrs.entrySet()) {
      accu += objectOverhead + computeCost(attr.getKey()) + computeCost(attr.getValue());
    }
    return accu;
  }

  public int computeCost(AnnotationBoundaryMap map) {
    int accu = 0;
    for (int i = 0; i < map.changeSize(); i++) {
      accu += objectOverhead + computeCost(map.getChangeKey(i))
          + computeCostNullable(map.getOldValue(i))
          + computeCostNullable(map.getNewValue(i));
    }
    for (int i = 0; i < map.endSize(); i++) {
      accu += objectOverhead + computeCost(map.getEndKey(i));
    }
    return accu;
  }

  /**
   * Computes the cost of the given document.
   */
  public int computeCost(DocInitialization doc) {
    final int[] accu = { 0 };
    doc.apply(new DocInitializationCursor() {
        @Override
        public void annotationBoundary(AnnotationBoundaryMap map) {
          accu[0] += objectOverhead + computeCost(map);
        }

        @Override
        public void characters(String chars) {
          accu[0] += objectOverhead + computeCost(chars);
        }

        @Override
        public void elementStart(String type, Attributes attrs) {
          accu[0] += objectOverhead + computeCost(type) + computeCost(attrs);
        }

        @Override
        public void elementEnd() {
          accu[0] += objectOverhead;
        }
      });
    return accu[0];
  }

}
