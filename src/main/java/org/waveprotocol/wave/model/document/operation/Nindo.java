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
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A non-invertible document operation
 *
 */
public final class Nindo {

  public interface NindoCursor {
    void begin();
    void finish();
    void characters(String s);
    void elementStart(String type, Attributes attrs);
    void elementEnd();
    void startAnnotation(String key, String value);
    void endAnnotation(String key);
    // skip to end is not required
    void skip(int n);
    void deleteCharacters(int n);
    void deleteElementStart();
    void deleteElementEnd();
    void replaceAttributes(Attributes attrs);
    void updateAttributes(Map<String, String> attrUpdate);
  }

  /**
   * A builder for Nindo objects.
   */
  public static class Builder implements NindoCursor {

    private final List<MutationComponent> mutationList = new ArrayList<MutationComponent>();

    public void skip(int skipSize) {
      assert skipSize > 0;
      mutationList.add(new Skip(skipSize));
    }

    public void characters(String characters) {
      if (!characters.isEmpty()) {
        mutationList.add(new Characters(characters));
      }
    }

    public void elementStart(String tagName, Attributes attributes) {
      assert attributes != null;
      mutationList.add(new ElementStart(tagName, attributes));
    }

    public void elementEnd() {
      mutationList.add(ElementEnd.INSTANCE);
    }

    public void deleteCharacters(int deletionSize) {
      assert deletionSize > 0;
      mutationList.add(new DeleteCharacters(deletionSize));
    }

    public void deleteElementStart() {
      mutationList.add(DeleteElementStart.INSTANCE);
    }

    public void deleteElementEnd() {
      mutationList.add(DeleteElementEnd.INSTANCE);
    }

    public void replaceAttributes(Attributes attributes) {
      mutationList.add(new ReplaceAttributes(attributes));
    }

    public void updateAttributes(Map<String, String> attributes) {
      mutationList.add(new UpdateAttributes(attributes));
    }

    public void startAnnotation(String key, String value) {
      mutationList.add(new StartAnnotation(key, value));
    }

    public void endAnnotation(String key) {
      mutationList.add(new EndAnnotation(key));
    }

    /**
     * Obtains a DocumentMutation from the builder.
     *
     * This method may do some error checking on the mutation and throw an
     * exception if it is ill-formed.
     *
     * @return The built DocumentMutation.
     */
    public Nindo build() {
      return new Nindo(mutationList);
    }

    private Nindo buildWithoutTrailingSkip() {
      int size = mutationList.size();
      if (size > 0 && mutationList.get(size - 1) instanceof Skip) {
        mutationList.remove(size - 1);
      }
      return build();
    }

    boolean hasBeenUsed = false;

    @Override
    public void begin() {
      Preconditions.checkState(!hasBeenUsed, "Cannot reuse a builder");
      hasBeenUsed = true;
    }

    @Override
    public void finish() {
      // Do nothing. build() actually builds.
    }
  }

  /**
   * A builder for applying annotations for a single key to a document in a linear order.
   *
   * This builder will generate the minimal mutation sequence to apply the requested
   * annotations.
   */
  public static class AnnotationBuilder<N, E extends N, T extends N> {
    /** The builder we will use to construct the annotation */
    private final Builder b = new Builder();
    /** The document we will be applying the ops to */
    private final IndexedDocument<N,E,T> doc;
    /** The key that we are applying annotations for */
    private final String key;
    /** Accumulated skips that are yet to be applied */
    private int skipAccum;
    /** Current position in the range */
    private int currentPos;
    /** The limit of the range that we are applying to */
    private final int rangeEnd;
    /** Whether we have actually applied anything yet */
    private boolean dirty = false;

    /**
     * Construct a new annotation builder.
     *
     * @param doc indexed doc to apply the annotations to
     * @param rangeStart document location to begin applying annotations from
     * @param rangeEnd document location to apply annotations up to
     * @param key key to apply annotations for
     */
    public AnnotationBuilder(IndexedDocument<N,E,T> doc, int rangeStart, int rangeEnd, String key) {
      this.doc = doc;
      this.skipAccum = rangeStart;
      this.currentPos = rangeStart;
      this.rangeEnd = rangeEnd;
      this.key = key;
    }

