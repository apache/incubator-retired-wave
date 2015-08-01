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

package org.waveprotocol.wave.model.document.operation.automaton;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema.PermittedCharacters;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationMap;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationsUpdateImpl;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Utf16Util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A state machine that can be used to accept or generate valid or invalid document
 * operations.
 *
 * The basic usage model is as follows: An automaton is parameterized by a
 * document and a set of constraints based on an XML schema, and will
 * accept/generate all valid operations for that document and those constraints.
 *
 * Every possible operation component (such as "elementStart(...)") corresponds
 * to a potential transition of the automaton.  The checkXXX methods
 * (such as checkElementStart(...)) determine whether a given transition exists
 * and is valid, or whether it is invalid, or ill-formed.  The doXXX methods
 * will perform the transition.  Ill-formed transitions must not be performed.
 * Invalid transitions are permitted, but after performing an invalid transition,
 * the validity of the operation components that follow is not well-defined.
 *
 * The checkFinish() method determines whether ending an operation is acceptable
 * (or whether any opening components are missing the corresponding closing
 * component or similar).
 *
 * The checkXXX methods accept a ViolationCollector object where they will record
 * details about any violations.  If a proposed transition is invalid for more
 * than one reason, the checkXXX method may detect only one (or any subset) of
 * the reasons and record only those violations.  The ViolationCollector parameter
 * may also be null, in which case details about the violations will not be
 * recorded.
 *
 * To validate an operation, the automaton needs to be driven according to
 * the operation components in that operation.  DocOpValidator does
 * this.
 *
 * To generate a random operation, the automaton needs to be driven based on
 * a random document operation component generator.  RandomDocOpGenerator does
 * this.
 */
// TODO: size limits
public final class DocOpAutomaton {

  /**
   * Set this to true when debugging the random generator.
   */
  // Perhaps we should merge each checkXXX() and doXXX() method pair
  // into one XXX method that does the check but then unconditionally
  // performs the transition (or destroys the automaton if the step
  // was ill-formed) and returns the check result.  This would make
  // the random generator less efficient (because it would have to
  // clone all the time), but simplify all other uses.
  private static final boolean EXPENSIVE_ASSERTIONS = false;

  /**
   * The overall result of validating an operation.
   */
  public enum ValidationResult {
    // These need to be ordered most severe to least severe due to the way
    // we implement mergeWith().

    /**
     * The operation is meaningless. One ore more components of this operation
     * have fields with illegal values and/or the sequence of components does
     * not have proper nesting, or is in some way illegal. The result of
     * applying such an operation to any document is undefined.
     */
    ILL_FORMED,

    /**
     * The operation is well-formed, but it does not match the document state it
     * was being checked against. It would be meaningless to attempt to apply
     * the operation to the state being checked against.
     */
    INVALID_DOCUMENT,

    /**
     * The operation is well formed, and if applied to the document being
     * checked against, would have a well defined and well formed result.
     * However, applying it would mean the resulting document would not conform
     * to the document schema being checked against.
     */
    INVALID_SCHEMA,

    /**
     * The operation is valid in every way with respect to the document and
     * schema being checked against.
     */
    VALID;

    /** @see #ILL_FORMED */
    public boolean isIllFormed() {
      return this == ILL_FORMED;
    }
    /** @see #INVALID_DOCUMENT */
    public boolean isInvalidDocument() {
      return this == INVALID_DOCUMENT;
    }
    /** @see #INVALID_SCHEMA */
    public boolean isInvalidSchema() {
      return this == INVALID_SCHEMA;
    }
    /** @see #VALID */
    public boolean isValid() {
      return this == VALID;
    }

    public ValidationResult mergeWith(ValidationResult other) {
      Preconditions.checkNotNull(other, "Null ValidationResult");
      return ValidationResult.values()[Math.min(this.ordinal(), other.ordinal())];
    }
  }

  /**
   * An object containing information about one individual reason why an
   * operation is not valid, e.g. "retain past end" or "deletion inside insertion".
   */
  public abstract static class Violation {
    private final String description;
    private final int originalDocumentPos;
    private final int resultingDocumentPos;
    Violation(String description, int originalPos, int resultingPos) {
      this.description = description;
      this.originalDocumentPos = originalPos;
      this.resultingDocumentPos = resultingPos;
    }
    public abstract ValidationResult validationResult();
    /**
     * @return a developer-readable description of the violation
     */
    public String description() {
      return description + " at original document position " + originalDocumentPos
          + " / resulting document position " + resultingDocumentPos;
    }
  }

  /**
   * An object containing information about the way in which an operation is
   * ill-formed.
   */
  public static final class OperationIllFormed extends Violation {
    public OperationIllFormed(String description, int originalPos, int resultingPos) {
      super(description, originalPos, resultingPos);
    }
    @Override
    public ValidationResult validationResult() { return ValidationResult.ILL_FORMED; }
  }

  /**
   * An object containing information about how an operation is invalid
   * for a reason that does not depend on XML schema constraints.
   */
  public static final class OperationInvalid extends Violation {
    public OperationInvalid(String description, int originalPos, int resultingPos) {
      super(description, originalPos, resultingPos);
    }
    @Override
    public ValidationResult validationResult() { return ValidationResult.INVALID_DOCUMENT; }
  }

  /**
   * An object containing information about how an operation violates XML
   * schema constraints.
   */
  public static final class SchemaViolation extends Violation {
    public SchemaViolation(String description, int originalPos, int resultingPos) {
      super(description, originalPos, resultingPos);
    }
    @Override
    public ValidationResult validationResult() { return ValidationResult.INVALID_SCHEMA; }
  }

