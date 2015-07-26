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

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.experimental.schema.IntermediateSchemaFragment.AttributesPatternBuilder;
import org.waveprotocol.wave.model.experimental.schema.IntermediateSchemaFragment.CharacterPatternBuilder;
import org.waveprotocol.wave.model.experimental.schema.IntermediateSchemaFragment.DirectPrologueFragment;
import org.waveprotocol.wave.model.experimental.schema.IntermediateSchemaFragment.IntermediatePrologueEntry;
import org.waveprotocol.wave.model.experimental.schema.IntermediateSchemaFragment.IntermediatePrologueFragment;
import org.waveprotocol.wave.model.experimental.schema.IntermediateSchemaFragment.ReferencePrologueFragment;
import org.waveprotocol.wave.model.util.Utf16Util;
import org.waveprotocol.wave.model.util.Utf16Util.CodePointHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A factory for schemas.
 *
 */
public final class SchemaFactory {

  /*
   * The following is a description of how createSchemaPattern() works.
   *
   * The given DocInitialization is traversed from start to finish in linear
   * order. During this process, pushHandler() is called whenever an element
   * start is encountered and popHandler() is called whenever an element end is
   * encountered. There is a handler class for each type of context that can
   * exist in a schema document. In this way, handlers are pushed onto and
   * popped off a stack in a way that reflects the nesting structure of the
   * elements in the schema document.
   *
   * When a handler is being popped off the stack, it may register the
   * information about whatever it has handled through a call to some
   * register*() method of the next element on the stack (which would be the
   * handler for its parent context).
   */

  private static final class BoxedSchemaException extends RuntimeException {

    private final InvalidSchemaException exception;

    BoxedSchemaException(InvalidSchemaException e) {
      exception = e;
    }

    InvalidSchemaException unbox() {
      return exception;
    }

  }

  private abstract static class Handler {
    abstract Handler pushHandler(String type, Attributes attrs) throws InvalidSchemaException;
    abstract Handler popHandler() throws InvalidSchemaException;
  }

  /**
   * A handler for the contents of the root of the schema document.
   */
  private static final class BaseHandler extends Handler {

    private static final String DEFINITION_TYPE_NAME = "definition";
    private static final String ROOT_TYPE_NAME = "root";

    private final Map<String, IntermediateSchemaFragment> definitions =
        new TreeMap<String, IntermediateSchemaFragment>();

    private String root;

    @Override
    Handler pushHandler(String type, Attributes attrs) throws InvalidSchemaException {
      if (type.equals(DEFINITION_TYPE_NAME)) {
        String name = extractName(DEFINITION_TYPE_NAME, attrs);
        return new DefinitionHandler(name, this);
      } else if (type.equals(ROOT_TYPE_NAME)) {
        if (root != null) {
          throw new InvalidSchemaException("More than one declaration of root");
        }
        root = extractName(ROOT_TYPE_NAME, attrs);
        return new EmptyHandler(this);
      }
      throw new InvalidSchemaException("Unrecognized element type: " + type);
    }

    @Override
    Handler popHandler() {
      return null;
    }

    void registerDefinition(String name, IntermediateSchemaFragment pattern)
        throws InvalidSchemaException {
      if (definitions.containsKey(name)) {
        throw new InvalidSchemaException("Duplicate definition for: " + name);
      }
      definitions.put(name, pattern);
    }

  }

  /**
   * A handler for any pattern description that can directly contain a
   * collection of "element" subpatterns.
   */
  private abstract static class ElementCollectionHandler extends Handler {
    abstract void registerElement(String elementType, IntermediateSchemaFragment pattern);
  }

  /**
   * A handler for a pattern description for a definition or element.
   */
  private abstract static class PatternHandler extends ElementCollectionHandler {

