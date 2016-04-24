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

package org.waveprotocol.wave.model.experimental.schema;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.experimental.schema.AttributeValidationResult.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Tests for AttributesValidator.
 *
 */

public class AttributesValidatorTest extends TestCase {

  /**
   * Tests an AttributesValidator with only required attributes.
   */
  public void testRequiredAttributes() {
    AttributesValidator validator = new AttributesValidator(
        toSet("a1", "a2"),
        createValidators(
            "a1", "v1",
            "a2", "v2"));
    checkAttributes(validator, Type.MISSING_REQUIRED_ATTRIBUTE, AttributesImpl.EMPTY_MAP);
    checkAttributes(validator, Type.VALID, new AttributesImpl(
        "a1", "v1",
        "a2", "v2"));
    checkAttributes(validator, Type.INVALID_ATTRIBUTE_VALUE, new AttributesImpl(
        "a1", "v2",
        "a2", "v2"));
    checkAttributes(validator, Type.INVALID_ATTRIBUTE_VALUE, new AttributesImpl(
        "a1", "v1",
        "a2", "v1"));
    checkAttributes(validator, Type.MISSING_REQUIRED_ATTRIBUTE, new AttributesImpl(
        "a2", "v2"));
    checkAttributes(validator, Type.MISSING_REQUIRED_ATTRIBUTE, new AttributesImpl(
        "a1", "v1"));
    checkAttributes(validator, Type.ATTRIBUTE_NOT_ALLOWED, new AttributesImpl(
        "a1", "v1",
        "a2", "v2",
        "bad", "bad"));
    checkUpdate(validator, Type.VALID, AttributesUpdateImpl.EMPTY_MAP);
    checkUpdate(validator, Type.VALID, new AttributesUpdateImpl(
        "a1", "v1", "v1",
        "a2", "v2", "v2"));
    checkUpdate(validator, Type.INVALID_ATTRIBUTE_VALUE, new AttributesUpdateImpl(
        "a1", "v2", "v2",
        "a2", "v2", "v2"));
    checkUpdate(validator, Type.INVALID_ATTRIBUTE_VALUE, new AttributesUpdateImpl(
        "a1", "v1", "v1",
        "a2", "v1", "v1"));
    checkUpdate(validator, Type.VALID, new AttributesUpdateImpl(
        "a2", "v2", "v2"));
    checkUpdate(validator, Type.VALID, new AttributesUpdateImpl(
        "a1", "v1", "v1"));
    checkUpdate(validator, Type.ATTRIBUTE_NOT_ALLOWED, new AttributesUpdateImpl(
        "a1", "v1", "v1",
        "a2", "v2", "v2",
        "bad", "bad", "bad"));
    checkUpdate(validator, Type.REMOVING_REQUIRED_ATTRIBUTE, new AttributesUpdateImpl(
        "a1", "v1", null,
        "a2", "v2", "v2"));
  }

  /**
   * Tests an AttributesValidator with only non-required attributes.
   */
  public void testNonRequiredAttributes() {
    AttributesValidator validator = new AttributesValidator(
        Collections.<String>emptySet(),
        createValidators(
            "a1", "v1",
            "a2", "v2"));
    checkAttributes(validator, Type.VALID, AttributesImpl.EMPTY_MAP);
    checkAttributes(validator, Type.VALID, new AttributesImpl(
        "a1", "v1",
        "a2", "v2"));
    checkAttributes(validator, Type.INVALID_ATTRIBUTE_VALUE, new AttributesImpl(
        "a1", "v2",
        "a2", "v2"));
    checkAttributes(validator, Type.INVALID_ATTRIBUTE_VALUE, new AttributesImpl(
        "a1", "v1",
        "a2", "v1"));
    checkAttributes(validator, Type.VALID, new AttributesImpl(
        "a2", "v2"));
    checkAttributes(validator, Type.VALID, new AttributesImpl(
        "a1", "v1"));
    checkAttributes(validator, Type.ATTRIBUTE_NOT_ALLOWED, new AttributesImpl(
        "a1", "v1",
        "a2", "v2",
        "bad", "bad"));
    checkUpdate(validator, Type.VALID, AttributesUpdateImpl.EMPTY_MAP);
    checkUpdate(validator, Type.VALID, new AttributesUpdateImpl(
        "a1", "v1", "v1",
        "a2", "v2", "v2"));
    checkUpdate(validator, Type.INVALID_ATTRIBUTE_VALUE, new AttributesUpdateImpl(
        "a1", "v2", "v2",
        "a2", "v2", "v2"));
    checkUpdate(validator, Type.INVALID_ATTRIBUTE_VALUE, new AttributesUpdateImpl(
        "a1", "v1", "v1",
        "a2", "v1", "v1"));
    checkUpdate(validator, Type.VALID, new AttributesUpdateImpl(
        "a2", "v2", "v2"));
    checkUpdate(validator, Type.VALID, new AttributesUpdateImpl(
        "a1", "v1", "v1"));
    checkUpdate(validator, Type.ATTRIBUTE_NOT_ALLOWED, new AttributesUpdateImpl(
        "a1", "v1", "v1",
        "a2", "v2", "v2",
        "bad", "bad", "bad"));
    checkUpdate(validator, Type.VALID, new AttributesUpdateImpl(
        "a1", "v1", null,
        "a2", "v2", "v2"));
  }