  /**
   * A class to hold a set of violations.  The checkXXX methods take this
   * as an input parameter and add violations to it if there are any.
   */
  // I'm not particularly proud of the design of this class.
  public static final class ViolationCollector {
    private final List<OperationIllFormed> operationIllFormed = new ArrayList<OperationIllFormed>();
    private final List<OperationInvalid> operationInvalid = new ArrayList<OperationInvalid>();
    private final List<SchemaViolation> schemaViolations = new ArrayList<SchemaViolation>();
    public void add(OperationIllFormed v) {
      operationIllFormed.add(v);
    }
    public void add(OperationInvalid v) {
      operationInvalid.add(v);
    }
    public void add(SchemaViolation v) {
      schemaViolations.add(v);
    }
    /** True iff at least one violation of the well-formedness constraints was detected. */
    public boolean isIllFormed() {
      return getValidationResult().isIllFormed();
    }
    /**
     * True iff the most severe validation constraint detected was
     * {@link ValidationResult#INVALID_DOCUMENT}
     */
    public boolean isInvalidDocument() {
      return getValidationResult().isInvalidDocument();
    }
    /**
     * True iff the most severe validation constraint detected was
     * {@link ValidationResult#INVALID_SCHEMA}
     */
    public boolean isInvalidSchema() {
      return getValidationResult().isInvalidSchema();
    }
    /** True iff there were no violations. */
    public boolean isValid() {
      return getValidationResult().isValid();
    }
    /** The merged (most severe) validation result */
    public ValidationResult getValidationResult() {
      if (!operationIllFormed.isEmpty()) {
        return ValidationResult.ILL_FORMED;
      } else if (!operationInvalid.isEmpty()) {
        return ValidationResult.INVALID_DOCUMENT;
      } else if (!schemaViolations.isEmpty()) {
        return ValidationResult.INVALID_SCHEMA;
      } else {
        return ValidationResult.VALID;
      }
    }

    /** Returns a description of a single violation, or null if there are none. */
    public String firstDescription() {
      for (OperationIllFormed v : operationIllFormed) {
        return "ill-formed: " + v.description();
      }
      for (OperationInvalid v : operationInvalid) {
        return "invalid operation: " + v.description();
      }
      for (SchemaViolation v : schemaViolations) {
        return "schema violation: " + v.description();
      }
      return null;
    }

    /** Prints descriptions of violations that have been detected. */
    public void printDescriptions(PrintStream out) {
      printDescriptions(out, "");
    }

    /**
     * Prints descriptions of violations that have been detected, prefixing
     * each line of output with the given prefix.
     */
    public void printDescriptions(PrintStream out, String prefix) {
      if (isValid()) {
        out.println(prefix + "no violations");
        return;
      }
      for (OperationIllFormed v : operationIllFormed) {
        out.println(prefix + "ill-formed: " + v.description());
      }
      for (OperationInvalid v : operationInvalid) {
        out.println(prefix + "invalid operation: " + v.description());
      }
      for (SchemaViolation v : schemaViolations) {
        out.println(prefix + "schema violation: " + v.description());
      }
    }

    private int size() {
      return operationIllFormed.size() + operationInvalid.size() + schemaViolations.size();
    }

    @Override
    public String toString() {
      if (size() == 0) {
        return "ViolationCollector[0]";
      }
      StringBuilder b = new StringBuilder();
      b.append("ViolationCollector[" +
          size() + ": " + firstDescription() + "]");
      return b.toString();
    }
  }


  private ValidationResult addViolation(ViolationCollector a, OperationIllFormed v) {
    if (a != null) {
      a.add(v);
    }
    return v.validationResult();
  }

  private ValidationResult addViolation(ViolationCollector a, OperationInvalid v) {
    if (a != null) {
      a.add(v);
    }
    return v.validationResult();
  }

  private ValidationResult addViolation(ViolationCollector a, SchemaViolation v) {
    if (a != null) {
      a.add(v);
    }
    return v.validationResult();
  }

  private OperationIllFormed illFormedOperation(String description) {
    return new OperationIllFormed(description, effectivePos, resultingPos);
  }

  private OperationInvalid invalidOperation(String description) {
    return new OperationInvalid(description, effectivePos, resultingPos);
  }

  private SchemaViolation schemaViolation(String description) {
    return new SchemaViolation(description, effectivePos, resultingPos);
  }

  private ValidationResult valid() {
    return ValidationResult.VALID;
  }

  private ValidationResult mismatchedInsertStart(ViolationCollector v) {
    return addViolation(v, illFormedOperation("elementStart with no matching elementEnd"));
  }

  private ValidationResult mismatchedDeleteStart(ViolationCollector v) {
    return addViolation(v, illFormedOperation(
        "deleteElementStart with no matching deleteElementEnd"));
  }

  private ValidationResult mismatchedInsertEnd(ViolationCollector v) {
    return addViolation(v, illFormedOperation("elementEnd with no matching elementStart"));
  }

  private ValidationResult mismatchedDeleteEnd(ViolationCollector v) {
    return addViolation(v, illFormedOperation(
        "deleteElementEnd with no matching deleteElementStart"));
  }

  private ValidationResult mismatchedStartAnnotation(ViolationCollector v, String key) {
    return addViolation(v, illFormedOperation("annotation of key " + key
        + " starts but never ends"));
  }

  private ValidationResult mismatchedEndAnnotation(ViolationCollector v, String key) {
    return addViolation(v, illFormedOperation("annotation of key " + key
        + " ends without having started"));
  }

  private ValidationResult retainItemCountNotPositive(ViolationCollector v) {
    return addViolation(v, illFormedOperation("retain item count not positive"));
  }

  private ValidationResult retainInsideInsertOrDelete(ViolationCollector v) {
    return addViolation(v, illFormedOperation("retain inside insert or delete"));
  }

  private ValidationResult attributeChangeInsideInsertOrDelete(ViolationCollector v) {
    return addViolation(v, illFormedOperation("attribute change inside insert or delete"));
  }

  private ValidationResult retainPastEnd(ViolationCollector v, int expectedLength,
      int retainItemCount) {
    return addViolation(v, invalidOperation("retain past end of document, document length "
        + expectedLength + ", retain item count " + retainItemCount));
  }

  private ValidationResult missingRetainToEnd(ViolationCollector v,
      int expectedLength, int actualLength) {
    return addViolation(v, invalidOperation("operation shorter than document, document length "
        + expectedLength + ", length of input of operation " + actualLength));
  }

  private ValidationResult nullCharacters(ViolationCollector v) {
    return addViolation(v, illFormedOperation("characters is null"));
  }

  private ValidationResult emptyCharacters(ViolationCollector v) {
    return addViolation(v, illFormedOperation("characters is empty"));
  }

  private ValidationResult insertInsideDelete(ViolationCollector v) {
    return addViolation(v, illFormedOperation("insertion inside deletion"));
  }

  private ValidationResult deleteInsideInsert(ViolationCollector v) {
    return addViolation(v, illFormedOperation("deletion inside insertion"));
  }

  private ValidationResult nullTag(ViolationCollector v) {
    return addViolation(v, illFormedOperation("element type is null"));
  }

  private ValidationResult nullAttributes(ViolationCollector v) {
    return addViolation(v, illFormedOperation("attributes is null"));
  }

  private ValidationResult nullAttributeKey(ViolationCollector v) {
    return addViolation(v, illFormedOperation("attribute key is null"));
  }