    /**
     * Sets the annotation to the given value up to the given location.
     *
     * @param value value to set for the annotation
     * @param end absolute location to set up to
     */
    public void setUpTo(String value, int end) {
      Preconditions.checkPositionIndexes(currentPos, end, rangeEnd);
      while (currentPos < end) {
        String currentValue = doc.getAnnotation(currentPos, key);
        int nextChange = doc.firstAnnotationChange(currentPos, end, key, currentValue);
        if (nextChange == -1) {
          nextChange = end;
        }
        int size = nextChange - currentPos;
        if (size > 0) {
          boolean applyAnnotation = false;
          if (value == null) {
            // We want to apply a nullify annotation if the currentValue isn't
            // already null
            if (currentValue != null) {
              applyAnnotation = true;
            }
          } else {
            // We want to apply the value if the currentValue is different
            if ((currentValue == null) || (!currentValue.equals(value))) {
              applyAnnotation = true;
            }
          }

          if (applyAnnotation) {
            if (skipAccum > 0) {
              b.skip(skipAccum);
              skipAccum = 0;
            }
            b.startAnnotation(key, value);
            b.skip(size);
            b.endAnnotation(key);
            dirty = true;
          } else {
            skipAccum += size;
          }
        }
        currentPos = nextChange;
      }
      assert (currentPos == end);
    }

    /**
     * Clears the annotation up to the given point.
     *
     * Same effect as <code>setUpTo(null, end)</code>.
     *
     * @param end location to nullify up to
     */
    public void clearUpTo(int end) {
      setUpTo(null, end);
    }

    /** @return the current position we have applied up to */
    public int getCurrentPos() {
      return currentPos;
    }

    /** @return true if we have build an annotation to apply */
    public boolean getDirty() {
      return dirty;
    }

    /** @return the built nindo */
    public Nindo build() {
      return b.build();
    }
  }

  /**
   * A component of the nindo.
   */
  private static abstract class MutationComponent {
    abstract void apply(NindoCursor document);
  }

  /**
   * A "skip" mutation component.
   */
  private static class Skip extends MutationComponent {

    final int skipSize;

    Skip(int skipSize) {
      this.skipSize = skipSize;
    }

    @Override
    void apply(NindoCursor document) {
      document.skip(skipSize);
    }

  }

  /**
   * A "characters" mutation component.
   */
  private static class Characters extends MutationComponent {

    final String characters;

    Characters(String characters) {
      this.characters = characters;
    }

    @Override
    void apply(NindoCursor document) {
      document.characters(characters);
    }

  }

  /**
   * An "elementStart" mutation component.
   */
  private static class ElementStart extends MutationComponent {

    final String tagName;
    final Attributes attributes;

    ElementStart(String tagName, Attributes attributes) {
      this.tagName = tagName;
      this.attributes = attributes;
    }

    @Override
    void apply(NindoCursor document) {
      document.elementStart(tagName, attributes);
    }

  }

  /**
   * An "elementEnd" mutation component.
   */
  private static class ElementEnd extends MutationComponent {

    static final ElementEnd INSTANCE = new ElementEnd();

    // Defining this private constructor doesn't achieve any extra privacy, but
    // is a hint that this class is intended to have a singleton instance.
    private ElementEnd() {}

    @Override
    void apply(NindoCursor document) {
      document.elementEnd();
    }

  }

  /**
   * A "deleteCharacters" mutation component.
   */
  private static class DeleteCharacters extends MutationComponent {

    final int deletionSize;

    DeleteCharacters(int deletionSize) {
      this.deletionSize = deletionSize;
    }

    @Override
    void apply(NindoCursor document) {
      document.deleteCharacters(deletionSize);
    }

  }

  /**
   * A "deleteElementEnd" mutation component.
   */
  private static class DeleteElementStart extends MutationComponent {

    static final DeleteElementStart INSTANCE = new DeleteElementStart();

    // Defining this private constructor doesn't achieve any extra privacy, but
    // is a hint that this class is intended to have a singleton instance.
    private DeleteElementStart() {}

