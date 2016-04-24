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

import com.google.common.collect.Maps;

import org.waveprotocol.box.stat.ExecutionTree;
import org.waveprotocol.box.stat.RequestScope;

import java.util.Map;

/**
 * Request scope with execution tree and current execution node for use in multi-thread environment.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MultiThreadedRequestScope implements RequestScope {

  private final ThreadLocal<Map<Class, RequestScope.Value>> values = new ThreadLocal<>();

  public MultiThreadedRequestScope() {
  }

  @Override
  public void enter() {
    values.set(Maps.<Class, RequestScope.Value>newHashMap());
    values.get().put(ExecutionTree.class, new ExecutionTree());
  }

  @Override
  public void enter(Map<Class, RequestScope.Value> values) {
    this.values.set(Maps.<Class, RequestScope.Value>newHashMap(values));
  }

  @Override
  public boolean isEntered() {
    return values.get() != null;
  }

  @Override
  public void exit() {
    values.remove();
  }

  @Override
  public <T extends RequestScope.Value> void set(Class<T> clazz, T value) {
    values.get().put(clazz, value);
  }

  @Override
  public <T extends RequestScope.Value> T get(Class<T> clazz) {
    return (T)values.get().get(clazz);
  }

  @Override
  public Map<Class, RequestScope.Value> cloneValues() {
    Map<Class, Value> map = Maps.<Class, Value>newHashMap();
    for (Map.Entry<Class, Value> entry : values.get().entrySet()) {
      map.put(entry.getKey(), entry.getValue().clone());
    }
    return map;
  }
}