  private ValidationResult nullAttributeValue(ViolationCollector v) {
    return addViolation(v, illFormedOperation("attribute value is null"));
  }

  private ValidationResult nullAttributesUpdate(ViolationCollector v) {
    return addViolation(v, illFormedOperation("attributes update is null"));
  }

  private ValidationResult attributeKeysNotStrictlyMonotonic(ViolationCollector v,
      String key1, String key2) {
    return addViolation(v, illFormedOperation("attribute keys not strictly monotonic: "
        + key1 + " >= " + key2));
  }

  private ValidationResult annotationKeysNotStrictlyMonotonic(ViolationCollector v,
      String key1, String key2) {
    return addViolation(v, illFormedOperation("annotation keys not strictly monotonic: "
        + key1 + " >= " + key2));
  }

  private ValidationResult nullAnnotationKey(ViolationCollector v) {
    return addViolation(v, illFormedOperation("annotation key is null"));
  }

  private ValidationResult invalidCharacterInAnnotationKey(ViolationCollector v, String key) {
    return addViolation(v, illFormedOperation("invalid character in annotation key: " + key));
  }

  private ValidationResult annotationKeyNotValidUtf16(ViolationCollector v) {
    return addViolation(v, illFormedOperation("annotation key is not valid UTF-16"));
  }

  private ValidationResult annotationValueNotValidUtf16(ViolationCollector v) {
    return addViolation(v, illFormedOperation("annotation value is not valid UTF-16"));
  }

  private ValidationResult charactersContainsSurrogate(ViolationCollector v) {
    return addViolation(v, illFormedOperation("characters component contains surrogate"));
  }

  private ValidationResult deleteCharactersContainsSurrogate(ViolationCollector v) {
    return addViolation(v, illFormedOperation("delete characters component contains surrogate"));
  }

  private ValidationResult charactersInvalidUnicode(ViolationCollector v) {
    return addViolation(v, illFormedOperation("characters component contains invalid unicode"));
  }

  private ValidationResult deleteCharactersInvalidUnicode(ViolationCollector v) {
    return addViolation(v, illFormedOperation("delete characters component contains invalid unicode"));
  }

  private ValidationResult attributeNameNotXmlName(ViolationCollector v, String name) {
    return addViolation(v, illFormedOperation("attribute name is not an XML Name: \""
        + name + "\""));
  }

  private ValidationResult attributeValueNotValidUtf16(ViolationCollector v) {
    return addViolation(v, illFormedOperation("attribute value is not valid UTF-16"));
  }

  private ValidationResult elementTypeNotXmlName(ViolationCollector v, String name) {
    return addViolation(v, illFormedOperation("element type is not an XML Name: \""
        + name + "\""));
  }

  private ValidationResult duplicateAnnotationKey(ViolationCollector v, String key) {
    return addViolation(v, illFormedOperation("annotation boundary contains duplicate key "
        + key));
  }

  private ValidationResult adjacentAnnotationBoundaries(ViolationCollector v) {
    return addViolation(v, illFormedOperation("adjacent annotation boundaries"));
  }

  private ValidationResult textNotAllowedInElement(ViolationCollector v, String tag) {
    return addViolation(v, schemaViolation("element type " + tag
        + " does not allow text content"));
  }

  private ValidationResult onlyBlipTextAllowedInElement(ViolationCollector v, String tag) {
    return addViolation(v, schemaViolation("element type " + tag
        + " only allows blip text content, not arbitrary characters"));
  }

  private ValidationResult cannotDeleteSoManyCharacters(ViolationCollector v,
      int available, String chars) {
    int attempted = chars.length();
    return addViolation(v, invalidOperation("cannot delete " + attempted + " characters,"
        + " only " + available + " available"));
  }

  private ValidationResult invalidAttribute(ViolationCollector v, String type, String attr,
      String value) {
    return addViolation(v, schemaViolation("type " + type + " does not permit attribute "
        + attr + " with value " + value));
  }

  private ValidationResult invalidChild(ViolationCollector v, String parentTag, String childTag) {
    if (parentTag == null) {
      return addViolation(v, schemaViolation("element type " + childTag
          + " not permitted at top level"));
    } else {
      return addViolation(v, schemaViolation("element type " + parentTag
          + " does not permit subelement type " + childTag));
    }
  }

  private ValidationResult differentElementTypeRequired(ViolationCollector v, String expectedType,
      String actualType) {
    return addViolation(v, schemaViolation("element of type " + expectedType
        + " required, not " + actualType));
  }

  private ValidationResult childElementRequired(ViolationCollector v, String expectedType) {
    return addViolation(v, schemaViolation("child element required, expected type "
        + expectedType));
  }

  private ValidationResult attemptToDeleteRequiredChild(ViolationCollector v) {
    return addViolation(v, schemaViolation("attempt to delete required child"));
  }

  private ValidationResult attemptToInsertBeforeRequiredChild(ViolationCollector v) {
    return addViolation(v, schemaViolation("attempt to insert before required child"));
  }

  private ValidationResult noElementStartToDelete(ViolationCollector v) {
    return addViolation(v, invalidOperation("no element start to delete here"));
  }

  private ValidationResult noElementEndToDelete(ViolationCollector v) {
    return addViolation(v, invalidOperation("no element end to delete here"));
  }

  private ValidationResult noElementStartToChangeAttributes(ViolationCollector v) {
    return addViolation(v, invalidOperation("no element start to change attributes here"));
  }

  private ValidationResult oldAnnotationsDifferFromDocument(ViolationCollector v,
      String key, String oldValue, String valueInDoc) {
    return addViolation(v, invalidOperation("old annotations differ from document: "
        + "purported old value for key " + key + " is " + oldValue
        + ", actual value in document is " + valueInDoc));
  }

  private ValidationResult newAnnotationsIncorrectForDeletion(ViolationCollector v) {
    return addViolation(v, invalidOperation("new annotation value incorrect for deletion"));
  }

  private ValidationResult oldTagDifferFromDocument(ViolationCollector v) {
    return addViolation(v, invalidOperation("old element type differs from document"));
  }

  private ValidationResult oldAttributesDifferFromDocument(ViolationCollector v) {
    return addViolation(v, invalidOperation("old attributes differ from document"));
  }

  private ValidationResult missingAnnotationForDeletion(ViolationCollector v, String key,
      String valueInDoc, String requiredValue) {
    return addViolation(v, invalidOperation("deletion does not reset value for key "
        + key + " from " + valueInDoc + " to " + requiredValue));
  }

