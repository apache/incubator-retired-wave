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

package org.waveprotocol.wave.model.testing;

import org.waveprotocol.wave.model.document.bootstrap.BootstrapDocument;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.automaton.AutomatonDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ValidationResult;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationMap;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.operation.impl.DocOpValidator;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.Parameters.AnnotationOption;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generates random document operations based on a document.  They can be
 * valid or invalid, depending on parameters.
 */
public final class RandomDocOpGenerator {

  /**
   * Random number generator interface, to avoid the dependency on java.util.Random,
   * which would prevent the use of this class with GWT.
   */
  public interface RandomProvider {
    /** @returns a pseudorandom non-negative integer smaller than upperBound */
    int nextInt(int upperBound);

    /** @returns a pseudorandom boolean */
    boolean nextBoolean();
  }

  private RandomDocOpGenerator() {}

  /** Parameters for random DocOp generation. */
  public static final class Parameters {

    /**
     * An annotation key with the corresponding list of value alternatives.
     */
    public static final class AnnotationOption {
      final String key;
      final List<String> valueAlternatives;

      public AnnotationOption(String key, List<String> valueAlternatives) {
        Preconditions.checkNotNull(key, "key must not be null");
        Preconditions.checkNotNull(valueAlternatives, "valueAlternatives must not be null");
        this.key = key;
        this.valueAlternatives = valueAlternatives;
      }

      public String getKey() {
        return key;
      }

      public String randomValue(RandomProvider r) {
        return randomElement(r, valueAlternatives);
      }
    }

    int maxOpeningComponents = 16;
    int maxInsertLength = 10;
    int maxDeleteLength = 5;
    boolean valid = true;
    // only relevant when producing invalid ops.
    int maxSkipAfterEnd = 5;

    // We use lists here instead of sets to have an explicit fixed ordering,
    // which helps reproducibility when generating pseudo-random operations.
    // SortedSets would also work for this, but then we'd have to make
    // AnnotationOptions comparable, which is more work.

    List<String> elementTypes = Collections.unmodifiableList(Arrays.asList(
        "body", "line", "input",
        "image", "caption", "br"// "gadget",
        ));
    List<String> attributeNames = Collections.unmodifiableList(Arrays.asList(
        "_t", "t", "i", "attachment",
        "style", "blipId", "state", "url", "fontWeight", "fontStyle", "invalid_dummy"));
    // TODO: We should make attributeValues dependent on attributeNames (and perhaps on element
    // types) so that we can randomly insert chess gadgets with a valid state and inline images
    // with a proper attachment spec.
    //
    // updateAttributes will only generate attribute removals if null is in this list.
    List<String> attributeValues = Collections.unmodifiableList(Arrays.asList(
        null, "title", "li",
        "h1", "h2", "h3", "h4", "",
        "0", "1", "2", "3", "4", "5", "114", "9817"));

    List<AnnotationOption> annotationOptions = Collections.unmodifiableList(
        Arrays.asList(
            new AnnotationOption("a", Arrays.asList(null, "1", "2")),
            new AnnotationOption("b", Arrays.asList(null, "1")),
            new AnnotationOption("c", Arrays.asList(null, "1"))
        ));

    public static final List<AnnotationOption> RENDERABLE_ANNOTATION_OPTIONS =
        Collections.unmodifiableList(Arrays.asList(
            new AnnotationOption("link/auto",
                Arrays.asList(null,
                    "http://www.youtube.com/watch?v=NBplLTBBmiA&feature=hd",
                    "http://code.google.com/p/wave-protocols/issues/entry")),
            new AnnotationOption("style/fontWeight", Arrays.asList(null, "bold")),
            new AnnotationOption("style/textDecoration", Arrays.asList(null, "underline"))
        ));


    public List<String> attributeValues() {
      return Collections.unmodifiableList(Arrays.asList("title", "li", "h1", "h2", "h3", "h4", "",
          "0", "1", "2", "3", "4", "5", "114", "9817"));
    }


    public Parameters() {
    }

    public int getMaxOpeningComponents() {
      return maxOpeningComponents;
    }

    /**
     * @return the maxInsertLength
     */
    public int getMaxInsertLength() {
      return maxInsertLength;
    }

    /**
     * @return the maxDeleteLength
     */
    public int getMaxDeleteLength() {
      return maxDeleteLength;
    }

    /**
     * @return the annotationOptions
     */
    public List<AnnotationOption> getAnnotationOptions() {
      return Collections.unmodifiableList(annotationOptions);
    }

    public Parameters setMaxOpeningComponents(int maxOpeningComponents) {
      this.maxOpeningComponents = maxOpeningComponents;
      return this;
    }

    /**
     * @param maxInsertLength the maxInsertLength to set
     */
    public Parameters setMaxInsertLength(int maxInsertLength) {
      this.maxInsertLength = maxInsertLength;
      return this;
    }

    /**
     * @param maxDeleteLength the maxDeleteLength to set
     */
    public Parameters setMaxDeleteLength(int maxDeleteLength) {
      this.maxDeleteLength = maxDeleteLength;
      return this;
    }

    /**
     * @param annotationOptions the annotationOptions to set
     */
    public Parameters setAnnotationOptions(List<AnnotationOption> annotationOptions) {
      this.annotationOptions = annotationOptions;
      return this;
    }

