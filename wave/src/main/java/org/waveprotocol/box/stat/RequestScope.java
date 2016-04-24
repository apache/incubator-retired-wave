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
package org.waveprotocol.box.stat;

import java.util.Map;

/**
 * Request scope with execution tree and current execution node.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
@SuppressWarnings("rawtypes")
public interface RequestScope {

  public interface Value {
    public Value clone();
  }

  /**
   * Initializes scope.
   */
  public void enter();

  /**
   * Initializes scope with specified values.
   */
  public void enter(Map<Class, Value> values);

  /**
   * Clears scope.
   */
  public void exit();

  /**
   * Checks is scope initialized.
   */
  public boolean isEntered();

  /**
   * Sets value.
   */
  public <T extends Value> void set(Class<T> clazz, T value);

  /**
   * Gets value.
   */
  public <T extends Value> T get(Class<T> clazz);

  /**
   * Clone values.
   */
  public Map<Class, Value> cloneValues();
}