  private ValidationResult oldCharacterDiffersFromDocument(ViolationCollector v,
      char expected, char actual) {
    return addViolation(v, invalidOperation("attempt to delete character " + actual
        + " when the actual character is " + expected));
  }

  private enum DocSymbol { CHARACTER, OPEN, CLOSE, END }

  private static class InsertStart {
    final String tag;

    InsertStart(String tag) {
      this.tag = tag;
    }

    static InsertStart getInstance(String tag) {
      assert tag != null;
      return new InsertStart(tag);
    }

    ValidationResult notClosed(DocOpAutomaton a, ViolationCollector v) {
      return a.mismatchedInsertStart(v);
    }
  }

  private String elementStartingHere() {
    Preconditions.checkPositionIndex(effectivePos, doc.length());
    return doc.elementStartingAt(effectivePos);
  }

  private String elementEndingNext() {
    Preconditions.checkPositionIndex(effectivePos, doc.length());
    return doc.elementEndingAt(effectivePos);
  }

  // tag==null means text allowed at top level
  private PermittedCharacters permittedCharacters(String type) {
    return constraints.permittedCharacters(type);
  }

  private boolean elementAllowsAttribute(String type, String attributeName, String attributeValue) {
    return constraints.permitsAttribute(type, attributeName, attributeValue);
  }

  // parentType==null means childType allowed at top level
  private boolean elementAllowsChild(String parentType, String childType) {
    return constraints.permitsChild(parentType, childType);
  }

  // returns either null or the type of the first required child
  private String requiredFirstChild(String parentType) {
    List<String> list = constraints.getRequiredInitialChildren(parentType);
    if (list.isEmpty()) {
      return null;
    } else if (list.size() > 1) {
      throw new UnsupportedOperationException("Schema requires multiple initial children");
    } else {
      return list.get(0);
    }
  }


  private final AutomatonDocument doc;
  private final DocumentSchema constraints;


  // current state

  private int effectivePos = 0;
  // first item is bottom of stack, last is top

  private final ArrayList<InsertStart> insertionStack;
  private String nextRequiredElement = null;
  private int deletionStackDepth = 0;
  private AnnotationsUpdateImpl annotationsUpdate = new AnnotationsUpdateImpl();
  private boolean afterAnnotationBoundary = false;
  // This can become null if the operation is invalid.
  private AnnotationMap targetAnnotationsForDeletion = EMPTY_ANNOTATIONS;


  // more state to track just to be able to produce better diagnostic messages

  private int resultingPos = 0;


  public static final AutomatonDocument EMPTY_DOCUMENT = new AutomatonDocument() {
    @Override
    public AnnotationMap annotationsAt(int pos) {
      return AnnotationMapImpl.EMPTY_MAP;
    }

    @Override
    public Attributes attributesAt(int pos) {
      return null;
    }

    @Override
    public int charAt(int pos) {
      return -1;
    }

    @Override
    public String elementEndingAt(int pos) {
      return null;
    }

    @Override
    public String elementStartingAt(int pos) {
      return null;
    }

    @Override
    public int length() {
      return 0;
    }

    @Override
    public String nthEnclosingElementTag(int insertionPoint, int depth) {
      return null;
    }

    @Override
    public int remainingCharactersInElement(int insertionPoint) {
      return 0;
    }

    @Override
    public String getAnnotation(int pos, String key) {
      return null;
    }

    @Override
    public int firstAnnotationChange(int start, int end, String key, String fromValue) {
      Preconditions.checkPositionIndexes(start, end, 0);
      // if (fromValue != null && end > start): can't happen since end == start == 0
      return -1;
    }
  };

  /**
   * Creates an automaton that corresponds to the set of all possible operations
   * on the given document under the given schema constraints.
   */
  public DocOpAutomaton(AutomatonDocument doc, DocumentSchema constraints) {
    this.doc = doc;
    this.constraints = constraints;
    this.nextRequiredElement = requiredFirstChild(null);
    this.insertionStack = new ArrayList<InsertStart>();
  }


  /**
   * Copy Constructor
   */
  public DocOpAutomaton(DocOpAutomaton other) {
    this(other, other.constraints);
  }

  /**
   * Copy Constructor 2
   */
  public DocOpAutomaton(DocOpAutomaton other, DocumentSchema constraints) {
    this.afterAnnotationBoundary = other.afterAnnotationBoundary;
    this.annotationsUpdate = other.annotationsUpdate;
    this.constraints = constraints;
    this.deletionStackDepth = other.deletionStackDepth;
    this.doc = other.doc;
    this.effectivePos = other.effectivePos;
    this.insertionStack = new ArrayList<InsertStart>(other.insertionStack);
    this.nextRequiredElement = other.nextRequiredElement;
    this.resultingPos = other.resultingPos;
    this.targetAnnotationsForDeletion = other.targetAnnotationsForDeletion;
  }

  // current state primitive readers

  private DocSymbol effectiveDocSymbol() {
    if (effectivePos >= doc.length()) {
      return DocSymbol.END;
    }
    {
      String s = elementStartingHere();
      if (s != null) {
        return DocSymbol.OPEN;
      }
    }
    {
      String s = elementEndingNext();
      if (s != null) {
        return DocSymbol.CLOSE;
      }
    }
    return DocSymbol.CHARACTER;
  }

  // only defined for open and close
  private String effectiveDocSymbolTag() {
    switch (effectiveDocSymbol()) {
      case OPEN: {
        String tag = elementStartingHere();
        assert tag != null;
        return tag;
      }
      case CLOSE: {
        String tag = elementEndingNext();
        assert tag != null;
        return tag;
      }
      default:
        throw new IllegalStateException("not at element start or end");
    }
  }

  // only defined for open
  private Attributes effectiveDocSymbolAttributes() {
    switch (effectiveDocSymbol()) {
      case OPEN: {
        Attributes attributes = doc.attributesAt(effectivePos);
        assert attributes != null;
        return attributes;
      }
      default:
        throw new IllegalStateException("not at element start");
    }
  }

  private boolean insertionStackIsEmpty() {
    return insertionStack.isEmpty();
  }

  private boolean deletionStackIsEmpty() {
    return deletionStackDepth == 0;
  }

  // null if at top level
  private String effectiveEnclosingElementTag() {
    // This procedure will find the element at depth == 0.
    int depth = 0;
    for (int i = insertionStack.size() - 1; i >= 0; i--) {
      InsertStart e = insertionStack.get(i);
      if (depth == 0) { return e.tag; }
      depth--;
    }
    if (effectivePos > doc.length()) { return null; }
    return doc.nthEnclosingElementTag(effectivePos, depth);
  }