    // Gotta love auto-generated javadoc.
    /**
     * @return the valid
     */
    public boolean getValidity() {
      return valid;
    }

    /**
     * @param valid the valid to set
     */
    public Parameters setValidity(boolean valid) {
      this.valid = valid;
      return this;
    }

    public int getMaxSkipAfterEnd() {
      return maxSkipAfterEnd;
    }

    public Parameters setMaxSkipBeyondEnd(int maxSkipAfterEnd) {
      this.maxSkipAfterEnd = maxSkipAfterEnd;
      return this;
    }

    /**
     * Returns the list of keys from annotationOptions.
     */
    public List<String> getAnnotationKeys() {
      List<String> keys = new ArrayList<String>(annotationOptions.size());
      for (AnnotationOption o : annotationOptions) {
        keys.add(o.key);
      }
      return Collections.unmodifiableList(keys);
    }

    public List<String> getElementTypes() {
      return elementTypes;
    }

    public Parameters setElementTypes(List<String> elementTypes) {
      this.elementTypes = elementTypes;
      return this;
    }

    public List<String> getAttributeNames() {
      return attributeNames;
    }

    public Parameters setAttributeNames(List<String> attributeNames) {
      Preconditions.checkArgument(
          new HashSet<String>(attributeNames).size() == attributeNames.size(),
          "duplicate attribute name");
      this.attributeNames = attributeNames;
      return this;
    }

    public List<String> getAttributeValues() {
      return attributeValues;
    }

    public Parameters setAttributeValues(List<String> attributeValues) {
      this.attributeValues = attributeValues;
      return this;
    }

  }

  private static <T> T randomElement(RandomProvider r, List<T> l) {
    return l.get(r.nextInt(l.size()));
  }

  private static int randomIntFromRange(RandomProvider r, int min, int limit) {
    assert 0 <= min; // not really a precondition, but true in our case
    assert min < limit;

    int x = r.nextInt(limit - min) + min;
    assert min <= x;
    assert x < limit;
    return x;
  }

  private static <T> void swap(ArrayList<T> a, int i, int j) {
    T temp = a.get(i);
    a.set(i, a.get(j));
    a.set(j, temp);
  }

  private static void shuffle(RandomProvider r, ArrayList<?> a) {
    int N = a.size();
    for (int i = 0; i < N; i++) {
      int j = randomIntFromRange(r, i, N);
      swap(a, i, j);
    }
  }


  private interface Mapper<I, O> {
    O map(I in);
  }

  private static <I, O> O pickRandomNonNullMappedElement(RandomProvider r, List<I> in,
      Mapper<I, O> mapper) {
    List<I> list = new ArrayList<I>(in);
    while (!list.isEmpty()) {
      int index = randomIntFromRange(r, 0, list.size());
      O value = mapper.map(list.get(index));
      if (value != null) {
        return value;
      }
      // Remove element efficiently by swapping in an element from the end.
      list.set(index, list.get(list.size() - 1));
      list.remove(list.size() - 1);
    }
    return null;
  }


  private static class Generator {

    abstract class RandomizerOperationComponent {
      abstract ValidationResult check(DocOpAutomaton a, ViolationCollector v);
      abstract void apply(DocOpAutomaton a);
      abstract void output(DocOpCursor c);
      boolean isAnnotationBoundary() { return false; }
    }

    enum Stage {
      // all components are permitted
      S1_UNRESTRICTED,
      // if deletion stack and insertion stack are empty, permit nothing (go to next stage).
      // while deletion stack is nonempty, permit annotation boundaries, deleteCharacters,
      // deleteElementStarts and deleteElementEnds.  Must move on to next stage as soon as
      // deletion stack becomes empty.
      // while insertion stack is nonempty, permit elementEnds.
      S2_CLOSE_STRUCTURE,
      // if annotations are open, close them
      S3_CLOSE_ANNOTATIONS,
      // if not at end of document, assert invalidity and skip to end of document.
      S4_SKIP_TO_END;
    }

    abstract class RandomOperationComponentGenerator {
      // returns null if it couldn't generate a matching component
      abstract RandomizerOperationComponent generate(DocOpAutomaton a, boolean valid, Stage stage);
    }

    class SkipGenerator extends RandomOperationComponentGenerator {
      @SuppressWarnings("fallthrough")
      @Override
      RandomizerOperationComponent generate(DocOpAutomaton a, boolean valid, Stage stage) {
        final int distance;
        switch (stage) {
          case S1_UNRESTRICTED:
            int maxDistance = a.maxRetainItemCount();
            if (maxDistance == 0) {
              return null;
            }
            if (a.checkRetain(1, null).isIllFormed()) {
              return null;
            }
            if (valid) {
              if (!a.checkRetain(1, null).isValid()) {
                return null;
              }
              int d = randomIntFromRange(r, 1, maxDistance + 1);
              while (!a.checkRetain(d, null).isValid()) {
                d--;
                assert d > 0;
              }
              distance = d;
              assert a.checkRetain(distance, null).isValid();
            } else {
              distance = randomIntFromRange(r, maxDistance + 1, maxDistance + p.getMaxSkipAfterEnd());
              assert a.checkRetain(distance, null) == ValidationResult.INVALID_DOCUMENT;
            }
            break;
          case S2_CLOSE_STRUCTURE:
          case S3_CLOSE_ANNOTATIONS:
            return null;
          case S4_SKIP_TO_END:
            if (!valid) {
              throw new RuntimeException("Not implemented");
            }
            switch (a.checkRetain(1, null)) {
              case INVALID_DOCUMENT:
                assert a.checkFinish(null).isValid();
                return null;
              case VALID:
                distance = a.maxRetainItemCount();
                assert distance > 0;
                assert !a.checkFinish(null).isValid();
                break;
              case INVALID_SCHEMA:
              case ILL_FORMED: assert false;
              default:
                throw new RuntimeException("Unexpected validation result");
            }
            break;
          default:
            throw new RuntimeException("Unexpected stage: " + stage);
        }
        return new RandomizerOperationComponent() {
          @Override
          public ValidationResult check(DocOpAutomaton a, ViolationCollector v) {
            return a.checkRetain(distance, v);
          }

          @Override
          public void apply(DocOpAutomaton a) {
            a.doRetain(distance);
          }

          @Override
          public void output(DocOpCursor c) {
            c.retain(distance);
          }

          @Override
          public String toString() {
            return "Skip(" + distance + ")";
          }
        };
      }
    }

