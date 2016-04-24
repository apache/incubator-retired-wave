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

import java.util.AbstractList;
import java.util.List;
import java.util.Map;

/**
 * A schema pattern.
 *
 */
public final class SchemaPattern {

  /**
   * An entry in the prologue, containing an element type and a pattern.
   */
  public static final class PrologueEntry {

    private final String type;
    private final SchemaPattern pattern;

    PrologueEntry(String type, SchemaPattern pattern) {
      this.type = type;
      this.pattern = pattern;
    }

    String elementType() {
      return type;
    }

    SchemaPattern pattern() {
      return pattern;
    }

  }

  // TODO(user): Change prologue implementation to prevent exponential space blowout attacks.
  /**
   * A prologue, containing a list of element patterns.
   */
  public static final class Prologue extends AbstractList<PrologueEntry> {

    private final List<PrologueEntry> entries;

    /**
     * The package-private constructor for constructing prologues.
     *
     * Warning: The entries list should not be modified after being used to
     * construct this prologue. It is not defensively copied because this
     * constructor is package-private.
     *
     * @param entries The list of element patterns that make up the prologue.
     */
    Prologue(List<PrologueEntry> entries) {
      this.entries = entries;
    }

    @Override
    public PrologueEntry get(int index) {
      return entries.get(index);
    }

    @Override
    public int size() {
      return entries.size();
    }

  }

  private AttributesValidator attributesValidator;
  private Prologue prologue;
  private Map<String, SchemaPattern> potentialChildren;
  private CharacterValidator characterValidator;

  SchemaPattern() {}

  void initialize(
      AttributesValidator attributesValidator,
      Prologue prologue,
      Map<String, SchemaPattern> potentialChildren,
      CharacterValidator characterValidator) {
    // It should be impossible for the fields to be non-null at this point.
    assert this.attributesValidator == null;
    assert this.prologue == null;
    assert this.potentialChildren == null;
    assert this.characterValidator == null;
    this.attributesValidator = attributesValidator;
    this.prologue = prologue;
    this.potentialChildren = potentialChildren;
    this.characterValidator = characterValidator;
  }

  /**
   * @return the prologue of this schema pattern
   */
  public Prologue prologue() {
    return prologue;
  }

  /**
   * Returns the schema pattern corresponding to the given child element type.
   *
   * @param elementType the element type of a child
   * @return the schema pattern the child should conform to
   */
  public SchemaPattern child(String elementType) {
    return potentialChildren.get(elementType);
  }

  /**
   * Checks whether the given <code>Attributes</code> conforms to this schema
   * pattern.
   *
   * @param attributes the <code>Attributes</code> to check
   * @return the result of validating the <code>Attributes</code>
   */
  public AttributeValidationResult validateAttributes(Attributes attributes) {
    return attributesValidator.validate(attributes);
  }

  /**
   * Checks whether the given <code>AttributesUpdate</code> conforms to this
   * schema pattern.
   *
   * @param update the <code>AttributesUpdate</code> to check
   * @return the result of validating the <code>AttributesUpdate</code>
   */
  public AttributeValidationResult validateAttributesUpdate(AttributesUpdate update) {
    return attributesValidator.validate(update);
  }

  /**
   * Checks whether the given characters conform to this schema pattern.
   *
   * @param characters the characters to check
   * @return -1 if the characters are all valid, or otherwise the number of
   *         valid Unicode characters before the first invalid character
   */
  public int validateCharacters(String characters) {
    return characterValidator.validate(characters);
  }

}
