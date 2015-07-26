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

package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.util.OffsetList;

/**
 * A simple immutable container/offset pair, useful for representing locations
 * in an {@link OffsetList}. The offset represents the offset within the
 * container, not of the offset in the list.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <T> type parameter of the data stored in the offset list containers.
 */
public final class OffsetPoint<T> {

  /**
   * Convenience type alias for a "Finder", a {@link OffsetList.LocationAction}
   * that returns an {@link OffsetPoint} for a given integer location.
   */
  public interface Finder<T> extends OffsetList.LocationAction<T, OffsetPoint<T>> {
  }

  /**
   * Creates a {@link OffsetList.LocationAction} that returns an
   * <code>OffsetPoint</code> for an integer location.
   *
   * @param <T> type parameter of the container
   * @return a new finder
   */
  public static <T> Finder<T> finder() {
    return new Finder<T>() {
      public OffsetPoint<T> performAction(OffsetList.Container<T> container, int offset) {
        return new OffsetPoint<T>(container, offset);
      }
    };
  }

  private final OffsetList.Container<T> container;
  private final int offset;

  /**
   * @param container the container
   * @param offset the offset into the container
   */
  public OffsetPoint(OffsetList.Container<T> container, int offset) {
    this.container = container;
    this.offset = offset;
  }

  /**
   * @return the container
   */
  public OffsetList.Container<T> getContainer() {
    return container;
  }

  /**
   * @return the offset within the container
   */
  public int getOffset() {
    return offset;
  }

  /**
   * @return the container's value
   */
  public T getValue() {
    return container.getValue();
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "OffsetPoint(" + offset + "," + container + ")";
  }

  // eclipse-generated equals and hashCode

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((container == null) ? 0 : container.hashCode());
    result = prime * result + offset;
    return result;
  }

  /** {@inheritDoc} */
  @SuppressWarnings({"unchecked", "rawtypes"})
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof OffsetPoint)) 
      return false;
    final OffsetPoint other = (OffsetPoint) obj;
    if (container == null) {
      if (other.container != null)
        return false;
    } else if (!container.equals(other.container))
      return false;
    if (offset != other.offset)
      return false;
    return true;
  }
}
