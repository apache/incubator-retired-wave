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

import java.util.regex.Pattern;

/**
 * A validator for attribute values that tests whether attribute values match
 * against a regular expression.
 *
 */
final class ValueValidator {

  /*
   * TODO(user): In the future, we should use our own implementation to avoid
   * java quirks.
   */
  private final Pattern pattern;

  private ValueValidator(String re) {
    pattern = Pattern.compile(re);
  }

  /**
   * Validates an attribute value.
   *
   * @param value a value to validate
   * @return whether the entire value matches the regular expression associated
   *         with this validator
   */
  boolean validate(String value) {
    return pattern.matcher(value).matches();
  }

  /**
   * Creates a <code>ValueValidator</code> from a regular expression.
   *
   * @param re a regular expression
   * @return the validator that matches attribute values against the given
   *         regular expression
   */
  static ValueValidator fromRegex(String re) {
    return new ValueValidator(re);
  }
}
