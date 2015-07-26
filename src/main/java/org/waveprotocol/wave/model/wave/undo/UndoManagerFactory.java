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

import org.waveprotocol.wave.model.operation.OperationPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.undo.UndoManagerImpl;
import org.waveprotocol.wave.model.undo.UndoManagerPlus;

import java.util.List;

/**
 * A factory for undo managers.
 *
 */
final class UndoManagerFactory {
  private UndoManagerFactory() { }

  private static final UndoManagerImpl.Algorithms<AggregateOperation> algorithms =
      new UndoManagerImpl.Algorithms<AggregateOperation>() {

    @Override
    public AggregateOperation compose(List<AggregateOperation> operations) {
      return AggregateOperation.compose(operations);
    }

    @Override
    public AggregateOperation invert(AggregateOperation operation) {
      return operation.invert();
    }

    @Override
    public OperationPair<AggregateOperation> transform(AggregateOperation op1,
        AggregateOperation op2) throws TransformException {
      return AggregateOperation.transform(op1, op2);
    }

  };

  /**
   * Create a new undo manager.
   *
   * @return A new undo manager.
   */
  public static UndoManagerPlus<AggregateOperation> createUndoManager() {
    return new UndoManagerImpl<AggregateOperation>(algorithms);
  }

  private static final UndoManagerImpl.Algorithms<WaveAggregateOp> wAlgorithms =
    new UndoManagerImpl.Algorithms<WaveAggregateOp>() {
    @Override
    public WaveAggregateOp compose(List<WaveAggregateOp> operations) {
      return WaveAggregateOp.compose(operations);
    }

    @Override
    public WaveAggregateOp invert(WaveAggregateOp operation) {
      return operation.invert();
    }

    @Override
    public OperationPair<WaveAggregateOp> transform(WaveAggregateOp op1, WaveAggregateOp op2)
      throws TransformException {
      return WaveAggregateOp.transform(op1, op2);
    }
  };

  /**
   * Create a new undo manager.
   *
   * @return A new undo manager.
   */
  public static UndoManagerPlus<WaveAggregateOp> createWUndoManager() {
    return new UndoManagerImpl<WaveAggregateOp>(wAlgorithms);
  }
}
