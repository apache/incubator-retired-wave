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

package org.waveprotocol.wave.model.document.operation;

import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.indexed.NodeType;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.OperationIllFormed;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.OperationInvalid;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.SchemaViolation;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ValidationResult;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.HashSet;
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
 * Every possible mutation component (such as "elementStart(...)") corresponds
 * to a potential transition of the automaton.  The checkXXX methods
 * (such as checkElementStart(...)) determine whether a given transition exists
 * and is valid, or whether it is invalid, or ill-formed.  The doXXX methods
 * will perform the transition.  Ill-formed transitions must not be performed.
 * Invalid transitions are permitted, but after performing an invalid transition,
 * the validity of any further mutation components is not clearly specified.
 *
 * The checkFinish() method determines whether ending an operation is acceptable,
 * or whether any opening components are missing the corresponding closing
 * component.
 *
 * The checkXXX methods accept a ViolationsAccu object where they will record
 * details about any violations.  If a proposed transition is invalid for more
 * than one reason, the checkXXX method may detect only one (or any subset) of
 * the reasons and record only those violations.  The ViolationsAccu parameter
 * may also be null, in which case details about the violations will not be
 * recorded.
 *
 * To validate an operation, the automaton needs to be driven according to
 * the mutation components in that operation.  DocumentOperationValidator does
 * this.
 *
 * To generate a random operation, the automaton needs to be driven based on
 * a random document mutation component generator.
 * RandomDocumentMutationGenerator does this.
 *
 * @author ohler@google.com (Christian Ohler)
 */
// TODO(ohler/danilatos): Incorporate initial required elements schema checks
public final class NindoAutomaton<N, E extends N, T extends N> {

