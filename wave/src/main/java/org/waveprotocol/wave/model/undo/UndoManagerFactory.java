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

package org.waveprotocol.wave.model.undo;


import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.algorithm.Transformer;
import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;

import java.util.List;

/**
 * A factory for creating undo managers for document operations.
 *
 */
public final class UndoManagerFactory {

  private static final UndoManagerImpl.Algorithms<DocOp> algorithms =
      new UndoManagerImpl.Algorithms<DocOp>() {

    @Override
    public DocOp invert(DocOp operation) {
      return DocOpInverter.invert(operation);
    }

    @Override
    public DocOp compose(List<DocOp> operations) {
      return Composer.compose(operations);
    }

    @Override
    public OperationPair<DocOp> transform(DocOp operation1,
        DocOp operation2) throws TransformException {
      return Transformer.transform(operation1, operation2);
    }

  };

  /**
   * Creates a new undo manager.
   *
   * @return A new undo manager.
   */
  public static UndoManagerPlus<DocOp> createUndoManager() {
    return new UndoManagerImpl<DocOp>(algorithms);
  }

}
