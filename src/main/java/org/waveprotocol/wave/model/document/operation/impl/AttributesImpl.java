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

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.util.ImmutableStateMap;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AttributesImpl
    extends ImmutableStateMap<AttributesImpl, AttributesUpdate>
    implements Attributes {

  public AttributesImpl() {
    super();
  }

  public AttributesImpl(Map<String, String> map) {
    super(map);
  }

  AttributesImpl(List<Attribute> attributes) {
    super(attributes);
  }

  public AttributesImpl(String... pairs) {
    super(pairs);
  }

  @Override
  protected AttributesImpl createFromList(List<Attribute> attributes) {
    return new AttributesImpl(attributes);
  }

  public static AttributesImpl fromSortedAttributes(List<Attribute> sortedAttributes) {
    checkAttributesSorted(sortedAttributes);
    return fromSortedAttributesUnchecked(sortedAttributes);
  }

  public static AttributesImpl fromSortedAttributesUnchecked(List<Attribute> sortedAttributes) {
    return new AttributesImpl(sortedAttributes);
  }

  public static AttributesImpl fromUnsortedAttributes(List<Attribute> unsortedAttributes) {
    List<Attribute> sorted = new ArrayList<Attribute>(unsortedAttributes);
    Collections.sort(sorted, comparator);
    // Use the checked variant here to check for duplicates.
    return fromSortedAttributes(sorted);
  }

  public static AttributesImpl fromStringMap(ReadableStringMap<String> stringMap) {
    final List<Attribute> attrs = new ArrayList<Attribute>();
    stringMap.each(new ProcV<String>() {
      @Override
      public void apply(String key, String value) {
        attrs.add(new Attribute(key, value));
      }
    });
    return fromUnsortedAttributes(attrs);
  }

  /**
   * Sorts the input but doesn't check for duplicate names.
   */
  public static AttributesImpl fromUnsortedAttributesUnchecked(List<Attribute> unsortedAttributes) {
    List<Attribute> sorted = new ArrayList<Attribute>(unsortedAttributes);
    Collections.sort(sorted, comparator);
    return fromSortedAttributesUnchecked(sorted);
  }

  /**
   * Sorts the input but doesn't check for duplicate names.
   *
   * @param pairs [name, value, name, value, ...]
   */
  public static Attributes fromUnsortedPairsUnchecked(String ... pairs) {
    Preconditions.checkArgument(pairs.length % 2 == 0, "pairs.length must be even");
    List<Attribute> attrs = new ArrayList<Attribute>();
    for (int i = 0; i < pairs.length; i += 2) {
      attrs.add(new Attribute(pairs[i], pairs[i + 1]));
    }

    return fromUnsortedAttributesUnchecked(attrs);
  }
}
