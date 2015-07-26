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
package org.waveprotocol.box.server.stat;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Singleton;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.waveprotocol.box.stat.Timed;
import org.waveprotocol.box.stat.Timer;
import org.waveprotocol.box.stat.Timing;

import java.lang.reflect.Method;

/**
 * Intercepts method calls that have a {@link Timed} annotation.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author David Byttow
 */
@Singleton
public class TimingInterceptor implements MethodInterceptor {

  private LoadingCache<Method, String> nameCache =
      CacheBuilder.newBuilder().build(new CacheLoader<Method, String>() {
        @Override
        public String load(Method method) throws Exception {
          return method.getDeclaringClass().getSimpleName() + "." + method.getName();
        }
      });

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    Method method = methodInvocation.getMethod();
    Timed timed = method.getAnnotation(Timed.class);
    String name = timed.value();
    if (name.isEmpty()) {
      name = nameCache.get(methodInvocation.getMethod());
    }
    Timer timer;
    if (timed.isRequest()) {
      timer = Timing.startRequest(name, timed.threshold());
    } else {
      timer = Timing.start(name, timed.threshold());
    }
    try {
      return methodInvocation.proceed();
    } finally {
      Timing.stop(timer);
    }
  }
}