    private static final String PROLOGUE_TYPE_NAME = "prologue";
    private static final String ATTRIBUTE_TYPE_NAME = "attribute";
    private static final String ELEMENT_TYPE_NAME = "element";
    private static final String TEXT_TYPE_NAME = "text";
    private static final String REFERENCE_TYPE_NAME = "reference";
    private static final String REQUIRED_ATTRIBUTE_NAME = "required";
    private static final String VALUES_ATTRIBUTE_NAME = "values";
    private static final String TRUE_ATTRIBUTE_VALUE = "true";
    private static final String FALSE_ATTRIBUTE_VALUE = "false";
    private static final String TYPE_ATTRIBUTE_NAME = "type";
    private static final String CHARACTERS_ATTRIBUTE_NAME = "characters";
    private static final String BLACKLIST_ATTRIBUTE_VALUE = "blacklist";
    private static final String WHITELIST_ATTRIBUTE_VALUE = "whitelist";

    private final AttributesPatternBuilder attributesPattern = new AttributesPatternBuilder();

    private final List<IntermediatePrologueFragment> prologueFragments =
        new ArrayList<IntermediatePrologueFragment>();

    // NOTE: This could be optimized with a trie.
    private final Map<String, IntermediateSchemaFragment> freeElements =
        new TreeMap<String, IntermediateSchemaFragment>();

    private final Set<String> references = new TreeSet<String>();

    private final CharacterPatternBuilder characterPattern = new CharacterPatternBuilder();

    @Override
    final Handler pushHandler(String type, Attributes attrs) throws InvalidSchemaException {
      // NOTE: Is it worth using a map here?
      if (type.equals(PROLOGUE_TYPE_NAME)) {
        return new PrologueHandler(this);
      } else if (type.equals(ATTRIBUTE_TYPE_NAME)) {
        handleAttribute(attrs);
        return new EmptyHandler(this);
      } else if (type.equals(ELEMENT_TYPE_NAME)) {
        String name = extractName(ELEMENT_TYPE_NAME, attrs);
        if (freeElements.containsKey(name)) {
          throw new InvalidSchemaException("Element pattern defined more than once: " + name);
        }
        return new ElementHandler(name, this);
      } else if (type.equals(TEXT_TYPE_NAME)) {
        handleText(attrs);
        return new EmptyHandler(this);
      } else if (type.equals(REFERENCE_TYPE_NAME)) {
        handleReference(attrs);
        return new EmptyHandler(this);
      }
      throw new InvalidSchemaException("Unrecognized element type: " + type);
    }

    @Override
    final void registerElement(String elementType, IntermediateSchemaFragment pattern) {
      freeElements.put(elementType, pattern);
    }

    final void registerPrologue(List<IntermediatePrologueEntry> prologueEntries) {
      this.prologueFragments.add(new DirectPrologueFragment(prologueEntries));
    }

    final IntermediateSchemaFragment extractPattern() {
      return new IntermediateSchemaFragment(attributesPattern, prologueFragments, freeElements,
          references, characterPattern);
    }

    final void handleAttribute(Attributes attrs) throws InvalidSchemaException {
      String name = attrs.get(NAME_ATTRIBUTE_NAME);
      String values = attrs.get(VALUES_ATTRIBUTE_NAME);
      String required = attrs.get(REQUIRED_ATTRIBUTE_NAME);
      if (name == null) {
        throw new InvalidSchemaException("Missing attribute: " + NAME_ATTRIBUTE_NAME);
      }
      if (values == null) {
        throw new InvalidSchemaException("Missing attribute: " + VALUES_ATTRIBUTE_NAME);
      }
      if (required == null) {
        throw new InvalidSchemaException("Missing attribute: " + REQUIRED_ATTRIBUTE_NAME);
      }
      if (attrs.size() != 3) {
        throw new InvalidSchemaException("Encountered more attributes than the expected three");
      }
      checkAttributeName(name);
      if (required.equals(TRUE_ATTRIBUTE_VALUE)) {
        attributesPattern.addRequired(name);
      } else if (!required.equals(FALSE_ATTRIBUTE_VALUE)) {
        throw new InvalidSchemaException(
            "Invalid value for attribute " + REQUIRED_ATTRIBUTE_NAME + ": " + required);
      }
      RegularExpressionChecker.checkRegularExpression(values);
      attributesPattern.addValuePattern(name, values);
    }

