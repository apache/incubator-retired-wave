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
package org.waveprotocol.box.server.executor;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.waveprotocol.box.stat.RequestScope;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Session-based request scope executor.
 * Runs on specified executor.
 * Clones request scope on scheduling and restores scope on executing of task.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
@SuppressWarnings("rawtypes")
public class ScheduledRequestScopeExecutor implements ScheduledExecutorService {
  private final static Logger LOG = Logger.getLogger(ScheduledRequestScopeExecutor.class.getName());

  private ScheduledExecutorService executor;

  @Inject
  public ScheduledRequestScopeExecutor() {
  }

  /**
   * Sets the original executor.
   */
  public void setExecutor(ScheduledExecutorService executor, String name) {
    Preconditions.checkArgument(this.executor == null, "Executor is already defined.");
    this.executor = executor;
  }

  @Override
  public ScheduledFuture<?> schedule(final Runnable runnable, final long delay, final TimeUnit unit) {
    Preconditions.checkNotNull(executor, "Executor is not defined.");
    return executor.schedule(makeScopedRunnable(runnable), delay, unit);
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long l, TimeUnit tu) {
    Preconditions.checkNotNull(executor, "Executor is not defined.");
    return executor.schedule(makeScopedCallable(callable), l, tu);
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable, long l, long l1, TimeUnit tu) {
    Preconditions.checkNotNull(executor, "Executor is not defined.");
    return executor.scheduleAtFixedRate(makeScopedRunnable(runnable), l, l1, tu);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable runnable, long l, long l1, TimeUnit tu) {
    Preconditions.checkNotNull(executor, "Executor is not defined.");
    return executor.scheduleAtFixedRate(makeScopedRunnable(runnable), l, l1, tu);
  }

  @Override
  public void shutdown() {
    executor.shutdown();
  }

  @Override
  public List<Runnable> shutdownNow() {
    return executor.shutdownNow();
  }

  @Override
  public boolean isShutdown() {
    return executor.isShutdown();
  }

  @Override
  public boolean isTerminated() {
    return executor.isTerminated();
  }

  @Override
  public boolean awaitTermination(long l, TimeUnit tu) throws InterruptedException {
    return executor.awaitTermination(l, tu);
  }

  @Override
  public <T> Future<T> submit(Callable<T> clbl) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> Future<T> submit(Runnable r, T t) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Future<?> submit(Runnable r) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> clctn) throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> clctn, long l, TimeUnit tu) throws InterruptedException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> clctn) throws InterruptedException, ExecutionException {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(Collection<? extends Callable<T>> clctn, long l, TimeUnit tu) throws InterruptedException, ExecutionException, TimeoutException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void execute(Runnable r) {
    throw new UnsupportedOperationException();
  }

  private Runnable makeScopedRunnable(final Runnable runnable) {
    final Map<Class, RequestScope.Value> scopeValues
      = (Timing.isEnabled()) ? Timing.cloneScopeValues() : null;

    return new Runnable() {
      @Override
      public void run() {
        if (scopeValues != null) {
          Timing.enterScope(scopeValues);
        }
        try {
          runnable.run();
        } finally {
          Timing.exitScope();
        }
      }
    };
  }

  private <T> Callable<T> makeScopedCallable(final Callable<T> callable) {
    final Map<Class, RequestScope.Value> scopeValues
      = (Timing.isEnabled()) ? Timing.cloneScopeValues() : null;

    return new Callable<T> () {
      @Override
      public T call() throws Exception {
        if (scopeValues != null) {
          Timing.enterScope(scopeValues);
        }
        try {
          return callable.call();
        } finally {
          Timing.exitScope();
        }
      }
    };
  }
}