    class CharactersGenerator extends RandomOperationComponentGenerator {
      @Override
      RandomizerOperationComponent generate(DocOpAutomaton a, boolean valid, Stage stage) {
        if (stage != Stage.S1_UNRESTRICTED) {
          return null;
        }
        ValidationResult v = a.checkCharacters("a", null);
        if (v.isIllFormed()) {
          return null;
        }
        int count;
        if (valid) {
          if (!v.isValid()) {
            return null;
          }
          // TODO: implement this once we have size limits.
          int max = p.getMaxInsertLength();
          if (max == 0) {
            return null;
          }
          count = randomIntFromRange(r, 1, max + 1);
        } else {
          if (v.isValid()) {
            // Exceed length of document (if p.maxInsertLength allows it).
            int max = p.getMaxInsertLength();
            // TODO: implement this once we have size limits.
            //count = randomIntFromRange(r, min, max + 1);
            return null;
          } else {
            count = randomIntFromRange(r, 1, p.getMaxInsertLength());
          }
        }
        StringBuilder sb = new StringBuilder();
        assert count > 0;
        char startChar = r.nextBoolean() ? 'a' : 'A';
        for (int i = 0; i < count; i++) {
          if (i <= 26) {
            sb.append((char) (startChar + i));
          } else {
            sb.append('.');
          }
        }
        final String s = sb.toString();
        return new RandomizerOperationComponent() {
          @Override
          public ValidationResult check(DocOpAutomaton a, ViolationCollector v) {
            return a.checkCharacters(s, v);
          }

          @Override
          public void apply(DocOpAutomaton a) {
            a.doCharacters(s);
          }

          @Override
          public void output(DocOpCursor c) {
            c.characters(s);
          }

          @Override
          public String toString() {
            return "Characters(" + s + ")";
          }
        };
      }
    }

    class DeleteCharactersGenerator extends RandomOperationComponentGenerator {
      @Override
      RandomizerOperationComponent generate(DocOpAutomaton a, boolean valid, Stage stage) {
        if (stage != Stage.S1_UNRESTRICTED && (stage != Stage.S2_CLOSE_STRUCTURE || a.deletionStackComplexityMeasure() == 0)) {
          return null;
        }
        // TODO: In stage 2, this should perhaps be less random about how many characters
        // it deletes.  Alternatively, skip in stage 4 could be more random.
        int nextChar = a.nextChar(0);
        if (nextChar == -1 ||
            a.checkDeleteCharacters("" + ((char) nextChar), null).isIllFormed()) {
          return null;
        }
        final int count;
        if (valid) {
          int max = Math.min(a.maxCharactersToDelete(), p.getMaxDeleteLength());
          if (max == 0) {
            return null;
          }
          count = randomIntFromRange(r, 1, max + 1);
        } else {
          int max = p.getMaxDeleteLength();
          int min = a.maxCharactersToDelete() + 1;
          if (min > max) {
            return null;
          }
          count = randomIntFromRange(r, min, max + 1);
        }
        // TODO: implement invalid case, both by right char but wrong
        // annotations (if possible) and wrong char.
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < count; i++) {
          int c = a.nextChar(i);
          assert c != -1;
          b.append((char) c);
          if (valid && !a.checkDeleteCharacters(b.toString(), null).isValid()) {
            b.deleteCharAt(b.length() - 1);
            break;
          }
        }
        if (b.length() == 0) {
          // TODO: simplify this method
          return null;
        }
        final String s = b.toString();
        RandomizerOperationComponent c = new RandomizerOperationComponent() {
          @Override
          public ValidationResult check(DocOpAutomaton a, ViolationCollector v) {
            return a.checkDeleteCharacters(s, v);
          }

          @Override
          public void apply(DocOpAutomaton a) {
            a.doDeleteCharacters(s);
          }

          @Override
          public void output(DocOpCursor c) {
            c.deleteCharacters(s);
          }

          @Override
          public String toString() {
            return "DeleteCharacters(" + s + ")";
          }
        };
        if (c.check(a, null).isValid() != valid) {
          return null;
        } else {
          return c;
        }
      }
    }

    interface AttributesUpdateChecker {
      ValidationResult check(AttributesUpdate u);
    }

