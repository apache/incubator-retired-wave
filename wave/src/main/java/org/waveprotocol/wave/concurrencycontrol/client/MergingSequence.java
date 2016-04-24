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

package org.waveprotocol.wave.concurrencycontrol.client;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.Composer;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.BlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * A sequence of operations that composes adjacent operations when
 * they can be safely merged together into a single operation.
 *
 * @author zdwang@google.com (David Wang)
 */
public class MergingSequence extends AbstractList<WaveletOperation> {
  private final List<WaveletOperation> ops = CollectionUtils.newArrayList();

  /**
   * Constructs and empty operation sequence
   */
  public MergingSequence() {
  }

  /**
   * Constructs a delta from another delta's operations. Does not copy any
   * version or signature information.
   */
  public MergingSequence(Iterable<WaveletOperation> ops) {
    for (WaveletOperation op : ops) {
      add(op);
    }
  }

  public void optimise() {
    int startSize = size();
    if (startSize == 1) {
      return;
    }

    ArrayList<DocOp> docOps = CollectionUtils.newArrayList();
    List<WaveletOperation> oldOperations = CollectionUtils.newArrayList(this);
    String currentId = null;
    WaveletOperationContext lastOperationContext = null;
    this.clear();

    for (WaveletOperation waveletOp : oldOperations) {
      if (waveletOp instanceof WaveletBlipOperation) {
        WaveletBlipOperation waveletBlipOp = ((WaveletBlipOperation) waveletOp);
        String id = waveletBlipOp.getBlipId();
        BlipOperation blipOp = waveletBlipOp.getBlipOp();
        if (blipOp instanceof BlipContentOperation) {
          if (!docOps.isEmpty() && !id.equals(currentId)) {
            composeDocOps(this, currentId, lastOperationContext, docOps);
          }
          docOps.add(((BlipContentOperation) blipOp).getContentOp());
          lastOperationContext = blipOp.getContext();
          currentId = id;
          continue;
        }
      }
      if (!docOps.isEmpty()) {
        composeDocOps(this, currentId, lastOperationContext, docOps);
      }
      add(waveletOp);
    }
    if (!docOps.isEmpty()) {
      composeDocOps(this, currentId, lastOperationContext, docOps);
    }
  }

  private static void composeDocOps(List<WaveletOperation> operations, String id,
      WaveletOperationContext context, List<DocOp> docOps) {
    operations.add(new WaveletBlipOperation(id,
        new BlipContentOperation(context, Composer.compose(docOps))));
    docOps.clear();
  }

  @Override
  public WaveletOperation get(int index) {
    return ops.get(index);
  }

  @Override
  public int size() {
    return ops.size();
  }

  @Override
  public boolean add(WaveletOperation op) {
    return ops.add(op);
  }

  @Override
  public WaveletOperation remove(int index) {
    return ops.remove(index);
  }
}
