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

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.concurrencycontrol.common.DeltaPair;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * Ordered list of operations extractable as single-creator deltas. Consecutive
 * operations with matching creators are merged into a single delta. Adjacent
 * operations with mismatched creators result in two separate deltas. Allows
 * transformation of the enqueued client operations against a server delta.
 *
 */
class OperationQueue {
  /**
   * Helper for transforming a client delta against a server delta in such a way
   * that can be substituted out for testing.
   */
  @VisibleForTesting
  interface Transformer {
    /**
     * Transforms a client delta against a server delta in a manner which can be
     * overridden for testing.
     */
    DeltaPair transform(Iterable<WaveletOperation> client, Iterable<WaveletOperation> server)
        throws TransformException;
  }

  private enum ItemState {
    /**
     * This delta has been sent. You are not allowed to take the operations in this
     * delta and create a new delta by concatenating the operations together with another delta.
     */
    SENT,
    /**
     * The delta have been optimised.
     */
    OPTIMISED,
    /**
     * This is a newly created, untouched delta
     */
    NONE
  }

  /**
   * This class is used to keep additional information with OperationMergingDelta. This
   * is what lives internally in the queue.
   */
  private static class Item {
    final MergingSequence opSequence;
    final ItemState state;

    /**
     * @param delta assumed not null
     * @param state The state of the item.
     */
    public Item(MergingSequence delta, ItemState state) {
      this.opSequence = delta;
      this.state = state;
    }

    @Override
    public String toString() {
      return "Delta: " + opSequence + ", item state: " + state;
    }
  }

  /** Transforms deltas using {@link DeltaPair#transform()}. */
  private static final Transformer TRANSFORMER = new Transformer() {
    @Override
    public DeltaPair transform(Iterable<WaveletOperation> client, Iterable<WaveletOperation> server)
        throws TransformException {
      return (new DeltaPair(client, server)).transform();
    }
  };

  /** Number of head deltas to inspect when estimating queue size. */
  private static final int ESTIMATE_DELTAS_TO_COUNT = 4;

  private final LinkedList<Item> queue;
  private ParticipantId tailCreator;
  private final Transformer transformer;

  /**
   * Creates an empty {@link OperationQueue} that will transform deltas using
   * {@link DeltaPair#transform()}.
   */
  public OperationQueue() {
    this(TRANSFORMER);
  }

  /**
   * Creates an empty {@link OperationQueue} which will use the given
   * {@link Transformer}.
   */
  @VisibleForTesting
  OperationQueue(Transformer transformer) {
    this.transformer = transformer;
    queue = new LinkedList<Item>();
    tailCreator = null;
  }

  /**
   * Adds the given operation to the tail of the operation queue. Merges with
   * the delta at the tail if the creators match, otherwise creates a new tail
   * delta.
   */
  public void add(WaveletOperation op) {
    ParticipantId creator = op.getContext().getCreator();
    if (queue.isEmpty() || !creator.equals(tailCreator) ||
        (queue.getLast().state != ItemState.NONE)) {
      queue.addLast(new Item(new MergingSequence(), ItemState.NONE));
      tailCreator = creator;
    }
    queue.getLast().opSequence.add(op);
  }

  /**
   * Prepends the given delta onto the queue's head. No merging is
   * allows on this delta. This is because we don't know if the server have actually got
   * the previously sent delta, we can't change the delta once it's sent.
   *
   * @param newHead delta to use for the queue. Must only contain operations from a
   *        single author. May be empty, in which case this call will do
   *        nothing.
   */
  public void insertHead(List<WaveletOperation> newHead) {
    if (newHead.isEmpty()) {
      return;
    }
    MergingSequence mergingHead = new MergingSequence(newHead);
    Item item = new Item(mergingHead, ItemState.SENT);
    ParticipantId creator = mergingHead.get(0).getContext().getCreator();
    if (queue.isEmpty()) {
      queue.add(item);
      tailCreator = creator;
    } else {
      queue.addFirst(item);
    }
  }

