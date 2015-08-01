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

package org.waveprotocol.wave.model.document.operation.impl;


import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.bootstrap.BootstrapDocument;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ValidationResult;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.util.ImmutableStateMap.Attribute;
import org.waveprotocol.wave.model.operation.OperationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ohler@google.com (Christian Ohler)
 */

public final class DocOpValidatorTest extends TestCase {
  public static final DocumentSchema TEST_CONSTRAINTS =
    new DocumentSchema() {
      @Override
      public boolean permitsAttribute(String type, String attributeName) {
        return false;
      }

      @Override
      public boolean permitsAttribute(String type, String attributeName, String attributeValue) {
        return false;
      }

      @Override
      public boolean permitsChild(String parent, String child) {
        return null == parent && "body".equals(child)
            || "body".equals(parent) && "line".equals(child)
            || "body".equals(parent) && "otherchildofbody".equals(child);
      }

      @Override
      public PermittedCharacters permittedCharacters(String type) {
        if ("body".equals(type)) {
          return PermittedCharacters.BLIP_TEXT;
        }
        return PermittedCharacters.NONE;
      }

      @Override
      public List<String> getRequiredInitialChildren(String typeOrNull) {
        if ("body".equals(typeOrNull)) {
          return Collections.singletonList("line");
        }
        return Collections.emptyList();
      }
    };

  // TODO(ohler): These tests are by far not enough.  Need more coverage.

  abstract class TestData {
    abstract boolean build(DocInitializationCursor d, DocOpCursor m);
    DocumentSchema getSchemaConstraints() {
      return DocumentSchema.NO_SCHEMA_CONSTRAINTS;
    }
  }

