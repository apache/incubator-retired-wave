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

import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.Nindo.NindoCursor;
import org.waveprotocol.wave.model.document.operation.NindoAutomaton;
import org.waveprotocol.wave.model.document.operation.NindoValidator;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ValidationResult;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.Parameters;
import org.waveprotocol.wave.model.testing.RandomDocOpGenerator.RandomProvider;
import org.waveprotocol.wave.model.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Generates random document operations based on a document.  They can be
 * valid or invalid, depending on parameters.
 *
 * @author ohler@google.com (Christian Ohler)
 */
@SuppressWarnings("unchecked") // TODO(ohler, danilatos): declare generics properly
public final class RandomNindoGenerator {

  private RandomNindoGenerator() {}

  private static <T> T randomElement(RandomProvider r, List<T> l) {
    return l.get(r.nextInt(l.size()));
  }

  private static <T> T randomElement(RandomProvider r, Set<T> s) {
    int n = randomIntFromRange(r, 0, s.size());
    for (T e : s) {
      if (n == 0) {
        return e;
      }
      n--;
    }
    assert false;
    throw new RuntimeException("fell off end of loop");
  }

  private static int randomIntFromRange(RandomProvider r, int min, int limit) {
    assert 0 <= min; // not really a precondition, but true in our case
    assert min < limit;

    int x = r.nextInt(limit - min) + min;
    assert min <= x;
    assert x < limit;
    return x;
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

    interface RandomizerMutationComponent {
      ValidationResult check(ViolationCollector v);
      void apply();
    }

    abstract class RandomMutationComponentGenerator {
      abstract RandomizerMutationComponent generate(boolean valid);
      // 0 means this transition will never be needed to complete an operation
      // (e.g., skip or setAttributes)
      // -1 means this transition may be needed to complete an operation but
      // increases the size of the structural stack (e.g. deleteElementStart)
      // -2 means this transition may be needed to complete an operation but
      // does not change the size of the structural stack (e.g. deleteCharacters)
      // -3 means this transition may be needed to complete an operation and
      // decreases the size of the structural stack (e.g. deleteElementEnd)
      abstract int potential();
    }

    class SkipGenerator extends RandomMutationComponentGenerator {
      @Override
      public int potential() {
        return 0;
      }

      @Override
      RandomizerMutationComponent generate(boolean valid) {
        int maxDistance = a.maxSkipDistance();
        if (maxDistance == 0) {
          return null;
        }
        if (a.checkSkip(1, null).isIllFormed()) {
          return null;
        }
        final int distance;
        if (valid) {
          distance = randomIntFromRange(r, 1, maxDistance + 1);
          assert a.checkSkip(distance, null).isValid();
        } else {
          distance = randomIntFromRange(r, maxDistance + 1, maxDistance + p.getMaxSkipAfterEnd());
          assert a.checkSkip(distance, null) == ValidationResult.INVALID_DOCUMENT;
        }
        return new RandomizerMutationComponent() {
          @Override
          public ValidationResult check(ViolationCollector v) {
            return a.checkSkip(distance, v);
          }

          @Override
          public void apply() {
            a.doSkip(distance);
            targetDoc.skip(distance);
          }
        };
      }
    }

    class CharactersGenerator extends RandomMutationComponentGenerator {
      @Override
      public int potential() {
        return 0;
      }

      @Override
      RandomizerMutationComponent generate(boolean valid) {
        ValidationResult v = a.checkCharacters("a", null);
        if (v.isIllFormed()) {
          return null;
        }
        int count;
        if (valid) {
          if (!v.isValid()) {
            return null;
          }
          int max = Math.min(a.maxLengthIncrease(), p.getMaxInsertLength());
          if (max == 0) {
            return null;
          }
          count = randomIntFromRange(r, 1, max + 1);
        } else {
          if (v.isValid()) {
            // Exceed length of document (if p.maxInsertLength allows it).
            int max = p.getMaxInsertLength();
            int min = a.maxLengthIncrease() + 1;
            if (min > max) {
              return null;
            }
            count = randomIntFromRange(r, min, max + 1);
          } else {
            count = randomIntFromRange(r, 1, p.getMaxInsertLength());
          }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
          if (i <= 26) {
            sb.append((char) ('a' + i));
          } else {
            sb.append('.');
          }
        }
        final String s = sb.toString();
        return new RandomizerMutationComponent() {
          @Override
          public ValidationResult check(ViolationCollector v) {
            return a.checkCharacters(s, v);
          }

          @Override
          public void apply() {
            a.doCharacters(s);
            targetDoc.characters(s);
          }
        };
      }
    }

    class DeleteCharactersGenerator extends RandomMutationComponentGenerator {
      @Override
      RandomizerMutationComponent generate(boolean valid) {
        if (a.checkDeleteCharacters(1, null).isIllFormed()) {
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
        return new RandomizerMutationComponent() {
          @Override
          public ValidationResult check(ViolationCollector v) {
            return a.checkDeleteCharacters(count, v);
          }

          @Override
          public void apply() {
            a.doDeleteCharacters(count);
            targetDoc.deleteCharacters(count);
          }
        };
      }

      @Override
      public int potential() {
        return -2;
      }
    }

    interface AttributesChecker {
      ValidationResult check(Attributes attrs);
    }

    Attributes generateRandomAttributes(final boolean valid, final AttributesChecker checker) {
      Attributes attrAccu = Attributes.EMPTY_MAP;
      if (valid && !checker.check(Attributes.EMPTY_MAP).isValid()
          || !valid && checker.check(Attributes.EMPTY_MAP).isIllFormed()) {
        return null;
      }
      if (!valid) {
        // If we want an invalid component, and it's not already invalid without
        // any attributes, make it invalid by adding an invalid attribute first.
        if (checker.check(attrAccu).isValid()) {
          assert attrAccu.isEmpty();
          attrAccu = pickRandomNonNullMappedElement(r,
              p.getAttributeNames(), new Mapper<String, Attributes>() {
            @Override
            public Attributes map(final String name) {
              return pickRandomNonNullMappedElement(r, p.getAttributeValues(),
                  new Mapper<String, Attributes> () {
                @Override
                public Attributes map(String value) {
                  Attributes b = new AttributesImpl(name, value);
                  switch (checker.check(b)) {
                    case ILL_FORMED:
                      return null;
                    case INVALID_DOCUMENT:
                    case INVALID_SCHEMA:
                      return b;
                    case VALID:
                      return null;
                    default:
                      throw new RuntimeException("unexpected validation result");
                  }
                }
              });
            }
          });
          if (attrAccu == null) {
            return null;
          }
        }
        assert !checker.check(attrAccu).isValid();
        // Flip a coin and terminate if the number of attributes was really
        // supposed to be zero.
        if (r.nextBoolean()) {
          return attrAccu;
        }
      }
      while (r.nextBoolean()) {
        final Attributes finalAttrAccu = attrAccu;
        Pair<String, String> newAttr = pickRandomNonNullMappedElement(r,
            p.getAttributeNames(), new Mapper<String, Pair<String, String>>() {
          @Override
          public Pair<String, String> map(final String name) {
            if (finalAttrAccu.containsKey(name)) {
              return null;
            }
            return pickRandomNonNullMappedElement(r, p.getAttributeValues(),
                new Mapper<String, Pair<String, String>>() {
              @Override
              public Pair<String, String> map(String value) {
                Attributes b = finalAttrAccu.updateWith(
                    new AttributesUpdateImpl(name, null, value));
                assert b != finalAttrAccu; // assert non-destructiveness
                ValidationResult v = checker.check(b);
                if (valid && !v.isValid() || !valid && v.isIllFormed()) {
                  return null;
                } else {
                  return Pair.of(name, value);
                }
              }
            });
          }
        });
        if (newAttr == null) {
          return attrAccu;
        }
        attrAccu = attrAccu.updateWith(
            new AttributesUpdateImpl(newAttr.getFirst(), null, newAttr.getSecond()));
      }
      return attrAccu;
    }

    class ElementStartGenerator extends RandomMutationComponentGenerator {
      @Override
      public int potential() {
        return 0;
      }

      @Override
      RandomizerMutationComponent generate(final boolean valid) {
        Pair<String, Attributes> args = pickRandomNonNullMappedElement(r, p.getElementTypes(),
            new Mapper<String, Pair<String, Attributes>>() {
              @Override
              public Pair<String, Attributes> map(final String tag) {
                {
                  ValidationResult v = a.checkElementStart(tag, Attributes.EMPTY_MAP, null);
                  if (valid && !v.isValid() || !valid && v.isIllFormed()) {
                    // Early exit if we can't build an element start with this tag.
                    return null;
                  }
                }

                Attributes attrs = generateRandomAttributes(valid,
                    new AttributesChecker() {
                      @Override
                      public ValidationResult check(Attributes attrs) {
                        return a.checkElementStart(tag, attrs, null);
                      }
                    });
                if (attrs == null) {
                  return null;
                } else {
                  return Pair.of(tag, attrs);
                }
              }
            });
        if (args == null) {
          return null;
        }
        final String tag = args.getFirst();
        final Attributes attributes = args.getSecond();
        return new RandomizerMutationComponent() {
          @Override
          public ValidationResult check(ViolationCollector v) {
            return a.checkElementStart(tag, attributes, v);
          }

          @Override
          public void apply() {
            a.doElementStart(tag, attributes);
            targetDoc.elementStart(tag, attributes);
          }
        };
      }
    }

    abstract class RandomConstantMutationComponentGenerator
        extends RandomMutationComponentGenerator {
      abstract ValidationResult check(ViolationCollector v);
      abstract void apply();

      @Override
      RandomizerMutationComponent generate(boolean valid) {
        switch (check(null)) {
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
            throw new RuntimeException("unexpected validation result");
        }
        return new RandomizerMutationComponent() {
          @Override
          public ValidationResult check(ViolationCollector v) {
            return RandomConstantMutationComponentGenerator.this.check(v);
          }

          @Override
          public void apply() {
            RandomConstantMutationComponentGenerator.this.apply();
          }

          @Override
          public String toString() {
            return this.getClass().getName() + " from "
                + RandomConstantMutationComponentGenerator.this.getClass().getName();
          }
        };
      }
    }

    class ElementEndGenerator extends RandomConstantMutationComponentGenerator {
      @Override
      int potential() {
        return -3;
      }

      @Override
      ValidationResult check(ViolationCollector v) {
        return a.checkElementEnd(v);
      }

      @Override
      void apply() {
        a.doElementEnd();
        targetDoc.elementEnd();
      }
    }

    class DeleteElementStartGenerator extends RandomConstantMutationComponentGenerator {
      @Override
      int potential() {
        return -1;
      }

      @Override
      ValidationResult check(ViolationCollector v) {
        return a.checkDeleteElementStart(v);
      }

      @Override
      void apply() {
        a.doDeleteElementStart();
        targetDoc.deleteElementStart();
      }
    }

    class DeleteElementEndGenerator extends RandomConstantMutationComponentGenerator {
      @Override
      int potential() {
        return -3;
      }

      @Override
      ValidationResult check(ViolationCollector v) {
        return a.checkDeleteElementEnd(v);
      }

      @Override
      void apply() {
        a.doDeleteElementEnd();
        targetDoc.deleteElementEnd();
      }
    }

    abstract class AttributesOnlyRandomMutationComponentGenerator
        extends RandomMutationComponentGenerator {
      abstract ValidationResult check(Attributes attrs, ViolationCollector v);
      abstract void apply(Attributes attrs);
      @Override
      RandomizerMutationComponent generate(boolean valid) {
        final Attributes attrs = generateRandomAttributes(valid, new AttributesChecker() {
          @Override
          public ValidationResult check(Attributes attrs) {
            return AttributesOnlyRandomMutationComponentGenerator.this.check(attrs, null);
          }
        });

        if (attrs == null) {
          return null;
        }

        return new RandomizerMutationComponent() {
          @Override
          public ValidationResult check(ViolationCollector v) {
            return AttributesOnlyRandomMutationComponentGenerator.this.check(attrs, v);
          }

          @Override
          public void apply() {
            AttributesOnlyRandomMutationComponentGenerator.this.apply(attrs);
          }

          @Override
          public String toString() {
            return this.getClass().getName() + " from "
                + AttributesOnlyRandomMutationComponentGenerator.this.getClass().getName()
                + " " + attrs;
          }
        };
      }
    }

    class SetAttributesGenerator extends AttributesOnlyRandomMutationComponentGenerator {
      @Override
      int potential() {
        return 0;
      }

      @Override
      ValidationResult check(Attributes attrs, ViolationCollector v) {
        return a.checkSetAttributes(attrs, v);
      }

      @Override
      void apply(Attributes attrs) {
        a.doSetAttributes(attrs);
        targetDoc.replaceAttributes(attrs);
      }
    }

    class UpdateAttributesGenerator extends AttributesOnlyRandomMutationComponentGenerator {
      @Override
      int potential() {
        return 0;
      }

      @Override
      ValidationResult check(Attributes attrs, ViolationCollector v) {
        return a.checkUpdateAttributes(attrs, v);
      }

      @Override
      void apply(Attributes attrs) {
        a.doUpdateAttributes(attrs);
        targetDoc.updateAttributes(attrs);
      }
    }

    class StartAnnotationGenerator extends RandomMutationComponentGenerator {
      @Override
      int potential() {
        return 0;
      }

      @Override
      RandomizerMutationComponent generate(boolean valid) {
        if (!valid) {
          return null;
        }
        if (p.getAnnotationOptions().isEmpty()) {
          return null;
        }

        Parameters.AnnotationOption option = randomElement(r, p.getAnnotationOptions());
        final String key = option.getKey();
        final String value = option.randomValue(r);
        return new RandomizerMutationComponent() {
          @Override
          public ValidationResult check(ViolationCollector v) {
            return a.checkStartAnnotation(key, value, v);
          }

          @Override
          public void apply() {
            a.doStartAnnotation(key, value);
            targetDoc.startAnnotation(key, value);
          }
        };
      }
    }

    class EndAnnotationGenerator extends RandomMutationComponentGenerator {
      @Override
      int potential() {
        return -3;
      }

      @Override
      RandomizerMutationComponent generate(boolean valid) {
        if (!valid) {
          return null;
        }
        return pickRandomNonNullMappedElement(r, p.getAnnotationKeys(),
            new Mapper<String, RandomizerMutationComponent>() {
              @Override
              public RandomizerMutationComponent map(final String key) {
                switch (a.checkEndAnnotation(key, null)) {
                  case ILL_FORMED:
                    return null;
                  case VALID:
                    return new RandomizerMutationComponent() {
                      @Override
                      public ValidationResult check(ViolationCollector v) {
                        return a.checkEndAnnotation(key, v);
                      }

                      @Override
                      public void apply() {
                        a.doEndAnnotation(key);
                        targetDoc.endAnnotation(key);
                      }
                    };
                  case INVALID_DOCUMENT:
                  case INVALID_SCHEMA:
                  default:
                    throw new RuntimeException("unexpected validation result");
                }
              }
            }
          );
      }
    }

    final RandomProvider r;
    final Parameters p;
    final DocumentSchema schemaConstraints;
    @SuppressWarnings("rawtypes")
    NindoAutomaton a;
    NindoCursor targetDoc;
    final IndexedDocument<Node, Element, Text> doc;

    Generator(RandomProvider r, Parameters p, DocumentSchema s,
        IndexedDocument<Node, Element, Text> doc) {
      this.r = r;
      this.p = p;
      this.doc = doc;
      this.schemaConstraints = s;
    }

    final List<RandomMutationComponentGenerator> componentGenerators =
      Arrays.asList(new SkipGenerator(),
          new CharactersGenerator(),
          new DeleteCharactersGenerator(),
          new ElementStartGenerator(),
          new ElementEndGenerator(),
          new DeleteElementStartGenerator(),
          new DeleteElementEndGenerator(),
          new SetAttributesGenerator(),
          new UpdateAttributesGenerator(),
          new StartAnnotationGenerator(),
          new EndAnnotationGenerator()
          );

    @SuppressWarnings("rawtypes")
    Nindo generate() {
      while (true) {
        this.a = new NindoAutomaton(schemaConstraints, doc);
        Nindo.Builder b = new Nindo.Builder();
        targetDoc = b;
        boolean ok = generate1();
        if (ok) {
          return b.build();
        }
      }
    }

    boolean generate1() {
      int desiredNumComponents = randomIntFromRange(r, 0, p.getMaxOpeningComponents());
      for (int i = 0; i < desiredNumComponents; i++) {
        RandomizerMutationComponent component = pickRandomNonNullMappedElement(r,
            componentGenerators,
            new Mapper<RandomMutationComponentGenerator, RandomizerMutationComponent>() {
              @Override
              public RandomizerMutationComponent map(RandomMutationComponentGenerator g) {
                return g.generate(p.getValidity());
              }
        });
        if (component == null) {
          // This can happen e.g. if we have skipped to the end of the document, and valid
          // may be true, and there may not be any annotation options.
          break;
        }
        component.apply();
      }

      // Close all open components.
      while (a.checkFinish(null) == ValidationResult.ILL_FORMED) {
        int potential = -3 - 1;
        RandomizerMutationComponent component;
        do {
          potential++;
          final int finalPotential = potential;
          component = pickRandomNonNullMappedElement(r, componentGenerators,
              new Mapper<RandomMutationComponentGenerator, RandomizerMutationComponent>() {
            @Override
            public RandomizerMutationComponent map(RandomMutationComponentGenerator g) {
              if (g.potential() >= finalPotential) {
                return null;
              }
              return g.generate(p.getValidity());
            }
          });
        } while (potential < 0 && component == null);
        if (component == null) {
          // This can happen e.g. if we did an deleteAntiElementStart on the
          // final </p> of the blip, where there is nothing to join with.
          return false;
        }
        component.apply();
      }
      return true;
    }
  }

  /**
   * Returns a randomly-generated document mutation based on the given document,
   * parameters, and schema.
   */
  public static Nindo generate(RandomProvider r, Parameters p,
      DocumentSchema s, IndexedDocument<Node, Element, Text> doc) {
    Nindo m = new Generator(r, p, s, doc).generate();
    ViolationCollector v = NindoValidator.validate(doc, m, s);
    assert !v.isIllFormed();
    assert p.getValidity() == v.isValid();
    return m;
  }


  /**
   * Stand-alone main() for quick experimentation.
   */
  public static void main(String[] args) {
//    IndexedDocument<Node, Element, Text> doc =
//      DocProviders.POJO.parse("<body><line></line>a</body>");
//
//    Parameters p = new Parameters();
//
//    p.setMaxOpeningComponents(10);
//
//    for (int i = 0; i < 200; i++) {
//      System.out.println("i=" + i);
//      RandomProvider r = RandomProviderImpl.ofSeed(i);
//      Nindo m = generate(r, p,
//          NindoValidator.DEFAULT_BLIP_SCHEMA_CONSTRAINTS, doc);
//      System.out.print(m);
//    }
  }

}
