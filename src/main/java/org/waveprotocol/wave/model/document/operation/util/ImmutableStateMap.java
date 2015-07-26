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

import org.waveprotocol.wave.model.document.operation.util.ImmutableUpdateMap.AttributeUpdate;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class ImmutableStateMap<T extends ImmutableStateMap<T, U>, U extends UpdateMap>
    extends AbstractMap<String, String> {

  /**
   * A name-value pair representing an attribute.
   */
  public static final class Attribute implements Map.Entry<String,String> {
    // TODO: This class can be simplified greatly if
    // AbstractMap.SimpleImmutableEntry from Java 6 can be used.

    private final String name;
    private final String value;

    /**
     * Creates an attribute with a map entry representing an attribute
     * name-value pair.
     *
     * @param entry The attribute's name-value pair.
     */
    public Attribute(Map.Entry<String,String> entry) {
      this(entry.getKey(), entry.getValue());
    }

    /**
     * Creates an attribute given a name-value pair.
     *
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public Attribute(String name, String value) {
      Preconditions.checkNotNull(name, "Null attribute name");
      Preconditions.checkNotNull(value, "Null attribute value");
      this.name = name;
      this.value = value;
    }

    @Override
    public String getKey() {
      return name;
    }

    @Override
    public String getValue() {
      return value;
    }

    @Override
    public String setValue(String value) {
      throw new UnsupportedOperationException("Attempt to modify an immutable map entry.");
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Map.Entry)) {
        return false;
      }
      Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
      return ((name == null) ? entry.getKey() == null : name.equals(entry.getKey())) &&
          ((value == null) ? entry.getValue() == null : value.equals(entry.getValue()));
    }

    @Override
    public int hashCode() {
      return ((name == null) ? 0 : name.hashCode()) ^
          ((value == null) ? 0 : value.hashCode());
    }

    @Override
    public String toString() {
      return "Attribute(" + name + "=" + value + ")";
    }
  }

  private final List<Attribute> attributes;

  private final Set<Map.Entry<String,String>> entrySet =
      new AbstractSet<Map.Entry<String,String>>() {

        @Override
        public Iterator<Map.Entry<String,String>> iterator() {
          return new Iterator<Map.Entry<String,String>>() {
            private final Iterator<Attribute> iterator = attributes.iterator();
            public boolean hasNext() {
              return iterator.hasNext();
            }
            public Attribute next() {
              return iterator.next();
            }
            public void remove() {
              throw new UnsupportedOperationException("Attempt to modify an immutable set.");
            }
          };
        }

        @Override
        public int size() {
          return attributes.size();
        }

      };

  /**
   * Creates a new T object containing no T.
   */
  public ImmutableStateMap() {
    attributes = Collections.emptyList();
  }

  protected static final Comparator<Attribute> comparator = new Comparator<Attribute>() {
    @Override
    public int compare(Attribute a, Attribute b) {
      return a.name.compareTo(b.name);
    }
  };

  /**
   * Constructs a new <code>T</code> object with the T
   * specified by the given mapping.
   *
   * @param map The mapping of attribute names to attribute values.
   */
  public ImmutableStateMap(Map<String,String> map) {
    this.attributes = attributeListFromMap(map);
  }

  public ImmutableStateMap(String ... pairs) {
    Preconditions.checkArgument(pairs.length % 2 == 0, "Pairs must come in groups of two");

    Map<String, String> map = new HashMap<String, String>();

    for (int i = 0; i < pairs.length; i += 2) {
      Preconditions.checkNotNull(pairs[i], "Null key");
      Preconditions.checkNotNull(pairs[i + 1], "Null value");
      if (map.containsKey(pairs[i])) {
        Preconditions.illegalArgument("Duplicate key: " + pairs[i]);
      }
      map.put(pairs[i], pairs[i + 1]);
    }

    this.attributes = attributeListFromMap(map);
  }

  private List<Attribute> attributeListFromMap(Map<String, String> map) {
    ArrayList<Attribute> attributeList = new ArrayList<Attribute>(map.size());
    for (Map.Entry<String, String> entry : map.entrySet()) {
      if (entry.getKey() == null || entry.getValue() == null) {
        Preconditions.nullPointer("This map does not allow null keys or values");
      }
      attributeList.add(new Attribute(entry));
    }
    Collections.sort(attributeList, comparator);
    return attributeList;
  }

  protected ImmutableStateMap(List<Attribute> attributes) {
    this.attributes = attributes;
  }

  @Override
  public Set<Map.Entry<String,String>> entrySet() {
    return entrySet;
  }

  /**
   * Returns a <code>T</code> object obtained by applying the update
   * specified by the <code>U</code> object into this
   * <code>T</code> object.
   *
   * @param attributeUpdate The update to apply.
   * @return A <code>T</code> object obtained by applying the given
   *         update onto this object.
   */
  public T updateWith(U attributeUpdate) {
    return updateWith(attributeUpdate, true);
  }

  public T updateWithNoCompatibilityCheck(U attributeUpdate) {
    return updateWith(attributeUpdate, false);
  }

  private T updateWith(U attributeUpdate, boolean checkCompatibility) {
    List<Attribute> newImmutableStateMap = new ArrayList<Attribute>();
    Iterator<Attribute> iterator = attributes.iterator();
    Attribute nextAttribute = iterator.hasNext() ? iterator.next() : null;
    // TODO: Have a slow path when the cast would fail.
    List<AttributeUpdate> updates = ((ImmutableUpdateMap<?,?>) attributeUpdate).updates;
    for (AttributeUpdate update : updates) {
      while (nextAttribute != null) {
        int comparison = update.name.compareTo(nextAttribute.name);
        if (comparison > 0) {
          newImmutableStateMap.add(nextAttribute);
          nextAttribute = iterator.hasNext() ? iterator.next() : null;
        } else if (comparison < 0) {
          if (checkCompatibility && update.oldValue != null) {
            Preconditions.illegalArgument(
                "Mismatched old value: attempt to update unset attribute with " + update);
          }
          break;
        } else if (comparison == 0) {
          if (checkCompatibility && !nextAttribute.value.equals(update.oldValue)) {
            Preconditions.illegalArgument(
                "Mismatched old value: attempt to update " + nextAttribute + " with " + update);
          }
          nextAttribute = iterator.hasNext() ? iterator.next() : null;
          break;
        }
      }
      if (update.newValue != null) {
        newImmutableStateMap.add(new Attribute(update.name, update.newValue));
      }
    }
    if (nextAttribute != null) {
      newImmutableStateMap.add(nextAttribute);
      while (iterator.hasNext()) {
        newImmutableStateMap.add(iterator.next());
      }
    }
    return createFromList(newImmutableStateMap);
  }

  protected abstract T createFromList(List<Attribute> attributes);

  public static void checkAttributesSorted(List<Attribute> attributes) {
    Attribute previous = null;
    for (Attribute a : attributes) {
      Preconditions.checkNotNull(a, "Null attribute");
      assert a.name != null;
      assert a.value != null;
      if (previous != null && previous.name.compareTo(a.name) >= 0) {
        Preconditions.illegalArgument(
            "Attribute keys not strictly monotonic: " + previous.name + ", " + a.name);
      }
      previous = a;
    }
  }

  public static <T extends ImmutableStateMap<T, U>, U extends ImmutableUpdateMap<U, ?>> T
      updateWithoutCompatibilityCheck(T state, U update) {
    // the cast below is required to work with javac from OpenJDK 7
    return ((ImmutableStateMap<T, U>) state).updateWith(update, false);
  }
}