    @Override
    void apply(NindoCursor document) {
      document.deleteElementStart();
    }

  }

  /**
   * A "deleteElementEnd" mutation component.
   */
  private static class DeleteElementEnd extends MutationComponent {

    static final DeleteElementEnd INSTANCE = new DeleteElementEnd();

    // Defining this private constructor doesn't achieve any extra privacy, but
    // is a hint that this class is intended to have a singleton instance.
    private DeleteElementEnd() {}

    @Override
    void apply(NindoCursor document) {
      document.deleteElementEnd();
    }

  }

  /**
   * A "setAttributes" mutation component.
   */
  private static class ReplaceAttributes extends MutationComponent {

    final Attributes attributes;

    ReplaceAttributes(Attributes attributes) {
      this.attributes = attributes;
    }

    @Override
    void apply(NindoCursor document) {
      document.replaceAttributes(attributes);
    }

  }

  /**
   * An "updateAttributes" mutation component.
   */
  private static class UpdateAttributes extends MutationComponent {

    final Map<String, String> attributesUpdate;

    UpdateAttributes(Map<String, String>  update) {
      // TODO(danilatos): This is unsafe, and not immutable.
      this.attributesUpdate = update;
    }

    @Override
    void apply(NindoCursor document) {
      document.updateAttributes(attributesUpdate);
    }
  }

  /**
   * A "startAnnotation" mutation component.
   */
  private static class StartAnnotation extends MutationComponent {

    final String key;
    final String value;

    StartAnnotation(String key, String value) {
      Preconditions.checkNotNull(key, "Null annotation key");
      this.key = key;
      this.value = value;
    }

    @Override
    void apply(NindoCursor document) {
      document.startAnnotation(key, value);
    }

  }

  /**
   * A "endAnnotation" mutation component.
   */
  private static class EndAnnotation extends MutationComponent {

    final String key;

    EndAnnotation(String key) {
      Preconditions.checkNotNull(key, "Null annotation key");
      this.key = key;
    }

    @Override
    void apply(NindoCursor document) {
      document.endAnnotation(key);
    }

  }

  private final List<MutationComponent> mutationList;

  private Nindo(List<MutationComponent> mutationList) {
    this.mutationList = new ArrayList<MutationComponent>(mutationList);
  }

  /**
   * Creates a new nindo builder.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Addes a skip of the given size to the start of the nindo if distance is
   * positive, reduces initial skips if the distance is negative.
   *
   * @return a new nindo
   */
  public static Nindo shift(final int distance, Nindo nindo) {
    int remaining = Math.max(0, -distance);

    Builder b = builder();

    if (distance > 0) {
      b.skip(distance);
    }

    for (MutationComponent c : nindo.mutationList) {
      if (remaining > 0) {
        if (c instanceof Skip) {
          int skipSize = ((Skip) c).skipSize;
          int diff = Math.min(remaining, skipSize);
          remaining -= diff;
          if (diff < skipSize) {
            b.skip(skipSize - diff);
          }
        } else {
          Preconditions.illegalArgument("Not enough initial skips in nindo to take off");
        }
      } else {
        c.apply(b);
      }
    }
    return b.build();
  }

  /**
   * Creates a new annotation builder.
   *
   * @param doc indexed doc to apply the annotations to
   * @param rangeStart document location to begin applying annotations from
   * @param rangeEnd document location to apply annotations up to
   * @param key key to apply annotations for
   */
  public static <N, E extends N, T extends N> AnnotationBuilder<N, E, T> annotationBuilder(
      IndexedDocument<N,E,T> doc, int rangeStart, int rangeEnd, String key) {
    return new AnnotationBuilder<N, E, T>(doc, rangeStart, rangeEnd, key);
  }

  /**
   * Creates a document mutation that inserts the given characters at the given
   * location.
   *
   * @param location The location at which to insert characters.
   * @param characters The characters to insert.
   * @return The document mutation.
   */
  public static Nindo insertCharacters(int location, String characters) {
    assert !characters.isEmpty();
    List<MutationComponent> mutationList = new ArrayList<MutationComponent>(2);
    mutationList.add(new Skip(location));
    mutationList.add(new Characters(characters));
    return new Nindo(mutationList);
  }

