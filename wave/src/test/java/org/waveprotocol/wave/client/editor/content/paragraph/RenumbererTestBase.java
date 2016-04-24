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

package org.waveprotocol.wave.client.editor.content.paragraph;

import static org.waveprotocol.wave.client.editor.Editor.ROOT_HANDLER_REGISTRY;

import com.google.gwt.dom.client.Element;

import junit.framework.TestCase;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorTestingUtil;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentDocElement;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentDocument.PermanentMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.HasImplNodelets;
import org.waveprotocol.wave.client.editor.content.paragraph.OrderedListRenumberer.LevelNumbers;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.Alignment;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.Direction;
import org.waveprotocol.wave.client.scheduler.FinalTaskRunner;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.model.document.indexed.IndexedDocumentImpl;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Utilities for testing ordered list numbering.
 *
 * A bunch of methods refer to lines by "index". This is index into the
 * conceptual list of lines, so, 0 for the first line, 1 for the second line,
 * and so forth.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class RenumbererTestBase extends TestCase {

  /**
   * Simple enum for representing a style of line, that maps to the type and
   * li-style type attributes. Contains a representative sample of the types of
   * lines that could possibly have different effects on renumbering.
   */
  enum Type {
    /** No attributes */
    NONE,
    /** t=h1 */
    HEADING,
    /** t=li without listyle */
    LIST,
    /** t=li with listyle = decimal */
    DECIMAL // DECIMAL must come last
  }

  /**
   * Fake renderer that doesn't depend on any DOM stuff.
   */
  ParagraphHtmlRenderer renderer = new ParagraphHtmlRenderer() {
    @Override
    public Element createDomImpl(Renderable element) {
      return null;
    }

    @Override
    public void updateRendering(HasImplNodelets element, String type, String listStyle, int indent,
        Alignment alignment, Direction direction) {
    }

    @Override
    public void updateListValue(HasImplNodelets element, int value) {
      assertEquals(Line.fromParagraph(((ContentElement) element)).getCachedNumberValue(), value);
    }
  };

  /**
   * Renumberer being tested.
   */
  final OrderedListRenumberer renumberer = new OrderedListRenumberer(renderer);

  /**
   * Batch render task that will get scheduled.
   */
  Task scheduledTask;

  /**
   * Simple fake take runner that just sets {@link #scheduledTask}
   */
  final FinalTaskRunner runner = new FinalTaskRunner() {
    @Override public void scheduleFinally(Task task) {
      assertTrue(scheduledTask == null || scheduledTask == task);
      scheduledTask = task;
    }
  };

  /**
   * Same as a regular ParagraphRenderer but tagged with
   * {@link PermanentMutationHandler} so that it gets used even in POJO document mode.
   */
  static class Renderer extends ParagraphRenderer implements PermanentMutationHandler {
    Renderer(ParagraphHtmlRenderer htmlRenderer, OrderedListRenumberer renumberer,
        FinalTaskRunner finalRaskRunner) {
      super(htmlRenderer, renumberer, finalRaskRunner);
      // TODO Auto-generated constructor stub
    }
  }

  ContentDocument content1;
  ContentDocument content2;

  CMutableDocument doc1;
  CMutableDocument doc2;

  /**
   * Current doc being used. For some tests we render more than one doc to test
   * the sharing of a single renumberer between multiple documents.
   */
  CMutableDocument doc;

  /** Number of lines in test documents */
  final int SIZE = 10;

  @Override
  protected void setUp() {
    EditorTestingUtil.setupTestEnvironment();

    ContentDocElement.register(ROOT_HANDLER_REGISTRY, ContentDocElement.DEFAULT_TAGNAME);
    Paragraph.register(ROOT_HANDLER_REGISTRY);
    LineRendering.registerLines(ROOT_HANDLER_REGISTRY);

    LineRendering.registerParagraphRenderer(Editor.ROOT_HANDLER_REGISTRY,
        new Renderer(renderer, renumberer, runner));

    renumberer.updateHtmlEvenWhenNullImplNodelet = true;

    DocInitializationBuilder builder = new DocInitializationBuilder();
    builder.elementStart("body", Attributes.EMPTY_MAP);
    for (int i = 0; i < SIZE; i++) {
      builder.elementStart("line", Attributes.EMPTY_MAP).elementEnd();
    }
    builder.elementEnd();

    content1 = new ContentDocument(ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS);
    content1.setRegistries(Editor.ROOT_REGISTRIES);
    content1.consume(builder.build());
    doc1 = content1.getMutableDoc();

    content2 = new ContentDocument(ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS);
    content2.setRegistries(Editor.ROOT_REGISTRIES);
    content2.consume(builder.build());
    doc2 = content2.getMutableDoc();

    doc = doc1;

    runTask();
  }

  /**
   * Performs a randomized test of renumbering logic.
   *
   * @param testIterations number of test iterations on the same document. Each
   *        iteration does a substantial amount of work (depending on document
   *        size).
   * @param seed initial random seed.
   */
  void doRandomTest(int testIterations, int seed) {
    ContentDocument.performExpensiveChecks = false;
    ContentDocument.validateLocalOps = false;
    IndexedDocumentImpl.performValidation = false;

    final int LEVELS = 4;
    final int MAX_RUN = 3;
    final int ITERS_PER_BATCH_RENDER = 6;
    final int DECIMALS_TO_OTHERS = 4; // ratio of decimal bullets to other stuff
    final int UPDATE_TO_ADD_REMOVE = 4; // ratio of updates to node adds/removals

    assertNull(scheduledTask);

    int maxRand = 5;
    Random r  = new Random(seed);

    // For each iteration
    for (int iter = 0; iter < testIterations; iter++) {
      info("Iter: " + iter);

      // Repeat several times for a single batch render, to make sure we are
      // able to handle multiple overlapping, redundant updates.
      // Times two because we are alternating between two documents to test
      // the ability of the renumberer to handle more than one document
      // correctly.
      int innerIters = (r.nextInt(ITERS_PER_BATCH_RENDER) + 1) * 2;
      for (int inner = 0; inner < innerIters; inner++) {
        doc = doc1; // (inner % 2 == 0) ? doc1 : doc2;

        int totalLines = (doc.size() - 2) / 2;

        Line line = getFirstLine();

        // Pick a random section of the document to perform a bunch of random
        // changes to
        int i = 0;
        int a = r.nextInt(totalLines);
        int b = r.nextInt(totalLines);
        int startSection = Math.min(a, b);
        int endSection = Math.max(a, b);

        while (i < startSection) {
          i++;
          line = line.next();
        }

        while (i < endSection && line != null) {
          // Pick a random indentation to set
          int level = r.nextInt(LEVELS);
          // Length of run of elements to update
          int length;
          // Whether we are making them numbered items or doing something else
          boolean decimal;

          if (r.nextInt(DECIMALS_TO_OTHERS) == 0) {
            // No need making it a long run for non-numbered items.
            length = r.nextInt(2);
            decimal = false;
          } else {
            decimal = true;
            length = r.nextInt(MAX_RUN - 1) + 1;
          }

          while (length > 0 && i < endSection && line != null) {
            boolean fiftyFifty = i % 2 == 0;
            // If we're numbering these lines, then DECIMAL, otherwise choose a
            // random other type.
            Type type = decimal ? Type.DECIMAL : Type.values()[r.nextInt(Type.values().length - 1)];

            // Randomly decide to add/remove, or to update
            if (r.nextInt(UPDATE_TO_ADD_REMOVE) == 0) {

              int index = index(line);
              // Randomly decide to add or remove.
              // Include some constraints to ensure the document doesn't get too small or too large.
              boolean add = index == 0 ||
                  totalLines < SIZE / 2 ? true : (totalLines > SIZE * 2 ? false : r.nextBoolean());

              if (add) {
                line = create(index, type, level, r.nextBoolean());
              } else {
                line = delete(index);
                if (line == null) {
                  // We just deleted the last line.
                  continue;
                }
              }
              assert line != null;

            } else {
              update(index(line), type, level, fiftyFifty);
            }

            length--;
            i++;
            line = line.next();
          }
        }
      }

      check(iter);
    }
  }

  /**
   * @return index for the given line object (0 for the first line, etc).
   */
  int index(Line line) {
    return (doc.getLocation(line.getLineElement()) - 1) / 2;
  }

  /**
   * @return the line element for the given index.
   */
  ContentElement getLineElement(int index) {
    return doc.locate(index * 2 + 1).getNodeAfter().asElement();
  }

  /**
   * @return the first line object
   */
  Line getFirstLine() {
     return Line.getFirstLineOfContainer(doc.getDocumentElement().getFirstChild().asElement());
  }

  /**
   * Creates and returns a new line.
   *
   * @param createAndUpdateSeparately if true, creates a line, then sets the
   *        attributes as a separate operation. Otherwise, sets them all at
   *        once. We want to test both scenarios.
   */
  Line create(int index, Type type, int indent, boolean createAndUpdateSeparately) {
//    info("Creating @" + index + " " +
//      type + " " + indent + " " + createAndUpdateSeparately);

    Point<ContentNode> loc = doc.locate(index * 2 + 1);
    Line l;
    if (createAndUpdateSeparately) {
      l = Line.fromLineElement(
          doc.createElement(loc, "line", Attributes.EMPTY_MAP));
      update(index, type, indent);
    } else {
      l = Line.fromLineElement(
          doc.createElement(loc, "line", attributes(type, indent, false, true)));
    }
    assertNotNull(l);
    return l;
  }

  /**
   * Deletes the line at the specified index.
   */
  Line delete(int index) {
//    info("Deleting @" + index);

    assert index != 0 : "Code doesn't (yet) support killing the initial line";
    ContentElement e = getLineElement(index);
    Line line = Line.fromLineElement(e).next();
    doc.deleteNode(e);
    return line;
  }

  /**
   * Updates the attributes of the line at the specified index.
   */
  void update(int index, Type type, int indent) {
    update(index, type, indent, true);
  }

  /**
   * Updates the attributes of the line at the specified index.
   *
   * @param alwaysSetRedundant if true, always set the listyle attribute even if it
   *        is not necessary. For example, if the listyle attribute was
   *        "decimal", but the type is "HEADING", the listyle attribute should
   *        normally be ignored and has no meaning. It won't make a difference
   *        if it is set or not. We want to test both scenarios.
   */
  void update(int index, Type type, int indent, boolean alwaysSetRedundant) {
    ContentElement e = getLineElement(index);
//    info("Making @" + ((doc.getLocation(e) - 1)/2) + " " +
//        type + " " + indent + " " + alwaysSetStyle);

    Map<String, String> updates = attributes(type, indent, alwaysSetRedundant, false);

    for (Map.Entry<String, String> pair : updates.entrySet()) {
      doc.setElementAttribute(e, pair.getKey(), pair.getValue());
    }
  }

  /**
   * Creates the map of element attributes for the given parameters.
   *
   * @param alwaysSetStyle see {@link #update(int, Type, int, boolean)}
   * @param noNulls eliminate keys that would have null values. We want nulls
   *        for updates, but no nulls for creates.
   */
  Map<String, String> attributes(Type type, int indent, boolean alwaysSetStyle, boolean noNulls) {
    Map<String, String> updates = new HashMap<String, String>();

    String levelStr = (indent > 0 ? "" + indent : null);
    maybePut(updates, Paragraph.INDENT_ATTR, levelStr, noNulls);

    String t = null;
    String lt = null;
    switch (type) {
      case HEADING: t = "h1"; break;
      case LIST: t = Paragraph.LIST_TYPE; break;
      case DECIMAL: t = Paragraph.LIST_TYPE; lt = Paragraph.LIST_STYLE_DECIMAL; break;
    }
    maybePut(updates, Paragraph.SUBTYPE_ATTR, t, noNulls);
    if (alwaysSetStyle || type == Type.LIST || type == Type.DECIMAL) {
      maybePut(updates, Paragraph.LIST_STYLE_ATTR, lt, noNulls);
    }

    return updates;
  }

  void maybePut(Map<String, String> map, String key, String val, boolean noNull) {
    if (val != null || !noNull) {
      map.put(key, val);
    }
  }

  /**
   * Check the current line numbering is consistent with the document state.
   */
  void check() {
    check(-1);
  }

  /**
   * Check the current line numbering is consistent with the document state.
   *
   * @param iter current test iteration, for debugging/logging purposes.
   */
  void check(int iter) {

    runTask();

//    if (iter >= 1740) {
//      info("\n\nCHECKING\n");
//      printInfo(null, "XX");
//      info("---");
//    }

    LevelNumbers numbers = new LevelNumbers(0, 1);
    Line line = getFirstLine();

    while (line != null) {

      int indent = line.getIndent();

      numbers.setLevel(indent);
      if (line.isDecimalListItem()) {
        int num = numbers.getNumberAndIncrement();
        assertFalse(line.getCachedNumberValue() == Line.DIRTY);
        if (num != line.getCachedNumberValue()) {
          String msg = "Expected: " + num + ", got: " + line.getCachedNumberValue();
          printInfo(line, msg);
          fail("Wrong number on iteration " + iter + ". " + msg +
            ". See stdout & stderr for debug details");
        }
      } else {
        numbers.setNumber(1);
      }

      line = line.next();
    }

//    info("^^^");
  }

  void runTask() {
    if (scheduledTask != null) {
      scheduledTask.execute();
    }

    scheduledTask = null;
  }

  void printInfo(Line badLine, String msg) {
    Line line = getFirstLine();
    PrintStream stream = System.out;

    int i = 0;
    while (line != null) {

      int indent = line.getIndent();

      stream.println(
        CollectionUtils.repeat('.', line.getIndent()) +
        line.toString() +
        " indent:" + indent +
        CollectionUtils.repeat(' ', 20) + line.getCachedNumberValue() + "  (" + i + ")");

      if (line == badLine) {
        stream.println("\n\n\n");
        stream = System.err;
        stream.println(msg);
        stream.println(">>>>>>>>>>>>>>>>>>>>>>>>> DIED ON LINE ABOVE <<<<<<<<<<<<<<<<<<\n\n");
      }

      line = line.next();
      i++;
    }
  }

  void info(Object msg) {
//    Uncomment for debugging
//    System.out.println(msg == null ? "null" : msg.toString());
  }

}
