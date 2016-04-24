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
package org.waveprotocol.box.webclient.stat;

import com.google.common.collect.Maps;

import org.waveprotocol.box.stat.ExecutionTree;
import org.waveprotocol.box.stat.RequestScope;

import java.util.Map;

/**
 * Request scope with execution tree and current execution node for use in single-thread environment.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class SingleThreadedRequestScope implements RequestScope {

  private Map<Class, Value> values;

  public SingleThreadedRequestScope() {
  }

  @Override
  public void enter() {
    values = Maps.<Class, Value>newHashMap();
    values.put(ExecutionTree.class, new ExecutionTree());
  }

  @Override
  public void enter(Map<Class, Value> values) {
    this.values = Maps.<Class, Value>newHashMap(values);
  }

  @Override
  public boolean isEntered() {
    return values != null;
  }

  @Override
  public void exit() {
    checkScoping();
    values = null;
  }

  @Override
  public <T extends Value> void set(Class<T> clazz, T value) {
    checkScoping();
    values.put(clazz, value);
  }

  @Override
  public <T extends Value> T get(Class<T> clazz) {
    checkScoping();
    return (T)values.get(clazz);
  }

  @Override
  public Map<Class, Value> cloneValues() {
    checkScoping();
    Map<Class, Value> map = Maps.<Class, Value>newHashMap();
    for (Map.Entry<Class, Value> entry : values.entrySet()) {
      map.put(entry.getKey(), entry.getValue().clone());
    }
    return map;
  }

  private void checkScoping() {
    if (values == null) {
      enter();
    }
  }
}
