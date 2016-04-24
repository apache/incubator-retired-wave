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
 * Wraps a JavaScriptObject to provide checked access and implements the
 * TypedSource interface.
 *
 *
 */
public class WrappedJSObject implements TypedSource {
  private final ExtendedJSObject jso;

  public WrappedJSObject(ExtendedJSObject jso) {
    this.jso = jso;
  }

  @Override
  public Boolean getBoolean(String key) {
    return jso.hasBoolean(key) ? jso.getBooleanUnchecked(key) : null;
  }

  @Override
  public Double getDouble(String key) {
    return jso.hasNumber(key) ? jso.getDoubleUnchecked(key) : null;
  }

  @Override
  public Integer getInteger(String key) {
    return jso.hasNumber(key) ? jso.getIntegerUnchecked(key) : null;
  }

  @Override
  public String getString(String key) {
    return jso.hasString(key) ? jso.getStringUnchecked(key) : null;
  }

  public WrappedJSObject getObject(String key) {
    return jso.hasObject(key) ? new WrappedJSObject(jso.getObjectUnchecked(key)) : null;
  }

  public ExtendedJSObject getRawObject(String key) {
    return jso.hasObject(key) ? jso.getObjectUnchecked(key) : null;
  }
}