    // returns null on failure
    AttributesUpdate generateRandomAttributesUpdate(final boolean valid,
        final Attributes oldAttributes,
        final AttributesUpdateChecker checker) {
      AttributesUpdate accu = new AttributesUpdateImpl();
      if (valid && !checker.check(accu).isValid()
          || !valid && checker.check(accu).isIllFormed()) {
        return null;
      }
      if (!valid) {
        // If we want an invalid component, and it's not already invalid without
        // any attributes, make it invalid by adding an invalid attribute first.
        if (checker.check(accu).isValid()) {
          assert accu.changeSize() == 0;
          accu = pickRandomNonNullMappedElement(r,
              p.getAttributeNames(), new Mapper<String, AttributesUpdate>() {
            @Override
            public AttributesUpdate map(final String name) {
              return pickRandomNonNullMappedElement(r, p.getAttributeValues(),
                  new Mapper<String, AttributesUpdate> () {
                @Override
                public AttributesUpdate map(String value) {
                  AttributesUpdate b = new AttributesUpdateImpl(name,
                      oldAttributes.get(name), value);
                  switch (checker.check(b)) {
                    case ILL_FORMED:
                      return null;
                    case INVALID_DOCUMENT:
                    case INVALID_SCHEMA:
                      return b;
                    case VALID:
                      return null;
                    default:
                      throw new RuntimeException("Unexpected validation result");
                  }
                }
              });
            }
          });
          if (accu == null) {
            return null;
          }
        }
        assert !checker.check(accu).isValid();
        // Flip a coin and terminate if the number of attributes was really
        // supposed to be zero.
        if (r.nextBoolean()) {
          return accu;
        }
      }
      while (r.nextBoolean()) {
        final AttributesUpdate finalAccu = accu;
        AttributesUpdate newAccu = pickRandomNonNullMappedElement(r,
            p.getAttributeNames(), new Mapper<String, AttributesUpdate>() {
          @Override
          public AttributesUpdate map(final String name) {
            for (int i = 0; i < finalAccu.changeSize(); i++) {
              if (finalAccu.getChangeKey(i).equals(name)) {
                return null;
              }
            }
            return pickRandomNonNullMappedElement(r, p.getAttributeValues(),
                new Mapper<String, AttributesUpdate>() {
              @Override
              public AttributesUpdate map(String value) {
                AttributesUpdate b = finalAccu.composeWith(new AttributesUpdateImpl(name,
                    oldAttributes.get(name), value));
                assert b != finalAccu; // assert non-destructiveness
                ValidationResult v = checker.check(b);
                if (valid && !v.isValid() || !valid && v.isIllFormed()) {
                  return null;
                } else {
                  return b;
                }
              }
            });
          }
        });
        if (newAccu == null) {
          return accu;
        }
        accu = newAccu;
      }
      return accu;
    }

    class ElementStartGenerator extends RandomOperationComponentGenerator {
      @Override
      RandomizerOperationComponent generate(DocOpAutomaton a, boolean valid, Stage stage) {
        switch (stage) {
          case S1_UNRESTRICTED:
            return generate(a, valid);
          case S2_CLOSE_STRUCTURE:
          case S3_CLOSE_ANNOTATIONS:
          case S4_SKIP_TO_END:
            return null;
          default:
            throw new RuntimeException("Unexpected stage: " + stage);
        }
      }

      RandomizerOperationComponent generateGivenTag(final DocOpAutomaton a, final boolean valid,
          final String tag) {
        {
          ValidationResult v = a.checkElementStart(tag, Attributes.EMPTY_MAP, null);
          if (valid && !v.isValid() || !valid && v.isIllFormed()) {
            // Early exit if we can't build an element start with this tag.
            return null;
          }
        }

        AttributesUpdate u = generateRandomAttributesUpdate(valid, Attributes.EMPTY_MAP,
            new AttributesUpdateChecker() {
              @Override
              public ValidationResult check(AttributesUpdate u) {
                Attributes attrs = Attributes.EMPTY_MAP.updateWith(u);
                return a.checkElementStart(tag, attrs, null);
              }
            });
        if (u == null) {
          return null;
        } else {
          final Attributes attributes = Attributes.EMPTY_MAP.updateWith(u);
          return new RandomizerOperationComponent() {
            @Override
            public ValidationResult check(DocOpAutomaton a, ViolationCollector v) {
              return a.checkElementStart(tag, attributes, v);
            }

            @Override
            public void apply(DocOpAutomaton a) {
              a.doElementStart(tag, attributes);
            }

            @Override
            public void output(DocOpCursor c) {
              c.elementStart(tag, attributes);
            }

            @Override
            public String toString() {
              return "ElementStart(" + tag + ", " + attributes + ")";
            }
          };
        }
      }

      RandomizerOperationComponent generate(final DocOpAutomaton a, final boolean valid) {
        return pickRandomNonNullMappedElement(r, p.getElementTypes(),
            new Mapper<String, RandomizerOperationComponent>() {
              @Override
              public RandomizerOperationComponent map(final String tag) {
                return generateGivenTag(a, valid, tag);
              }
            });
      }
    }