  /**
   * Returns the maximum permitted retain item count, assuming that a retain
   * component is valid.
   */
  public int maxRetainItemCount() {
    if (effectivePos >= doc.length()) {
      return 0;
    } else {
      return doc.length() - effectivePos;
    }
  }

  public String currentElementStartTag() {
    return doc.elementStartingAt(effectivePos);
  }

  public Attributes currentElementStartAttributes() {
    return doc.attributesAt(effectivePos);
  }

  public AnnotationMap currentAnnotations() {
    if (effectivePos >= doc.length()) {
      return EMPTY_ANNOTATIONS;
    } else {
      return doc.annotationsAt(effectivePos);
    }
  }

  public int nextChar(int offset) {
    Preconditions.checkArgument(offset >= 0, "Offset must be positive");
    if (offset >= doc.length() - effectivePos) { return -1; }
    return doc.charAt(effectivePos + offset);
  }

  /**
   * Non-negative.  0 means neutral.  Larger values mean more complexity.
   */
  public int insertionStackComplexityMeasure() {
    return insertionStack.size();
  }

  /**
   * Non-negative.  0 means neutral.  Larger values mean more complexity.
   */
  public int deletionStackComplexityMeasure() {
    return deletionStackDepth;
  }

  public Set<String> openAnnotations() {
    HashSet<String> r = new HashSet<String>();
    for (int i = 0; i < annotationsUpdate.changeSize(); i++) {
      r.add(annotationsUpdate.getChangeKey(i));
    }
    return r;
  }

  private boolean canRetain(int itemCount) {
    assert itemCount >= 0;
    return itemCount <= maxRetainItemCount();
  }

  /**
   * If a deleteCharacters operation component is permitted as the next
   * component, returns the maximum number of characters that it can delete.
   * Otherwise, the return value is undefined.
   */
  public int maxCharactersToDelete() {
    if (effectivePos >= doc.length()) {
      return 0;
    }
    return doc.remainingCharactersInElement(effectivePos);
  }


  // current state manipulators

  private void advance(int distance) {
    // we're not asserting canIncreaseLength() or similar here, since
    // the driver may be generating an invalid op deliberately.
    assert distance >= 0;
    effectivePos += distance;
  }

  private void insertionStackPush(InsertStart e) {
    insertionStack.add(e);
  }

  private void deletionStackPush() {
    deletionStackDepth++;
  }

  private void insertionStackPop() {
    assert !insertionStack.isEmpty();
    insertionStack.remove(insertionStack.size() - 1);
  }

  private void deletionStackPop() {
    assert !deletionStackIsEmpty();
    deletionStackDepth--;
  }


  private static boolean equal(Object a, Object b) {
    return a == null ? b == null : a.equals(b);
  }


  // check/do methods

  private ValidationResult checkAnnotationsForRetain(ViolationCollector v, int itemCount) {
    for (int i = 0; i < annotationsUpdate.changeSize(); i++) {
      String key = annotationsUpdate.getChangeKey(i);
      String oldValue = annotationsUpdate.getOldValue(i);
      int firstChange = doc.firstAnnotationChange(effectivePos, effectivePos + itemCount,
          key, oldValue);
      if (firstChange != -1) {
        return oldAnnotationsDifferFromDocument(v, key, oldValue,
            doc.getAnnotation(firstChange, key));
      }
    }
    return valid();
  }

  public ValidationResult checkRetain(int itemCount, ViolationCollector v) {
    // well-formedness
    if (itemCount <= 0) { return retainItemCountNotPositive(v); }
    if (!insertionStackIsEmpty()) { return retainInsideInsertOrDelete(v); }
    if (!deletionStackIsEmpty()) { return retainInsideInsertOrDelete(v); }
    // validity
    if (!canRetain(itemCount)) { return retainPastEnd(v, doc.length(), itemCount); }
    return checkAnnotationsForRetain(v, itemCount);
  }

  public void doRetain(int itemCount) {
    if (EXPENSIVE_ASSERTIONS) {
      assert checkRetain(itemCount, null) != ValidationResult.ILL_FORMED;
    }
    advance(itemCount);
    updateDeletionTargetAnnotations();
    resultingPos += itemCount;
    afterAnnotationBoundary = false;
  }


  private ValidationResult validateAnnotationKey(String key, ViolationCollector v) {
    if (key == null) { return nullAnnotationKey(v); }
    if (key.contains("?") || key.contains("@")) { return invalidCharacterInAnnotationKey(v, key); }
    if (!Utf16Util.isValidUtf16(key)) { return annotationKeyNotValidUtf16(v); }
    return ValidationResult.VALID;
  }

  private ValidationResult validateAnnotationValue(String value, ViolationCollector v) {
    if (value == null) { return ValidationResult.VALID; }
    if (!Utf16Util.isValidUtf16(value)) { return annotationValueNotValidUtf16(v); }
    return ValidationResult.VALID;
  }


  public ValidationResult checkAnnotationBoundary(AnnotationBoundaryMap map,
      ViolationCollector v) {
    // well-formedness
    if (afterAnnotationBoundary) { return adjacentAnnotationBoundaries(v); }
    HashSet<String> endKeys = new HashSet<String>();
    for (int i = 0; i < map.endSize(); i++) {
      String key = map.getEndKey(i);
      {
        ValidationResult r = validateAnnotationKey(key, v);
        if (!r.isValid()) { return r; }
      }
      if (i > 0 && map.getEndKey(i - 1).compareTo(key) >= 0) {
        return annotationKeysNotStrictlyMonotonic(v, map.getEndKey(i - 1), key);
      }
      if (!annotationsUpdate.containsKey(key)) { return mismatchedEndAnnotation(v, key); }
      endKeys.add(key);
    }
    for (int i = 0; i < map.changeSize(); i++) {
      String key = map.getChangeKey(i);
      {
        ValidationResult r = validateAnnotationKey(key, v);
        if (!r.isValid()) { return r; }
      }
      {
        ValidationResult r = validateAnnotationValue(map.getOldValue(i), v);
        if (!r.isValid()) { return r; }
      }
      {
        ValidationResult r = validateAnnotationValue(map.getNewValue(i), v);
        if (!r.isValid()) { return r; }
      }
      if (i > 0 && map.getChangeKey(i - 1).compareTo(key) >= 0) {
        return annotationKeysNotStrictlyMonotonic(v, map.getChangeKey(i - 1), key);
      }
      if (endKeys.contains(key)) { return duplicateAnnotationKey(v, key); }
    }
    return valid();
  }

