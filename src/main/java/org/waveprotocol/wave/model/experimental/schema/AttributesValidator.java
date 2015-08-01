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

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.experimental.schema.AttributeValidationResult.AttributeNotAllowed;
import org.waveprotocol.wave.model.experimental.schema.AttributeValidationResult.InvalidAttributeValue;
import org.waveprotocol.wave.model.experimental.schema.AttributeValidationResult.MissingRequiredAttribute;
import org.waveprotocol.wave.model.experimental.schema.AttributeValidationResult.RemovingRequiredAttribute;

import java.util.Map;
import java.util.Set;

/**
 * A validator of attributes.
 *
 */
final class AttributesValidator {

  /**
   * The attributes which are required.
   */
  private final Set<String> requiredAttributes;

  /**
   * The validators for the values of each of the allowed attributes. These
   * validators determine what values are allowed for each attribute.
   */
  private final Map<String, ValueValidator> valueValidators;

  AttributesValidator(Set<String> requiredAttributes, Map<String,
      ValueValidator> valueValidators) {
    assert requiredAttributes != null;
    assert valueValidators != null;
    assert valueValidators.keySet().containsAll(requiredAttributes) :
        valueValidators.keySet() + ", " + requiredAttributes;
    this.requiredAttributes = requiredAttributes;
    this.valueValidators = valueValidators;
  }

  /**
   * Validate the given <code>Attributes</code>.
   *
   * @param attributes the <code>Attributes</code> to validate
   * @return information about whether the <code>Attributes</code> is valid and,
   *         if not, why it is not
   */
  AttributeValidationResult validate(Attributes attributes) {
    for (String name : requiredAttributes) {
      if (!attributes.containsKey(name)) {
        return new MissingRequiredAttribute(name);
      }
    }
    for (Map.Entry<String, String> attribute : attributes.entrySet()) {
      String name = attribute.getKey();
      ValueValidator validator = valueValidators.get(name);
      if (validator == null) {
        return new AttributeNotAllowed(name);
      }
      String value = attribute.getValue();
      if (!validator.validate(value)) {
        return new InvalidAttributeValue(name, value);
      }
    }
    return AttributeValidationResult.VALID;
  }

  /**
   * Validate the given <code>AttributesUpdate</code>.
   *
   * @param update the <code>AttributesUpdate</code> to validate
   * @return information about whether the <code>AttributesUpdate</code> is
   *         valid and, if not, why it is not
   */
  AttributeValidationResult validate(AttributesUpdate update) {
    for (int i = 0; i < update.changeSize(); ++i) {
      String name = update.getChangeKey(i);
      ValueValidator validator = valueValidators.get(name);
      if (validator == null) {
        return new AttributeNotAllowed(name);
      }
      String value = update.getNewValue(i);
      if (value == null) {
        if (requiredAttributes.contains(name)) {
          return new RemovingRequiredAttribute(name);
        }
      } else {
        if (!validator.validate(value)) {
          return new InvalidAttributeValue(name, value);
        }
      }
    }
    return AttributeValidationResult.VALID;
  }

}
