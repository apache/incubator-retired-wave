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

package org.waveprotocol.wave.client.editor.harness;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.common.util.KeySignalListener;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.debug.logger.LoggerListener;
import org.waveprotocol.wave.client.doodad.diff.DiffAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentDocument.Level;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.editor.util.EditorDocFormatter;
import org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil;
import org.waveprotocol.wave.client.scheduler.ScheduleCommand;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeProvider;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.common.logging.AbstractLogger;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.automaton.DocOpAutomaton.ViolationCollector;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.DocOpValidator;
import org.waveprotocol.wave.model.document.parser.XmlParseException;
import org.waveprotocol.wave.model.document.util.DocIterate;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;

import java.util.ArrayList;
import java.util.Map;

/**
 *
 */
public class EditorHarness extends Composite implements KeySignalListener {

  private static final String TOPLEVEL_CONTAINER_TAGNAME = "body";

  static {
    LineContainers.setTopLevelContainerTagname(TOPLEVEL_CONTAINER_TAGNAME);
  }

  /**
   * Debug logger for the test module
   */
  LoggerBundle logger = new DomLogger("test");

  ContentDocument doc1, doc2;

  /**
   * The editors we are testing
   */
  Editor editor1;
  EditorBundle editorBundle1;

  /**
   * The editors we are testing
   */
  Editor editor2;
  EditorBundle editorBundle2;

  LogicalPanel.Impl displayDoc1 = new LogicalPanel.Impl() {
    {
      setElement(Document.get().createDivElement());
      getElement().getStyle().setProperty("border", "1px dashed silver");
    }
  };

  /**
   * Flag is ops coming out from editor1 should go to editor2
   */
  public boolean sendOps = true;

  /**
   * Flag if harness should operate quitly
   */
  boolean quiet = false;

  /**
   * Output widgets for editor1's content
   */
  private HTML prettyContent1;

  /**
   * Output widget for editor1's html content
   */
  private HTML prettyHtml1;

  /**
   * Output widgets for editor2's content
   */
  private HTML prettyContent2;

  /**
   * Output widget for editor2's html content
   */
  private HTML prettyHtml2;

  /**
   * Oracle for contentBox
   */
  MultiWordSuggestOracle contentOracle;

  /**
   * Input content box. Note: Only one of these regions is used.
   */
  private TextArea contentBox;
  private SuggestBox contentSuggestBox;

  /**
   * Input content box
   */
  private final Button setContentButton = new Button("Set content:", new ClickHandler() {
    public void onClick(ClickEvent e) {
      setFromContentBox();
    }
  });

  /**
   * Output widget for last operation
   */
  private final HTML operationOutput = new HTML();

  /**
   * Report errors here
   *
   * Error is triggered by error log messages.
   * Fatal is triggered by fatal log messages and uncaught exceptions.
   */
  private HTML error = null;
  private HTML fatal = null;

  /**
   * Our log div
   */
  private HTML log = null;

  private final Registries testEditorRegistries = Editor.ROOT_REGISTRIES;

  /**
   * Checkbox that toggles editing/display
   */
  private CheckBox toggleEditCheck1;

  private CheckBox toggleEditCheck2;

