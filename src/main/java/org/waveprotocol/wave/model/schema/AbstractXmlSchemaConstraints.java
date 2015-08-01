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

package org.waveprotocol.wave.model.schema;

import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;

import java.util.Collections;
import java.util.List;

/**
 * Helper base class for somewhat simplifying the chore of defining schema
 * constraints, while allowing any method to be overridden for Turing complete
 * schema validation goodness.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class AbstractXmlSchemaConstraints implements DocumentSchema {
  private static final String IMPLICIT_ROOT_TYPE = "";

  // Signals an error if type is IMPLICIT_ROOT_TYPE, and translates null to IMPLICIT_ROOT_TYPE.
  // We do this because we can't use null as a key in StringMaps.
  private static String fixType(String type) {
    Preconditions.checkArgument(!IMPLICIT_ROOT_TYPE.equals(type), "Invalid type");
    if (type == null) {
      return IMPLICIT_ROOT_TYPE;
    } else {
      return type;
    }
  }

  private static void checkNotTopLevel(String type) {
    Preconditions.checkArgument(!IMPLICIT_ROOT_TYPE.equals(type), "Invalid type");
    Preconditions.checkNotNull(type, "Null type");
  }

  private final StringMap<StringSet> permittedChildren = CollectionUtils.createStringMap();
  private final StringMap<StringMap<StringSet>> permittedAttrs = CollectionUtils.createStringMap();
  private final StringMap<PermittedCharacters> textPermitted = CollectionUtils.createStringMap();
  private final StringMap<List<String>> requiredInitial = CollectionUtils.createStringMap();

  public void addRequiredInitial(String key, List<String> list) {
    requiredInitial.put(key, list);
  }

  public void containsBlipText(String ... types) {
    for (String type : types) {
      type = fixType(type);
      textPermitted.put(type, PermittedCharacters.BLIP_TEXT);
    }
  }

  public void containsAnyText(String ... types) {
    for (String type : types) {
      type = fixType(type);
      textPermitted.put(type, PermittedCharacters.ANY);
    }
  }

  public void addChildren(String parentType, String ... childTypes) {
    parentType = fixType(parentType);
    StringSet permitted = permittedChildren.get(parentType);
    if (permitted == null) {
      permittedChildren.put(parentType, permitted = CollectionUtils.createStringSet());
    }
    for (String childType : childTypes) {
      checkNotTopLevel(childType);
      permitted.add(childType);
    }
  }

  public void addAttrs(String type, String ... attrNames) {
    checkNotTopLevel(type);
    for (String name : attrNames) {
      addAttrWithValues(type, name);
    }
  }

  public void addAttrWithValues(String type, String attrName, String ... permittedValues) {
    checkNotTopLevel(type);
    StringMap<StringSet> attrs = permittedAttrs.get(type);
    if (attrs == null) {
      permittedAttrs.put(type, attrs = CollectionUtils.createStringMap());
    }

    if (permittedValues.length == 0) {
      attrs.put(attrName, null);
    } else {
      StringSet values = attrs.get(attrName);
      if (values == null) {
        attrs.put(attrName, values = CollectionUtils.createStringSet());
      }

      for (String value : permittedValues) {
        values.add(value);
      }
    }
  }

  @Override
  public boolean permitsAttribute(String type, String attr) {
    checkNotTopLevel(type);
    StringMap<StringSet> attrs = permittedAttrs.get(type);
    return attrs != null && (attrs.containsKey(attr));
  }

  @Override
  public boolean permitsAttribute(String type, String attr, String value) {
    checkNotTopLevel(type);
    StringMap<StringSet> attrs = permittedAttrs.get(type);
    return attrs != null && attrs.containsKey(attr) &&
        (attrs.get(attr) == null || attrs.get(attr).contains(value));
  }

  @Override
  public boolean permitsChild(String parentType, String childType) {
    parentType = fixType(parentType);
    checkNotTopLevel(childType);
    StringSet permitted = permittedChildren.get(parentType);
    return permitted != null && permitted.contains(childType);
  }

  @Override
  public PermittedCharacters permittedCharacters(String typeOrNull) {
    typeOrNull = fixType(typeOrNull);
    PermittedCharacters permitted = textPermitted.get(typeOrNull);
    return permitted != null ? permitted : PermittedCharacters.NONE;
  }

  @Override
  public List<String> getRequiredInitialChildren(String type) {
    type = fixType(type);
    return requiredInitial.containsKey(type)
        ? requiredInitial.get(type) : Collections.<String>emptyList();
  }
}
