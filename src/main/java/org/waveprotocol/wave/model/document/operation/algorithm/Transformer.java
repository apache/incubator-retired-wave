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
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.util.Pair;

/**
 * A utility class for transforming document operations.
 *
 * TODO: Tweak the behaviour of this transformer to exactly match the
 * reference implementation in the tests.  Specifically, the optimized
 * implementation in this class may output extraneous annotations
 * which have no operational effect.
 *
 * @author Alexandre Mah
 */
public final class Transformer {

  private Transformer() {}

  /**
   * Transforms a pair of operations.
   *
   * @param clientOp the operation from the client
   * @param serverOp the operation from the server
   * @return the transformed pair of operations
   * @throws TransformException if a problem was encountered during the
   *         transformation process
   */
  public static OperationPair<DocOp> transform(DocOp clientOp,
      DocOp serverOp) throws TransformException {
    try {
      // The transform process consists of decomposing the client and server
      // operations into two constituent operations each and performing four
      // transforms structured as in the following diagram:
      //     ci0     cn0
      // si0     si1     si2
      //     ci1     cn1
      // sn0     sn1     sn2
      //     ci2     cn2
      //
      Pair<DocOp, DocOp> c = Decomposer.decompose(clientOp);
      Pair<DocOp, DocOp> s = Decomposer.decompose(serverOp);
      DocOp ci0 = c.first;
      DocOp cn0 = c.second;
      DocOp si0 = s.first;
      DocOp sn0 = s.second;
      OperationPair<DocOp> r1 = new InsertionTransformer().transformOperations(ci0, si0);
      DocOp ci1 = r1.clientOp();
      DocOp si1 = r1.serverOp();
      OperationPair<DocOp> r2 =
          new InsertionNoninsertionTransformer().transformOperations(ci1, sn0);
      DocOp ci2 = r2.clientOp();
      DocOp sn1 = r2.serverOp();
      OperationPair<DocOp> r3 =
          new InsertionNoninsertionTransformer().transformOperations(si1, cn0);
      DocOp si2 = r3.clientOp();
      DocOp cn1 = r3.serverOp();
      OperationPair<DocOp> r4 = new NoninsertionTransformer().transformOperations(cn1, sn1);
      DocOp cn2 = r4.clientOp();
      DocOp sn2 = r4.serverOp();
      return new OperationPair<DocOp>(
          Composer.compose(ci2, cn2),
          Composer.compose(si2, sn2));
    } catch (OperationException e) {
      throw new TransformException(e);
    }
  }

}
