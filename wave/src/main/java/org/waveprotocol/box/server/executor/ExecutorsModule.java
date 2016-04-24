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

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.*;
import com.typesafe.config.Config;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.*;

import java.util.concurrent.*;

/**
 * Module with executors.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class ExecutorsModule extends AbstractModule {

  @Inject
  public ExecutorsModule() {
  }

  @Override
  public void configure() {
    bind(RequestScopeExecutor.class);
    bind(ScheduledRequestScopeExecutor.class);
  }

  @Provides
  @Singleton
  @ClientServerExecutor
  protected Executor provideClientServerExecutor(Provider<RequestScopeExecutor> executorProvider) {
    return provideThreadPoolExecutor(executorProvider, -1, ClientServerExecutor.class
        .getSimpleName());
  }

  @Provides
  @Singleton
  @DeltaPersistExecutor
  protected Executor provideDeltaPersistExecutor(Provider<RequestScopeExecutor> executorProvider,
      Config config) {
    return provideThreadPoolExecutor(executorProvider, config
        .getInt("threads.delta_persist_executor_thread_count"), DeltaPersistExecutor.class
        .getSimpleName());
  }

  @Provides
  @Singleton
  @IndexExecutor
  protected Executor provideIndexExecutor(Provider<RequestScopeExecutor> executorProvider) {
    return provideThreadPoolExecutor(executorProvider, 1, IndexExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @ListenerExecutor
  protected Executor provideListenerExecutor(Provider<RequestScopeExecutor> executorProvider,
      Config config) {
    return provideThreadPoolExecutor(executorProvider, config
        .getInt("threads.listener_executor_thread_count"), ListenerExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @LookupExecutor
  protected Executor provideLookupExecutor(Provider<RequestScopeExecutor> executorProvider,
      Config config) {
    return provideThreadPoolExecutor(executorProvider, config
        .getInt("threads.lookup_executor_thread_count"), LookupExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @StorageContinuationExecutor
  protected Executor provideStorageContinuationExecutor(
      Provider<RequestScopeExecutor> executorProvider, Config config) {
    return provideThreadPoolExecutor(executorProvider, config
        .getInt("threads.storage_continuation_executor_thread_count"),
        StorageContinuationExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @WaveletLoadExecutor
  protected Executor provideWaveletLoadExecutor(Provider<RequestScopeExecutor> executorProvider,
      Config config) {
    return provideThreadPoolExecutor(executorProvider, config
        .getInt("threads.wavelet_load_executor_thread_count"), WaveletLoadExecutor.class
        .getSimpleName());
  }

  @Provides
  @Singleton
  @ContactExecutor
  protected ScheduledExecutorService provideContactExecutor(
      Provider<ScheduledRequestScopeExecutor> executorProvider, Config config) {
    return provideScheduledThreadPoolExecutor(executorProvider, config
        .getInt("threads.contact_executor_thread_count"), ContactExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @RobotConnectionExecutor
  protected ScheduledExecutorService provideRobotConnectionExecutor(
      Provider<ScheduledRequestScopeExecutor> executorProvider, Config config) {
    return provideScheduledThreadPoolExecutor(executorProvider, config
        .getInt("threads.robot_connection_thread_count"), RobotConnectionExecutor.class
        .getSimpleName());
  }

  @Provides
  @Singleton
  @RobotGatewayExecutor
  protected Executor provideRobotGatewayExecutor(Provider<RequestScopeExecutor> executorProvider,
      Config config) {
    return provideThreadPoolExecutor(executorProvider, config
        .getInt("threads.robot_gateway_thread_count"), RobotGatewayExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @XmppExecutor
  protected ScheduledExecutorService provideXmppExecutor(
      Provider<ScheduledRequestScopeExecutor> executorProvider) {
    return provideScheduledThreadPoolExecutor(executorProvider, 1, XmppExecutor.class
        .getSimpleName());
  }

  @Provides
  @Singleton
  @SolrExecutor
  protected Executor provideSolrExecutor(Provider<RequestScopeExecutor> executorProvider,
      Config config) {
    return provideThreadPoolExecutor(executorProvider, config.getInt("threads.solr_thread_count"),
        SolrExecutor.class.getSimpleName());
  }

  private Executor provideThreadPoolExecutor(Provider<RequestScopeExecutor> executorProvider,
      int threadCount, String name) {
    if (threadCount == 0) {
      return MoreExecutors.sameThreadExecutor();
    }
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(name).build();
    ExecutorService executor;
    if (threadCount < 0) {
      executor = Executors.newCachedThreadPool(threadFactory);
    } else if (threadCount == 1) {
      executor = Executors.newSingleThreadExecutor(threadFactory);
    } else {
      executor = Executors.newFixedThreadPool(threadCount, threadFactory);
    }
    RequestScopeExecutor scopeExecutor = executorProvider.get();
    scopeExecutor.setExecutor(executor, name);
    return scopeExecutor;
  }

  private ScheduledExecutorService provideScheduledThreadPoolExecutor(
      Provider<ScheduledRequestScopeExecutor> executorProvider, int threadCount, String name) {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(name).build();
    ScheduledExecutorService executor;
    if (threadCount == 1) {
      executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    } else {
      executor = Executors.newScheduledThreadPool(threadCount, threadFactory);
    }
    ScheduledRequestScopeExecutor scopeExecutor = executorProvider.get();
    scopeExecutor.setExecutor(executor, name);
    return scopeExecutor;
  }
}