  /**
   * Tests an AttributesValidator with a mix of required and non-required attributes.
   */
  public void testRequiredAndNonRequiredAttributes() {
    AttributesValidator validator = new AttributesValidator(
        toSet("a1"),
        createValidators(
            "a1", "v1",
            "a2", "v2"));
    checkAttributes(validator, Type.MISSING_REQUIRED_ATTRIBUTE, AttributesImpl.EMPTY_MAP);
    checkAttributes(validator, Type.VALID, new AttributesImpl(
        "a1", "v1",
        "a2", "v2"));
    checkAttributes(validator, Type.INVALID_ATTRIBUTE_VALUE, new AttributesImpl(
        "a1", "v2",
        "a2", "v2"));
    checkAttributes(validator, Type.INVALID_ATTRIBUTE_VALUE, new AttributesImpl(
        "a1", "v1",
        "a2", "v1"));
    checkAttributes(validator, Type.MISSING_REQUIRED_ATTRIBUTE, new AttributesImpl(
        "a2", "v2"));
    checkAttributes(validator, Type.VALID, new AttributesImpl(
        "a1", "v1"));
    checkAttributes(validator, Type.ATTRIBUTE_NOT_ALLOWED, new AttributesImpl(
        "a1", "v1",
        "a2", "v2",
        "bad", "bad"));
    checkUpdate(validator, Type.VALID, AttributesUpdateImpl.EMPTY_MAP);
    checkUpdate(validator, Type.VALID, new AttributesUpdateImpl(
        "a1", "v1", "v1",
        "a2", "v2", "v2"));
    checkUpdate(validator, Type.INVALID_ATTRIBUTE_VALUE, new AttributesUpdateImpl(
        "a1", "v2", "v2",
        "a2", "v2", "v2"));
    checkUpdate(validator, Type.INVALID_ATTRIBUTE_VALUE, new AttributesUpdateImpl(
        "a1", "v1", "v1",
        "a2", "v1", "v1"));
    checkUpdate(validator, Type.VALID, new AttributesUpdateImpl(
        "a2", "v2", "v2"));
    checkUpdate(validator, Type.VALID, new AttributesUpdateImpl(
        "a1", "v1", "v1"));
    checkUpdate(validator, Type.ATTRIBUTE_NOT_ALLOWED, new AttributesUpdateImpl(
        "a1", "v1", "v1",
        "a2", "v2", "v2",
        "bad", "bad", "bad"));
    checkUpdate(validator, Type.REMOVING_REQUIRED_ATTRIBUTE, new AttributesUpdateImpl(
        "a1", "v1", null,
        "a2", "v2", "v2"));
  }

  private static Set<String> toSet(String... v) {
    return new TreeSet<String>(Arrays.asList(v));
  }

  private static Map<String, ValueValidator> createValidators(String... keyValueList) {
    Map<String, ValueValidator> validators = new TreeMap<String, ValueValidator>();
    if (keyValueList.length % 2 != 0) {
      throw new IllegalArgumentException("The key-value list must have an even number of entries");
    }
    for (int i = 0; i < keyValueList.length; i += 2) {
      validators.put(keyValueList[i], ValueValidator.fromRegex(keyValueList[i+1]));
    }
    return validators;
  }

  private static void checkAttributes(AttributesValidator validator, Type expected,
      Attributes attributes) {
    assertEquals(expected, validator.validate(attributes).getType());
  }

  private static void checkUpdate(AttributesValidator validator, Type expected,
      AttributesUpdate attributesUpdate) {
    assertEquals(expected, validator.validate(attributesUpdate).getType());
  }

}
