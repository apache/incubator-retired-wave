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

package org.waveprotocol.wave.client.editor.extract;

import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations.AnnotationResolver;
import org.waveprotocol.wave.model.document.util.DocProviders;

import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.AnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.AnnotationFamily;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.BiasDirection;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.ContentType;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.DefaultAnnotationBehaviour;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;
import org.waveprotocol.wave.model.document.util.AnnotationRegistryImpl;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;
import org.waveprotocol.wave.model.util.ValueUtils;

import java.util.List;

/**
 * Tests a number of cases for the logic of when to replace / inherit annotations while pasting.
 *
 * NOTE(patcoleman):
 *   a) Rich text paste is all that's done so far, so that's all that's currently tested
 *   b) The paste extractor doesn't currently respect right cursor bias.
 */

public class PasteAnnotationLogicTest extends TestCase {
  private AnnotationRegistry REGISTRY = null;
  private static final String LINK_KEY = "link";
  private static final String STYLE_KEY = "style";
  private static final String SPELL_KEY = "spell";

  /** Sets up a state with anything needed by all the tests. */
  private AnnotationRegistry initTestAnnotationBehaviour(AnnotationRegistry root) {
    LineContainers.setTopLevelContainerTagname("body");
    /**
     * Create a registry that has three types of behaviours:
     *   - style-like (no bias change, inherits inside)
     *   - link-like (bias away, with priority overriding containers).
     *   - spell-like (metadata, never inherits)
     */
    AnnotationRegistry registry = root.createExtension();
    registry.registerBehaviour(STYLE_KEY, new DefaultAnnotationBehaviour(AnnotationFamily.CONTENT));
    registry.registerBehaviour(LINK_KEY, new AnnotationBehaviour() {
      public BiasDirection getBias(StringMap<Object> left, StringMap<Object> right,
          CursorDirection cursor) {
        assert left.containsKey(LINK_KEY) && right.containsKey(LINK_KEY);
        return left.get(LINK_KEY) == null ? BiasDirection.LEFT : BiasDirection.RIGHT; // away
      }
      public double getPriority() {
        return 10.0; // higher than default
      }
      public InheritDirection replace(StringMap<Object> inside, StringMap<Object> outside,
          ContentType type) {
        return InheritDirection.INSIDE;
      }
      public AnnotationFamily getAnnotationFamily() {
        return AnnotationFamily.CONTENT;
      }
    });
    registry.registerBehaviour(SPELL_KEY, AnnotationBehaviour.REPLACE_NEITHER);
    useCaretAnnotations();
    return registry;
  }
  private final PasteAnnotationLogic<Node, Element, Text> initDefault() {
    REGISTRY = initTestAnnotationBehaviour(AnnotationRegistryImpl.ROOT);
    return new PasteAnnotationLogic<Node, Element, Text>(doc, REGISTRY);
  }

  //
  // TESTS
  //

  // strip keys - paste into middle of normal region
  public void testStripKeysInUnstyled() {
    useDocument("<line/>abcd");
    PasteAnnotationLogic<Node, Element, Text> logic = initDefault();
    final StringMap<String> built = CollectionUtils.createStringMap();
    Nindo.Builder builder = new Nindo.Builder() {
      @Override public void startAnnotation(String key, String value) {
        built.put(key, value);
      }
    };
    final StringMap<String> changed =
        logic.stripKeys(doc, 5, BiasDirection.LEFT, ContentType.RICH_TEXT, builder);
    checkEquals(built, changed);
  }

  // strip keys - paste into middle of bold region
  public void testStripKeysInStyled() {
    useDocument("<line/>abcd", "style:S:4:6");
    PasteAnnotationLogic<Node, Element, Text> logic = initDefault();
    final StringMap<String> built = CollectionUtils.createStringMap();
    Nindo.Builder builder = new Nindo.Builder() {
      @Override public void startAnnotation(String key, String value) {
        built.put(key, value);
      }
    };
    final StringMap<String> changed =
        logic.stripKeys(doc, 5, BiasDirection.LEFT, ContentType.RICH_TEXT, builder);
    checkEquals(built, changed, "style", null);
  }

  // strip keys - paste onto divider with normal on right, bold on left
  public void testStripKeysBetweenUnstyledStyled() {
    useDocument("<line/>abcd", "style:S:5:7");
    PasteAnnotationLogic<Node, Element, Text> logic = initDefault();
    final StringMap<String> built = CollectionUtils.createStringMap();
    Nindo.Builder builder = new Nindo.Builder() {
      @Override public void startAnnotation(String key, String value) {
        built.put(key, value);
      }
    };
    final StringMap<String> changed =
        logic.stripKeys(doc, 5, BiasDirection.LEFT, ContentType.RICH_TEXT, builder);
    checkEquals(built, changed);
  }

  // strip keys - paste onto divider with normal on right, bold on left
  public void testStripKeysBetweenStyledUnstyled() {
    useDocument("<line/>abcd", "style:S:3:5", "spell:S:2:6");
    PasteAnnotationLogic<Node, Element, Text> logic = initDefault();
    final StringMap<String> built = CollectionUtils.createStringMap();
    Nindo.Builder builder = new Nindo.Builder() {
      @Override public void startAnnotation(String key, String value) {
        built.put(key, value);
      }
    };
    final StringMap<String> changed =
        logic.stripKeys(doc, 5, BiasDirection.LEFT, ContentType.RICH_TEXT, builder);
    checkEquals(built, changed, "style", null, "spell", null);
  }

