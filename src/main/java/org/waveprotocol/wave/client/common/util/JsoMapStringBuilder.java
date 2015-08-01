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

package org.waveprotocol.wave.client.common.util;

import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.ReadableIntMap;
import org.waveprotocol.wave.model.util.ReadableNumberMap;
import org.waveprotocol.wave.model.util.ReadableStringMap;

/**
 * Can render each kind of JsoMapBase as a string.
 *
 */
public final class JsoMapStringBuilder implements
    ReadableStringMap.ProcV<Object>, ReadableIntMap.ProcV<Object>,
    ReadableNumberMap.ProcV<Object>, IdentityMap.ProcV<Object, Object> {

  /** Singleton used by statics. */
  private static final JsoMapStringBuilder INSTANCE = new JsoMapStringBuilder();

  /** Builder used during lifetime of each doString. */
  private StringBuilder builder = null;

  public static String toString(IntMapJsoView<?> m) {
    return INSTANCE.doString(m);
  }

  public static String toString(NumberMapJsoView<?> m) {
    return INSTANCE.doString(m);
  }

  public static String toString(IdentityMap<?,?> m) {
    return INSTANCE.doString(m);
  }

  public String doString(IntMapJsoView<?> m) {
    builder = new StringBuilder();
    builder.append("{");
    m.each(this);
    builder.append("}");
    String result = builder.toString();
    builder = null;
    return result;
  }

  public String doString(NumberMapJsoView<?> m) {
    builder = new StringBuilder();
    builder.append("{");
    m.each(this);
    builder.append("}");
    String result = builder.toString();
    builder = null;
    return result;
  }

  public String doString(IdentityMap<?,?> m) {
    builder = new StringBuilder();
    builder.append("{");
    m.each(this);
    builder.append("}");
    String result = builder.toString();
    builder = null;
    return result;
  }

  @Override
  public void apply(String key, Object item) {
    builder.append(" " + key + ": " + item + "; ");
  }

  @Override
  public void apply(double key, Object item) {
    builder.append(" " + key + ": " + item + "; ");
  }

  @Override
  public void apply(int key, Object item) {
    builder.append(" " + key + ": " + item + "; ");
  }

  @Override
  public void apply(Object key, Object item) {
    builder.append(" " + key + ": " + item + "; ");
  }
}
