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

import org.waveprotocol.wave.client.util.OverridingTypedSource.Builder;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * A {@link TypedSource} that adds some overrides to an existing source. Given a
 * base source, overrides are specified using a {@link #of(TypedSource) builder}
 * over that source, and then {@link Builder#build() concretized}.
 *
 */
public final class OverridingTypedSource implements TypedSource {

  /** Builder through which overrides are specified. */
  public interface Builder {
    Builder withBoolean(String key, boolean value);

    Builder withDouble(String key, double value);

    Builder withInteger(String key, int value);

    Builder withString(String key, String value);

    OverridingTypedSource build();
  }

  /**
   * Holds maps that override the values from a base source.
   */
  private static class MapsHolder implements TypedSource, Builder {
    private final StringMap<Boolean> booleans = CollectionUtils.createStringMap();
    private final StringMap<Double> doubles = CollectionUtils.createStringMap();
    private final StringMap<Integer> ints = CollectionUtils.createStringMap();
    private final StringMap<String> strings = CollectionUtils.createStringMap();

    private final TypedSource base;

    private MapsHolder(TypedSource base) {
      this.base = base;
    }

    @Override
    public Boolean getBoolean(String key) {
      Boolean value;
      return (value = booleans.get(key)) != null ? value : base.getBoolean(key);
    }

    @Override
    public Double getDouble(String key) {
      Double value;
      return (value = doubles.get(key)) != null ? value : base.getDouble(key);
    }

    @Override
    public Integer getInteger(String key) {
      Integer value;
      return (value = ints.get(key)) != null ? value : base.getInteger(key);
    }

    @Override
    public String getString(String key) {
      String value;
      return (value = strings.get(key)) != null ? value : base.getString(key);
    }

    @Override
    public MapsHolder withBoolean(String key, boolean value) {
      booleans.put(key, value);
      return this;
    }

    @Override
    public MapsHolder withDouble(String key, double value) {
      doubles.put(key, value);
      return this;
    }

    @Override
    public MapsHolder withInteger(String key, int value) {
      ints.put(key, value);
      return this;
    }

    @Override
    public MapsHolder withString(String key, String value) {
      strings.put(key, value);
      return this;
    }

    @Override
    public OverridingTypedSource build() {
      return new OverridingTypedSource(this);
    }
  }

  /** Built set of overrides. */
  private final MapsHolder overrides;

  private OverridingTypedSource(MapsHolder overrides) {
    this.overrides = overrides;
  }

  public static Builder of(TypedSource base) {
    return new MapsHolder(base);
  }

  @Override
  public Boolean getBoolean(String key) {
    return overrides.getBoolean(key);
  }

  @Override
  public Double getDouble(String key) {
    return overrides.getDouble(key);
  }

  @Override
  public Integer getInteger(String key) {
    return overrides.getInteger(key);
  }

  @Override
  public String getString(String key) {
    return overrides.getString(key);
  }
}
