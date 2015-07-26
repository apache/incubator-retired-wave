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
package org.waveprotocol.box.server;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import static com.google.inject.matcher.Matchers.annotatedWith;
import static com.google.inject.matcher.Matchers.any;
import com.google.inject.name.Named;
import com.typesafe.config.Config;
import org.waveprotocol.box.server.stat.MultiThreadedRequestScope;

import org.waveprotocol.box.server.stat.TimingInterceptor;

import org.waveprotocol.box.stat.Timed;
import org.waveprotocol.box.stat.Timing;

/**
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class StatModule extends AbstractModule {
  private final boolean enableProfiling;

  @Inject
  public StatModule(Config config) {
    this.enableProfiling = config.getBoolean("core.enable_profiling");
  }

  @Override
  protected void configure() {
    TimingInterceptor interceptor = new TimingInterceptor();
    requestInjection(interceptor);
    if (enableProfiling) {
      bindInterceptor(any(), annotatedWith(Timed.class), interceptor);
    }
    Timing.setScope(new MultiThreadedRequestScope());
    Timing.setEnabled(enableProfiling);
  }
}
