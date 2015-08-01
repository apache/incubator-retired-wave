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

import java.util.ArrayList;
import java.util.List;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.util.ImmutableUpdateMap;
import org.waveprotocol.wave.model.util.Preconditions;

public class AnnotationsUpdateImpl
    extends ImmutableUpdateMap<AnnotationsUpdateImpl, AnnotationsUpdate>
    implements AnnotationsUpdate {

  public static final AnnotationsUpdateImpl EMPTY_MAP = new AnnotationsUpdateImpl();

  public AnnotationsUpdateImpl() {}

  private AnnotationsUpdateImpl(List<AttributeUpdate> updates) {
    super(updates);
  }

  @Override
  protected AnnotationsUpdateImpl createFromList(List<AttributeUpdate> updates) {
    return new AnnotationsUpdateImpl(updates);
  }

  /**
   * A string that is larger (according to compareTo) than any valid annotation key.
   */
  private static final String MAX_STRING = "\uFFFF";

  @Override
  public AnnotationsUpdateImpl composeWith(AnnotationBoundaryMap map) {
    List<AttributeUpdate> newUpdates = new ArrayList<AttributeUpdate>();
    int existingIndex = 0;
    int changeIndex = 0;
    int endIndex = 0;
    while (existingIndex < updates.size()
        || changeIndex < map.changeSize()
        || endIndex < map.endSize()) {
      String existingKey = existingIndex < updates.size() ? updates.get(existingIndex).name
          : MAX_STRING;
      String changeKey = changeIndex < map.changeSize() ? map.getChangeKey(changeIndex)
          : MAX_STRING;
      String endKey = endIndex < map.endSize() ? map.getEndKey(endIndex) : MAX_STRING;
      // cases:
      // existingKey < endKey && existingKey < changeKey: keep, advance existing
      // existingKey < endKey && existingKey = changeKey: replace, advance existing & change
      // existingKey < endKey && existingKey > changeKey: add change, advance change
      // existingKey = endKey && existingKey < changeKey: remove, advance existing & end
      // existingKey = endKey && existingKey = changeKey: error (key in both change and end)
      // existingKey = endKey && existingKey > changeKey: remove, add change, advance all 3
      // existingKey > endKey: error (attempt to end key that is not part of the update)
      int existingVsEnd = existingKey.compareTo(endKey);
      int existingVsChange = existingKey.compareTo(changeKey);
      if (existingVsEnd < 0) {
        if (existingVsChange < 0) {
          newUpdates.add(updates.get(existingIndex));
          existingIndex++;
        } else if (existingVsChange == 0) {
          newUpdates.add(new AttributeUpdate(changeKey,
              map.getOldValue(changeIndex),
              map.getNewValue(changeIndex)));
          existingIndex++;
          changeIndex++;
        } else if (existingVsChange > 0) {
          newUpdates.add(new AttributeUpdate(changeKey,
              map.getOldValue(changeIndex),
              map.getNewValue(changeIndex)));
          changeIndex++;
        } else {
          assert false;
        }
      } else if (existingVsEnd == 0) {
        if (existingVsChange < 0) {
          existingIndex++;
          endIndex++;
        } else if (existingVsChange == 0) {
          Preconditions.illegalArgument("AnnotationBoundaryMap with key both in change and end: "
              + changeKey);
        } else if (existingVsChange > 0) {
          newUpdates.add(new AttributeUpdate(changeKey,
              map.getOldValue(changeIndex),
              map.getNewValue(changeIndex)));
          existingIndex++;
          endIndex++;
          changeIndex++;
        } else {
          assert false;
        }
      } else if (existingVsEnd > 0) {
        Preconditions.illegalArgument("Attempt to end key that is not part of the update: "
            + endKey);
      } else {
        assert false;
      }
    }
    return createFromList(newUpdates);
  }

  public boolean containsKey(String key) {
    Preconditions.checkNotNull(key, "Null key");
    // TODO: Use Arrays.binarySearch(a, key, c).
    for (AttributeUpdate u : updates) {
      if (key.equals(u.name)) {
        return true;
      }
    }
    return false;
  }
}