    final void handleText(Attributes attrs) throws InvalidSchemaException {
      String type = attrs.get(TYPE_ATTRIBUTE_NAME);
      String chars = attrs.get(CHARACTERS_ATTRIBUTE_NAME);
      if (type == null) {
        throw new InvalidSchemaException("Missing attribute: " + TYPE_ATTRIBUTE_NAME);
      }
      if (chars == null) {
        throw new InvalidSchemaException("Missing attribute: " + CHARACTERS_ATTRIBUTE_NAME);
      }
      if (attrs.size() != 2) {
        throw new InvalidSchemaException("Encountered more attributes than the expected two");
      }
      if (type.equals(BLACKLIST_ATTRIBUTE_VALUE)) {
        characterPattern.blacklistCharacters(extractCodePoints(chars));
      } else if (type.equals(WHITELIST_ATTRIBUTE_VALUE)) {
        characterPattern.whitelistCharacters(extractCodePoints(chars));
      } else {
        throw new InvalidSchemaException(
            "Invalid value for attribute " + TYPE_ATTRIBUTE_NAME + ": " + type);
      }
    }

    final void handleReference(Attributes attrs) throws InvalidSchemaException {
      String name = extractName(REFERENCE_TYPE_NAME, attrs);
      prologueFragments.add(new ReferencePrologueFragment(name));
      references.add(name);
    }

  }

  /**
   * A handler for a pattern description for a definition.
   */
  private static final class DefinitionHandler extends PatternHandler {

    private final String name;
    private final BaseHandler previousTop;

    DefinitionHandler(String name, BaseHandler previousTop) {
      this.name = name;
      this.previousTop = previousTop;
    }

    @Override
    Handler popHandler() throws InvalidSchemaException {
      previousTop.registerDefinition(name, extractPattern());
      return previousTop;
    }

  }

  /**
   * A handler for a pattern description for an element.
   */
  private static final class ElementHandler extends PatternHandler {

    private final String elementType;
    private final ElementCollectionHandler previousTop;

    ElementHandler(String elementType, ElementCollectionHandler previousTop)
        throws InvalidSchemaException {
      this.elementType = elementType;
      this.previousTop = previousTop;
      checkElementType(elementType);
    }

    @Override
    Handler popHandler() {
      previousTop.registerElement(elementType, extractPattern());
      return previousTop;
    }

  }

  /**
   * A handler for a pattern description for a prologue.
   */
  private static final class PrologueHandler extends ElementCollectionHandler {

    private static final String ELEMENT_TYPE_NAME = "element";

    private final PatternHandler previousTop;
    private final List<IntermediatePrologueEntry> prologueEntries =
        new ArrayList<IntermediatePrologueEntry>();

    PrologueHandler(PatternHandler previousTop) {
      this.previousTop = previousTop;
    }

    @Override
    final Handler pushHandler(String type, Attributes attrs) throws InvalidSchemaException {
      if (type.equals(ELEMENT_TYPE_NAME)) {
        String name = extractName(ELEMENT_TYPE_NAME, attrs);
        return new ElementHandler(name, this);
      }
      throw new InvalidSchemaException(
          "Element encountered where no child element is allowed: " + type);
    }

    @Override
    Handler popHandler() {
      previousTop.registerPrologue(prologueEntries);
      return previousTop;
    }

    @Override
    void registerElement(String elementType, IntermediateSchemaFragment pattern) {
      prologueEntries.add(new IntermediatePrologueEntry(elementType, pattern));
    }

  }

  /**
   * A handler for any context that should have no child contexts.
   */
  private static final class EmptyHandler extends Handler {

    private final Handler previousTop;

    EmptyHandler(Handler previousTop) {
      this.previousTop = previousTop;
    }

    @Override
    final Handler pushHandler(String type, Attributes attrs) throws InvalidSchemaException {
      throw new InvalidSchemaException("Unrecognized element type: " + type);
    }

    @Override
    Handler popHandler() {
      return previousTop;
    }

  }

  /**
   * A builder that builds up a <code>SchemaPattern</code> when a
   * <code>DocInitialization</code> representing the schema pattern is applied
   * to it.
   */
  private static final class SchemaBuilder implements DocInitializationCursor {

    private final BaseHandler baseHandler = new BaseHandler();