  /**
   * An object containing information about one individual reason why an
   * operation is not valid, e.g. "skip past end" or "deletion inside insertion".
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
    protected abstract ValidationResult validationResult();
    /**
     * @return a developer-readable description of the violation
     */
    public String description() {
      return description + " at original document position " + originalDocumentPos
          + " / resulting document position " + resultingDocumentPos;
    }
  }

  // http://www.w3.org/TR/xml/#NT-NameStartChar

  private static boolean isXmlNameStartChar(char c) {
    // NameStartChar ::= ":" | [A-Z] | "_" | [a-z]
    return c == ':' || ('A' <= c && c <= 'Z') || c == '_' | ('a' <= c && c <= 'z')
        //             | [#xC0-#xD6] | [#xD8-#xF6] | [#xF8-#x2FF]
        || (0xC0 <= c && c <= 0xD6) || (0xD8 <= c && c <= 0xF6) || (0xF8 <= c && c <= 0x2FF)
        //             | [#x370-#x37D] | [#x37F-#x1FFF]
        || (0x370 <= c && c <= 0x37D) || (0x37F <= c && c <= 0x1FFF)
        //             | [#x200C-#x200D] | [#x2070-#x218F]
        || (0x200C <= c && c <= 0x200D) || (0x2070 <= c && c <= 0x218F)
        //             | [#x2C00-#x2FEF] | [#x3001-#xD7FF]
        || (0x2C00 <= c && c <= 0x2FEF) || (0x3001 <= c && c <= 0xD7FF)
        //             | [#xF900-#xFDCF] | [#xFDF0-#xFFFD]
        || (0xF900 <= c && c <= 0xFDCF) || (0xFDF0 <= c && c <= 0xFFFD)
        //             | [#x10000-#xEFFFF]
        // Ha, ha.
        || (0x10000 <= c && c <= 0xEFFFF);
  }

  private static boolean isXmlNameChar(char c) {
    // NameChar ::= NameStartChar | "-" | "." | [0-9]
    return isXmlNameStartChar(c) || c == '-' || c == '.' || ('0' <= c && c <= '9')
        //          | #xB7 | [#x0300-#x036F] | [#x203F-#x2040]
        || c == 0xB7 || (0x0300 <= c && c <= 0x036F) || (0x203F <= c && c <= 0x2040);
  }

  private static boolean isXmlName(String s) {
    // Name ::= NameStartChar (NameChar)*
    assert s != null;
    if (s.length() == 0) {
      return false;
    }
    if (!isXmlNameStartChar(s.charAt(0))) {
      return false;
    }
    for (int i = 1; i < s.length(); i++) {
      if (!isXmlNameChar(s.charAt(i))) {
        return false;
      }
    }
    return true;
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
    return new OperationIllFormed(description, effectivePos(), resultingPos);
  }

  private OperationInvalid invalidOperation(String description) {
    return new OperationInvalid(description, effectivePos(), resultingPos);
  }

  private SchemaViolation schemaViolation(String description) {
    return new SchemaViolation(description, effectivePos(), resultingPos);
  }

  private ValidationResult valid() {
    return ValidationResult.VALID;
  }

  private ValidationResult mismatchedElementStart(ViolationCollector v) {
    return addViolation(v, illFormedOperation("elementStart with no elementEnd"));
  }

  private ValidationResult mismatchedDeleteElementStart(ViolationCollector v) {
    return addViolation(v, illFormedOperation("deleteElementStart with no deleteElementEnd"));
  }

  private ValidationResult mismatchedElementEnd(ViolationCollector v) {
    return addViolation(v, illFormedOperation("elementEnd with no elementStart"));
  }

  private ValidationResult mismatchedDeleteElementEnd(ViolationCollector v) {
    return addViolation(v, illFormedOperation("deleteElementEnd with no deleteElementStart"));
  }

  private ValidationResult mismatchedStartAnnotation(ViolationCollector v, String key) {
    return addViolation(v, illFormedOperation("startAnnotation of key " + key
        + " with no endAnnotation"));
  }

  private ValidationResult mismatchedEndAnnotation(ViolationCollector v, String key) {
    return addViolation(v, illFormedOperation("endAnnotation of key " + key
        + " with no startAnnotation"));
  }

  private ValidationResult skipDistanceNotPositive(ViolationCollector v) {
    return addViolation(v, illFormedOperation("skip distance not positive"));
  }

  private ValidationResult skipInsideInsertOrDelete(ViolationCollector v) {
    return addViolation(v, illFormedOperation("skip inside insert or delete"));
  }

  private ValidationResult attributeChangeInsideInsertOrDelete(ViolationCollector v) {
    return addViolation(v, illFormedOperation("attribute change inside insert or delete"));
  }

  private ValidationResult skipPastEnd(ViolationCollector v) {
    return addViolation(v, invalidOperation("skip past end of document"));
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

  private ValidationResult elementTypeNotXmlName(ViolationCollector v, String name) {
    return addViolation(v, illFormedOperation("element type is not an XML name: \""
        + name + "\""));
  }

  private ValidationResult nullAttributes(ViolationCollector v) {
    return addViolation(v, illFormedOperation("attributes is null"));
  }

  private ValidationResult nullAttributeKey(ViolationCollector v) {
    return addViolation(v, illFormedOperation("attribute key is null"));
  }

  private ValidationResult attributeKeyNotXmlName(ViolationCollector v, String name) {
    return addViolation(v, illFormedOperation("attribute key is not an XML name: \""
        + name + "\""));
  }

  private ValidationResult nullAttributeValue(ViolationCollector v) {
    return addViolation(v, illFormedOperation("attribute value is null"));
  }

  private ValidationResult nullAnnotationKey(ViolationCollector v) {
    return addViolation(v, illFormedOperation("annotation key is null"));
  }

  private ValidationResult textNotAllowedInElement(ViolationCollector v, String tag) {
    return addViolation(v, schemaViolation("element type " + tag
        + " does not allow text content"));
  }

  private ValidationResult tooLong(ViolationCollector v) {
    return addViolation(v, invalidOperation("intermediate or final document too long"));
  }

  private ValidationResult deleteLengthNotPositive(ViolationCollector v) {
    return addViolation(v, illFormedOperation("delete length not positive"));
  }

  private ValidationResult cannotDeleteSoManyCharacters(ViolationCollector v,
      int attempted, int available) {
    return addViolation(v, invalidOperation("cannot delete " + attempted + " characters,"
        + " only " + available + " available"));
  }

  private ValidationResult invalidAttribute(ViolationCollector v, String type, String attr) {
    return addViolation(v, schemaViolation("type " + type + " does not permit attribute " + attr));
  }

  private ValidationResult invalidAttribute(ViolationCollector v, String type, String attr,
      String value) {
    return addViolation(v, schemaViolation("type " + type + " does not permit attribute "
        + attr + " with value " + value));
  }

  private ValidationResult typeInvalidRoot(ViolationCollector v) {
    return addViolation(v, schemaViolation("type not permitted as root element"));
  }

  private ValidationResult invalidChild(ViolationCollector v, String parentTag, String childTag) {
    return addViolation(v, schemaViolation("element type " + parentTag
        + " does not permit subelement type " + childTag));
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

  private final DocumentSchema schemaConstraints;

  private enum DocSymbol { CHARACTER, OPEN, CLOSE, END }

  private abstract static class StackEntry {
    abstract ValidationResult notClosed(NindoAutomaton<?, ?, ?> a, ViolationCollector v);
    InsertElement asInsertElement() { return null; }
    DeleteElement asDeleteElement() { return null; }
  }

  private static class InsertElement extends StackEntry {
    final String tag;

    InsertElement(String tag) {
      this.tag = tag;
    }

    static InsertElement getInstance(String tag) {
      return new InsertElement(tag);
    }

    @Override
    ValidationResult notClosed(NindoAutomaton<?, ?, ?> a, ViolationCollector v) {
      return a.mismatchedElementStart(v);
    }

    @Override
    InsertElement asInsertElement() { return this; }
  }

  private static class DeleteElement extends StackEntry {
    static DeleteElement instance = new DeleteElement();
    static DeleteElement getInstance() { return instance; }

    @Override
    ValidationResult notClosed(NindoAutomaton<?, ?, ?> a, ViolationCollector v) {
      return a.mismatchedDeleteElementStart(v);
    }

    @Override
    DeleteElement asDeleteElement() { return this; }
  }

  /**
   * If effectivePos is at an element start, return that element.
   * Else return null.
   */
  private E elementStartingHere() {
    Preconditions.checkPositionIndex(effectivePos, doc.size());
    if (!(effectivePos + 1 < doc.size())) {
      // effectivePos + 1 is the smallest possible index of the corresponding
      // elementEnd; if that's beyond the end of the document, effectivePosition
      // can't be an elementStart.
      return null;
    }

    // Criterion: the enclosing element of effectivePos is the
    // parent of the enclosing element of effectivePos + 1
    // (if the enclosing element of effectivePos + 1 exists) (this
    // also covers the corner case of the enclosing element of
    // effectivePos being null).

    E elementHere =
      // can't create point for location 0
      effectivePos == 0 ? doc.getDocumentElement() :
          Point.enclosingElement(doc, doc.locate(effectivePos));
    E elementNext = Point.enclosingElement(doc, doc.locate(effectivePos + 1));
    if (elementNext == null) {
      return null;
    }
    if (elementHere != doc.getParentElement(elementNext)) {
      return null;
    } else {
      return elementNext;
    }
  }

  /**
   * If effectivePos is at an element end, return that element.
   * Else return null.
   */
  private E elementEndingNext() {
     Preconditions.checkPositionIndex(effectivePos, doc.size());
     if (effectivePos == 0)  {
       return null;
     }
     if (effectivePos == doc.size()) {
       return null;
     }
     if (effectivePos == doc.size() - 1) {
       // Root element ends here.
       E root = doc.getDocumentElement();
       assert root != null;
       return root;
     }

     Point<N> point = doc.locate(effectivePos);

     // Criterion: the enclosing element of effectivePos + 1 is the
     // parent of the enclosing element of effectivePos
     // (if the enclosing element of effectivePos exists) (this
     // also covers the corner case of the enclosing element of
     // effectivePos being null).

     E elementHere = Point.enclosingElement(doc, doc.locate(effectivePos));
     E elementNext = Point.enclosingElement(doc, doc.locate(effectivePos + 1));
     if (elementHere == null) {
       return null;
     }
     if (doc.getParentElement(elementHere) != elementNext) {
       return null;
     } else {
       return elementHere;
     }
  }

  // depth = 0 means enclosing element, depth = 1 means its parent, etc.
  private static <N, E extends N, T extends N> E nthEnclosingElement(IndexedDocument<N, E, T> doc,
      int pos, int depth) {
    assert depth >= 0;
    E e = Point.enclosingElement(doc, doc.locate(pos));
    for (int i = 0; i < depth; i++) {
      assert e != null;
      e = doc.getParentElement(e);
    }
    return e;
  }

  private static <N, E extends N, T extends N> int remainingCharactersInElement(
      IndexedDocument<N, E, T> doc, int pos) {
    if (pos >= doc.size()) {
      return 0;
    }
    Point<N> deletionPoint = doc.locate(pos);
    if (!deletionPoint.isInTextNode()) {
      return 0;
    }
    int offsetWithinNode = deletionPoint.getTextOffset();
    N container = deletionPoint.getContainer();
    assert doc.getNodeType(container) == NodeType.TEXT_NODE;
    int nodeLength = DocHelper.getItemSize(doc, container);
    int remainingChars = nodeLength - offsetWithinNode;
    assert remainingChars >= 0;
    while (true) {
      N next = doc.getNextSibling(container);
      if (next == null || doc.getNodeType(next) != NodeType.TEXT_NODE) {
        break;
      }
      remainingChars += DocHelper.getItemSize(doc, next);
      container = next;
    }
    return remainingChars;
  }

  private boolean tagAllowsText(String tag) {
    switch (schemaConstraints.permittedCharacters(tag)) {
      case ANY:
        return true;
      case BLIP_TEXT:
        return true;
      case NONE:
        return false;
      default:
        throw new AssertionError("unexpected return value from permittedCharacters");
    }
  }

  private boolean elementAllowedAsRoot(String tag) {
    return schemaConstraints.permitsChild(null, tag);
  }

  private boolean elementAllowsAttribute(String tag, String attributeName) {
    return schemaConstraints.permitsAttribute(tag, attributeName);
  }

  private boolean elementAllowsAttribute(String tag, String attributeName, String attributeValue) {
    return schemaConstraints.permitsAttribute(tag, attributeName, attributeValue);
  }

  private boolean elementAllowsChild(String parentType, String childType) {
    return schemaConstraints.permitsChild(parentType, childType);
  }


  // current state

  private final IndexedDocument<N, E, T> doc;
  private int effectivePos = 0;
  private int resultingLength;
  // first item is bottom of stack, last is top
  private final ArrayList<StackEntry> stack = new ArrayList<StackEntry>();
  private final Set<String> openAnnotationKeys = new HashSet<String>();


  // more state to track for debugging

  private int resultingPos = 0;

  /**
   * Creates an automaton that corresponds to the set of all possible operations
   * on the given document under the given schema constraints.
   */
  public NindoAutomaton(DocumentSchema schemaConstraints,
      IndexedDocument<N, E, T> doc) {
    this.schemaConstraints = schemaConstraints;
    this.doc = doc;
    this.resultingLength = doc.size();
  }


  // current state primitive readers

  private int resultingLength() {
    return resultingLength;
  }

  // note that effectivePos() is not, in general, <= resultingLength().  The
  // values are basically unrelated.
  private int effectivePos() {
    return effectivePos;
  }

  private DocSymbol effectiveDocSymbol() {
    if (effectivePos >= doc.size()) {
      return DocSymbol.END;
    }
    {
      E e = elementStartingHere();
      if (e != null) {
        return DocSymbol.OPEN;
      }
    }
    {
      E e = elementEndingNext();
      if (e != null) {
        return DocSymbol.CLOSE;
      }
    }
    return DocSymbol.CHARACTER;
  }

  // only defined for open and close
  private String effectiveDocSymbolTag() {
    switch (effectiveDocSymbol()) {
      case OPEN: {
        String tag = doc.getTagName(elementStartingHere());
        assert tag != null;
        return tag;
      }
      case CLOSE: {
        String tag = doc.getTagName(elementEndingNext());
        assert tag != null;
        return tag;
      }
      default:
        throw new IllegalStateException("not at tag");
    }
  }

  private boolean stackIsEmpty() {
    return stack.isEmpty();
  }

  // undefined if stack is empty
  private StackEntry topOfStack() {
    assert !stack.isEmpty();
    return stack.get(stack.size() - 1);
  }

  // null if outside root; must not be called if inserting
  private E effectiveEnclosingElement() {
    // need to take stack into account
    // stack may be deleting, which does not affect level (semantics
    // actually don't matter in this case, since no call sites call us during
    // deletions)
    // stack may be joining, which does not affect level (nested join needs
    // to know tag that it is joining)
    // stack may be splitting, which means we go down
    // stack may be adding elements, which means we add levels

    // if top of stack is insert element, that tells us the
    // tag already

    if (effectivePos == 0 || effectivePos >= doc.size()) {
      return null;
    }

    if (stackIsEmpty()) {
      return nthEnclosingElement(doc, effectivePos, 0);
    } else {
      if (topOfStack().asInsertElement() != null) {
        assert false;
      }
      if (topOfStack().asDeleteElement() != null) {
        {
          boolean foundDeleteElement = false;

          // TODO(ohler): Simplify this logic now that we no longer have anti elements.
          // Even better, merge/consolidate with DocOpAutomaton

          // The stack, when looking at it from bottom to top, must consist of a
          // sequence of deleteAntiElements followed by a sequence deleteElements.
          for (StackEntry e : stack) {
            if (e.asDeleteElement() != null) {
              foundDeleteElement = true;
            } else {
              assert false;
            }
          }
        }
        E e = nthEnclosingElement(doc, effectivePos, 0);
        assert e != null; // cannot delete root
        return e;
      }
      throw new RuntimeException("unexpected top of stack: " + topOfStack());
    }
  }

  private String effectiveEnclosingElementTag() {
    if (!stackIsEmpty() && topOfStack().asInsertElement() != null) {
      return topOfStack().asInsertElement().tag;
    } else {
      E e = effectiveEnclosingElement();
      if (e == null) {
        return null;
      } else {
        return doc.getTagName(e);
      }
    }
  }

  // only defined if effective enclosing element is not null.
  // null if effective enclosing element is root.
  private String effectiveEnclosingElementParentTag() {
    if (!stackIsEmpty() &&  topOfStack().asInsertElement() != null) {
      if (stack.size() > 1) {
        StackEntry s = stack.get(stack.size() - 2);
        assert s != null;
        assert s.asInsertElement() != null;
        return s.asInsertElement().tag;
      } else {
        return topOfStack().asInsertElement().tag;
      }
    } else {
      E e = effectiveEnclosingElement();
      assert e != null;
      E p = doc.getParentElement(e);
      if (p == null) {
        return null;
      } else {
        return doc.getTagName(p);
      }
    }
  }

  private boolean isAnnotationOpen(String key) {
    return openAnnotationKeys.contains(key);
  }

  // Note that this is inclusive.
  //
  // NOTE(ohler): Some annotation-related indexing tricks may require
  // storing 4 times the index in an int.  Dividing by 5 just for some
  // headroom.
  //
  // TODO(ohler): make sure the size limit for intermediate document states
  // is compatible with composition.
  private static final int MAX_DOC_LENGTH = Integer.MAX_VALUE / 5;

  /**
   * If an inserting mutation component is permitted as the next mutation
   * component, returns the maximum number of items that can be added to the
   * document in that component without exceeding any document size limits.
   * Otherwise, the return value is undefined.
   */
  // Need to be careful with potential overflows here in case we ever
  // increase maxLength to Integer.MAX_VALUE.
  public int maxLengthIncrease() {
    int result = MAX_DOC_LENGTH - resultingLength;
    assert result >= 0;
    return result;
  }

  /**
   * If a skip mutation component is permitted as the next mutation component,
   * returns the maximum skip distance.
   * Otherwise, the return value is undefined.
   */
  public int maxSkipDistance() {
    if (effectivePos >= doc.size()) {
      return 0;
    } else {
      return doc.size() - effectivePos;
    }
  }

  private boolean canIncreaseLength(int delta) {
    assert delta >= 0;
    return delta <= maxLengthIncrease();
  }

  private boolean canSkip(int distance) {
    assert distance >= 0;
    assert doc.size() <= MAX_DOC_LENGTH;
    return distance <= maxSkipDistance();
  }

  /**
   * If a deleteCharacters mutation component is permitted as the next mutation
   * component, returns the maximum number of characters that it can delete.
   * Otherwise, the return value is undefined.
   */
  public int maxCharactersToDelete() {
    return remainingCharactersInElement(doc, effectivePos);
  }

  private boolean topOfStackIsDeletion() {
    return !stackIsEmpty() && topOfStack().asDeleteElement() != null;
  }

  private boolean topOfStackIsInsertion() {
    return !stackIsEmpty() && topOfStack().asInsertElement() != null;
  }

  private boolean topOfStackIsInsertElement() {
    return !stackIsEmpty() && (topOfStack().asInsertElement() != null);
  }

  private boolean topOfStackIsDeleteElement() {
    return !stackIsEmpty() && (topOfStack().asDeleteElement() != null);
  }


  // current state manipulators

  private void advance(int distance) {
    // we're not asserting canIncreaseLength() or similar here, since
    // the driver may be generating an invalid op deliberately.
    assert distance >= 0;
    effectivePos += distance;
  }

  private void increaseLength(int delta) {
    resultingLength += delta;
  }

  private void decreaseLength(int delta){
    resultingLength -= delta;
  }

  private void pushOntoStack(StackEntry e) {
    stack.add(e);
  }

  private void popStack() {
    assert !stack.isEmpty();
    stack.remove(stack.size() - 1);
  }

  private void setAnnotationOpen(String key) {
    openAnnotationKeys.add(key);
  }

  private void setAnnotationClosed(String key) {
    openAnnotationKeys.remove(key);
  }


  // check/do methods

  /**
   * Checks if a skip transition with the given parameters would be valid.
   */
  public ValidationResult checkSkip(int distance, ViolationCollector v) {
    if (distance <= 0) { return skipDistanceNotPositive(v); }
    if (!stackIsEmpty()) { return skipInsideInsertOrDelete(v); }
    if (!canSkip(distance)) { return skipPastEnd(v); }
    return valid();
  }

  /**
   * Performs a skip transition with the given parameters.
   */
  public void doSkip(int distance) {
    assert checkSkip(distance, null) != ValidationResult.ILL_FORMED;
    advance(distance);
    resultingPos += distance;
  }


  /**
   * Checks if a characters transition with the given parameters would be valid.
   */
  public ValidationResult checkCharacters(String characters, ViolationCollector v) {
    // TODO(danilatos/ohler): Check schema and surrogates
    if (characters == null) { return nullCharacters(v); }
    if (characters.length() == 0) { return emptyCharacters(v); }
    if (topOfStackIsDeletion()) { return insertInsideDelete(v); }
    String enclosingTag = effectiveEnclosingElementTag();
    if (!tagAllowsText(enclosingTag)) { return textNotAllowedInElement(v, enclosingTag); }
    if (!canIncreaseLength(characters.length())) { return tooLong(v); }
    return valid();
  }

  /**
   * Performs a characters transition with the given parameters.
   */
  public void doCharacters(String characters) {
    assert checkCharacters(characters, null) != ValidationResult.ILL_FORMED;
    increaseLength(characters.length());
    resultingPos += characters.length();
  }


  /**
   * Checks if a deleteCharacters transition with the given parameters would be valid.
   */
  public ValidationResult checkDeleteCharacters(int count, ViolationCollector v) {
    if (count <= 0) { return deleteLengthNotPositive(v); }
    if (topOfStackIsInsertion()) { return deleteInsideInsert(v); }
    int available = maxCharactersToDelete();
    if (count > available) { return cannotDeleteSoManyCharacters(v, count, available); }
    return valid();
  }

  /**
   * Performs a deleteCharacters transition with the given parameters.
   */
  public void doDeleteCharacters(int count) {
    assert checkDeleteCharacters(count, null) != ValidationResult.ILL_FORMED;
    advance(count);
    decreaseLength(count);
  }


  private ValidationResult validateAttributes(String tag, Map<String, String> attr,
      ViolationCollector v, boolean allowRemovals) {
    if (attr == null) { return nullAttributes(v); }
    for (Map.Entry<String, String> e : attr.entrySet()) {
      String key = e.getKey();
      String value = e.getValue();
      if (key == null) { return nullAttributeKey(v); }
      if (!isXmlName(key)) { return attributeKeyNotXmlName(v, key); }
      if (value == null) {
        if (!allowRemovals) { return nullAttributeValue(v); }
        if (!elementAllowsAttribute(tag, key)) { return invalidAttribute(v, tag, key); }
      } else {
        if (!elementAllowsAttribute(tag, key, value)) {
          return invalidAttribute(v, tag, key, value);
        }
      }
    }
    return ValidationResult.VALID;
  }


  /**
   * Checks if an elementStart with the given parameters would be valid.
   */
  public ValidationResult checkElementStart(String tag, Map<String, String> attr,
      ViolationCollector v) {
    if (tag == null) { return nullTag(v); }
    if (!isXmlName(tag)) { return elementTypeNotXmlName(v, tag); }
    {
      ValidationResult attrViolation = validateAttributes(tag, attr, v, false);
      if (attrViolation != ValidationResult.VALID) { return attrViolation; }
    }
    if (topOfStackIsDeletion()) { return insertInsideDelete(v); }
    if (!canIncreaseLength(2)) { return tooLong(v); }
    if (effectiveDocSymbol() == DocSymbol.END
        && effectiveEnclosingElementTag() == null
        && stackIsEmpty()
        && resultingLength() == 0) {
      if (elementAllowedAsRoot(tag)) {
        return valid();
      } else {
        return typeInvalidRoot(v);
      }
    }
    String parentTag = effectiveEnclosingElementTag();
    if (parentTag == null) {
      if (!elementAllowedAsRoot(tag)) { return typeInvalidRoot(v); }
    } else {
      if (!elementAllowsChild(parentTag, tag)) { return invalidChild(v, parentTag, tag); }
    }
    return valid();
  }

  /**
   * Performs an elementStart transition with the given parameters.
   */
  public void doElementStart(String tag, Map<String, String> attr) {
    assert checkElementStart(tag, attr, null) != ValidationResult.ILL_FORMED;
    pushOntoStack(InsertElement.getInstance(tag));
    increaseLength(2);
    resultingPos += 1;
  }


  /**
   * Checks if an elementEnd transition with the given parameters would be valid.
   */
  public ValidationResult checkElementEnd(ViolationCollector v) {
    if (!topOfStackIsInsertElement()) { return mismatchedElementEnd(v); }
    return valid();
  }

  /**
   * Performs an elementEnd transition with the given parameters.
   */
  public void doElementEnd() {
    assert checkElementEnd(null) != ValidationResult.ILL_FORMED;
    popStack();
    // size increase happens in element start
    resultingPos += 1;
  }


  /**
   * Checks if a deleteElementStart with the given parameters would be valid.
   */
  public ValidationResult checkDeleteElementStart(ViolationCollector v) {
    if (topOfStackIsInsertion()) { return deleteInsideInsert(v); }
    if (effectiveDocSymbol() != DocSymbol.OPEN) { return noElementStartToDelete(v); }
    return valid();
  }

  /**
   * Performs a deleteElementStart transition with the given parameters.
   */
  public void doDeleteElementStart() {
    assert checkDeleteElementStart(null) != ValidationResult.ILL_FORMED;
    pushOntoStack(DeleteElement.getInstance());
    advance(1);
    // size decrease happens in delete element end
  }


  /**
   * Checks if a deleteElementEnd with the given parameters would be valid.
   */
  public ValidationResult checkDeleteElementEnd(ViolationCollector v) {
    if (!topOfStackIsDeleteElement()) { return mismatchedDeleteElementEnd(v); }
    if (effectiveDocSymbol() != DocSymbol.CLOSE) { return noElementEndToDelete(v); }
    return valid();
  }

  /**
   * Performs a deleteElementEnd transition with the given parameters.
   */
  public void doDeleteElementEnd() {
    assert checkDeleteElementEnd(null) != ValidationResult.ILL_FORMED;
    popStack();
    decreaseLength(2);
    advance(1);
  }

  private ValidationResult checkChangeAttributes(Map<String, String> attr, ViolationCollector v,
      boolean allowNullValues) {
    if (!stackIsEmpty()) { return attributeChangeInsideInsertOrDelete(v); }
    if (effectiveDocSymbol() != DocSymbol.OPEN) { return noElementStartToChangeAttributes(v); }
    String actualTag = effectiveDocSymbolTag();
    assert actualTag != null;
    return validateAttributes(actualTag, attr, v, allowNullValues);
  }


  /**
   * Checks if a setAttributes with the given parameters would be valid.
   */
  public ValidationResult checkSetAttributes(Map<String, String> attr, ViolationCollector v) {
    return checkChangeAttributes(attr, v, false);
  }

  /**
   * Performs a setAttributes transition with the given parameters.
   */
  public void doSetAttributes(Map<String, String> attr) {
    assert checkSetAttributes(attr, null) != ValidationResult.ILL_FORMED;
    advance(1);
    resultingPos += 1;
  }


  /**
   * Checks if an updateAttributes with the given parameters would be valid.
   */
  public ValidationResult checkUpdateAttributes(Map<String, String> attr, ViolationCollector v) {
    return checkChangeAttributes(attr, v, true);
  }

  /**
   * Performs an updateAttributes transition with the given parameters.
   */
  public void doUpdateAttributes(Map<String, String> attr) {
    assert checkUpdateAttributes(attr, null) != ValidationResult.ILL_FORMED;
    advance(1);
    resultingPos += 1;
  }


  private ValidationResult validateAnnotationKey(String key, ViolationCollector v) {
    if (key == null) { return nullAnnotationKey(v); }
    return ValidationResult.VALID;
  }


  /**
   * Checks if a startAnnotation with the given parameters would be valid.
   */
  public ValidationResult checkStartAnnotation(String key, String value, ViolationCollector v) {
    {
      ValidationResult r = validateAnnotationKey(key, v);
      if (r != ValidationResult.VALID) { return r; }
    }
    return valid();
  }

  /**
   * Performs a startAnnotation transition with the given parameters.
   */
  public void doStartAnnotation(String key, String value) {
    assert checkStartAnnotation(key, value, null) != ValidationResult.ILL_FORMED;
    setAnnotationOpen(key);
  }


  /**
   * Checks if an endAnnotation with the given parameters would be valid.
   */
  public ValidationResult checkEndAnnotation(String key, ViolationCollector v) {
    {
      ValidationResult r = validateAnnotationKey(key, v);
      if (r != ValidationResult.VALID) { return r; }
    }
    if (!isAnnotationOpen(key)) { return mismatchedEndAnnotation(v, key); }
    return valid();
  }

  /**
   * Performs an endAnnotation transition with the given parameters.
   */
  public void doEndAnnotation(String key) {
    assert checkEndAnnotation(key, null) != ValidationResult.ILL_FORMED;
    setAnnotationClosed(key);
  }


  /**
   * Checks whether the automaton is in an accepting state, i.e., whether the
   * operation would be valid if no further mutation components follow.
   */
  public ValidationResult checkFinish(ViolationCollector v) {
    for (StackEntry e : stack) {
      return e.notClosed(this, v);
    }
    for (String key : openAnnotationKeys) {
      return mismatchedStartAnnotation(v, key);
    }
    return ValidationResult.VALID;
  }

  /**
   * Notifies the automaton that no further mutation components follow.
   */
  // This doesn't actually do anything important.  It's here for symmetry only.
  public void doFinish() {
    assert checkFinish(null) != ValidationResult.ILL_FORMED;
  }

}