    abstract class RandomConstantOperationComponentGenerator
        extends RandomOperationComponentGenerator {
      abstract ValidationResult check(DocOpAutomaton a, ViolationCollector v);
      abstract void apply(DocOpAutomaton a);
      abstract void output(DocOpCursor c);

      RandomizerOperationComponent generate(DocOpAutomaton a, boolean valid) {
        switch (check(a, null)) {
          case ILL_FORMED:
            return null;
          case VALID:
            if (!valid) {
              return null;
            }
            break;
          case INVALID_DOCUMENT:
            if (valid) {
              return null;
            }
            break;
          case INVALID_SCHEMA:
            if (valid) {
              return null;
            }
            break;
          default:
            throw new RuntimeException("Unexpected validation result");
        }
        return new RandomizerOperationComponent() {
          @Override
          public ValidationResult check(DocOpAutomaton a, ViolationCollector v) {
            return RandomConstantOperationComponentGenerator.this.check(a, v);
          }

          @Override
          public void apply(DocOpAutomaton a) {
            RandomConstantOperationComponentGenerator.this.apply(a);
          }

          @Override
          public void output(DocOpCursor c) {
            RandomConstantOperationComponentGenerator.this.output(c);
          }

          @Override
          public String toString() {
            return "Constant component from "
                + RandomConstantOperationComponentGenerator.this.getClass().getName();
          }
        };
      }
    }

    class ElementEndGenerator extends RandomConstantOperationComponentGenerator {
      @Override
      RandomizerOperationComponent generate(DocOpAutomaton a, boolean valid, Stage stage) {
        switch (stage) {
          case S1_UNRESTRICTED:
            return generate(a, valid);
          case S2_CLOSE_STRUCTURE:
            if (a.insertionStackComplexityMeasure() == 0) {
              return null;
            }
            return generate(a, valid);
          case S3_CLOSE_ANNOTATIONS:
          case S4_SKIP_TO_END:
            return null;
          default:
            throw new RuntimeException("Unexpected stage: " + stage);
        }
      }

      @Override
      ValidationResult check(DocOpAutomaton a, ViolationCollector v) {
        return a.checkElementEnd(v);
      }

      @Override
      void apply(DocOpAutomaton a) {
        a.doElementEnd();
      }

      @Override
      void output(DocOpCursor c) {
        c.elementEnd();
      }
    }

    class DeleteElementStartGenerator extends RandomOperationComponentGenerator {
      @Override
      RandomizerOperationComponent generate(DocOpAutomaton a, boolean valid, Stage stage) {
        switch (stage) {
          case S1_UNRESTRICTED:
            return generate(a, valid);
          case S2_CLOSE_STRUCTURE:
            if (a.deletionStackComplexityMeasure() == 0) {
              return null;
            }
            return generate(a, valid);
          case S3_CLOSE_ANNOTATIONS:
          case S4_SKIP_TO_END:
            return null;
          default:
            throw new RuntimeException("Unexpected stage: " + stage);
        }
      }

      RandomizerOperationComponent generate(final DocOpAutomaton a, final boolean valid) {
        final String tag = a.currentElementStartTag();
        final Attributes oldAttrs = a.currentElementStartAttributes();
        if (tag == null) {
          assert oldAttrs == null;
          return null;
        }
        assert oldAttrs != null;
        switch (a.checkDeleteElementStart(tag, oldAttrs, null)) {
          case ILL_FORMED:
          case INVALID_DOCUMENT: // TODO: bring back generating invalid ops
          case INVALID_SCHEMA:
            return null;
          case VALID:
            return new RandomizerOperationComponent() {
              @Override
              public ValidationResult check(DocOpAutomaton a, ViolationCollector v) {
                return a.checkDeleteElementStart(tag, oldAttrs, v);
              }

              @Override
              public void apply(DocOpAutomaton a) {
                a.doDeleteElementStart(tag, oldAttrs);
              }

              @Override
              public void output(DocOpCursor c) {
                c.deleteElementStart(tag, oldAttrs);
              }
            };
          default:
            throw new RuntimeException("Unexpected validation result");
        }
      }
    }

    class DeleteElementEndGenerator extends RandomConstantOperationComponentGenerator {
      @Override
      void apply(DocOpAutomaton a) {
        a.doDeleteElementEnd();
      }

      @Override
      ValidationResult check(DocOpAutomaton a, ViolationCollector v) {
        return a.checkDeleteElementEnd(v);
      }

      @Override
      void output(DocOpCursor c) {
        c.deleteElementEnd();
      }

      @Override
      RandomizerOperationComponent generate(DocOpAutomaton a, boolean valid, Stage stage) {
        switch (stage) {
          case S1_UNRESTRICTED:
            return generate(a, valid);
          case S2_CLOSE_STRUCTURE:
            if (a.deletionStackComplexityMeasure() == 0) {
              return null;
            }
            return generate(a, valid);
          case S3_CLOSE_ANNOTATIONS:
          case S4_SKIP_TO_END:
            return null;
          default:
            throw new RuntimeException("Unexpected stage: " + stage);
        }
      }
    }