  // unstrip keys - cover everything (both skipped and unskipped keys)
  public void testUnstripKeys() {
    useDocument("<line/>abcd", "link:L:4:6", "spell:S:5:7");
    PasteAnnotationLogic<Node, Element, Text> logic = initDefault();
    final StringSet ended = CollectionUtils.createStringSet();
    Nindo.Builder builder = new Nindo.Builder() {
      @Override public void endAnnotation(String key) {
        ended.add(key);
      }
    };

    ReadableStringSet keyCheck = CollectionUtils.newStringSet("A", "B", "C");
    ReadableStringSet toIgnore = CollectionUtils.newStringSet("B", "D");
    logic.unstripKeys(builder, keyCheck, toIgnore);

    assertEquals(2, ended.countEntries());
    assertTrue(ended.contains("A"));
    assertFalse(ended.contains("B"));
    assertTrue(ended.contains("C"));
    assertFalse(ended.contains("D"));
  }

  // extract normalized - check simple case with link and spell
  public void testExtractNormalizedAnnotation() {
    // a[b{c]d} where [] = link, {} = spell annotation, normalizing over abcd
    useDocument("<line/>abcd", "link:L:4:6", "spell:S:5:7", "style:Q:1:2");
    PasteAnnotationLogic<Node, Element, Text> logic = initDefault();
    List<RangedAnnotation<String>> extracted =
        logic.extractNormalizedAnnotation(doc.locate(3), doc.locate(7));
    assertEquals(4, extracted.size());
    checkRangedAnnotation(extracted, 0, 4, STYLE_KEY, null); // no styles
    checkRangedAnnotation(extracted, 0, 1, LINK_KEY, null);  // before linked bit
    checkRangedAnnotation(extracted, 1, 3, LINK_KEY, "L");   // linked bit
    checkRangedAnnotation(extracted, 3, 4, LINK_KEY, null);  // after linked bit
  }

  //
  // Utilities
  //

  // CHeck whether A = B, and both contain mappings (key = 2n, value = 2n+1);
  private void checkEquals(ReadableStringMap<String> A, ReadableStringMap<String> B, String... C) {
    StringMap<String> shouldBe = CollectionUtils.createStringMap();
    for (int i = 0; i < C.length; i += 2) {
      shouldBe.put(C[i], C[i+1]);
    }
    checkMapEquals(A, shouldBe);
    checkMapEquals(B, shouldBe);
  }

  private void checkMapEquals(ReadableStringMap<String> A, final ReadableStringMap<String> B) {
    assertEquals(B.countEntries(), A.countEntries());
    A.each(new ProcV<String>() {
      public void apply(String key, String value) {
        assertTrue(B.containsKey(key));
        assertTrue(ValueUtils.equal(value, B.get(key)));
      }
    });
  }

  private void checkRangedAnnotation(List<RangedAnnotation<String>> extracted,
      int from, int to, String key, String value) {
    boolean hasAnnotation = true;
    for (RangedAnnotation<String> annotation : extracted) {
      if (annotation.start() == from && annotation.end() == to &&
          ValueUtils.equal(key, annotation.key()) && ValueUtils.equal(value, annotation.value())) {
        return;
      }
    }
    fail();
  }


  ///
  /// Convenience methods for a context, copied from EditorAnnotationUtilTest.
  /// TODO(patcoleman): make it so this doesn't have to be copied everywhere.
  ///

  // Parameters to make generating the EditorContext wrappers easier
  private int start;
  private int end;
  private MutableDocument<Node, Element, Text> doc;
  private CaretAnnotations caret;

  // Resolve the annotations by left-biasing within the document, stores it in doc.
  AnnotationResolver annotationResolver = new AnnotationResolver() {
    public String getAnnotation(String key) {
      return start == 0 || doc.getAnnotation(start - 1, key) == null ?
          null : doc.getAnnotation(start - 1, key).toString();
    }
  };

  // Converts the document and annotations into a CMutableDocument and stores in doc
  private void useDocument(String docContent, String... annotations) {
    MutableDocument<Node, Element, Text> mutable =
      DocProviders.MOJO.parse("<body>" + docContent + "</body>");

    // format: key:value:start:end
    for (String value : annotations) {
      String[] vals = value.split(":");
      mutable.setAnnotation(Integer.parseInt(vals[2]), Integer.parseInt(vals[3]), vals[0], vals[1]);
    }
    doc = mutable;
  }

  // Converts the annotations passed into a CaretAnnotations bundle and stores in caret
  private void useCaretAnnotations(String... annotations) {
    CaretAnnotations localCaret = new CaretAnnotations();
    for (String value : annotations) {
      String[] dup = value.split(":");
      localCaret.setAnnotation(dup[0], dup[1]);
    }
    caret = localCaret;
    caret.setAnnotationResolver(annotationResolver);
  }

  // Stores the start and end of the selection range to simulate.
  private void useSelection(int begin, int finish) {
    start = begin;
    end = finish;
  }
}

// PASTE TESTS to write?
  // plaintext paste of normal in bold
  // richtext paste of normal in bold

  // richtext paste of normal on a blank line between bold and normal
  // richtext paste of bold on a blank line between bold and normal
  // richtext paste of normal + bold on a blank line between bold and normal

  // richtext paste of normal at the end of a line after normal
  // richtext paste of bold + normal at the end of a line after normal
  // richtext paste of normal at the end of a line after bold
  // richtext paste of normal + bold at the end of a line after bold