  /**
   * Creates a document mutation that inserts an element at the given location.
   *
   * @param location The location at which to insert the element.
   * @param tagName The tag name of the element.
   * @param attributes The attributes of the element.
   * @return The document mutation.
   */
  public static Nindo insertElement(int location, String tagName,
      Attributes attributes) {
    List<MutationComponent> mutationList = new ArrayList<MutationComponent>(4);
    if (location > 0) {
      mutationList.add(new Skip(location));
    }
    mutationList.add(new ElementStart(tagName, attributes));
    mutationList.add(ElementEnd.INSTANCE);
    return new Nindo(mutationList);
  }

  /**
   * Creates a document mutation that deletes the characters denoted by the
   * given range.
   *
   * @param start The start of the range of the characters to delete.
   * @param end The end of the range of the characters to delete.
   * @return The document mutation.
   */
  public static Nindo deleteCharacters(int start, int end) {
    List<MutationComponent> mutationList = new ArrayList<MutationComponent>(2);
    mutationList.add(new Skip(start));
    mutationList.add(new DeleteCharacters(end - start));
    return new Nindo(mutationList);
  }

  /**
   * Creates a document mutation that deletes an empty element at a given
   * location.
   *
   * @param elementLocation The location of the empty element to delete.
   * @return The document mutation.
   */
  public static Nindo deleteElement(int elementLocation) {
    List<MutationComponent> mutationList = new ArrayList<MutationComponent>(2);
    // It may be impossible to delete the root element, but let's check
    // that elsewhere.
    if (elementLocation > 0) {
      mutationList.add(new Skip(elementLocation));
    }
    mutationList.add(DeleteElementStart.INSTANCE);
    mutationList.add(DeleteElementEnd.INSTANCE);
    return new Nindo(mutationList);
  }

  /**
   * Creates a document mutation that sets all the attributes of an element.
   *
   * @param location The location of the element whose attributes are to be set.
   * @param attributes The attributes that the element should have.
   * @return The document mutation.
   */
  public static Nindo replaceAttributes(int location, Attributes attributes) {
    List<MutationComponent> mutationList = new ArrayList<MutationComponent>(2);
    if (location > 0) {
      mutationList.add(new Skip(location));
    }
    mutationList.add(new ReplaceAttributes(attributes));
    return new Nindo(mutationList);
  }

  /**
   * Creates a document mutation that sets an attribute of an element.
   *
   * @param location The location of the element whose attribute is to be set.
   * @param name The name of the attribute to set.
   * @param value The value to which to set the attribute.
   * @return The document mutation.
   */
  public static Nindo setAttribute(int location, String name, String value) {
    List<MutationComponent> mutationList = new ArrayList<MutationComponent>(2);
    if (location > 0) {
      mutationList.add(new Skip(location));
    }
    mutationList.add(new UpdateAttributes(Collections.singletonMap(name, value)));
    return new Nindo(mutationList);
  }

  /**
   * Creates a document mutation that removes an attribute of an element.
   *
   * @param location The location of the element whose attribute is to be
   *        removed.
   * @param name The name of the attribute to remove.
   * @return The document mutation.
   */
  public static Nindo removeAttribute(int location, String name) {
    List<MutationComponent> mutationList = new ArrayList<MutationComponent>(2);
    if (location > 0) {
      mutationList.add(new Skip(location));
    }
    mutationList.add(new UpdateAttributes(Collections.singletonMap(name, (String)null)));
    return new Nindo(mutationList);
  }

  /**
   * Creates a document mutation that sets an annotation over a range.
   *
   * @param start The location of the start of the range on which the annotation
   *        is to be set.
   * @param end The location of the end of the range on which the annotation is
   *        to be set.
   * @param key The annotation key.
   * @param value The annotation value.
   * @return The document mutation.
   */
  public static Nindo setAnnotation(int start, int end, String key, String value) {
    List<MutationComponent> mutationList = new ArrayList<MutationComponent>(0);
    if (start != end) {
      Preconditions.checkPositionIndexes(start, end, Integer.MAX_VALUE);
      if (start > 0) {
        mutationList.add(new Skip(start));
      }
      mutationList.add(new StartAnnotation(key, value));
      mutationList.add(new Skip(end - start));
      mutationList.add(new EndAnnotation(key));
    }
    return new Nindo(mutationList);
  }