    class ReplaceAttributesGenerator extends RandomOperationComponentGenerator {
      @Override
      RandomizerOperationComponent generate(final DocOpAutomaton a, boolean valid, Stage stage) {
        if (stage != Stage.S1_UNRESTRICTED) {
          return null;
        }
        final Attributes oldAttrs = a.currentElementStartAttributes();
        if (oldAttrs == null) {
          if (valid) {
            return null;
          }
        }
        if (!valid) {
          // TODO: bring this back.
          // several cases: invalid because of wrong old attributes, or invalid
          // because of schema violation of new attributes, or because no
          // element start here
          throw new RuntimeException("Not implemented");
        }
        AttributesUpdate u = generateRandomAttributesUpdate(valid,
            oldAttrs, new AttributesUpdateChecker() {
          @Override
          public ValidationResult check(AttributesUpdate u) {
            return a.checkReplaceAttributes(oldAttrs, oldAttrs.updateWith(u), null);
          }
        });

        if (u == null) {
          return null;
        }

        final Attributes newAttrs = oldAttrs.updateWith(u);
        return new RandomizerOperationComponent() {
          @Override
          public void apply(DocOpAutomaton a) {
            a.doReplaceAttributes(oldAttrs, newAttrs);
          }

          @Override
          public ValidationResult check(DocOpAutomaton a, ViolationCollector v) {
            return a.checkReplaceAttributes(oldAttrs, newAttrs, v);
          }

          @Override
          public void output(DocOpCursor c) {
            c.replaceAttributes(oldAttrs, newAttrs);
          }

          @Override
          public String toString() {
            return "ReplaceAttributes(" + oldAttrs + ", " + newAttrs + ")";
          }
        };
      }
    }

    class UpdateAttributesGenerator extends RandomOperationComponentGenerator {
      @Override
      RandomizerOperationComponent generate(final DocOpAutomaton a, boolean valid, Stage stage) {
        if (stage != Stage.S1_UNRESTRICTED) {
          return null;
        }
        final Attributes oldAttrs = a.currentElementStartAttributes();
        if (oldAttrs == null) {
          if (valid) {
            return null;
          }
        }
        if (!valid) {
          // TODO: bring this back.
          // several cases: invalid because of wrong old attributes, or invalid
          // because of schema violation of new attributes, or because no
          // element start here
          throw new RuntimeException("Not implemented");
        }
        final AttributesUpdate update = generateRandomAttributesUpdate(valid,
            oldAttrs, new AttributesUpdateChecker() {
          @Override
          public ValidationResult check(AttributesUpdate u) {
            return a.checkUpdateAttributes(u, null);
          }
        });

        if (update == null) {
          return null;
        }

        return new RandomizerOperationComponent() {
          @Override
          public void apply(DocOpAutomaton a) {
            a.doUpdateAttributes(update);
          }

          @Override
          public ValidationResult check(DocOpAutomaton a, ViolationCollector v) {
            return a.checkUpdateAttributes(update, v);
          }

          @Override
          public void output(DocOpCursor c) {
            c.updateAttributes(update);
          }

          @Override
          public String toString() {
            return "UpdateAttributes(" + update + ")";
          }
        };
      }
    }

    interface RunnableWithException<E extends Throwable> {
      void run() throws E;
    }

    class AnnotationBoundaryGenerator extends RandomOperationComponentGenerator {
      @Override
      RandomizerOperationComponent generate(DocOpAutomaton a, boolean valid, Stage stage) {
        switch (stage) {
          case S1_UNRESTRICTED:
          case S2_CLOSE_STRUCTURE:
            return generateWithLookahead(a, valid, stage);
          case S3_CLOSE_ANNOTATIONS:
            assert valid;
            return generateClosing(a);
          case S4_SKIP_TO_END:
            return null;
          default:
            throw new RuntimeException("Unexpected stage: " + stage);
        }
      }

      RandomizerOperationComponent generate(final AnnotationBoundaryMapImpl map) {
        return new RandomizerOperationComponent() {
          @Override
          void apply(DocOpAutomaton a) {
            a.doAnnotationBoundary(map);
          }

          @Override
          ValidationResult check(DocOpAutomaton a, ViolationCollector v) {
            return a.checkAnnotationBoundary(map, v);
          }

          @Override
          void output(DocOpCursor c) {
            c.annotationBoundary(map);
          }

          @Override
          boolean isAnnotationBoundary() { return true; }

          @Override
          public String toString() {
            return "AnnotationBoundary(" + map + ")";
          }
        };
      }

      String[] toArray(ArrayList<String> a) {
        return a.toArray(new String[0]);
      }

      RandomizerOperationComponent generateClosing(DocOpAutomaton a) {
        if (a.openAnnotations().isEmpty()) {
          return null;
        }
        ArrayList<String> l = new ArrayList<String>(a.openAnnotations());
        Collections.sort(l);
        AnnotationBoundaryMapImpl map =
          AnnotationBoundaryMapImpl.builder().initializationEnd(
              toArray(l)).build();
        assert !a.checkAnnotationBoundary(map, null).isIllFormed();
        return generate(map);
      }

      class Result extends Exception {
        final RandomizerOperationComponent component;
        Result(RandomizerOperationComponent component) {
          this.component = component;
        }
      }

      class StringNullComparator implements Comparator<String> {
        @Override
        public int compare(String a, String b) {
          if (a == b) {
            return 0;
          }
          if (a == null) {
            return -1;
          }
          if (b == null) {
            return 1;
          }
          return a.compareTo(b);
        }
      }