    private Handler handler = baseHandler;

    @Override
    public void characters(String chars) {
      throw new BoxedSchemaException(
          new InvalidSchemaException("Encountered character data in schema: " + chars));
    }

    @Override
    public void elementStart(String type, Attributes attrs) {
      try {
        handler = handler.pushHandler(type, attrs);
      } catch (InvalidSchemaException e) {
        throw new BoxedSchemaException(e);
      }
    }

    @Override
    public void elementEnd() {
      try {
        handler = handler.popHandler();
      } catch (InvalidSchemaException e) {
        throw new BoxedSchemaException(e);
      }
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
      throw new BoxedSchemaException(
          new InvalidSchemaException("Encountered annotation boundary"));
    }

    public SchemaPattern buildSchema() throws InvalidSchemaException {
      if (handler != baseHandler) {
        // This should normally not occur with proper usage of this class.
        throw new InvalidSchemaException("Ill-formed schema");
      }
      if (baseHandler.root == null) {
        throw new InvalidSchemaException("Root not specified");
      }
      return IntermediateSchemaFragment.compile(baseHandler.definitions, baseHandler.root);
    }

  }

  /**
   * A code point extractor.
   */
  private static final CodePointHandler<List<Integer>> codePointExtractor =
      new CodePointHandler<List<Integer>>() {

        List<Integer> codePoints = new ArrayList<Integer>();

        @Override
        public List<Integer> codePoint(int cp) {
          codePoints.add(cp);
          return null;
        }

        @Override
        public List<Integer> endOfString() {
          return codePoints;
        }

        @Override
        public List<Integer> unpairedSurrogate(char c) {
          return ERROR_LIST;
        }

  };

  /**
   * A dummy list used as an error indicator used by <code>codePointExtractor</code>.
   */
  private static final List<Integer> ERROR_LIST = new ArrayList<Integer>() {};

  private static final String NAME_ATTRIBUTE_NAME = "name";

  private SchemaFactory() {}

  private static String extractName(String elementType, Attributes attrs)
      throws InvalidSchemaException {
    String name = attrs.get(NAME_ATTRIBUTE_NAME);
    if (name == null) {
      throw new InvalidSchemaException(
          "Missing attribute \"" + NAME_ATTRIBUTE_NAME + "\" in element: " + elementType);
    }
    if (attrs.size() != 1) {
      throw new InvalidSchemaException(
          "Encountered an attribute other than \"" + NAME_ATTRIBUTE_NAME + "\" in element: "
          + elementType);
    }
    return name;
  }

  /**
   * Extracts code points from a given character string.
   *
   * @param chars a character string from which to extract code points
   * @return the code points extracted from the given string
   */
  private static List<Integer> extractCodePoints(String chars) throws InvalidSchemaException {
    List<Integer> codePoints = Utf16Util.traverseUtf16String(chars, codePointExtractor);
    if (codePoints == ERROR_LIST) {
      throw new InvalidSchemaException("Invalid code point in string: " + chars);
    }
    return codePoints;
  }

  private static void checkElementType(String name) throws InvalidSchemaException {
    if (!Utf16Util.isXmlName(name)) {
      throw new InvalidSchemaException("Invalid element type: " + name);
    }
  }

  private static void checkAttributeName(String name) throws InvalidSchemaException {
    if (!Utf16Util.isXmlName(name)) {
      throw new InvalidSchemaException("Invalid attribute name: " + name);
    }
  }

  /**
   * Creates a <code>SchemaPattern</code> from its representation as a
   * <code>DocInitialization</code>.
   *
   * @param schemaDescription the schema represented as a
   *        <code>DocInitialization</code>
   * @return the constructed <code>SchemaPattern</code>
   * @throws InvalidSchemaException if the given <code>DocInitialization</code>
   *         does not represent a valid schema
   */
  public static SchemaPattern createSchemaPattern(DocInitialization schemaDescription)
      throws InvalidSchemaException {
    SchemaBuilder builder = new SchemaBuilder();
    try {
      schemaDescription.apply(builder);
    } catch (BoxedSchemaException e) {
      throw e.unbox();
    }
    return builder.buildSchema();
  }

}
