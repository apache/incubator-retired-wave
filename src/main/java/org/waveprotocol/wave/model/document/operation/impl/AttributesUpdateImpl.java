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

package org.waveprotocol.wave.model.document.operation.impl;

import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.util.ImmutableUpdateMap;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AttributesUpdateImpl
    extends ImmutableUpdateMap<AttributesUpdateImpl, AttributesUpdate>
    implements AttributesUpdate {

  public static final AttributesUpdateImpl EMPTY_MAP = new AttributesUpdateImpl();

  public AttributesUpdateImpl() {
  }

  public AttributesUpdateImpl(String ... triples) {
    super(triples);
  }

  public AttributesUpdateImpl(Map<String, Pair<String, String>> updates) {
    super(updates);
  }

  private AttributesUpdateImpl(List<AttributeUpdate> updates) {
    super(updates);
  }

  @Override
  protected AttributesUpdateImpl createFromList(List<AttributeUpdate> updates) {
    return new AttributesUpdateImpl(updates);
  }

  public static AttributesUpdateImpl fromSortedUpdates(List<AttributeUpdate> sortedUpdates) {
    checkUpdatesSorted(sortedUpdates);
    return fromSortedUpdatesUnchecked(sortedUpdates);
  }

  public static AttributesUpdateImpl fromSortedUpdatesUnchecked(
      List<AttributeUpdate> sortedUpdates) {
    return new AttributesUpdateImpl(sortedUpdates);
  }

  public static AttributesUpdateImpl fromUnsortedUpdates(
      List<AttributeUpdate> unsortedUpdates) {
    List<AttributeUpdate> sorted = new ArrayList<AttributeUpdate>(unsortedUpdates);
    Collections.sort(sorted, comparator);
    // Use the checked variant here to check for duplicates.
    return fromSortedUpdates(sorted);
  }

  /**
   * Sorts the input but doesn't check for duplicate names.
   */
  public static AttributesUpdateImpl fromUnsortedUpdatesUnchecked(
      List<AttributeUpdate> unsortedUpdates) {
    List<AttributeUpdate> sorted = new ArrayList<AttributeUpdate>(unsortedUpdates);
    Collections.sort(sorted, comparator);
    return fromSortedUpdatesUnchecked(sorted);
  }

  /**
   * Sorts the input but doesn't check for duplicate names.
   *
   * @param triplets [name, oldValue, newValue, name, oldValue, newValue, ...]
   */
  public static AttributesUpdate fromUnsortedTripletsUnchecked(String ... triplets) {
    Preconditions.checkArgument(triplets.length % 3 == 0, "triplets.length must be divisible by 3");
    List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>();
    for (int i = 0; i < triplets.length; i += 3) {
      updates.add(new AttributeUpdate(triplets[i], triplets[i + 1], triplets[i + 2]));
    }

    return fromUnsortedUpdatesUnchecked(updates);
  }
}
