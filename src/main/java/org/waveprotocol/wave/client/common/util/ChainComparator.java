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

import java.util.Comparator;

/**
 * Used to chain comparators. First compare the arguments using the first
 * comparator. If non-zero, return the result. Return the results of the second
 * comparator.
 *
 * This can be nested to combine arbitrary number of comparators.
 *
 *
 * @param <T>
 */
public final class ChainComparator<T> implements Comparator<T> {
  private final Comparator<? super T> firstComparator;
  private final Comparator<? super T> secondComparator;

  public ChainComparator(Comparator<? super T> firstComparator,
      Comparator<? super T> secondComparator) {
    this.firstComparator = firstComparator;
    this.secondComparator = secondComparator;
  }

  /** {@inheritDoc} **/
  public int compare(T o1, T o2) {
    int cmp = firstComparator.compare(o1, o2);
    return cmp != 0 ? cmp : secondComparator.compare(o1, o2);
  }
}
