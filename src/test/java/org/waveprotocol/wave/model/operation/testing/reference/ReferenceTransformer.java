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

package org.waveprotocol.wave.model.operation.testing.reference;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpCollector;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.testing.reference.Decomposer.Decomposition;
import org.waveprotocol.wave.model.util.Pair;

/**
 * A utility class for transforming document operations. This is intended to be
 * a reference implementation for defining the transform behavior and is
 * inefficient.
 */
public final class ReferenceTransformer {

  private ReferenceTransformer() {}

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
      Decomposition c = Decomposer.decompose(clientOp);
      Decomposition s = Decomposer.decompose(serverOp);
      DocOp ci = c.insertion;
      DocOp cp = c.preservation;
      DocOp cd = c.deletion;
      DocOp si = s.insertion;
      DocOp sp = s.preservation;
      DocOp sd = s.deletion;
      OperationPair<DocOp> r =
          new InsertionInsertionTransformer().transformOperations(ci, si);
      ci = r.clientOp();
      si = r.serverOp();
      r = new InsertionPreservationTransformer().transformOperations(ci, sp);
      ci = r.clientOp();
      sp = r.serverOp();
      r = new InsertionPreservationTransformer().transformOperations(si, cp);
      si = r.clientOp();
      cp = r.serverOp();
      r = new InsertionDeletionTransformer().transformOperations(ci, sd);
      ci = r.clientOp();
      sd = r.serverOp();
      r = new InsertionDeletionTransformer().transformOperations(si, cd);
      si = r.clientOp();
      cd = r.serverOp();
      DocOpCollector clientCollector = new DocOpCollector();
      DocOpCollector serverCollector = new DocOpCollector();
      clientCollector.add(ci);
      serverCollector.add(si);
      while (!AnnotationTamenessChecker.checkTameness(cp, sp, cd, sd)) {
        r = new PreservationPreservationTransformer().transformOperations(cp, sp);
        cp = r.clientOp();
        sp = r.serverOp();
        Pair<DocOp, Pair<DocOp, DocOp>> rc =
            new PreservationDeletionTransformer().transformOperations(cp, sd);
        Pair<DocOp, Pair<DocOp, DocOp>> rs =
            new PreservationDeletionTransformer().transformOperations(sp, cd);
        clientCollector.add(rc.first);
        serverCollector.add(rs.first);
        sp = rc.second.first;
        sd = rc.second.second;
        cp = rs.second.first;
        cd = rs.second.second;
      }
      r = new DeletionDeletionTransformer().transformOperations(cd, sd);
      cd = r.clientOp();
      sd = r.serverOp();
      clientCollector.add(cd);
      serverCollector.add(sd);
      return new OperationPair<DocOp>(
          clientCollector.composeAll(), serverCollector.composeAll());
    // We're catching runtime exceptions here, but checked exceptions may be better.
    } catch (RuntimeException e) {
      throw new TransformException(e);
    }
  }

}