      RandomizerOperationComponent generateWithLookahead(final DocOpAutomaton a, boolean valid,
          final Stage stage) {
        {
          ValidationResult r = a.checkAnnotationBoundary(
              AnnotationBoundaryMapImpl.builder().updateValues("a", null, "1").build(), null);
          assert r.isIllFormed() || r.isValid();
          if (r.isIllFormed()) {
            return null;
          }
        }
        Set<String> keySet = new TreeSet<String>(new StringNullComparator());
        for (AnnotationOption o : p.getAnnotationOptions()) {
          keySet.add(o.key);
        }
        keySet.addAll(a.currentAnnotations().keySet());
        keySet.addAll(a.inheritedAnnotations().keySet());
        final ArrayList<String> keys = new ArrayList<String>(keySet);

        Collections.sort(keys);

        // For every key, either pick it, or don't (choice point, recursively
        // explore both options).

        // For each key, one option is to end that key if it currently is in
        // openAnnotations().
        // Another option is not to end that key: In that case, given the key,
        // the valid old values are those from annotationOptions and
        // those from currentAnnotations() (for deletions) and
        // those from inheritedAnnotations() (for insertions);
        // the valid new values are those from annotationOptions and
        // those from inheritedAnnotations() (for deletion).
        //
        // Given the full map, we need to check if the component is valid, then
        // temporarily apply it to find out if there is any valid component
        // to follow up with.

        final RunnableWithException<Result> chooseKeys = new RunnableWithException<Result>() {

          ArrayList<String> keysToEnd = new ArrayList<String>();
          ArrayList<String> changeKeys = new ArrayList<String>();
          ArrayList<String> changeOldValues = new ArrayList<String>();
          ArrayList<String> changeNewValues = new ArrayList<String>();

          void tryThisOption() throws Result {
            AnnotationBoundaryMapImpl map = AnnotationBoundaryMapImpl.builder()
                .initializationEnd(toArray(keysToEnd))
                .updateValues(toArray(changeKeys), toArray(changeOldValues),
                    toArray(changeNewValues)).build();
            final RandomizerOperationComponent component = generate(map);
            DocOpAutomaton temp = new DocOpAutomaton(a);
            ViolationCollector v = new ViolationCollector();
            component.check(temp, v);
            assert !component.check(temp, null).isIllFormed();
            component.apply(temp);
//            System.err.println("begin lookahead for " + map);
            RandomizerOperationComponent followup = pickComponent(temp, stage);
            if (followup != null) {
//              System.err.println("end lookahead, success");
              throw new Result(component);
            }
//            System.err.println("end lookahead, failed");
          }

          void removeLastMaybe(ArrayList<String> l, int lastItemIndex) {
            assert lastItemIndex == l.size() || lastItemIndex == l.size() - 1;
            if (lastItemIndex == l.size() - 1) {
              l.remove(lastItemIndex);
            }
          }

          void take(int nextKeyIndex, String key) throws Result {
            assert key != null;
            if (a.openAnnotations().contains(key)) {
              int oldSize = keysToEnd.size();
              try {
                keysToEnd.add(key);
                nextKey(nextKeyIndex);
              } finally {
                removeLastMaybe(keysToEnd, oldSize);
              }
            }

            Set<String> valueSet = new TreeSet<String>(new StringNullComparator());
            for (AnnotationOption o : p.getAnnotationOptions()) {
              if (key.equals(o.key)) {
                valueSet.addAll(o.valueAlternatives);
              }
            }
            AnnotationMap inheritedAnnotations = a.inheritedAnnotations();
            if (inheritedAnnotations.containsKey(key)) {
              valueSet.add(inheritedAnnotations.get(key));
            } else {
              valueSet.add(null);
            }
            ArrayList<String> newValues = new ArrayList<String>(valueSet);
            AnnotationMap currentAnnotations = a.currentAnnotations();
            if (currentAnnotations.containsKey(key)) {
              valueSet.add(currentAnnotations.get(key));
            } else {
              valueSet.add(null);
            }
            ArrayList<String> oldValues = new ArrayList<String>(valueSet);

            shuffle(r, oldValues);
            shuffle(r, newValues);

            for (String oldValue : oldValues) {
              for (String newValue : newValues) {
                assert changeKeys.size() == changeOldValues.size();
                assert changeKeys.size() == changeNewValues.size();
                int oldSize = changeKeys.size();
                try {
                  changeKeys.add(key);
                  changeOldValues.add(oldValue);
                  changeNewValues.add(newValue);
                  nextKey(nextKeyIndex);
                } finally {
                  removeLastMaybe(changeNewValues, oldSize);
                  removeLastMaybe(changeOldValues, oldSize);
                  removeLastMaybe(changeKeys, oldSize);
                  assert changeKeys.size() == changeOldValues.size();
                  assert changeKeys.size() == changeNewValues.size();
                }
              }
            }
          }

          void nextKey(int nextKeyIndex) throws Result {
            if (nextKeyIndex >= keys.size()) {
              tryThisOption();
              return;
            }
            String key = keys.get(nextKeyIndex);
            boolean take = r.nextBoolean();
            if (take) {
              take(nextKeyIndex + 1, key);
              nextKey(nextKeyIndex + 1);
            } else {
              nextKey(nextKeyIndex + 1);
              take(nextKeyIndex + 1, key);
            }
          }

          @Override
          public void run() throws Result {
            nextKey(0);
          }
        };

        try {
          chooseKeys.run();
        } catch (Result e) {
          return e.component;
        }
        return null;
      }
    }

    private static boolean equal(Object a, Object b) {
      return a == null ? b == null : a.equals(b);
    }

