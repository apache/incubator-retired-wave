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

import org.waveprotocol.wave.model.experimental.schema.SchemaPattern.Prologue;
import org.waveprotocol.wave.model.experimental.schema.SchemaPattern.PrologueEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * An intermediate representation for the process of constructing a
 * <code>SchemaPattern</code> from a schema document.
 *
 */
final class IntermediateSchemaFragment {

  /*
   * A note on how a schema pattern is represented and how the compilation to a
   * schema pattern works:
   *
   * A schema pattern is represented as a graph of references between different
   * pattern objects, with each object representing some subpattern of the full
   * pattern. Each schema pattern contains information about its prologue,
   * non-prologue elements, allowed attributes, and allowed character data. The
   * prologue's pattern is stored as a list, where each item in the list
   * corresponds to an element in the prologue and contains a reference to a
   * schema pattern for that element. The information corresponding to
   * non-prologue child elements is stored as a map from the element type to the
   * schema pattern object that corresponds to that element type.
   *
   * Compilation is split into two stages.
   *
   * In the first stage, only a partial compile (of just the top-level
   * structure) of each definition is performed. The compilation of the patterns
   * corresponding to both prologue and non-prologue child elements of the
   * definition is deferred (but their pattern objects are created and remain
   * uninitialized at this stage). This creates object references to schema
   * pattern objects required by a later full compile and also needed by the
   * partial top-level compiles of other definitions. Any circular references
   * encountered in this first stage will indicate a bad infinite recursion, and
   * should trigger an <code>InvalidSchemaException</code>.
   *
   * In the second stage of the compilation, all deferred compilation is brought
   * to completion. Any circular references encountered in this second stage are
   * the result of safe recursion.
   */

  /**
   * An intermediate format for a prologue entry.
   */
  static final class IntermediatePrologueEntry {

    private final String type;
    private final IntermediateSchemaFragment fragment;

    IntermediatePrologueEntry(String type, IntermediateSchemaFragment fragment) {
      this.type = type;
      this.fragment = fragment;
    }

    String elementType() {
      return type;
    }

    IntermediateSchemaFragment fragment() {
      return fragment;
    }

  }

  /**
   * An intermediate format for a fragment of a prologue, containing a sequence
   * of adjacent prologue entries.
   */
  abstract static class IntermediatePrologueFragment {

    /**
     * Performs a partial compile of the top-level of the prologue fragment,
     * creating a list of prologue entries in the fragment which pairs element
     * types with their uninitialized schema patterns (whose full compilation
     * has been deferred).
     *
     * @param state the state used by the compiler
     * @return the list of prologue entries, where the compilation of each
     *         entry's schema pattern has been deferred
     */
    abstract List<PrologueEntry> resolveTopLevel(CompilerState state) throws InvalidSchemaException;

    /**
     * Performs a full compile of the prologue fragment, returning a list of
     * prologue entries representing the content of the prologue fragment.
     *
     * @param state the state used by the compiler
     * @return the list of prologue entries
     */
    abstract List<PrologueEntry> compile(CompilerState state) throws InvalidSchemaException;

  }

  /**
   * A prologue fragment arising from the direct specification of the fragment's
   * content (as opposed to through referencing a definition).
   */
  static final class DirectPrologueFragment extends IntermediatePrologueFragment {

    private final List<IntermediatePrologueEntry> prologueEntries;

    DirectPrologueFragment(List<IntermediatePrologueEntry> prologueEntries) {
      this.prologueEntries = prologueEntries;
    }

    @Override
    List<PrologueEntry> resolveTopLevel(CompilerState state) {
      List<PrologueEntry> resolved = new ArrayList<PrologueEntry>();
      for (IntermediatePrologueEntry entry : prologueEntries) {
        SchemaPattern pattern = new SchemaPattern();
        IntermediateSchemaFragment fragment = entry.fragment();
        resolved.add(new PrologueEntry(entry.elementType(), pattern));
        state.deferredCompiles.add(new DeferredCompile(fragment, pattern));
      }
      return resolved;
    }

