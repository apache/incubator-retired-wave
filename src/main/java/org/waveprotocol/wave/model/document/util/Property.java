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

package org.waveprotocol.wave.model.document.util;

/**
 * Property object for use by external handlers to add extra
 * transient data to elements and guarantee their fields
 * won't conflict
 *
 * @param <T>
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class Property<T> {

  private static int uniqueId;

  private static class ImmutableProperty<T> extends Property<T> {
    private ImmutableProperty(String debugName) {
      super(debugName);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "ImmutableProperty[" + getName() + "]";
    }
  }

  private static class MutableProperty<T> extends Property<T> {
    private MutableProperty(String debugName) {
      super(debugName);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      return "MutableProperty[" + getName() + "]";
    }
  }

  /**
   * Create a property that may be set more than once
   *
   * @param debugName Name that is useful for debugging
   * @return A new property identifier
   */
  public static <T> MutableProperty<T> mutable(String debugName) {
    return new MutableProperty<T>(debugName == null ? "[unnamed]" : debugName);
  }

  /**
   * Create a property that may only be set once
   *
   * @param debugName Name that is useful for debugging
   * @return A new property identifier
   */
  public static <T> ImmutableProperty<T> immutable(String debugName) {
    return new ImmutableProperty<T>(debugName == null ? "[unnamed]" : debugName);
  }

  private final int id;
  private final String name;

  private Property(String debugName) {
    id = ++uniqueId;
    name = debugName;
  }

  /**
   * @return The unique id of this property. The id is not guaranteed to be the
   *   same between application runs
   */
  public int getId() {
    return id;
  }

  String getName() {
    return name;
  }
}