  private CheckBox createEditToggleCheckBox(final Editor editor) {
    CheckBox check = new CheckBox("Toggle edit");
    check.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
      @Override
      public void onValueChange(ValueChangeEvent<Boolean> event) {
        setEditing(editor, event.getValue());
      }
    });
    return check;
  }

  private void setEditing(Editor editor, boolean isEditing) {
    editor.setEditing(isEditing);
    if (editor.isEditing()) {
      editor.focus(true);
    }
  }

  /**
   * Clear log button
   */
  private final Button clearLogButton = new Button("Clear log", new ClickHandler() {
    public void onClick(ClickEvent e) {
      if (LogLevel.showDebug() && log != null) {
        log.setHTML("");
        error.setText("");
        fatal.setText("");
        editor1.focus(true);
      }
    }
  });

  /**
   * Quiet button
   */
  private final Button quietButton = new Button("Quiet (app runs faster)", new ClickHandler() {
    public void onClick(ClickEvent e) {
      littleLogging();
      quiet = true;
      editor1.focus(true);
    }
  });

  /**
   * Loud button
   */
  private final Button loudButton = new Button("Loud (more info)", new ClickHandler() {
    public void onClick(ClickEvent e) {
      lottaLogging();
      quiet = false;
      editor1.focus(true);
    }
  });


  private final CheckBox createEditor2DocDetached = new CheckBox("Create detached");

  /**
   * Sets content in both editors
   * @param content
   */
  private void syncEditors(String content)  {
    DocInitialization op;
    try {
      op = DocProviders.POJO.parse(content).asOperation();
    } catch (IllegalArgumentException e) {
      if (e.getCause() instanceof XmlParseException) {
        logger.error().log("Ill-formed XML string ", e.getCause());
      } else {
        logger.error().log("Error", e);
      }
      return;
    }

    DocumentSchema schema = getSchema();

    ViolationCollector vc = new ViolationCollector();
    if (!DocOpValidator.validate(vc, schema, op).isValid()) {
      logger.error().log("That content does not conform to the schema", vc);
      return;
    }

    boolean useHack = createEditor2DocDetached.getValue();

    // EDITOR1: Pojo -> editor mode
    double start = Duration.currentTimeMillis();
    doc1 = new ContentDocument(testEditorRegistries, op, schema);
    double middle = Duration.currentTimeMillis();
    editor1.setContent(doc1);
    double end = Duration.currentTimeMillis();
    logger.log(AbstractLogger.Level.TRACE, "Set content1 took: " + (end - start)
        + " (Pojo creation: " + (middle - start) + ", rendering: " + (end - middle));

    // EDITOR2: Build document in edit mode
    start = Duration.currentTimeMillis();
    doc2 = new ContentDocument(schema);
    doc2.setRegistries(testEditorRegistries);
    editor2.setContent(doc2);

    // HACK to ensure it's created non-attached to dom, to ensure fair speed comparisons
    double start2, end2;
    if (useHack) {
      Element docDiv = doc2.getFullContentView().getDocumentElement().getImplNodelet();
      Element editorDiv = editor2.getWidget().getElement();
      assert docDiv.getParentElement() == editorDiv;
      docDiv.removeFromParent();

      start2 = Duration.currentTimeMillis();
      doc2.consume(op);
      end2 = Duration.currentTimeMillis();

      editorDiv.appendChild(docDiv);
    } else {
      start2 = Duration.currentTimeMillis();
      doc2.consume(op);
      end2 = Duration.currentTimeMillis();
    }

    end = Duration.currentTimeMillis();
    logger.log(AbstractLogger.Level.TRACE, "Set content2 took: " + (end - start) +
        " Just the op: " + (end2 - start2) + " Op + appendChild: " + (end - start2));


    documentModeSelect.setSelectedIndex(doc1.getLevel().ordinal());

    outputBothEditorStates();
  }

  /**
   * Shows a red ERROR! message above the log panel.
   */
  private void showRedErrorIndicator() {
    error.setText("ERROR!");
  }

  /**
   * Shows a red FATAL! message above the log panel.
   */
  private void showRedFatalIndicator() {
    fatal.setText("FATAL!");
  }
  /**
   * Logs an error exception and shouts error!
   *
   * @param t
   */
  private void logUncaughtExceptions(Throwable t) {
    showRedFatalIndicator();
    logger.fatal().log(t);
    logger.trace();
    GWT.log("Uncaught Exception", t);
    t.printStackTrace(System.err);
  }
