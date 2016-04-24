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
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.waveprotocol.box.stat.RequestScope;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Session-based request scope executor.
 * Runs on specified executor.
 * Clones request scope on scheduling and restores scope on executing of task.
 *
 * @author (David Byttow)
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
@SuppressWarnings("rawtypes")
public class RequestScopeExecutor implements Executor {
  private final static Logger LOG = Logger.getLogger(RequestScopeExecutor.class.getName());

  private ExecutorService executor;

  @Inject
  public RequestScopeExecutor() {
  }

  public void setExecutor(ExecutorService executor, String name) {
    Preconditions.checkArgument(this.executor == null, "Executor is already defined.");
    this.executor = executor;
  }

  @Override
  public void execute(final Runnable runnable) {
    Preconditions.checkNotNull(executor, "Executor is not defined.");

    final Map<Class, RequestScope.Value> values =
            Timing.isEnabled() ? Timing.cloneScopeValues() : null;

    executor.submit(new Runnable() {
      @Override
      public void run() {
        if (values != null) {
          Timing.enterScope(values);
        }
        try {
          runnable.run();
        } catch (Exception e) {
          LOG.log(Level.WARNING, "Error executing with portable scope.", e);
        } finally {
          Timing.exitScope();
        }
      }
    });
  }
}
