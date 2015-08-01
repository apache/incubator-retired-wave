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

package org.waveprotocol.wave.model.document.operation.util;

import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class ImmutableUpdateMap<T extends ImmutableUpdateMap<T, U>, U extends UpdateMap>
    implements UpdateMap {

  public static class AttributeUpdate {
    public final String name;
    final String oldValue;
    final String newValue;

    public AttributeUpdate(String name, String oldValue, String newValue) {
      Preconditions.checkNotNull(name, "Null name in AttributeUpdate");
      this.name = name;
      this.oldValue = oldValue;
      this.newValue = newValue;
    }

    @Override
    public String toString() {
      return "[" + name + ": " + oldValue + " -> " + newValue + "]";
    }
  }

  protected final List<AttributeUpdate> updates;

  @Override
  public int changeSize() {
    return updates.size();
  }

  @Override
  public String getChangeKey(int i) {
    return updates.get(i).name;
  }

  @Override
  public String getOldValue(int i) {
    return updates.get(i).oldValue;
  }

  @Override
  public String getNewValue(int i) {
    return updates.get(i).newValue;
  }

  public ImmutableUpdateMap() {
    updates = Collections.emptyList();
  }

  public ImmutableUpdateMap(String ... triples) {
    Preconditions.checkArgument(triples.length % 3 == 0, "Triples must come in groups of three");

    ArrayList<AttributeUpdate> accu = new ArrayList<AttributeUpdate>(triples.length / 3);
    for (int i = 0; i < triples.length; i += 3) {
      Preconditions.checkNotNull(triples[i], "Null key");
      accu.add(new AttributeUpdate(triples[i], triples[i + 1], triples[i + 2]));
    }

    Collections.sort(accu, comparator);

    for (int i = 1; i < accu.size(); i++) {
      int x = comparator.compare(accu.get(i - 1), accu.get(i));
      if (x == 0) {
        throw new IllegalArgumentException("Duplicate key: " + accu.get(i).name);
      }
      assert x < 0;
    }

    updates = accu;
  }

  public ImmutableUpdateMap(Map<String, Pair<String, String>> updates) {
    this(tripletsFromMap(updates));
  }

  private static String[] tripletsFromMap(Map<String, Pair<String, String>> updates) {
    String[] triplets = new String[updates.size() * 3];
    int i = 0;
    for (Map.Entry<String, Pair<String, String>> e : updates.entrySet()) {
      triplets[i++] = e.getKey();
      triplets[i++] = e.getValue().getFirst();
      triplets[i++] = e.getValue().getSecond();
    }
    return triplets;
  }

  protected ImmutableUpdateMap(List<AttributeUpdate> updates) {
    this.updates = updates;
  }

  public T exclude(Collection<String> names) {
    List<AttributeUpdate> newAttributes = new ArrayList<AttributeUpdate>();
    for (AttributeUpdate update : updates) {
      if (!names.contains(update.name)) {
        newAttributes.add(update);
      }
    }
    return createFromList(newAttributes);
  }

  protected static final Comparator<AttributeUpdate> comparator =
      new Comparator<AttributeUpdate>() {
        @Override
        public int compare(AttributeUpdate a, AttributeUpdate b) {
          return a.name.compareTo(b.name);
        }
      };

  public T composeWith(U mutation) {
    List<AttributeUpdate> newAttributes = new ArrayList<AttributeUpdate>();
    Iterator<AttributeUpdate> iterator = updates.iterator();
    AttributeUpdate nextAttribute = iterator.hasNext() ? iterator.next() : null;
    // TODO: Have a slow path when the cast would fail.
    List<AttributeUpdate> mutationAttributes = ((ImmutableUpdateMap<?,?>) mutation).updates;
    loop: for (AttributeUpdate attribute : mutationAttributes) {
      while (nextAttribute != null) {
        int comparison = comparator.compare(attribute, nextAttribute);
        if (comparison < 0) {
          break;
        } else if (comparison > 0) {
          newAttributes.add(nextAttribute);
          nextAttribute = iterator.hasNext() ? iterator.next() : null;
        } else {
          if (!areEqual(nextAttribute.newValue, attribute.oldValue)) {
            Preconditions.illegalArgument(
                "Mismatched old value: attempt to update " + nextAttribute + " with " + attribute);
          }
          newAttributes.add(new AttributeUpdate(attribute.name, nextAttribute.oldValue,
              attribute.newValue));
          nextAttribute = iterator.hasNext() ? iterator.next() : null;
          continue loop;
        }
      }
      newAttributes.add(attribute);
    }
    if (nextAttribute != null) {
      newAttributes.add(nextAttribute);
      while (iterator.hasNext()) {
        newAttributes.add(iterator.next());
      }
    }
    return createFromList(newAttributes);
  }

  protected abstract T createFromList(List<AttributeUpdate> attributes);

  // TODO: Is there a utility method for this somewhere?
  private boolean areEqual(Object a, Object b) {
    return (a == null) ? b == null : a.equals(b);
  }

  @Override
  public String toString() {
    return "Updates: " + updates;
  }

  public static void checkUpdatesSorted(List<AttributeUpdate> updates) {
    AttributeUpdate previous = null;
    for (AttributeUpdate u : updates) {
      Preconditions.checkNotNull(u, "Null attribute update");
      assert u.name != null;
      if (previous != null && previous.name.compareTo(u.name) >= 0) {
        Preconditions.illegalArgument(
            "Attribute keys not strictly monotonic: " + previous.name + ", " + u.name);
      }
      previous = u;
    }
  }

}
