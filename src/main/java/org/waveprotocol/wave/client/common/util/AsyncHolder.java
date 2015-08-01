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

package org.waveprotocol.wave.client.common.util;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Queue;

/**
 * Something that holds a value, revealed by potentially asynchronous access.
 *
 * @param <T> type of held value
 */
public interface AsyncHolder<T> {

  /** Accessor of a value in this holder. */
  public interface Accessor<T> {
    void use(T x);
  }

  /**
   * Reveals the value held by this holder to a consumer. The value may be
   * revealed synchronously or asynchronously.
   *
   * @param accessor procedure to apply
   */
  void call(Accessor<T> accessor);

  public abstract class Impl<T> implements AsyncHolder<T>, Accessor<T> {
    private Queue<Accessor<T>> waiting;
    private T value;

    @Override
    public final void call(Accessor<T> accessor) {
      if (value == null) {
        addWaiter(accessor);
      } else {
        accessor.use(value);
      }
    }

    private void addWaiter(Accessor<T> accessor) {
      assert value == null;
      if (waiting == null) {
        waiting = CollectionUtils.createQueue();
        waiting.add(accessor);
        create(this);
      } else {
        waiting.add(accessor);
      }
    }

    protected abstract void create(Accessor<T> whenReady);

    @Override
    public void use(T x) {
      Preconditions.checkState(value == null && waiting != null);
      value = x;
      for (Accessor<T> waiter : waiting) {
        waiter.use(value);
      }
      waiting = null;
    }
  }
}