  static boolean printing = false;
  /**
   * Applies this document mutation to the given document.
   *
   * @param document The document on which to apply the document mutation.
   */
  public void apply(NindoCursor document) {
    document.begin();
    if (!printing) {
      printing = true;
      //System.out.println(this);
      printing = false;
    }
    int i = 0;
    for (MutationComponent mutationComponent : mutationList) {
      i++;
      if (!printing) {
        //System.out.println(i + "/" + mutationList.size() + " - " + mutationComponent);
      }
      mutationComponent.apply(document);
    }
    document.finish();
  }

//  /**
//   * @return the number of components in this mutation.
//   */
//  public int getComponentSize() {
//    return mutationList.size();
//  }

  @Override
  public String toString() {

    final StringBuilder builder = new StringBuilder();

    apply(new NindoCursor() {
      public void begin() {
        builder.append("{");
      }

      public void finish() {
        builder.append("}");
      }

      public void skip(int skipSize) {
        builder.append("__" + skipSize + "; ");
      }

      public void characters(String characters) {
        builder.append("++" + toLiteral(characters) + "; ");
      }

      public void elementStart(String tagName, Attributes attributes) {
        builder.append("<< " + tagName + " " + attributes + "; ");
      }

      public void elementEnd() {
        builder.append(">>; ");
      }

      public void deleteCharacters(int deletionSize) {
        builder.append("-- " + deletionSize + "; ");
      }

      public void deleteElementStart() {
        builder.append("x<; ");
      }

      public void deleteElementEnd() {
        builder.append("x>; ");
      }

      public void replaceAttributes(Attributes attributes) {
        builder.append("s@ " + attributes + "; ");
      }

      public void updateAttributes(Map<String, String> attributes) {
        builder.append("u@ " + attributes + "; ");
      }

      public void startAnnotation(String key, String value) {
        builder.append("(( " + key + "=" + (value == null ? "null" : toLiteral(value)) + "; ");
      }

      public void endAnnotation(String key) {
        builder.append(")) " + key + "; ");
      }

      private String toLiteral(String string) {
        return "\"" + string.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
      }

      @Override
      public String toString() {
        return builder.toString();
      }
    });
    return builder.toString();
  }

  public static Nindo fromDocOp(final DocOp docOp, boolean removeTrailingSkip) {
    final Builder b = new Builder();
    docOp.apply(new DocOpCursor() {

      @Override
      public void deleteCharacters(String chars) {
        b.deleteCharacters(chars.length());
      }

      @Override
      public void deleteElementEnd() {
        b.deleteElementEnd();
      }

      @Override
      public void deleteElementStart(String type, Attributes attrs) {
        b.deleteElementStart();
      }

      @Override
      public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
        b.replaceAttributes(newAttrs);
      }

      @Override
      public void retain(int itemCount) {
        b.skip(itemCount);
      }

      @Override
      public void updateAttributes(AttributesUpdate attrUpdate) {
        Map<String, String> updates = new HashMap<String, String>();
        for (int i = 0; i < attrUpdate.changeSize(); i++) {
          updates.put(attrUpdate.getChangeKey(i), attrUpdate.getNewValue(i));
        }
        b.updateAttributes(updates);
      }

      @Override
      public void annotationBoundary(AnnotationBoundaryMap map) {
        for (int i = 0; i < map.endSize(); i++) {
          b.endAnnotation(map.getEndKey(i));
        }
        for (int i = 0; i < map.changeSize(); i++) {
          b.startAnnotation(map.getChangeKey(i), map.getNewValue(i));
        }
      }

      @Override
      public void characters(String chars) {
        b.characters(chars);
      }

      @Override
      public void elementEnd() {
        b.elementEnd();
      }

      @Override
      public void elementStart(String type, Attributes attrs) {
        b.elementStart(type, attrs);
      }
    });

    return removeTrailingSkip ? b.buildWithoutTrailingSkip() : b.build();
  }
}
