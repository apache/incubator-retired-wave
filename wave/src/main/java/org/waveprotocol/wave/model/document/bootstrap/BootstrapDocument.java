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

package org.waveprotocol.wave.model.document.bootstrap;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOp.IsDocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.ModifiableDocument;
import org.waveprotocol.wave.model.document.operation.automaton.AutomatonDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationMap;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationsUpdate;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationsUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.operation.impl.DocOpValidator;
import org.waveprotocol.wave.model.operation.OpCursorException;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;

/**
 * A document implementation that is easy to understand but not efficient.
 */
public class BootstrapDocument implements ModifiableDocument, AutomatonDocument, IsDocOp {

  private abstract class Item {
    AnnotationMap annotations;
    Item(AnnotationMap annotations) {
      this.annotations = annotations;
    }

    void applyItem(DocInitializationCursor c) {

      int size = knownAnnotationKeys.size();
      String[] changeKeys = knownAnnotationKeys.toArray(new String[size]);
      assert changeKeys.length == size;
      String[] newValues = new String[size];

      for (int i = 0; i < size; i++) {
        newValues[i] = annotations.get(changeKeys[i]);
      }

      c.annotationBoundary(new AnnotationBoundaryMapImpl(
          new String[0], changeKeys, new String[size], newValues));

      applyData(c);
    }

    abstract void applyData(DocInitializationCursor c);

    AnnotationMap getAnnotations() {
      return annotations;
    }

    void updateAnnotations(AnnotationsUpdate annotationUpdate) {
      annotations = annotations.updateWith(annotationUpdate);
    }
  }

  private class CharacterItem extends Item {
    final char character;

    CharacterItem(char character, AnnotationMap annotations) {
      super(annotations);
      this.character = character;
    }

    @Override
    void applyData(DocInitializationCursor c) {
      c.characters("" + character);
    }

    @Override
    public String toString() {
      return "Character: " + character + " [" + annotations + "]";
    }
  }

  private class ElementStartItem extends Item {
    final String tag;
    Attributes attrs;

    ElementStartItem(String tag, Attributes attrs, AnnotationMap annotations) {
      super(annotations);
      this.tag = tag;
      this.attrs = attrs;
    }

    @Override
    void applyData(DocInitializationCursor c) {
      c.elementStart(tag, attrs);
    }

    void replaceAttributes(Attributes newAttributes) {
      attrs = newAttributes;
    }

    void updateAttributes(AttributesUpdate update) {
      attrs = attrs.updateWith(update);
    }

    String getTagName() {
      return tag;
    }

    @Override
    public String toString() {
      return "ElementStart: " + tag + " " + attrs + " [" + annotations + "]";
    }
  }

  private class ElementEndItem extends Item {

    ElementEndItem(AnnotationMap annotations) {
      super(annotations);
    }

    @Override
    void applyData(DocInitializationCursor c) {
      c.elementEnd();
    }

    @Override
    public String toString() {
      return "ElementEnd: [" + annotations + "]";
    }
  }

  private final DocumentSchema schemaConstraints;
  private final List<Item> items = new LinkedList<Item>();
  // All annotation keys that we've ever encountered.
  private final TreeSet<String> knownAnnotationKeys = new TreeSet<String>();
  private boolean inconsistent = false;

  public BootstrapDocument(DocumentSchema schemaConstraints) {
    this.schemaConstraints = schemaConstraints;
  }

  public BootstrapDocument() {
    this(DocumentSchema.NO_SCHEMA_CONSTRAINTS);
  }

  /** Copy constructor */
  public BootstrapDocument(BootstrapDocument other) {
    this(other.schemaConstraints);
    try {
      consume(other.asOperation());
    } catch (OperationException e) {
      throw new OperationRuntimeException("Invalid other document", e);
    }
  }

  @Override
  public DocInitialization asOperation() {
    checkConsistent();
    DocInitializationBuffer b = new DocInitializationBuffer();
    for (Item i : items) {
      i.applyItem(b);
    }

    if (!items.isEmpty()) {
      String[] endKeys = knownAnnotationKeys.toArray(new String[knownAnnotationKeys.size()]);
      b.annotationBoundary(new AnnotationBoundaryMapImpl(
          endKeys, new String[0], new String[0], new String[0]));
    }
    return b.finish();
  }