    @Override
    List<PrologueEntry> compile(CompilerState state) throws InvalidSchemaException {
      List<PrologueEntry> compiled = new ArrayList<PrologueEntry>();
      for (IntermediatePrologueEntry entry : prologueEntries) {
        compiled.add(new PrologueEntry(entry.type, entry.fragment().compile(state)));
      }
      return compiled;
    }

  }

  /**
   * A prologue fragment arising from referencing a definition.
   */
  static final class ReferencePrologueFragment extends IntermediatePrologueFragment {

    private final String reference;

    ReferencePrologueFragment(String reference) {
      this.reference = reference;
    }

    @Override
    List<PrologueEntry> resolveTopLevel(CompilerState state) throws InvalidSchemaException {
      IntermediateSchemaFragment fragment = state.lookupDefinition(reference);
      return fragment.resolveTopLevel(reference, state).pattern.prologue();
    }

    @Override
    List<PrologueEntry> compile(CompilerState state) {
      return state.references.get(reference).pattern.prologue();
    }

  }

  /**
   * Accumulates attribute patterns, which can then be used to construct a
   * <code>AttributesValidator</code>.
   */
  static final class AttributesPatternBuilder {

    // NOTE: This could be optimized with a trie.
    private final Set<String> requiredAttributes =
        new TreeSet<String>();

    // NOTE: This could be optimized with a trie.
    private final Map<String, String> attributeValuePatterns =
        new TreeMap<String, String>();

    void addRequired(String attributeName) {
      requiredAttributes.add(attributeName);
    }

    void addValuePattern(String attributeName, String values) throws InvalidSchemaException {
      if (attributeValuePatterns.containsKey(attributeName)) {
        throw new InvalidSchemaException(
            "Attribute pattern defined more than once: " + attributeName);
      }
      attributeValuePatterns.put(attributeName, values);
    }

    void importFrom(AttributesPatternBuilder other) throws InvalidSchemaException {
      requiredAttributes.addAll(other.requiredAttributes);
      for (Map.Entry<String, String> entry : other.attributeValuePatterns.entrySet()) {
        String key = entry.getKey();
        if (attributeValuePatterns.containsKey(key)) {
          throw new InvalidSchemaException("Attribute pattern defined more than once: " + key);
        }
        attributeValuePatterns.put(key, entry.getValue());
      }
    }

    AttributesValidator createValidator() {
      // NOTE: This could be optimized with a trie.
      Map<String, ValueValidator> attributeValidators = new TreeMap<String, ValueValidator>();
      for (Map.Entry<String, String> entry : attributeValuePatterns.entrySet()) {
        String name = entry.getKey();
        String values = entry.getValue();
        attributeValidators.put(name, ValueValidator.fromRegex(values));
      }
      return new AttributesValidator(requiredAttributes, attributeValidators);
    }

  }

  /**
   * Accumulates characters, which can then be used to construct a <code>CharacterValidator</code>.
   */
  static final class CharacterPatternBuilder {

    private Set<Integer> characters = new TreeSet<Integer>();

    private boolean blacklistCharacters = false;

    void whitelistCharacters(Collection<Integer> chars) {
      if (blacklistCharacters) {
        characters.removeAll(chars);
      } else {
        characters.addAll(chars);
      }
    }

    void blacklistCharacters(Collection<Integer> chars) {
      Set<Integer> oldCharacters = characters;
      characters = new TreeSet<Integer>();
      if (blacklistCharacters) {
        for (int character : chars) {
          if (oldCharacters.contains(character)) {
            characters.add(character);
          }
        }
      } else {
        for (int character : chars) {
          if (!oldCharacters.contains(character)) {
            characters.add(character);
          }
        }
        blacklistCharacters = true;
      }
    }

    void importFrom(CharacterPatternBuilder other) {
      if (other.blacklistCharacters) {
        blacklistCharacters(other.characters);
      } else {
        whitelistCharacters(other.characters);
      }
    }