  public void doAnnotationBoundary(AnnotationBoundaryMap map) {
    if (EXPENSIVE_ASSERTIONS) {
      assert !checkAnnotationBoundary(map, null).isIllFormed();
    }
    annotationsUpdate = annotationsUpdate.composeWith(map);
    afterAnnotationBoundary = true;
  }


  private static final AnnotationMap EMPTY_ANNOTATIONS = AnnotationMapImpl.EMPTY_MAP;

  public AnnotationMap inheritedAnnotations() {
    if (effectivePos == 0 || effectivePos > doc.length()) {
      return EMPTY_ANNOTATIONS;
    } else {
      int posToInheritFrom = effectivePos - 1;
      return doc.annotationsAt(posToInheritFrom);
    }
  }

  private void updateDeletionTargetAnnotations() {
    if (effectivePos > doc.length()) {
      targetAnnotationsForDeletion = null;
    } else {
      targetAnnotationsForDeletion =
          inheritedAnnotations().updateWithNoCompatibilityCheck(annotationsUpdate);
    }
  }

  private ValidationResult checkAnnotationsForInsertion(ViolationCollector v) {
    if (effectivePos > doc.length()) {
      // Invalid operation, nothing to check.
      return valid();
    }
    int posToInheritFrom = effectivePos - 1;
    for (int i = 0; i < annotationsUpdate.changeSize(); i++) {
      String key = annotationsUpdate.getChangeKey(i);
      String oldValue = annotationsUpdate.getOldValue(i);
      String defaultFromDocument = posToInheritFrom == -1 ? null :
        doc.getAnnotation(posToInheritFrom, key);
      if (!equal(oldValue, defaultFromDocument)) {
        return oldAnnotationsDifferFromDocument(v, key, oldValue, defaultFromDocument);
      }
    }
    return valid();
  }

  private ValidationResult checkForInsertionBeforeRequiredChild(ViolationCollector v) {
    if (effectivePos < doc.length() && insertionStackIsEmpty()) {
      String parentType = doc.nthEnclosingElementTag(effectivePos, 0);
      String requiredFirstChild = requiredFirstChild(parentType);
      boolean isFirstChild = effectivePos == 0 || doc.elementStartingAt(effectivePos - 1) != null;
      if (isFirstChild && requiredFirstChild != null) {
        return attemptToInsertBeforeRequiredChild(v);
      }
    }
    return ValidationResult.VALID;
  }

  public ValidationResult checkCharacters(String chars, ViolationCollector v) {
    // well-formedness
    if (chars == null) { return nullCharacters(v); }
    if (chars.isEmpty()) { return emptyCharacters(v); }
    if (Utf16Util.firstSurrogate(chars) != -1) { return charactersContainsSurrogate(v); }
    if (!Utf16Util.isValidUtf16(chars)) { return charactersInvalidUnicode(v); }
    if (!deletionStackIsEmpty()) { return insertInsideDelete(v); }
    // validity
    {
      ValidationResult r = checkAnnotationsForInsertion(v);
      if (!r.isValid()) { return r; }
    }
    // schema
    if (nextRequiredElement != null) {
      return childElementRequired(v, nextRequiredElement);
    }
    {
      ValidationResult r = checkForInsertionBeforeRequiredChild(v);
      if (!r.isValid()) { return r; }
    }
    String enclosingTag = effectiveEnclosingElementTag();
    switch (permittedCharacters(enclosingTag)) {
      case NONE:
        return textNotAllowedInElement(v, enclosingTag);
      case BLIP_TEXT:
        if (!Utf16Util.isGoodUtf16ForBlip(chars)) {
          return onlyBlipTextAllowedInElement(v, enclosingTag);
        }
        break;
      case ANY:
        break;
      default:
        throw new AssertionError("unexpected return value from permittedCharacters()");
    }
    return valid();
  }

  public void doCharacters(String characters) {
    if (EXPENSIVE_ASSERTIONS) {
      assert checkCharacters(characters, null) != ValidationResult.ILL_FORMED;
    }
    updateDeletionTargetAnnotations();
    resultingPos += characters.length();
    afterAnnotationBoundary = false;
  }


  private ValidationResult checkAnnotationsForDeletion(ViolationCollector v, int itemCount) {
    if (targetAnnotationsForDeletion == null) {
      // Invalid operation, nothing to check.
      return valid();
    }

    // Check that all annotations contained in the update have correct old and
    // new values.
    for (int i = 0; i < annotationsUpdate.changeSize(); i++) {
      String key = annotationsUpdate.getChangeKey(i);
      String oldValue = annotationsUpdate.getOldValue(i);
      int firstChange = doc.firstAnnotationChange(effectivePos, effectivePos + itemCount,
          key, oldValue);
      if (firstChange != -1) {
        return oldAnnotationsDifferFromDocument(v, key, oldValue,
            doc.getAnnotation(firstChange, key));
      }
      String newValue = annotationsUpdate.getNewValue(i);
      if (!equal(newValue, targetAnnotationsForDeletion.get(key))) {
        return newAnnotationsIncorrectForDeletion(v);
      }
    }
    // TODO: Find a way to speed this up.
    for (int offset = 0; offset < itemCount; offset++) {
      int pos = effectivePos + offset;
      Map<String, String> annotationsHere = doc.annotationsAt(pos);
      // Check that the update contains all values that need to be set; the set of
      // keys to check is the union of keys at the current position and at the
      // position that it would inherit from.
      for (String key : annotationsHere.keySet()) {
        String valueInDoc = annotationsHere.get(key);
        String requiredValue = targetAnnotationsForDeletion.get(key);
        if (!equal(valueInDoc, requiredValue)) {
          if (!annotationsUpdate.containsKey(key)) {
            return missingAnnotationForDeletion(v, key, valueInDoc, requiredValue);
          }
        }
      }
      for (String key : targetAnnotationsForDeletion.keySet()) {
        String valueInDoc = annotationsHere.get(key);
        String requiredValue = targetAnnotationsForDeletion.get(key);
        if (!equal(valueInDoc, requiredValue)) {
          if (!annotationsUpdate.containsKey(key)) {
            return missingAnnotationForDeletion(v, key, valueInDoc, requiredValue);
          }
        }
      }
    }
    return valid();
  }