  @Override
  public int length() {
    checkConsistent();
    return items.size();
  }

  private ListIterator<Item> readIterator;
  private final List<String> tagNames = new ArrayList<String>();

  @Override
  public String elementStartingAt(int pos) {
    checkConsistent();
    Item item = advance(pos);
    if (item instanceof ElementStartItem) {
      return ((ElementStartItem) item).getTagName();
    } else {
      return null;
    }
  }

  @Override
  public Attributes attributesAt(int pos) {
    checkConsistent();
    Item item = advance(pos);
    if (item instanceof ElementStartItem) {
      return ((ElementStartItem) item).attrs;
    } else {
      return null;
    }
  }

  @Override
  public String elementEndingAt(int pos) {
    checkConsistent();
    Item item = advance(pos);
    if (item instanceof ElementEndItem) {
      return tagNames.get((tagNames.size() - 1));
    } else {
      return null;
    }
  }

  @Override
  public int charAt(int pos) {
    checkConsistent();
    Item item = advance(pos);
    if (item instanceof CharacterItem) {
      int c = ((CharacterItem) item).character;
      assert c != -1;
      return c;
    } else {
      return -1;
    }
  }

  @Override
  public String nthEnclosingElementTag(int insertionPoint, int depth) {
    checkConsistent();
    advance(insertionPoint);
    if (depth >= tagNames.size()) {
      return null;
    }
    return tagNames.get(tagNames.size() - 1 - depth);
  }

  @Override
  public int remainingCharactersInElement(int insertionPoint) {
    checkConsistent();
    advance(insertionPoint);

    int num = 0;
    try {
      while (readIterator.next() instanceof CharacterItem) {
        num++;
      }
    } catch (NoSuchElementException ex) {
      // reached document end.
    }

    for (int i = 0; i < num; i++) {
      readIterator.previous();
    }

    return num;
  }

  @Override
  public AnnotationMap annotationsAt(int pos) {
    checkConsistent();
    Preconditions.checkElementIndex(pos, items.size());
    return advance(pos).getAnnotations();
  }

  @Override
  public String getAnnotation(int pos, String key) {
    checkConsistent();
    Preconditions.checkElementIndex(pos, items.size());
    return advance(pos).getAnnotations().get(key);
  }

  private static boolean equal(Object a, Object b) {
    return a == null ? b == null : a.equals(b);
  }

  @Override
  public int firstAnnotationChange(int start, int end, String key, String fromValue) {
    Preconditions.checkPositionIndexes(start, end, items.size());
    for (int pos = start; pos < end; pos++) {
      if (!equal(getAnnotation(pos, key), fromValue)) {
        return pos;
      }
    }
    return -1;
  }

  private Item currentItem() {
    if (!readIterator.hasNext()) {
      return null;
    }
    Item item = readIterator.next();
    readIterator.previous();
    return item;
  }

  // null if pos == items.size()
  private Item advance(int pos) {
    Preconditions.checkPositionIndex(pos, items.size());
    if (readIterator == null || pos < readIterator.nextIndex()) {
      resetReadState();
    }

    for (int i = readIterator.nextIndex(); i < pos; i++) {
      Item item = readIterator.next();
      if (item instanceof ElementStartItem) {
        tagNames.add(((ElementStartItem) item).getTagName());
      } else if (item instanceof ElementEndItem) {
        tagNames.remove(tagNames.size() - 1);
      }
    }
    return currentItem();
  }

  private void resetReadState() {
    readIterator = items.listIterator();
    tagNames.clear();
  }

  AnnotationsUpdate annotationUpdates;