    CharacterValidator createValidator() {
      return blacklistCharacters
          ? CharacterValidator.disallowedCharacters(characters)
          : CharacterValidator.allowedCharacters(characters);
    }

  }

  /**
   * A container for the information needed to perform the deferred compilation
   * of <code>IntermediateSchemaFragment</code>.
   */
  private static final class DeferredCompile {

    private final IntermediateSchemaFragment source;
    private final SchemaPattern target;

    DeferredCompile(IntermediateSchemaFragment source, SchemaPattern target) {
      this.source = source;
      this.target = target;
    }

  }

  /**
   * The result of resolving a reference.
   */
  private static final class ReferenceData {

    final SchemaPattern pattern;
    final Map<String, SchemaPattern> potentialChildren;
    final AttributesPatternBuilder attributesPattern;
    final CharacterPatternBuilder characterPattern;

    ReferenceData(SchemaPattern pattern, Map<String, SchemaPattern> potentialChildren,
        AttributesPatternBuilder attributesPattern, CharacterPatternBuilder characterPattern) {
      this.pattern = pattern;
      this.potentialChildren = potentialChildren;
      this.attributesPattern = attributesPattern;
      this.characterPattern = characterPattern;
    }

  }

  /**
   * The general storage container for state needed by the compiler to function.
   */
  private static final class CompilerState {

    final Map<String, IntermediateSchemaFragment> definitions;
    final Map<String, ReferenceData> references = new TreeMap<String, ReferenceData>();
    final Set<String> undergoingResolution = new TreeSet<String>();
    final List<DeferredCompile> deferredCompiles = new ArrayList<DeferredCompile>();

    CompilerState(Map<String, IntermediateSchemaFragment> definitions) {
      this.definitions = definitions;
    }

    IntermediateSchemaFragment lookupDefinition(String name) throws InvalidSchemaException {
      IntermediateSchemaFragment result = definitions.get(name);
      if (result == null) {
        throw new InvalidSchemaException("Reference to undefined pattern definition: " + name);
      }
      return result;
    }

  }

  private final AttributesPatternBuilder attributesPattern;
  private final List<IntermediatePrologueFragment> prologue;
  private final Map<String, IntermediateSchemaFragment> allowedChildren;
  private final Set<String> references;
  private final CharacterPatternBuilder characterPattern;

  IntermediateSchemaFragment(
      AttributesPatternBuilder attributesPattern,
      List<IntermediatePrologueFragment> prologue,
      Map<String, IntermediateSchemaFragment> allowedChildren,
      Set<String> references,
      CharacterPatternBuilder characterPattern) {
    this.attributesPattern = attributesPattern;
    this.prologue = prologue;
    this.allowedChildren = allowedChildren;
    this.references = references;
    this.characterPattern = characterPattern;
  }

  private void importReferences(Map<String, SchemaPattern> allowedChildrenMap, CompilerState state)
      throws InvalidSchemaException {
    for (String ref : references) {
      ReferenceData refData = state.lookupDefinition(ref).resolveTopLevel(ref, state);
      characterPattern.importFrom(refData.characterPattern);
      attributesPattern.importFrom(refData.attributesPattern);
      for (Map.Entry<String, SchemaPattern> entry : refData.potentialChildren.entrySet()) {
        String key = entry.getKey();
        if (allowedChildrenMap.containsKey(key)) {
          throw new InvalidSchemaException(
              "Subpattern defined more than once for element type: " + key);
        }
        allowedChildrenMap.put(key, entry.getValue());
      }
    }
  }