  private ValidationResult checkAttributesWellFormed(Attributes attr, ViolationCollector v) {
    if (attr == null) { return nullAttributes(v); }
    String previousKey = null;
    for (Map.Entry<String, String> e : attr.entrySet()) {
      if (e.getKey() == null) { return nullAttributeKey(v); }
      if (!Utf16Util.isXmlName(e.getKey())) { return attributeNameNotXmlName(v, e.getKey()); }
      if (e.getValue() == null) { return nullAttributeValue(v); }
      if (!Utf16Util.isValidUtf16(e.getValue())) { return attributeValueNotValidUtf16(v); }
      if (previousKey != null && previousKey.compareTo(e.getKey()) >= 0) {
        return attributeKeysNotStrictlyMonotonic(v, previousKey, e.getKey());
      }
      previousKey = e.getKey();
    }
    return ValidationResult.VALID;
  }

  private ValidationResult checkAttributesUpdateWellFormed(AttributesUpdate u,
      ViolationCollector v) {
    if (u == null) { return nullAttributesUpdate(v); }
    String previousKey = null;
    for (int i = 0; i < u.changeSize(); i++) {
      String key = u.getChangeKey(i);
      if (key == null) { return nullAttributeKey(v); }
      if (!Utf16Util.isXmlName(key)) { return attributeNameNotXmlName(v, key); }
      if (previousKey != null && previousKey.compareTo(key) >= 0) {
        return attributeKeysNotStrictlyMonotonic(v, previousKey, key);
      }
      if (u.getOldValue(i) != null && !Utf16Util.isValidUtf16(u.getOldValue(i))) {
        return attributeValueNotValidUtf16(v);
      }
      if (u.getNewValue(i) != null && !Utf16Util.isValidUtf16(u.getNewValue(i))) {
        return attributeValueNotValidUtf16(v);
      }
      previousKey = key;
    }
    return ValidationResult.VALID;
  }

  private ValidationResult validateAttributes(String tag, Attributes attr, ViolationCollector v) {
    for (Map.Entry<String, String> e : attr.entrySet()) {
      String key = e.getKey();
      String value = e.getValue();
      if (!elementAllowsAttribute(tag, key, value)) {
        return invalidAttribute(v, tag, key, value);
      }
    }
    return ValidationResult.VALID;
  }


  public ValidationResult checkElementStart(String type, Attributes attr, ViolationCollector v) {
    // well-formedness
    if (type == null) { return nullTag(v); }
    if (!Utf16Util.isXmlName(type)) { return elementTypeNotXmlName(v, type); }
    {
      ValidationResult r = checkAttributesWellFormed(attr, v);
      if (r != ValidationResult.VALID) { return r; }
    }
    if (!deletionStackIsEmpty()) { return insertInsideDelete(v); }

    // validity
    {
      ValidationResult r = checkAnnotationsForInsertion(v);
      if (!r.isValid()) { return r; }
    }

    // schema
    {
      ValidationResult r = validateAttributes(type, attr, v);
      if (r != ValidationResult.VALID) { return r; }
    }
    String parentTag = effectiveEnclosingElementTag();
    if (!elementAllowsChild(parentTag, type)) { return invalidChild(v, parentTag, type); }
    {
      ValidationResult r = checkForInsertionBeforeRequiredChild(v);
      if (!r.isValid()) { return r; }
    }
    if (nextRequiredElement != null && !nextRequiredElement.equals(type)) {
      return differentElementTypeRequired(v, nextRequiredElement, type);
    }
    return valid();
  }

  public void doElementStart(String type, Attributes attr) {
    if (EXPENSIVE_ASSERTIONS) {
      assert !checkElementStart(type, attr, null).isIllFormed();
    }
    updateDeletionTargetAnnotations();
    insertionStackPush(InsertStart.getInstance(type));
    nextRequiredElement = requiredFirstChild(type);
    resultingPos += 1;
    afterAnnotationBoundary = false;
   }


  public ValidationResult checkElementEnd(ViolationCollector v) {
    // well-formedness
    if (!deletionStackIsEmpty()) { return insertInsideDelete(v); }
    if (insertionStackIsEmpty()) { return mismatchedInsertEnd(v); }
    // validity
    {
      ValidationResult r = checkAnnotationsForInsertion(v);
      if (!r.isValid()) { return r; }
    }
    // schema
    if (nextRequiredElement != null) {
      return childElementRequired(v, nextRequiredElement);
    }
    return valid();
  }

  public void doElementEnd() {
    if (EXPENSIVE_ASSERTIONS) {
      assert !checkElementEnd(null).isIllFormed();
    }
    updateDeletionTargetAnnotations();
    insertionStackPop();
    resultingPos += 1;
    afterAnnotationBoundary = false;
  }


  private boolean attributesEqual(Attributes a, Attributes b) {
    if (a.size() != b.size()) { return false; }
    for (Map.Entry<String, String> ae : a.entrySet()) {
      if (!equal(ae.getValue(), b.get(ae.getKey()))) { return false; }
    }
    return true;
  }


  public ValidationResult checkDeleteCharacters(String chars, ViolationCollector v) {
    // well-formedness
    if (chars == null) { return nullCharacters(v); }
    if (chars.isEmpty()) { return emptyCharacters(v); }
    if (Utf16Util.firstSurrogate(chars) != -1) { return deleteCharactersContainsSurrogate(v); }
    if (!Utf16Util.isValidUtf16(chars)) { return deleteCharactersInvalidUnicode(v); }
    if (!insertionStackIsEmpty()) { return deleteInsideInsert(v); }
    // validity
    int docLength = doc.length();
    for (int offset = 0; offset < chars.length(); offset++) {
      if (effectivePos + offset >= docLength) {
        return cannotDeleteSoManyCharacters(v, offset, chars);
      }
      int charHereIfAny = doc.charAt(effectivePos + offset);
      if (charHereIfAny == -1) {
        return cannotDeleteSoManyCharacters(v, offset, chars);
      }
      char charHere = (char) charHereIfAny;
      if (charHere != chars.charAt(offset)) {
        return oldCharacterDiffersFromDocument(v, charHere, chars.charAt(offset));
      }
    }
    return checkAnnotationsForDeletion(v, chars.length());
  }

  public void doDeleteCharacters(String chars) {
    if (EXPENSIVE_ASSERTIONS) {
      assert !checkDeleteCharacters(chars, null).isIllFormed();
    }
    advance(chars.length());
    afterAnnotationBoundary = false;
  }