  /** Returns true if there are no pending operations in the queue. */
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  /**
   * Estimates the number of operations in this queue.
   *
   * In order to provide a bounded execution time the result is an underestimate
   * of the true number of queued operations.
   */
  public int estimateSize() {
    int estimate = 0;
    // Sum delta size for a fixed number of deltas at the start.
    int headDeltasToCount = ESTIMATE_DELTAS_TO_COUNT;
    Iterator<Item> itr = queue.iterator();
    while ((headDeltasToCount > 0) && itr.hasNext()) {
      estimate += itr.next().opSequence.size();
      --headDeltasToCount;
    }
    if (itr.hasNext()) {
      // Add size of the last delta as new ops are likely to be pushed into it.
      estimate += queue.getLast().opSequence.size();
      // Add one for each other delta in the queue.
      if (queue.size() > (ESTIMATE_DELTAS_TO_COUNT + 1)) {
        estimate += queue.size() - (ESTIMATE_DELTAS_TO_COUNT + 1);
      }
    }
    return estimate;
  }

  /**
   * Takes a delta full of operations by the same creator from the head of the
   * queue, removing those operations from the queue. It will contain all
   * operations from the head of the queue up until but not including the first
   * change in creator. Hence if all operations in the queue have the same
   * creator, it will contain all those operations and the queue will become
   * empty.
   *
   * @return A non-empty delta without signature or version information,
   *         containing operations which all have the same creator address in
   *         their context.
   * @throws NoSuchElementException If the queue is empty.
   */
  public List<WaveletOperation> take() {
    Item item = takeMergedAndOptimisedItem(queue);

    if (isEmpty()) {
      tailCreator = null;
    }

    return item.opSequence;
  }

  /**
   * Transforms the given server delta against the queued operations. Updates
   * the queued deltas with the results of the transformation and returns the
   * transformed server delta.
   *
   * Queued delta which have all of their operations transformed away and hence
   * become empty are discarded.
   *
   * @throws TransformException If transformation of any operations fails.
   */
  public List<WaveletOperation> transform(List<WaveletOperation> serverOps)
      throws TransformException {
    List<WaveletOperation> transformedServerOps = serverOps;
    Queue<Item> newQueue = new LinkedList<Item>();

    while (!queue.isEmpty()) {
      // Merge in all subsequent consecutive deltas that have the same author and
      // optimise the delta before transforming against the server delta because
      // it makes transformation more efficient.
      Item item = takeMergedAndOptimisedItem(queue);
      MergingSequence queuedDelta = item.opSequence;

      DeltaPair transformedDeltas = transformer.transform(queuedDelta, transformedServerOps);

      // Even if server op is nullified we must still count the nullified op,
      // hence we use the input server ops for incrementing the version. This
      // is already transformedDelta.getServer() and we don't touch it.
      transformedServerOps = transformedDeltas.getServer();

      // Discard client deltas which have had all their ops transformed away
      if (!transformedDeltas.getClient().isEmpty()) {
        newQueue.add(new Item(
            new MergingSequence(transformedDeltas.getClient()), item.state));
      }
    }
    queue.addAll(newQueue);

    return transformedServerOps;
  }

  /**
   * This removes the first item from the queue and subsequent consecutive items that
   * have the same author to produce a single item that contains all the ops in the items
   * removed.
   *
   * We don't merge sent ones due to non-commutativity of transformation and composition.
   *
   * @return If the returned item does not have the SENT state, it's delta is always optimised.
   * @throws NoSuchElementException If the queue is empty.
   */
  private Item takeMergedAndOptimisedItem(Queue<Item> queue) {
    Item item = queue.remove();

    // Cannot change delta of sent delta
    if (item.state == ItemState.SENT) {
      return item;
    }

    MergingSequence resultDelta = item.opSequence;
    ParticipantId creator = resultDelta.get(0).getContext().getCreator();
    boolean needOptimisation = item.state != ItemState.OPTIMISED;

    while (!queue.isEmpty()) {
      Item nextItem = queue.element();
      MergingSequence nextDelta = nextItem.opSequence;
      ParticipantId nextCreator = nextDelta.get(0).getContext().getCreator();

      // don't merge sent ones due to non-commutativity of transformation
      // and composition
      if ((nextItem.state != ItemState.SENT) && creator.equals(nextCreator)) {
        resultDelta.addAll(nextDelta);
        queue.remove();
        needOptimisation = true;
      } else {
        break;
      }
    }

    if (needOptimisation) {
      resultDelta.optimise();
    }

    return new Item(resultDelta, ItemState.OPTIMISED);
  }

  @Override
  public String toString() {
    // Empty space before \n intentional to print in browser
    return "Operation Queue = " +
        "[deltas: " + queue.size() + "] \n" +
        "[queue: " + queue + "] \n" +
        "[tailCreator: " + tailCreator + "] \n";
  }
}