  @Override
  public void consume(DocOp m) throws OperationException {
    checkConsistent();

    ViolationCollector v = new ViolationCollector();
    DocOpValidator.validate(v, schemaConstraints, this, m);
    if (!v.isValid()) {
      throw new OperationException("Validation failed: " + v);
    }

    inconsistent = true;

    annotationUpdates = AnnotationsUpdateImpl.EMPTY_MAP;
    final ListIterator<Item> iterator = items.listIterator();
    try {
      // In theory, the above call to the validator makes the error checking in
      // this DocOpCursor redundant.  We check for errors anyway in case the
      // validator is incorrect.
      m.apply(new DocOpCursor() {

        Item current = null;

        AnnotationMap inherited = AnnotationMapImpl.EMPTY_MAP;

        private AnnotationMap insertionAnnotations() {
          return inherited.updateWith(annotationUpdates);
        }

        @Override
        public void annotationBoundary(AnnotationBoundaryMap map) {
          annotationUpdates = annotationUpdates.composeWith(map);
          for (int i = 0; i < map.changeSize(); i++) {
            knownAnnotationKeys.add(map.getChangeKey(i));
          }
        }

        @Override
        public void characters(String s) {
          for (int i = 0; i < s.length(); i++) {
            iterator.add(new CharacterItem(s.charAt(i), insertionAnnotations()));
          }
        }

        @Override
        public void elementStart(String type, Attributes attrs) {
          iterator.add(new ElementStartItem(type, attrs, insertionAnnotations()));
        }

        @Override
        public void elementEnd() {
          iterator.add(new ElementEndItem(insertionAnnotations()));
        }

        @Override
        public void deleteCharacters(String s) {
          for (int i = 0; i < s.length(); i++) {
            CharacterItem item = nextCharacter();
            if (s.charAt(i) != item.character) {
              throw new OpCursorException("Mismatched deleted characters: " +
                  s.charAt(i) + " vs " + item.character);
            }
            inherited = item.getAnnotations();
            iterator.remove();
          }
        }

        @Override
        public void deleteElementEnd() {
          ElementEndItem item = nextElementEnd();
          inherited = item.getAnnotations();
          iterator.remove();
        }

        @Override
        public void deleteElementStart(String tag, Attributes attrs) {
          ElementStartItem item = nextElementStart();
          inherited = item.getAnnotations();
          iterator.remove();
        }

        @Override
        public void retain(int distance) {
          for (int i = 0; i < distance; i++) {
            inheritAndAnnotate(next());
          }
        }

        @Override
        public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
          ElementStartItem item = nextElementStart();
          item.replaceAttributes(newAttrs);
          inheritAndAnnotate(item);
        }

        @Override
        public void updateAttributes(AttributesUpdate attrUpdate) {
          ElementStartItem item = nextElementStart();
          item.updateAttributes(attrUpdate);
          inheritAndAnnotate(item);
        }

        private void inheritAndAnnotate(Item item) {
          inherited = item.getAnnotations();
          item.updateAnnotations(annotationUpdates);
        }

        Item next() {
          if (!iterator.hasNext()) {
            throw new OpCursorException("Action past end of document, of size: " + length());
          }
          current = iterator.next();
          return current;
        }

        ElementStartItem nextElementStart() {
          try {
            return (ElementStartItem) next();
          } catch (ClassCastException e) {
            throw new OpCursorException("Not at an element start, at: " + current);
          }
        }

        ElementEndItem nextElementEnd() {
          try {
            return (ElementEndItem) next();
          } catch (ClassCastException e) {
            throw new OpCursorException("Not at an element end, at: " + current);
          }
        }

        CharacterItem nextCharacter() {
          try {
            return (CharacterItem) next();
          } catch (ClassCastException e) {
            throw new OpCursorException("Not at a character, at: " + current);
          }
        }
      });
      if (iterator.hasNext()) {
        int remainingItems = 0;
        while (iterator.hasNext()) {
          remainingItems++;
          iterator.next();
        }
        throw new OperationException("Missing retain to end of document (" +
            remainingItems + " items)");
      }
    } catch (OpCursorException e) {
      throw new OperationException(e);
    }

    if (annotationUpdates.changeSize() != 0) {
      throw new OperationException("Unended annotations at end of operation: " + annotationUpdates);
    }

    resetReadState();

    inconsistent = false;
  }

  private void checkConsistent() {
    if (inconsistent) {
      throw new IllegalStateException("The document is in an inconsistent state");
    }
  }

  @Override
  public String toString() {
    return "BootstrapDocument: " + DocOpUtil.debugToXmlString(asOperation());
  }

}