    final RandomProvider r;
    final Parameters p;
    final AutomatonDocument doc;

    Generator(RandomProvider r, Parameters p, AutomatonDocument doc) {
      this.r = r;
      this.p = p;
      this.doc = doc;
    }

    final List<RandomOperationComponentGenerator> componentGenerators =
      Arrays.asList(
          new AnnotationBoundaryGenerator(),
          new CharactersGenerator(),
          new ElementStartGenerator(),
          new ElementEndGenerator(),
          new SkipGenerator(),
          new DeleteCharactersGenerator(),
          new DeleteElementStartGenerator(),
          new DeleteElementEndGenerator(),
          new ReplaceAttributesGenerator(),
          new UpdateAttributesGenerator()
          );

    DocOp generate() {
      DocOpAutomaton a = new DocOpAutomaton(doc, DocumentSchema.NO_SCHEMA_CONSTRAINTS);
      DocOpBuffer b = new DocOpBuffer();
      generate1(a, b);
      return b.finish();
    }

    RandomizerOperationComponent pickComponent(final DocOpAutomaton a, final Stage stage) {
//      System.err.println("stage: " + stage);
      RandomizerOperationComponent component = pickRandomNonNullMappedElement(r,
          componentGenerators,
          new Mapper<RandomOperationComponentGenerator, RandomizerOperationComponent>() {
        @Override
        public RandomizerOperationComponent map(RandomOperationComponentGenerator g) {
//          System.err.println("trying generator " + g);
          RandomizerOperationComponent c = g.generate(a, true, stage);
          if (c != null) {
            assert c.check(a, null).isValid();
          }
          return c;
        }
      });
//      System.err.println("picked " + component);
      return component;
    }

    RandomizerOperationComponent generate2(DocOpAutomaton a, DocOpCursor output, Stage stage) {
      RandomizerOperationComponent component = pickComponent(a, stage);
      assert component != null;
      component.apply(a);
      component.output(output);
      return component;
    }

    void generate1(DocOpAutomaton a, DocOpCursor output) {
      if (!p.getValidity()) {
        throw new RuntimeException("generation of invalid operations not supported yet");
      }
      int desiredNumComponents = randomIntFromRange(r, 0, p.getMaxOpeningComponents());
      int numComponentsPicked = 0;
      while (numComponentsPicked < desiredNumComponents) {
        RandomizerOperationComponent component = generate2(a, output, Stage.S1_UNRESTRICTED);
        if (!component.isAnnotationBoundary()) {
          numComponentsPicked++;
        }
      }

      while (a.deletionStackComplexityMeasure() > 0) {
        generate2(a, output, Stage.S2_CLOSE_STRUCTURE);
      }

      while (a.insertionStackComplexityMeasure() > 0) {
        int before = a.insertionStackComplexityMeasure();
        generate2(a, output, Stage.S2_CLOSE_STRUCTURE);
        assert a.insertionStackComplexityMeasure() <= before;
      }

      if (!a.openAnnotations().isEmpty()) {
        generate2(a, output, Stage.S3_CLOSE_ANNOTATIONS);
        assert a.openAnnotations().isEmpty();
      }

      if (a.maxRetainItemCount() > 0) {
        generate2(a, output, Stage.S4_SKIP_TO_END);
        assert a.maxRetainItemCount() == 0;
      }
    }
  }

  /**
   * Returns a randomly-generated document operation based on the given document,
   * parameters, and schema.
   */
  public static DocOp generate(RandomProvider r, Parameters p, AutomatonDocument doc) {
    DocOp op = new Generator(r, p, doc).generate();
    ViolationCollector v = new ViolationCollector();
    DocOpValidator.validate(v, null, doc, op);
    assert !v.isIllFormed();
    assert p.getValidity() == v.isValid();
    return op;
  }


  /**
   * Stand-alone main() for quick experimentation.
   */
  public static void main(String[] args) throws OperationException {
    BootstrapDocument initialDoc = new BootstrapDocument();
    initialDoc.consume(new DocInitializationBuilder()
        .elementStart("blip", Attributes.EMPTY_MAP)
        .elementStart("p", Attributes.EMPTY_MAP)
        .characters("abc")
        .elementEnd()
        .elementEnd().build());

    Parameters p = new Parameters();

    p.setMaxOpeningComponents(10);

    RandomProvider r = RandomProviderImpl.ofSeed(2538);
    for (int i = 0; i < 200; i++) {
      BootstrapDocument doc = new BootstrapDocument();
      doc.consume(initialDoc.asOperation());
      for (int j = 0; j < 20; j++) {
        System.err.println("i=" + i + ", j=" + j);
        System.err.println("old: " + DocOpUtil.toXmlString(doc.asOperation()));
        System.err.println("old: " + DocOpUtil.toConciseString(doc.asOperation()));
        DocOp op = generate(r, p, doc);
        System.err.println("op:  " + DocOpUtil.toConciseString(op));
        doc.consume(op);
        System.err.println("new: " + DocOpUtil.toConciseString(doc.asOperation()));
        System.err.println("new: " + DocOpUtil.toXmlString(doc.asOperation()));
        if (!DocOpValidator.validate(null, DocumentSchema.NO_SCHEMA_CONSTRAINTS,
            doc.asOperation()).isValid()) {
          throw new RuntimeException("doc not valid");
        }
      }
    }
  }

}