  /**
   * Performs a partial compile of just the top level of a referenced definition
   * (which is represented by this object).
   *
   * @param reference the name of the definition which this object represents
   * @param state state needed by the compiler
   * @return the result of the partial compilation
   */
  private ReferenceData resolveTopLevel(String reference, CompilerState state)
      throws InvalidSchemaException {
    if (state.undergoingResolution.contains(reference)) {
      throw new InvalidSchemaException("Infinite recursion encountered");
    }
    ReferenceData referenceData = state.references.get(reference);
    if (referenceData != null) {
      return referenceData;
    }
    state.undergoingResolution.add(reference);
    /*
     * NOTE: Is there a nicer way than mutating attributesPattern (and the other
     * fields) that is as efficient?
     */
    List<PrologueEntry> prologueEntries = new ArrayList<PrologueEntry>();
    Map<String, SchemaPattern> allowedChildrenMap = new TreeMap<String, SchemaPattern>();
    for (Map.Entry<String, IntermediateSchemaFragment> entry : allowedChildren.entrySet()) {
      String key = entry.getKey();
      SchemaPattern pattern = new SchemaPattern();
      allowedChildrenMap.put(key, pattern);
      state.deferredCompiles.add(new DeferredCompile(entry.getValue(), pattern));
    }
    importReferences(allowedChildrenMap, state);
    for (IntermediatePrologueFragment prologueFragment : prologue) {
      // TODO(user): Concatenating prologues can by slightly optimized here.
      prologueEntries.addAll(prologueFragment.resolveTopLevel(state));
    }
    SchemaPattern compiledPattern = new SchemaPattern();
    compiledPattern.initialize(attributesPattern.createValidator(), new Prologue(prologueEntries),
        allowedChildrenMap, characterPattern.createValidator());
    state.undergoingResolution.remove(reference);
    referenceData = new ReferenceData(compiledPattern, allowedChildrenMap, attributesPattern,
        characterPattern);
    state.references.put(reference, referenceData);
    return referenceData;
  }

  /**
   * Compiles this intermediate fragment into the given uninitialized pattern object.
   *
   * @param pattern an uninitialized pattern object into which to compile this intermediate fragment
   * @param state state needed by the compiler
   */
  private void compileTo(SchemaPattern pattern, CompilerState state) throws InvalidSchemaException {
    Map<String, SchemaPattern> allowedChildrenMap = new TreeMap<String, SchemaPattern>();
    for (Map.Entry<String, IntermediateSchemaFragment> entry : allowedChildren.entrySet()) {
      allowedChildrenMap.put(entry.getKey(), entry.getValue().compile(state));
    }
    importReferences(allowedChildrenMap, state);
    List<PrologueEntry> compiledPrologue = new ArrayList<PrologueEntry>();
    for (IntermediatePrologueFragment prologueFragment : prologue) {
      compiledPrologue.addAll(prologueFragment.compile(state));
    }
    pattern.initialize(attributesPattern.createValidator(), new Prologue(compiledPrologue),
        allowedChildrenMap, characterPattern.createValidator());
  }

  /**
   * Compiles this intermediate fragment into a new pattern object.
   *
   * @param state state needed by the compiler
   * @return a new pattern object
   */
  private SchemaPattern compile(CompilerState state) throws InvalidSchemaException {
    SchemaPattern pattern = new SchemaPattern();
    compileTo(pattern, state);
    return pattern;
  }

  /**
   * Compiles a map associating <code>IntermediateSchemaFragment</code>s with
   * definition names into a <code>SchemaPattern</code>.
   *
   * @param definitions a map from definition names to the intermediate
   *        representation of the pattern defined by the corresponding
   *        definition
   * @param root the root pattern
   * @return the schema pattern
   */
  static SchemaPattern compile(Map<String, IntermediateSchemaFragment> definitions, String root)
      throws InvalidSchemaException {
    CompilerState state = new CompilerState(definitions);
    for (Map.Entry<String, IntermediateSchemaFragment> definition : definitions.entrySet()) {
      definition.getValue().resolveTopLevel(definition.getKey(), state);
    }
    for (DeferredCompile deferredCompile : state.deferredCompiles) {
      deferredCompile.source.compileTo(deferredCompile.target, state);
    }
    ReferenceData rootReference = state.references.get(root);
    if (rootReference == null) {
      throw new InvalidSchemaException("Root not defined");
    }
    return rootReference.pattern;
  }

}
