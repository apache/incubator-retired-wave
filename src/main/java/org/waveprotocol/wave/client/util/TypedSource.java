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


package org.waveprotocol.wave.client.util;

/**
 * Interface for classes that provide typed getters.
 *
 */
public interface TypedSource {
  /**
   * Returns a <code>Boolean</code> corresponding to the key.
   *
   * @return <code>Boolean</code>, on error returns <code>null</code>.
   */
  public Boolean getBoolean(String key);

  /**
   * Returns an <code>Integer</code> corresponding to the key.
   *
   * @return <code>Integer</code>, on error returns <code>null</code>.
   */
  public Integer getInteger(String key);

  /**
   * Returns a <code>Double</code> corresponding to the key.
   *
   * @return <code>Double</code>, on error returns <code>null</code>.
   */
  public Double getDouble(String key);

  /**
   * Returns a <code>String</code> corresponding to the key.
   *
   * @return <code>String</code>, on error returns <code>null</code>.
   */
  public String getString(String key);
}