//
//  /**
//   * @param testCase
//   */
//  private void runTestCase(TestBase testCase) {
//    String content = editor1.getContent();
//    boolean pass = true;
//    try {
//      sendOps = false;
//      littleLogging();
//      testCase.runTests();
//    } catch (Throwable t) {
//      GWT.getUncaughtExceptionHandler().onUncaughtException(t);
//      pass = false;
//    } finally {
//      sendOps = true;
//      lottaLogging();
//    }
//
//    if (pass) {
//      logger.trace().log("<span style='color:green'>Pass :-)</span>");
//      syncEditors(content);
//    } else {
//      syncEditors();
//    }
//    editor1.focus();
//  }
//
//  /**
//   * Operations test button
//   */
//  private final Button operationTestButton = new Button("Ops tests", new ClickHandler() {
//    public void onClick(ClickEvent e) {
//      runTestCase(new OperationTest());
//    }
//  });
//
//  /**
//   * Paragaph test button
//   */
//  private final Button pTestButton = new Button("P tests", new ClickHandler() {
//    public void onClick(ClickEvent e) {
//      runTestCase(new ParagraphTest());
//    }
//  });
//
//  /**
//   * Title test button
//   */
//  private final Button titleTestButton = new Button("Title tests", new ClickHandler() {
//    public void onClick(ClickEvent e) {
//      runTestCase(new ParagraphTest());
//    }
//  });
//
//  /**
//   * Image thumbnail test button
//   */
//  private final Button thumbnailTestButton = new Button("Thumb tests", new ClickHandler() {
//    public void onClick(ClickEvent e) {
//      runTestCase(new ImageThumbnailTest());
//    }
//  });


  /**
   * A queue of operations
   */
  ArrayList<DocOp> queue = new ArrayList<DocOp>();

  /**
   * Checkbox if harness should queue up operations from editor1 rather
   * than sending them straight to editor2
   */
  private final CheckBox queuingCheck = new CheckBox("Queue");
  private final ClickHandler queuingCheckHandler = new ClickHandler() {
    public void onClick(ClickEvent e) {
      if (!queuingCheck.getValue()) {
        // Play any remaining operations in the queue
        while (queue.size() > 0) {
          playOne();
        }
      }
      editor1.focus(true);
    }
  };

  /**
   * Button for playing queued operations
   */
  private final Button playButton = new Button("Play", new ClickHandler() {
    public void onClick(ClickEvent e) {
      playOne();
    }
  });

  /**
   * Button for clearing diff annotations and problem markers
   */
  private final Button clearAnnotationsButton = new Button("Clear Annotations",
      new ClickHandler() {
        public void onClick(ClickEvent e) {
          editorBundle1.clearDiffs();
//          ((EditorImpl)editor1).clearProblemMarkers();
        }
      }
  );

  /**
   * Checkbox if editor2 should show incoming operations as diffs
   */
  private final CheckBox diffCheck = new CheckBox("Diffs");
  private final ClickHandler diffCheckHandler = new ClickHandler() {
    public void onClick(ClickEvent e) {
      editorBundle2.setShowDiffMode(diffCheck.getValue());
    }
  };

  private final CheckBox disabledCheck = new CheckBox("Disable");
  private final ClickHandler disabledCheckHandler = new ClickHandler() {
    public void onClick(ClickEvent e) {
      ((EditorImpl) editor1).debugSetDisabled(disabledCheck.getValue());
    }
  };

  private final ListBox documentModeSelect = new ListBox();
  {
    for (Level level : Level.values()) {
      documentModeSelect.addItem(level.name());
    }
    documentModeSelect.addChangeHandler(new ChangeHandler() {
      @Override
      public void onChange(ChangeEvent event) {
        setEditorLevel();
      }
    });
  }

  private void setEditorLevel() {
    Level newLevel = Level.values()[documentModeSelect.getSelectedIndex()];
    Level current = doc1.getLevel();

    logger.log(AbstractLogger.Level.TRACE, "Switching to " + newLevel);

    if (current == newLevel) {
      return;
    }

    if (current == Level.EDITING) {
      if (newLevel == Level.SHELVED) {
        editor1.removeContentAndUnrender();
      } else {
        editor1.removeContent();
      }
      doc1.replaceOutgoingSink(editor1Sink);
    }


    if (newLevel == Level.SHELVED) {
      doc1.setShelved();
    } else if (newLevel == Level.EDITING) {
      editor1.setContent(doc1);
    } else {
      assert newLevel == Level.RENDERED || newLevel == Level.INTERACTIVE;
      if (newLevel == Level.RENDERED) {
        doc1.setRendering();
      } else {
        doc1.setInteractive(displayDoc1);
      }
    }

    assert doc1.getLevel() == newLevel;

    if (doc1.getLevel() == Level.RENDERED || doc1.getLevel() == Level.INTERACTIVE) {
      displayDoc1.getElement().appendChild(
          doc1.getFullContentView().getDocumentElement().getImplNodelet());
    } else if (doc1.getLevel() == Level.SHELVED) {
      for (ContentNode n : DocIterate.deep(
          doc1.getFullContentView(), doc1.getFullContentView().getDocumentElement(), null)) {
        assert n.getImplNodelet() == null;
      }
    }
  }

  /**
   * Clears both editors
   */
  private final Button clearEditorsButton = new Button("Clear", new ClickHandler() {
    public void onClick(ClickEvent e) {
      clearEditors();
      editor1.focus(true);
    }
  });

  private final FlowPanel widgetRow;

  public void clearEditors() {
    syncEditors("");
    LineContainers.appendLine(((EditorImpl) editor2).mutable(), null);
  }

  private int randomTestCounter;
  Scheduler.IncrementalTask randomTestProcess = new Scheduler.IncrementalTask() {
    @Override
    public boolean execute() {
      randomTestCounter++;

      logger.trace().log("Random test #" + randomTestCounter);

      try {
        // TODO(nigeltao): This might trigger an (incorrect) exception where the
        // FakeAtttachmentsManager returns null. I should fix that.
//        new ContentAnnotationPainterRandomTest().testPainter();
      } catch (RuntimeException e) {
        logger.error().log(e);
        logger.error().log("TEST FAILED");
        return true;
      }

      return true;
    }
  };

  /**
   * Plays a single operation from the queue
   */
  private void playOne() {
    if (queue.size() > 0) {
      DocOp operation = queue.remove(0);
      try {
        outputOperation(operation);
        editorBundle2.execute(operation);
        outputEditorState(editor2, prettyContent2, prettyHtml2);
      } catch (Throwable t) {
        GWT.getUncaughtExceptionHandler().onUncaughtException(t);
      }
    }
    setPlayButtonState();
  }

  /**
   * Sets enabled/disabled state of play button
   */
  private void setPlayButtonState() {
    playButton.setEnabled(queue.size() > 0);
    playButton.setHTML("Play" +
        (queue.size() > 0 ? " (" + queue.size() + ")" : ""));
  }

  /**
   * Output content
   */
  private void outputBothEditorStates() {
    outputEditorState(editor1, prettyContent1, prettyHtml1);
    outputEditorState(editor2, prettyContent2, prettyHtml2);
  }

  /**
   * Output content for and editor
   */
  private void outputEditorState(final Editor editor,
      final HTML prettyContent, final HTML prettyHtml) {
    Runnable printer = new Runnable() {
      public void run() {
        if (!quiet) {
          String content = EditorDocFormatter.formatContentDomString(editor);
          String html = EditorDocFormatter.formatImplDomString(editor);
          if (content != null) {
            prettyContent.setText(content);
          }
          if (html != null) {
            prettyHtml.setText(html);
          }
        }
      }
    };

    if (LogLevel.showDebug()) {
      // Flush and update once it's safe
      if (editor.getContent().flush(printer)) {
        printer.run(); // note that if true is returned, the command isn't run inside flush.
      }
    }
  }

  /**
   * Output an operation
   *
   * @param operation
   */
  private void outputOperation(DocOp operation) {
    if (!quiet) {
      operationOutput.setText(operation != null ? operation.toString() : "");
    }
  }

  /**
   * Sets content in both editors from current text in content box.
   * Also adds that content to the content box oracle.
   */
  private void setFromContentBox() {
    String content = contentBox != null ? contentBox.getText() : contentSuggestBox.getText();
    if (contentOracle != null) {
      contentOracle.add(content);
    }
    syncEditors(bodyWrap(content));
    editor1.focus(true);
  }

  /**
   * This is used for unit testing to call this to attach the main panel to an
   * arbitrary root. The unit testing framework does not call onModuleLoad().
   *
   */
  public static EditorHarness createTestPage() {
    EditorHarness harness = new EditorHarness();
    RootPanel.get().add(harness, 0, 0);
    return harness;
  }

  void initContentText() {
    contentBox = new TextArea();
    contentBox.getElement().setId("content-box");
  }

  void initContentOracle() {
    contentOracle = new MultiWordSuggestOracle();

    contentSuggestBox = new SuggestBox(contentOracle);
    contentSuggestBox.getElement().setId("content-box");

    // Some initial content xml strings
    contentOracle.add("");
    contentOracle.add("abcd");

    contentSuggestBox.addSelectionHandler(new SelectionHandler<SuggestOracle.Suggestion>() {
      @Override public void onSelection(SelectionEvent<SuggestOracle.Suggestion> event) {
        setFromContentBox();
      }
    });

    String[] extra = extendSampleContent();
    if (extra != null) {
      for (String content : extra) {
        contentOracle.add(content);
      }
    }
  }

  /**
   * Override this to provide additional sample content for the suggest box
   */
  public String[] extendSampleContent() {
    return null;
  }

  /**
   * Convenience method to wrap sample content in the required body and line tags.
   */
  public String bodyWrap(String sampleContent) {
    // Put an extra space so that the content oracle indexing works nicely on
    // the actual sample content.
    return "<body><line/> " + sampleContent + "</body>";
  }

  public EditorHarness() {
    try {
      // Start with queueing off
      queuingCheck.setValue(false);
      setPlayButtonState();

      // Construct log panel
      if (LogLevel.showDebug()) {
        log = new HTML();
        log.setStyleName("log");
        log.getElement().setId("log1");
        error = new HTML();
        error.getElement().setId("error1");
        error.setStyleName("redIndicator");
        fatal = new HTML();
        fatal.getElement().setId("fatal1");
        fatal.setStyleName("redIndicator");

        // Initialize listener that turns error indicator red when any error
        // messages are logged
        DomLogger.addLoggerListener(new LoggerListener() {
          @Override
          public void onError() {
            showRedErrorIndicator();
          }

          @Override
          public void onFatal() {
            showRedFatalIndicator();
          }

          @Override
          public void onNeedOutput() {}

          @Override
          public void onNewLogger(String loggerName) {}
        });
      }

      // Setup content and html display
      if (LogLevel.showDebug()) {
        prettyContent1 = new HTML();
        prettyContent1.addStyleName("content");
        prettyContent2 = new HTML();
        prettyContent2.addStyleName("content");
        prettyHtml1 = new HTML();
        prettyHtml1.addStyleName("html");
        prettyHtml2 = new HTML();
        prettyHtml2.addStyleName("html");
      }

      EditorWebDriverUtil.setDocumentSchema(getSchema());

      registerDoodads(testEditorRegistries);

      // The editors we are testing
      editor1 = createEditor("editor1");
      editorBundle1 = new EditorBundle(editor1, true);
      editor2 = createEditor("editor2");
      editorBundle2 = new EditorBundle(editor2, false);
      toggleEditCheck1 = createEditToggleCheckBox(editor1);
      toggleEditCheck2 = createEditToggleCheckBox(editor2);

      operationOutput.setStyleName("operation");

      final FlowPanel editMain = new FlowPanel();
      editMain.setStyleName("main");
      HorizontalPanel editors = new HorizontalPanel();
      FlowPanel editorStack1 = new FlowPanel();
      FlowPanel editorStack2 = new FlowPanel();

      editorStack1.add(editor1.getWidget());
      editorStack1.add(displayDoc1);
      editorStack1.add(toggleEditCheck1);
      editorStack1.add(disabledCheck);
      editorStack1.add(documentModeSelect);
//      xx editorStack1.add(disabledCheck);
      if (LogLevel.showDebug()) {
        editorStack1.add(prettyContent1);
      }

      editorStack2.add(editor2.getWidget());
      editorStack2.add(toggleEditCheck2);
      editorStack2.add(diffCheck);
      editorStack2.add(createEditor2DocDetached);
      if (LogLevel.showDebug()) {
        editorStack2.add(prettyContent2);
      }

      editors.add(editorStack1);
      if (LogLevel.showDebug()) {
        editors.add(prettyHtml1);
      }
      editors.add(editorStack2);
      if (LogLevel.showDebug()) {
        editors.add(prettyHtml2);
      }

      HorizontalPanel operations = new HorizontalPanel();
      operations.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
      queuingCheck.addClickHandler(queuingCheckHandler);
      disabledCheck.addClickHandler(disabledCheckHandler);
      diffCheck.addClickHandler(diffCheckHandler);
      setPlayButtonState();
      operations.add(clearAnnotationsButton);
      operations.add(queuingCheck);
      operations.add(playButton);
      operations.add(new Label("Operation:"));
      operations.add(operationOutput);

      editMain.add(operations);
      editMain.add(editors);

      // Exactly one of these methods should be uncommented.
//      initContentText();
      initContentOracle();

      widgetRow = new FlowPanel();

      widgetRow.add(clearEditorsButton);
      widgetRow.add(setContentButton);

      widgetRow.add(new InlineLabel("<body><line/>"));
      widgetRow.add(contentBox != null ? contentBox : contentSuggestBox);
      widgetRow.add(new InlineLabel("</body>"));

      Button clearContentBoxButton = new Button("Clear text", new ClickHandler() {
        public void onClick(ClickEvent e) {
          if (contentBox != null) {
            contentBox.setValue("");
          } else {
            contentSuggestBox.setValue("");
          }
        }
      });
      clearContentBoxButton.getElement().setId("clear-content-box");
      widgetRow.add(clearContentBoxButton);
      // Hide it, only really used for webdriver tests
      clearContentBoxButton.setVisible(false);

      if (LogLevel.showDebug()) {
        widgetRow.add(new FlowPanel());
        widgetRow.add(clearLogButton);
        widgetRow.add(quietButton);
        widgetRow.add(loudButton);
      }

      editMain.add(widgetRow);

      if (LogLevel.showDebug()) {
        editMain.add(error);
        editMain.add(fatal);
        editMain.add(log);
        // We need our own uncaught exception hander to make sure the error shout happens
        GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
          public void onUncaughtException(Throwable e) {
            logUncaughtExceptions(e);
          }
        });
      } else {
        HTML spacer = new HTML();
        editMain.add(spacer);
      }

      initWidget(editMain);

      // Enable logging (important to do this after main is attached
      // such that exceptions occurring before this can be handled by
      // GWT's Default handler...)
      if (LogLevel.showDebug()) {
        DomLogger.enable(log.getElement());
        lottaLogging();
      }

      KeyBindingRegistry keysRegistry = new KeyBindingRegistry();
      extendKeyBindings(keysRegistry);

      // Start editing
      editor1.init(testEditorRegistries, keysRegistry, EditorSettings.DEFAULT);
      editor2.init(testEditorRegistries, keysRegistry, EditorSettings.DEFAULT);
      editor1.addUpdateListener(new EditorUpdateListener() {
        @Override
        public void onUpdate(EditorUpdateEvent event) {
          outputBothEditorStates();
        }

      });

      editor1.setOutputSink(editor1Sink);
      editor2.setOutputSink(editor2Sink);
      clearEditors();
      editor1.setEditing(true);
      editor2.setEditing(false);
      toggleEditCheck2.setValue(false);
      toggleEditCheck1.setValue(true);

      // Output initial state
      outputBothEditorStates();
      outputOperation(null);

    } catch (RuntimeException r) {
      // Do we need this at all?

      UncaughtExceptionHandler handler = GWT.getUncaughtExceptionHandler();
      if (handler != null) {
        handler.onUncaughtException(r);
      } else {
        logger.error().log(r);
      }
      throw r;
    }
  }

  public DocumentSchema getSchema() {
    return ConversationSchemas.BLIP_SCHEMA_CONSTRAINTS;
  }

  public void registerDoodads(Registries registries) {

    ElementHandlerRegistry testHandlerRegistry =
        testEditorRegistries.getElementHandlerRegistry();

    LineRendering.registerContainer(TOPLEVEL_CONTAINER_TAGNAME,
        registries.getElementHandlerRegistry());

    StyleAnnotationHandler.register(registries);
    DiffAnnotationHandler.register(
        registries.getAnnotationHandlerRegistry(),
        registries.getPaintRegistry());

    LinkAnnotationHandler.register(registries, new LinkAttributeAugmenter() {
      @Override
      public Map<String, String> augment(Map<String, Object> annotations, boolean isEditing,
          Map<String, String> current) {
        return current;
      }
    });

    // TODO(danilatos): Open source spelly stuff
//    SpellDocument testSpellDocument = SpellDebugHelper.createTestSpellDocument(
//        EditorStaticDeps.logger);
//    SpellAnnotationHandler.register(Editor.ROOT_REGISTRIES,
//        SpellySettings.DEFAULT, testSpellDocument);
//    SpellDebugHelper.setDebugSpellDoc(testSpellDocument);
//    SpellSuggestion.register(testHandlerRegistry, testSpellDocument);
//    SpellTesting.registerDebugCombo(keysRegistry);
//    SpellDebugHelper.setDebugSpellDoc(testSpellDocument);

    extend(registries);
  }

  /** Override this method to register additional doodads. */
  public void extend(Registries registries) {
  }

  /** Override this method to add keyboard handling hooks. */
  public void extendKeyBindings(KeyBindingRegistry registry) {
  }

  static {
    Editors.initRootRegistries();
  }

  private class EditorBundle {
    private final HighlightingDiffState diffState;
    private final boolean is1;
    private boolean showDiffs;

    EditorBundle(Editor editor, boolean is1) {
      this.diffState = new HighlightingDiffState(editor);
      this.is1 = is1;
    }

    void setShowDiffMode(boolean showDiffs) {
      this.showDiffs = showDiffs;
    }

    void execute(DocOp op) throws OperationException {
      if (showDiffs) {
        diffState.consume(op);
      } else {
        // TODO(danilatos): Clean this up
        (is1 ? doc1 : doc2).consume(op);
      }
    }

    void clearDiffs() {
      diffState.clearDiffs();
    }
  }

  private Editor createEditor(String id) {
    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
    EditorStaticDeps.setPopupChromeProvider(new PopupChromeProvider() {
      public PopupChrome createPopupChrome() {
        return null;
      }
    });

    Editor editor = Editors.create();
    editor.getWidget().getElement().setId(id);
    editor.addKeySignalListener(this);
    return editor;
  }

  /**
   * Log from all relevant modules
   */
  private void lottaLogging() {
    DomLogger.enableAllModules();
    DomLogger.enableModule("test", true);
    DomLogger.enableModule("editor", true);
    DomLogger.enableModule("editor-node", true);
    DomLogger.enableModule("operator", true);
    DomLogger.enableModule("dragdrop", true);
  }

  /**
   * Only log from test module
   */
  private void littleLogging() {
    DomLogger.setMaxLevel(AbstractLogger.Level.ERROR);
    DomLogger.enableModule("test", true);
  }

  private final SilentOperationSink<DocOp> editor1Sink =
      new SilentOperationSink<DocOp>() {
        public void consume(DocOp operation) {
          try {
            if (operation != null && sendOps) {
              if (queuingCheck.getValue()) {
                queue.add(operation);
                setPlayButtonState();
              } else {
                editorBundle2.execute(operation);
              }
            }
          } catch (Throwable t) {
            GWT.getUncaughtExceptionHandler().onUncaughtException(t);
          } finally {
            outputOperation(operation);
          }

        }
  };

  private final SilentOperationSink<DocOp> editor2Sink =
      new SilentOperationSink<DocOp>() {
        public void consume(DocOp operation) {
          try {
            if (operation != null && sendOps) {
              editorBundle1.execute(operation);
            }
          } catch (Throwable t) {
            GWT.getUncaughtExceptionHandler().onUncaughtException(t);
          } finally {
            outputOperation(operation);
          }

        }
  };

  /**
   * {@inheritDoc}
   */
  public boolean onKeySignal(final Widget sender, SignalEvent event) {
    // Deferred command so we have a look at the content after it's updated
    ScheduleCommand.addCommand(new Task() {
      public void execute() {
         if (sender == editor1) {
           outputEditorState(editor1, prettyContent1, prettyHtml1);
         } else {
           outputEditorState(editor2, prettyContent2, prettyHtml2);
         }
      }
    });
    return false;
  }

  public Editor getEditor1() {
    return editor1;
  }

  public Editor getEditor2() {
    return editor2;
  }

  /**
   * Convenience method to run the harness when it's the main application.
   */
  public void run() {
    RootPanel.get().add(this);
    getEditor1().focus(true);
  }
}