  public void test1() throws OperationException {
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        return true;
      }
    });
  }

  public void test2() throws OperationException {
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        m.elementStart("<", Attributes.EMPTY_MAP);
        m.elementEnd();
        return false;
      }
    });
  }

  public void test3() throws OperationException {
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        m.elementStart("blip", Attributes.EMPTY_MAP);
        m.elementEnd();
        return true;
      }
    });
  }

  public void test4() throws OperationException {
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        m.elementStart("p", Attributes.EMPTY_MAP);
        m.elementEnd();
        return false;
      }
      @Override
      DocumentSchema getSchemaConstraints() {
        return TEST_CONSTRAINTS;
      }
    });
  }

  public void test5() throws OperationException {
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        m.elementStart("blip", Attributes.EMPTY_MAP);
        m.elementEnd();
        return true;
      }
    });
  }

  public void testMaxSkipDistanceDoesntAssert() throws OperationException {
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.elementStart("blip", Attributes.EMPTY_MAP);
        d.elementEnd();
        m.retain(3);
        m.retain(1);
        return false;
      }
    });
  }

  public void testUnsortedAttributes() throws OperationException {
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        m.elementStart("blip",
            AttributesImpl.fromSortedAttributesUnchecked(
                Arrays.asList(new Attribute[] {
                    new Attribute("a", "1"),
                    new Attribute("b", "1")
                })));
        m.elementEnd();
        return true;
      }
    });
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        m.elementStart("blip",
            AttributesImpl.fromSortedAttributesUnchecked(
                Arrays.asList(new Attribute[] {
                    new Attribute("b", "1"),
                    new Attribute("a", "1")
                })));
        m.elementEnd();
        return false;
      }
    });
  }

  // We want to be able to test with AnnotationBoundaryMaps that
  // AnnotationBoundaryMapImpl's builder doesn't let us construct
  // because of its error checking.  So we need our own implementation.
  private static class DumbAnnotationBoundaryMap implements AnnotationBoundaryMap {
    final String[] endKeys;
    final String[] changeTriplets;

    DumbAnnotationBoundaryMap(String[] endKeys, String[] changeTriplets) {
      this.endKeys = endKeys;
      this.changeTriplets = changeTriplets;
    }

    @Override
    public int endSize() {
      return endKeys.length;
    }

    @Override
    public int changeSize() {
      return changeTriplets.length / 3;
    }

    @Override
    public String getEndKey(int endIndex) {
      return endKeys[endIndex];
    }

    @Override
    public String getChangeKey(int changeIndex) {
      return changeTriplets[changeIndex * 3];
    }

    @Override
    public String getOldValue(int changeIndex) {
      return changeTriplets[changeIndex * 3 + 1];
    }

    @Override
    public String getNewValue(int changeIndex) {
      return changeTriplets[changeIndex * 3 + 2];
    }

  }

  public void testDuplicateAnnotationKeys() throws OperationException {
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.characters("ab");
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder()
              .updateValues("a", null, "1")
              .build());
        m.retain(1);
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder()
              .initializationEnd("a")
              .updateValues("b", null, "2")
              .build());
        m.retain(1);
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder()
              .initializationEnd("b")
              .build());
        return true;
      }
    });
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.characters("ab");
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder()
              .updateValues("a", null, "1")
              .build());
        m.retain(1);
        m.annotationBoundary(new DumbAnnotationBoundaryMap(
            new String[] { "a" },
            new String[] { "a", null, "2" }));
        m.retain(1);
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder()
              .initializationEnd("a")
              .build());
        return false;
      }
    });
  }


  public void testUnsortedAnnotationKeys() throws OperationException {
    // all ok
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.characters("ab");
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder()
              .updateValues("a", null, "1", "b", null, "2")
              .build());
        m.retain(1);
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder()
              .initializationEnd("a")
              .updateValues("b", null, "2", "c", null, "3")
              .build());
        m.retain(1);
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder()
              .initializationEnd("b", "c")
              .build());
        return true;
      }
    });
    // change keys in wrong order
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.characters("ab");
        m.annotationBoundary(new DumbAnnotationBoundaryMap(
            new String[] {},
            new String[] { "b", null, "2", "a", null, "1" }));
        m.retain(1);
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder()
              .initializationEnd("a")
              .updateValues("b", null, "2", "c", null, "3")
              .build());
        m.retain(1);
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder()
              .initializationEnd("b", "c")
              .build());
        return false;
      }
    });
    // end keys in wrong order
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.characters("ab");
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder()
              .updateValues("a", null, "1", "b", null, "2")
              .build());
        m.retain(1);
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder()
              .initializationEnd("a")
              .updateValues("b", null, "2", "c", null, "3")
              .build());
        m.retain(1);
        m.annotationBoundary(new DumbAnnotationBoundaryMap(
            new String[] { "c", "b" },
            new String[] {}));
        return false;
      }
    });
  }


  public void testDeletionAnnotationsAreRelative() throws OperationException {
    // annotations are not needed if previous character has the same annotations
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationValues("a", "1").build());
        d.characters("a");
        d.characters("b");
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        m.retain(1);
        m.deleteCharacters("b");
        return true;
      }
    });
    // but may be specified
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationValues("a", "1").build());
        d.characters("a");
        d.characters("b");
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        m.retain(1);
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().updateValues("a", "1", "1").build());
        m.deleteCharacters("b");
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        return true;
      }
    });
    // even for null values
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.characters("a");
        d.characters("b");
        m.retain(1);
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationValues("a", null).build());
        m.deleteCharacters("b");
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        return true;
      }
    });
  }

  public void testDeletionAnnotationsAreRelative2() throws OperationException {
    // annotations are needed if previous character has different annotations
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationValues("a", "1").build());
        d.characters("a");
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        m.deleteCharacters("a");
        return false;
      }
    });
    // annotations are needed if previous character has different annotations
    // (positive case)
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationValues("a", "1").build());
        d.characters("a");
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().updateValues("a", "1", null).build());
        m.deleteCharacters("a");
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        return true;
      }
    });
    // this is even true when deleting multiple items in sequence
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationValues("a", "1").build());
        d.characters("ab");
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().updateValues("a", "1", null).build());
        m.deleteCharacters("a");
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        m.deleteCharacters("b");
        return false;
      }
    });
    // this is even true when deleting multiple items in sequence
    // (positive case)
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationValues("a", "1").build());
        d.characters("ab");
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().updateValues("a", "1", null).build());
        m.deleteCharacters("a");
        m.deleteCharacters("b");
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        return true;
      }
    });
  }

  public void testDeletionAnnotationsAreRelative3() throws OperationException {
    // annotations are needed if previous character has more annotations
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationValues("a", "1").build());
        d.characters("a");
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        d.characters("b");
        m.retain(1);
        m.deleteCharacters("b");
        return false;
      }
    });
    // annotations are needed if previous character has more annotations
    doTest(new TestData() {
      @Override
      public boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationValues("a", "1").build());
        d.characters("a");
        d.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        d.characters("b");
        m.retain(1);
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().updateValues("a", null, "1").build());
        m.deleteCharacters("b");
        m.annotationBoundary(
            AnnotationBoundaryMapImpl.builder().initializationEnd("a").build());
        return true;
      }
    });
  }

  public void testRequiredTag() throws OperationException {
    // ok
    doTest(new TestData() {
      @Override
      DocumentSchema getSchemaConstraints() {
        return TEST_CONSTRAINTS;
      }
      @Override
      boolean build(DocInitializationCursor d, DocOpCursor m) {
        m.elementStart("body", Attributes.EMPTY_MAP);
        m.elementStart("line", Attributes.EMPTY_MAP);
        m.elementEnd();
        m.elementEnd();
        return true;
      }});
    // missing required element
    doTest(new TestData() {
      @Override
      DocumentSchema getSchemaConstraints() {
        return TEST_CONSTRAINTS;
      }
      @Override
      boolean build(DocInitializationCursor d, DocOpCursor m) {
        m.elementStart("body", Attributes.EMPTY_MAP);
        m.elementEnd();
        return false;
      }});
    // characters before required element
    doTest(new TestData() {
      @Override
      DocumentSchema getSchemaConstraints() {
        return TEST_CONSTRAINTS;
      }
      @Override
      boolean build(DocInitializationCursor d, DocOpCursor m) {
        m.elementStart("body", Attributes.EMPTY_MAP);
        m.characters("a");
        m.elementStart("line", Attributes.EMPTY_MAP);
        m.elementEnd();
        m.elementEnd();
        return false;
      }});
    // different element before required element
    doTest(new TestData() {
      @Override
      DocumentSchema getSchemaConstraints() {
        return TEST_CONSTRAINTS;
      }
      @Override
      boolean build(DocInitializationCursor d, DocOpCursor m) {
        m.elementStart("body", Attributes.EMPTY_MAP);
        m.elementStart("otherchildofbody", Attributes.EMPTY_MAP);
        m.elementEnd();
        m.elementStart("line", Attributes.EMPTY_MAP);
        m.elementEnd();
        m.elementEnd();
        return false;
      }});
  }

  public void testDeletingRequiredTag() throws OperationException {
    // ok
    doTest(new TestData() {
      @Override
      DocumentSchema getSchemaConstraints() {
        return TEST_CONSTRAINTS;
      }
      @Override
      boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.elementStart("body", Attributes.EMPTY_MAP);
        d.elementStart("line", Attributes.EMPTY_MAP);
        d.elementEnd();
        d.elementEnd();
        m.deleteElementStart("body", Attributes.EMPTY_MAP);
        m.deleteElementStart("line", Attributes.EMPTY_MAP);
        m.deleteElementEnd();
        m.deleteElementEnd();
        return true;
      }});
    // missing required element
    doTest(new TestData() {
      @Override
      DocumentSchema getSchemaConstraints() {
        return TEST_CONSTRAINTS;
      }
      @Override
      boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.elementStart("body", Attributes.EMPTY_MAP);
        d.elementStart("line", Attributes.EMPTY_MAP);
        d.elementEnd();
        d.elementEnd();
        m.retain(1);
        m.deleteElementStart("line", Attributes.EMPTY_MAP);
        m.deleteElementEnd();
        m.retain(1);
        return false;
      }});
  }

  public void testInsertingAroundRequiredTag() throws OperationException {
    // ok to insert after
    doTest(new TestData() {
      @Override
      DocumentSchema getSchemaConstraints() {
        return TEST_CONSTRAINTS;
      }
      @Override
      boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.elementStart("body", Attributes.EMPTY_MAP);
        d.elementStart("line", Attributes.EMPTY_MAP);
        d.elementEnd();
        d.elementEnd();
        m.retain(3);
        m.elementStart("line", Attributes.EMPTY_MAP);
        m.elementEnd();
        m.retain(1);
        return true;
      }});
    // not ok to insert before
    doTest(new TestData() {
      @Override
      DocumentSchema getSchemaConstraints() {
        return TEST_CONSTRAINTS;
      }
      @Override
      boolean build(DocInitializationCursor d, DocOpCursor m) {
        d.elementStart("body", Attributes.EMPTY_MAP);
        d.elementStart("line", Attributes.EMPTY_MAP);
        d.elementEnd();
        d.elementEnd();
        m.retain(1);
        m.characters("a");
        m.retain(3);
        return false;
      }});
  }

  public void testCharAtPastEnd() throws OperationException {
    // this should not crash
    doTest(new TestData() {
      @Override
      boolean build(DocInitializationCursor d, DocOpCursor m) {
        m.retain(1);
        m.deleteCharacters("ab");
        return false;
      }});
  }

  void doTest(TestData t) throws OperationException {
    DocOpBuffer d = new DocOpBuffer();
    DocOpBuffer m = new DocOpBuffer();
    boolean expected = t.build(d, m);

    BootstrapDocument doc = new BootstrapDocument();

    // initialize document
    doc.consume(d.finishUnchecked());

    // check whether m would apply
    ViolationCollector v = new ViolationCollector();
    ValidationResult result =
        DocOpValidator.validate(v, t.getSchemaConstraints(), doc, m.finishUnchecked());

    try {
      assertEquals(expected, v.isValid());
      assertEquals(result, v.getValidationResult());
    } catch (AssertionFailedError e) {
      System.err.println("test data:");
      System.err.println(DocOpUtil.toConciseString(d.finish()));
      System.err.println(DocOpUtil.toConciseString(m.finish()));
      System.err.println("violations:");
      v.printDescriptions(System.err);
      throw e;
    }
  }

}