  public ValidationResult checkDeleteElementStart(String type, Attributes attr,
      ViolationCollector v) {
    // well-formedness
    if (type == null) { return nullTag(v); }
    if (!Utf16Util.isXmlName(type)) { return elementTypeNotXmlName(v, type); }
    {
      ValidationResult r = checkAttributesWellFormed(attr, v);
      if (r != ValidationResult.VALID) { return r; }
    }
    if (!insertionStackIsEmpty()) { return deleteInsideInsert(v); }
    // validity
    if (effectiveDocSymbol() != DocSymbol.OPEN) { return noElementStartToDelete(v); }
    if (!effectiveDocSymbolTag().equals(type)) { return oldTagDifferFromDocument(v); }
    if (!attributesEqual(attr, effectiveDocSymbolAttributes())) {
      return oldAttributesDifferFromDocument(v);
    }
    {
      ValidationResult r = checkAnnotationsForDeletion(v, 1);
      if (!r.isValid()) { return r; }
    }
    // schema
    if (deletionStackDepth == 0) {
      if (effectivePos < doc.length()) {
        String parentType = doc.nthEnclosingElementTag(effectivePos, 0);
        String requiredFirstChild = requiredFirstChild(parentType);
        boolean isFirstChild = effectivePos == 0 || doc.elementStartingAt(effectivePos - 1) != null;
        if (isFirstChild && requiredFirstChild != null) {
          return attemptToDeleteRequiredChild(v);
        }
      }
    }
    return valid();
  }

  public void doDeleteElementStart(String tag, Attributes attr) {
    if (EXPENSIVE_ASSERTIONS) {
      assert !checkDeleteElementStart(tag, attr, null).isIllFormed();
    }
    deletionStackPush();
    advance(1);
    afterAnnotationBoundary = false;
  }


  public ValidationResult checkDeleteElementEnd(ViolationCollector v) {
    // well-formedness
    if (!insertionStackIsEmpty()) { return deleteInsideInsert(v); }
    if (deletionStackIsEmpty()) { return mismatchedDeleteEnd(v); }
    // validity
    if (effectiveDocSymbol() != DocSymbol.CLOSE) { return noElementEndToDelete(v); }
    {
      ValidationResult r = checkAnnotationsForDeletion(v, 1);
      if (!r.isValid()) { return r; }
    }
    return valid();
  }

  public void doDeleteElementEnd() {
    if (EXPENSIVE_ASSERTIONS) {
      assert checkDeleteElementEnd(null) != ValidationResult.ILL_FORMED;
    }
    deletionStackPop();
    advance(1);
    afterAnnotationBoundary = false;
  }


  public ValidationResult checkUpdateAttributes(AttributesUpdate u, ViolationCollector v) {
    // well-formedness
    {
      ValidationResult r = checkAttributesUpdateWellFormed(u, v);
      if (!r.isValid()) { return r; }
    }
    if (!deletionStackIsEmpty()) { return attributeChangeInsideInsertOrDelete(v); }
    if (!insertionStackIsEmpty()) { return attributeChangeInsideInsertOrDelete(v); }

    // validity
    if (effectiveDocSymbol() != DocSymbol.OPEN) { return noElementStartToChangeAttributes(v); }
    String type = effectiveDocSymbolTag();
    assert type != null;
    Attributes oldAttrs = effectiveDocSymbolAttributes();
    for (int i = 0; i < u.changeSize(); i++) {
      String key = u.getChangeKey(i);
      String oldValue = u.getOldValue(i);
      if (!equal(oldValue, oldAttrs.get(key))) { return oldAttributesDifferFromDocument(v); }
    }
    {
      ValidationResult r = checkAnnotationsForRetain(v, 1);
      if (!r.isValid()) { return r; }
    }

    // schema
    for (int i = 0; i < u.changeSize(); i++) {
      String key = u.getChangeKey(i);
      String value = u.getNewValue(i);
      if (value != null) {
        if (!elementAllowsAttribute(type, key, value)) {
          return invalidAttribute(v, type, key, value);
        }
      }
    }
    return ValidationResult.VALID;
  }

  public void doUpdateAttributes(AttributesUpdate u) {
    if (EXPENSIVE_ASSERTIONS) {
      assert !checkUpdateAttributes(u, null).isIllFormed();
    }
    advance(1);
    updateDeletionTargetAnnotations();
    resultingPos += 1;
    afterAnnotationBoundary = false;
  }


  public ValidationResult checkReplaceAttributes(Attributes oldAttrs, Attributes newAttrs,
      ViolationCollector v) {
    // well-formedness
    {
      ValidationResult r = checkAttributesWellFormed(oldAttrs, v);
      if (!r.isValid()) { return r; }
    }
    {
      ValidationResult r = checkAttributesWellFormed(newAttrs, v);
      if (!r.isValid()) { return r; }
    }
    if (!deletionStackIsEmpty()) { return attributeChangeInsideInsertOrDelete(v); }
    if (!insertionStackIsEmpty()) { return attributeChangeInsideInsertOrDelete(v); }

    // validity
    if (effectiveDocSymbol() != DocSymbol.OPEN) { return noElementStartToChangeAttributes(v); }
    String type = effectiveDocSymbolTag();
    assert type != null;
    Attributes actualOldAttrs = effectiveDocSymbolAttributes();
    if (!attributesEqual(actualOldAttrs, oldAttrs)) { return oldAttributesDifferFromDocument(v); }
    {
      ValidationResult r = checkAnnotationsForRetain(v, 1);
      if (!r.isValid()) { return r; }
    }

    // schema
    {
      ValidationResult r = validateAttributes(type, newAttrs, v);
      if (!r.isValid()) { return r; }
    }
    return valid();
  }

  public void doReplaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
    if (EXPENSIVE_ASSERTIONS) {
      assert !checkReplaceAttributes(oldAttrs, newAttrs, null).isIllFormed();
    }
    advance(1);
    updateDeletionTargetAnnotations();
    resultingPos += 1;
    afterAnnotationBoundary = false;
  }


  /**
   * Checks whether the automaton is in an accepting state, i.e., whether the
   * operation would be valid if no further operation components follow.
   */
  public ValidationResult checkFinish(ViolationCollector v) {
    // well-formedness
    if (!insertionStackIsEmpty()) {
      for (InsertStart e : insertionStack) {
        return e.notClosed(this, v);
      }
    }
    if (!deletionStackIsEmpty()) {
      return mismatchedDeleteStart(v);
    }
    if (annotationsUpdate.changeSize() > 0) {
      return mismatchedStartAnnotation(v, annotationsUpdate.getChangeKey(0));
    }

    // validity
    if (effectivePos != doc.length()) {
      return missingRetainToEnd(v, doc.length(), effectivePos);
    }
    return ValidationResult.VALID;
  }

}
