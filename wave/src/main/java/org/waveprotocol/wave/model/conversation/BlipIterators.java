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

package org.waveprotocol.wave.model.conversation;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Predicate;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;


/**
 * Iterators for conversations.
 *
 * @author anorth@google.com (Alex North)
 */
public final class BlipIterators {
  /**
   * A predicate that accepts all blips.
   */
  private final static Predicate<ConversationBlip> ALL = new Predicate<ConversationBlip>() {
    public boolean apply(ConversationBlip target) {
      return true;
    }
  };

  private static final class BlipIterator implements Iterator<ConversationBlip> {

    private final Predicate<ConversationBlip> acceptsBlip;
    private final Queue<ConversationThread> threads = CollectionUtils.newLinkedList();
    private Iterator<? extends ConversationBlip> currentThreadItr;
    private ConversationBlip nextBlip;

    BlipIterator(ConversationThread thread, Predicate<ConversationBlip> acceptor) {
      this.acceptsBlip = acceptor;
      this.currentThreadItr = thread.getBlips().iterator();
      this.nextBlip = findNextBlip();
    }

    @Override
    public boolean hasNext() {
      return nextBlip != null;
    }

    @Override
    public ConversationBlip next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      ConversationBlip result = nextBlip;
      nextBlip = findNextBlip();
      return result;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    private ConversationBlip findNextBlip() {
      ConversationBlip next = null;
      while ((next == null) && currentThreadItr.hasNext()) {
        ConversationBlip maybeNext;
        maybeNext = currentThreadItr.next();
        for (ConversationThread reply : maybeNext.getReplyThreads()) {
          threads.add(reply);
        }
        if (acceptsBlip.apply(maybeNext)) {
          next = maybeNext;
        }
        while (!currentThreadItr.hasNext() && (threads.peek() != null)) {
          currentThreadItr = threads.remove().getBlips().iterator();
        }
      }
      return next;
    }
  }

  /**
   * A breadth-first iterator over blips in a conversation. Deleted blips are
   * skipped, but non-deleted descendant blips are included.
   */
  public static Iterable<ConversationBlip> breadthFirst(Conversation conversation) {
    return breadthFirst(conversation.getRootThread());
  }

  /**
   * A breadth-first iterator over the non-deleted descendant blips of a thread.
   */
  public static Iterable<ConversationBlip> breadthFirst(final ConversationThread thread) {
    return new Iterable<ConversationBlip>() {
      @Override
      public Iterator<ConversationBlip> iterator() {
        return new BlipIterator(thread, ALL);
      }
    };
  }

  private BlipIterators() {
  }
}
