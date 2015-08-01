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

package org.waveprotocol.wave.client.editor;

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.Text;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.FocusImpl;

import org.waveprotocol.wave.client.common.util.ClientDebugException;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.DomHelper.HandlerReference;
import org.waveprotocol.wave.client.common.util.DomHelper.JavaScriptEventListener;
import org.waveprotocol.wave.client.common.util.EventWrapper;
import org.waveprotocol.wave.client.common.util.KeyCombo;
import org.waveprotocol.wave.client.common.util.KeySignalListener;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.common.util.QuirksConstants;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.common.util.SignalEvent.KeyModifier;
import org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType;
import org.waveprotocol.wave.client.common.util.SignalEventImpl;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.editor.EditorInstrumentor.Action;
import org.waveprotocol.wave.client.editor.EditorInstrumentor.TimedAction;
import org.waveprotocol.wave.client.editor.Responsibility.Manager;
import org.waveprotocol.wave.client.editor.constants.BrowserEvents;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentDocument.Level;
import org.waveprotocol.wave.client.editor.content.ContentDocument.LocalOperationException;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentRange;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.ContentView;
import org.waveprotocol.wave.client.editor.content.FocusedContentRange;
import org.waveprotocol.wave.client.editor.content.NodeEventRouter;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations.AnnotationResolver;
import org.waveprotocol.wave.client.editor.content.misc.DisplayEditModeHandler;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.Line;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.debug.DebugPopupFactory;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.client.editor.event.EditorEventHandler;
import org.waveprotocol.wave.client.editor.event.EditorEventImpl;
import org.waveprotocol.wave.client.editor.event.EditorEventsSubHandler;
import org.waveprotocol.wave.client.editor.event.EditorInteractor;
import org.waveprotocol.wave.client.editor.extract.DomMutationReverter;
import org.waveprotocol.wave.client.editor.extract.DomMutationReverter.RevertListener;
import org.waveprotocol.wave.client.editor.extract.ImeExtractor;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlInserted;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.extract.PasteExtractor;
import org.waveprotocol.wave.client.editor.extract.RepairListener;
import org.waveprotocol.wave.client.editor.extract.Repairer;
import org.waveprotocol.wave.client.editor.extract.TypingExtractor;
import org.waveprotocol.wave.client.editor.extract.TypingExtractor.SelectionSource;
import org.waveprotocol.wave.client.editor.extract.TypingExtractor.TypingSink;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.editor.selection.content.AggressiveSelectionHelper;
import org.waveprotocol.wave.client.editor.selection.content.CaretMovementHelper;
import org.waveprotocol.wave.client.editor.selection.content.CaretMovementHelperImpl;
import org.waveprotocol.wave.client.editor.selection.content.CaretMovementHelperWebkitImpl;
import org.waveprotocol.wave.client.editor.selection.content.PassiveSelectionHelper;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;
import org.waveprotocol.wave.client.editor.selection.content.SelectionUtil;
import org.waveprotocol.wave.client.editor.selection.html.HtmlSelectionHelper;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;
import org.waveprotocol.wave.client.editor.sugg.InteractiveSuggestionsManager;
import org.waveprotocol.wave.client.editor.sugg.SuggestionsManager;
import org.waveprotocol.wave.client.editor.util.AnnotationBehaviourLogic;
import org.waveprotocol.wave.client.editor.webdriver.EditorJsniHelpers;
import org.waveprotocol.wave.client.editor.webdriver.EditorWebDriverUtil;
import org.waveprotocol.wave.client.scheduler.CommandQueue;
import org.waveprotocol.wave.client.scheduler.ScheduleCommand;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.BiasDirection;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.ContentType;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.CursorDirection;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema.PermittedCharacters;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Point.El;
import org.waveprotocol.wave.model.document.util.PointRange;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.document.util.RangeTracker;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.undo.UndoManagerFactory;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.IdentityMap;
import org.waveprotocol.wave.model.util.IdentitySet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableIdentitySet.Proc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * The DOM structure for an editor is as follows:
 *
 * <pre>
 * &lt;div class=&quot;editor&quot; contentEditable=&quot;...&quot;&gt; unselectable=&quot;on&quot;
 *   &lt;div contentEditable=&quot;...&quot;&gt; unselectable=&quot;on&quot;&gt;
 *     (this is the 'pre' decorator element, and the editor doesn't care what goes in here)
 *   &lt;/div&gt;
 *   &lt;div&gt;(editable content)&lt;/div&gt;
 * &lt;/div&gt;
 * </pre>
 *
 * The decorator is required for adding inline decorations on IE7 (because a contentEditable
 * region must be rectangular, so decorations must be included in that region).
 * The editor contains logic to ensure that, even though the editable region includes a
 * decorator, editing actions (including making selections) are restricted to the document.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
// (and the above again in ASCII)
// <div class="editor" contentEditable="..." unselectable="on">
//   <div contentEditable="false" unselectable="on">
//     (this is the decorator element, and the editor doesn't care what goes in here)
//   </div>
//   <div>(editable content)</div>
// </div>
//
public class EditorImpl extends LogicalPanel.Impl implements
    Editor, LogicalPanel, HtmlSelectionHelper, JavaScriptEventListener {

  /** CSS class applied to editor document's top level html element when in edit mode */
  public static final String WAVE_EDITOR_EDIT_ON = "wave-editor-on";
  /** CSS class applied to editor document's top level html element when not in edit mode */
  public static final String WAVE_EDITOR_EDIT_OFF = "wave-editor-off";

  /**
   * Minimal set of dependencies on the editor for ContentDocument
   * (mostly the fault of various doodads).
   */
  public interface MiniBundle {
    /***/
    TypingExtractor getTypingExtractor();
    /***/
    boolean inEditMode();
    /** Spell suggestions */
    SuggestionsManager getSuggestionsManager();
    /***/
    PassiveSelectionHelper getPassiveSelectionHelper();
    /** ContentDocument updates this registry of "elements with display modes" :( */
    CopyOnWriteSet<ContentElement> getElementsWithDisplayModes();

    RepairListener getRepairListener();
    EditorContext getEditorContext();

    /**
     * Brings the editor into a consistent state, possibly asynchronously.
     *
     * "Consistent" means that the editor's document state includes the effects of
     * all browser-events before now, and that that state is consistent with the
     * operations that this editor has pushed out to the outgoing operation stream
     * (i.e., no operations are buffered).
     *
     * This should be called immediately prior to applying operations to the
     * document, i.e.
     * {@link ContentDocument#consume(org.waveprotocol.wave.model.document.operation.DocOp)}
     *
     * NOTE(danilatos): While this method is re-entrant, if it returns false,
     * there is not much point calling it again until the continuation command has
     * been executed.
     *
     * @param resume if the editor's document is not in a consistent state, a
     *        callback to fire as soon as consistency is reached.
     * @return true if the editor's document is in a consistent state, false
     *         otherwise (note: {@code resume} is not called if this method
     *         returns true).
     */
    boolean flush(Runnable resume);

    /**
     * Notifies the editor that an external source (i.e. incoming op) has applied
     * a DocOp to the document.
     *
     * @param op
     */
    void onIncomingOp(DocOp op);
  }

  @VisibleForTesting final MiniBundle editorPackage = new MiniBundle() {

    /** {@inheritDoc} */
    public TypingExtractor getTypingExtractor() {
      return typing;
    }

    /** {@inheritDoc} */
    public boolean inEditMode() {
      return EditorImpl.this.isEditing();
    }

    /** {@inheritDoc} */
    public SuggestionsManager getSuggestionsManager() {
      return suggestionsManager;
    }
    /** {@inheritDoc} */
    public PassiveSelectionHelper getPassiveSelectionHelper() {
      return passiveSelectionHelper;
    }
    @Override
    public CopyOnWriteSet<ContentElement> getElementsWithDisplayModes() {
      return elementsWithDisplayEditModes;
    }
    @Override
    public RepairListener getRepairListener() {
      return repairListener;
    }
    @Override
    public EditorContext getEditorContext() {
      return EditorImpl.this;
    }

    @Override
    public boolean flush(Runnable resume) {
      return EditorImpl.this.flush(resume);
    }

    @Override
    public void onIncomingOp(DocOp op) {
      EditorImpl.this.onIncomingOp(op);
    }
  };

  /**
   * Resources used in the Editor.
   */
  public interface Resources extends ClientBundle {

    /** Css resource */
    // TODO(danilatos): extends CssResource to get obfuscated class name
    // TODO(danilatos): factor our CSS into per-widget bundles (e.g., image thumbnail, paragraph)
    @Source("Editor.css")
    @NotStrict  // TODO(danilatos): make Strict by including all classes in the CssResource
    CssResource css();
  }

  /**
   * Singleton instance of resource bundle
   */
  private static final CssResource css = GWT.<Resources>create(Resources.class).css();


  /**
   * Attribute used to mark the editable document element.
   */
  static final String EDITABLE_DOC_MARKER = "editableDocMarker";

  /**
   * Shorthand for the main div of the editor
   */
  private final Element div;

  /**
   * List of ContentElements that needs to be notified when we change mode
   */
  private final CopyOnWriteSet<ContentElement> elementsWithDisplayEditModes
      = CopyOnWriteSet.create();

  /**
   * Node manager
   */
  private NodeManager nodeManager;

  private CaretMovementHelper caretMoveHelper;

  private HtmlSelectionHelper htmlSelectionHelper;

  /**
   * Always use this unless you know what you are doing
   */
  private PassiveSelectionHelper passiveSelectionHelper;

  /**
   * Use this to possibly correct problems in the document (side effect of
   * generating operations) whilst getting the selection. If in doubt,
   * use the passive selection helper.
   */
  private AggressiveSelectionHelper aggressiveSelectionHelper;

  /**
   * Editor's content document, including doc div
   */
  protected ContentDocument content;

  /**
   * Configuration
   */
  private EditorSettings settings = EditorSettings.DEFAULT;

  private SilentOperationSink<? super DocOp> innerOutputSink;

  /**
   * Permits sending out operations, and receiving local operations.
   * Currently useful for debugging purposes only.
   */
  private boolean permitOperations = true;

  /** Buffer of outgoing ops, for when we suppress sending them. */
  List<DocOp> suppressedOutgoingOps;

  /**
   * Sink for outgoing operations
   *
   * TODO(mtsui/danilatos): Move this out of the editor as well.
   */
  private final SilentOperationSink<DocOp> outgoingOperationSink =
      new SilentOperationSink<DocOp>() {
        public void consume(DocOp op) {
          try {
            if (permitOperations) {
              innerOutputSink.consume(op);
            } else {
              suppressedOutgoingOps.add(op);
            }

            if (responsibility.withinDirectSequence()) {
              editorUndoManager.undoableOp(op);
            } else {
              editorUndoManager.nonUndoableOp(op);
            }

            // And schedule notification update
            // NOTE(danilatos): We might want the coord updated parameter to be true,
            // but see the discussion in #execute(op) for details...
            scheduleUpdateNotification(false, false, true, responsibility.withinDirectSequence());
          } catch (Exception e) {
            if (innerOutputSink == null) {
              EditorStaticDeps.logger.fatal().logPlainText("Output sink is null",
                  new ClientDebugException("", e));
            } else {
              EditorStaticDeps.logger.fatal().logPlainText("Output sink threw exception", e);
            }
          }
        }
      };

  /**
   * A command-queue for commands that need to run when the editor is in a
   * consistent state.  The addition of commands to this queue triggers a
   * deferred command to poll the consistency state and executing queued
   * commands until the queue is empty.
   */
  private class ConsistentStateCommandRunner implements Scheduler.Task {
    /** Queued commands, FIFO. */
    private Queue<Runnable> commands = CollectionUtils.createQueue();

    private ConsistentStateCommandRunner() {
    }

    public void schedule(Runnable c) {
      commands.add(c);
      if (permitOperations) { // Prevent constant rescheduling
        ScheduleCommand.addCommand(this);
      }
    }

    @Override
    public void execute() {
      Queue<Runnable> backup = commands;
      commands = CollectionUtils.createQueue();

      while (isConsistent() && !backup.isEmpty()) {
        backup.poll().run();
      }

      // Move any unexecuted commands back into the main queue, preserving any
      // commands that were scheduled during the above execution.
      backup.addAll(commands);
      // Restore the copy
      commands = backup;

      if (!commands.isEmpty()) {
        if (permitOperations) { // Prevent constant rescheduling
          ScheduleCommand.addCommand(this);
        }
      }
    }
  }

  /**
   * Deferred command for checking if we can resume incoming operations
   */
  private final ConsistentStateCommandRunner consistencyQueue = new ConsistentStateCommandRunner();

  /**
   * Handler for the results of typing
   */
  TypingSink typingSink = new TypingSink() {

    @Override
    public void aboutToFlush() {
      domMutationReverter.flush();

      // Some errors here aren't fatal, we can probably just ignore them, but
      // let's log them because they were unexpected, and might imply other problems...
      if (webkitEndOfLinkHackTextNode != null) {
        EditorStaticDeps.startIgnoreMutations();
        try {
          Point<Node> caret = getHtmlSelection().getFocus();
          if (caret.isInTextNode()) {
            Text hackedTextNode = caret.getContainer().cast();
            Node link = hackedTextNode.getPreviousSibling();
            while (link != null && DomHelper.isTextNode(link)) {
              link = link.getPreviousSibling();
            }
            if (link == null) {
              EditorStaticDeps.logger.error().log("No link before the link hack?");
              return;
            }
            Element anchor = link.cast();
            if (!anchor.getTagName().equalsIgnoreCase("a")) {
              EditorStaticDeps.logger.error().log(
                  "Some other element before the link hack? (" + anchor.getTagName() + ")");
              // Don't return, we still want to move the text nodes...
            }
            Node nodeAfter;
            if (caret.getTextOffset() == hackedTextNode.getLength()) {
              nodeAfter = hackedTextNode.getNextSibling();
            } else {
              nodeAfter = hackedTextNode.splitText(caret.getTextOffset());
            }
            DomHelper.moveNodes(anchor, anchor.getNextSibling(), nodeAfter, null);

            NativeSelectionUtil.setCaret(Point.<Node>inText(hackedTextNode, hackedTextNode.getLength()));

            // TODO: Set the annotation caret to turn the link off.
          } else {
            EditorStaticDeps.logger.error().log("End of link hack caret not in text node!?");
          }
        } finally {
          webkitEndOfLinkHackTextNode = null;
          EditorStaticDeps.endIgnoreMutations();
        }
      }
    }

    @Override
    public void typingReplace(Point<ContentNode> start, int deletionSize, String text,
        RestrictedRange<ContentNode> range) {
      int location = mapper().getLocation(start);

      Nindo op = generateReplaceTextOp(location, deletionSize, text);
      try {
        applyRepairingOperation(op);
        typingFinished(range);

      } catch (LocalOperationException e) {
        EditorStaticDeps.logger.error().log(
            "Swallowing Error: Invalid operation sent to TypingSink (" + e + ")");
      }
      currentSelectionBias = BiasDirection.LEFT;
    }

    void typingFinished(RestrictedRange<ContentNode> range) {
      FocusedPointRange<Node> selection = getHtmlSelection();
      boolean selectionAffected =
          repairer.zipRange(range, selection == null ? null : selection.getFocus().getContainer());
      if (selectionAffected) {
        assert selection != null && selection.getFocus() != null;
        Node n = selection.getFocus().getContainer();

        // Find selection in fragmented text nodes. What if we can't find it?
        Node newTextNode = n;
        // NOTE(user): Ideally, the selection here should always be inside a
        // text node. However, since we are just calling getHtmlSelection here,
        // there is no obvious guarantee. In particular, when tracing
        // TypingExtractor.flush() it may be called inside a deferred command,
        // which means the selection can be anywhere.
        // TODO(user): Refactor this code to make it more explicit. Consider
        // passing in the selection rather than calling getHtmlSelection.
        if (LogLevel.showErrors() && !selection.getFocus().isInTextNode()) {
          EditorStaticDeps.logger.error().log(
              "selection while typing is expected to be in text node, but is not ",
              selection.getFocus());
        } else {
          int newTextOffset = selection.getFocus().getTextOffset();
          while (DomHelper.isTextNode(n) && n.<Text> cast().getLength() < newTextOffset) {
            newTextOffset -= n.<Text> cast().getLength();
            newTextNode = n.getNextSibling();
          }
          NativeSelectionUtil.setCaret(Point.inText(newTextNode, newTextOffset));
        }
      }
    }

    /**
     * Apply an operation only to the content wrapper dom. The usual use case is
     * that the html has been affected in a certain way already, and we are
     * matching that in the content with a series of operations. Hence we only
     * apply the operation to the wrapper dom.
     *
     * @param op
     */
    private void applyRepairingOperation(Nindo op) {

      responsibility.startDirectSequence();
      try {
        content.sourceNindoWithoutModifyingHtml(op);
      } finally {
        responsibility.endDirectSequence();
      }
    }
  };

  /**
   * A typing operation extractor for the editor
   */
  protected TypingExtractor typing;

  /**
   * IME composition extractor helper
   */
  private final ImeExtractor imeExtractor = new ImeExtractor();

  /**
   * Our paste extractor
   */
  private PasteExtractor pasteExtractor;

  /**
   * The thing that makes the content match the html or vice versa
   */
  private Repairer repairer;

  /**
   * Flag if editor is editing or displaying
   */
  protected boolean editing = false;

  /**
   * Keeps track of what styles are to be applied to the collapsed caret.
   */
  private CaretAnnotations caretStyles;

  /**
   * Keeps track of what the cursor's current bias is.
   */
  private BiasDirection currentSelectionBias = BiasDirection.LEFT;

  /**
   * My keyboard listeners
   */
  protected Set<KeySignalListener> keySignalListeners;

  /**
   * Registries for everything
   */
  protected Registries registries;

  /**
   * Our friendly equally friendly suggestion manager
   */
  protected SuggestionsManager suggestionsManager = null;

  private DomMutationReverter domMutationReverter;

  /** Stores all known key -> action bindings. */
  private KeyBindingRegistry keyBindings = KeyBindingRegistry.NONE;

  /**
   * Application specific editor event handler.
   */
  private final EditorEventsSubHandler eventsSubHandler = new EditorEventsSubHandlerImpl();

  /** Application specific annotation logic manager. */
  AnnotationBehaviourLogic<ContentNode> annotationLogic = null;

  private final EditorUpdateEventImpl updateEvent = new EditorUpdateEventImpl(this);

  /** stored scrollTops used by maybeSave/RestoreAncestorScrollPositions */
  private IdentityMap<Element, Integer> ancestorScrollTops;

  // TODO(user): This class can be broken down further. Things like paste/dom
  // mutation extraction are not really application specific and can be handled
  // in a separate class.
  private class EditorEventsSubHandlerImpl implements EditorEventsSubHandler {
    @Override
    public boolean handleBlockLevelCommands(EditorEvent event, ContentRange selection) {
      Point<ContentNode> start = selection.getFirst();
      Point<ContentNode> end = selection.getSecond();

      // Shonky key combos until toolbar
      if (KeyModifier.CTRL.check(event)) {
        int startLoc = mapper().getLocation(start);
        int endLoc = mapper().getLocation(end);

        int num = event.getKeyCode() - '1' + 1;
        if (num >= 1 && num <= 6) {
          if (num == 5) {
            final String listStyle;

            Line l = Paragraph.getFirstLine(mapper(), mapper().getLocation(start));
            if (Paragraph.LIST_TYPE.equals(l.getAttribute(Paragraph.SUBTYPE_ATTR)) &&
                !Paragraph.LIST_STYLE_DECIMAL.equals(l.getAttribute(Paragraph.LIST_STYLE_ATTR))) {
              listStyle = Paragraph.LIST_STYLE_DECIMAL;
            } else {
              listStyle = null; // default style
            }
            Paragraph.apply(mapper(), startLoc, endLoc, Paragraph.listStyle(listStyle), true);
          } else {
            final String type;
            if (num == 6) {
              type = null;
            } else {
              type = "h" + num;
            }
            Paragraph.toggle(mapper(), startLoc, endLoc, Paragraph.regularStyle(type));
          }
          settings.getInstrumentor().record(Action.SHORTCUT_HEADINGSTYLE);
          return true;
        } else if (num == 7) {
          Paragraph.apply(mapper(), startLoc, endLoc, Paragraph.Alignment.LEFT, true);
          Paragraph.apply(mapper(), startLoc, endLoc, Paragraph.Direction.LTR, true);
          settings.getInstrumentor().record(Action.SHORTCUT_ALIGNMENT);
          return true;
        } else if (num == 8) {
          Paragraph.apply(mapper(), startLoc, endLoc, Paragraph.Alignment.RIGHT, true);
          Paragraph.apply(mapper(), startLoc, endLoc, Paragraph.Direction.RTL, true);
          settings.getInstrumentor().record(Action.SHORTCUT_ALIGNMENT);
          return true;
        }
      } else if (event.getKeyCode() == KeyCodes.KEY_TAB) {
        handleTab(start, end, event.getShiftKey());
        return true;
      }
      return false;
    }

    @Override
    public boolean handleRangeKeyCombo(EditorEvent event, ContentRange selection) {
      assert !selection.isCollapsed();

      KeyCombo combo = EventWrapper.getKeyCombo(event.asEvent());

      // TODO(patcoleman): separate collapsed and normal, maybe also incorporate handled flag.
      if (keyBindings.hasAction(combo)) {
        keyBindings.getAction(combo).execute(EditorImpl.this);
        return true;
      }

      Point<ContentNode> start = selection.getFirst();
      Point<ContentNode> end = selection.getSecond();

      switch (combo) {
        // NOTE(user): Ideally, these should be changed to ORDER_B, ORDER_I,
        // ORDER_U. However, Safari on OSX does not emit any key events for
        // those combos, so we're allowing both on mac.
        // Also EventWrapper is broken at the moment, so registering ORDER_B
        // actually overrides CTRL_B
        case CTRL_B:
        case ORDER_B:
          doStyle(start, end, "fontWeight", "bold");
          settings.getInstrumentor().record(Action.SHORTCUT_BOLD);
          return true;
        case CTRL_I:
        case ORDER_I:
          doStyle(start, end, "fontStyle", "italic");
          settings.getInstrumentor().record(Action.SHORTCUT_ITALIC);
          return true;
        case CTRL_U:
        case ORDER_U:
          doStyle(start, end, "textDecoration", "underline");
          settings.getInstrumentor().record(Action.SHORTCUT_UNDERLINE);
          return true;
        case CTRL_SHIFT_5:
        case ORDER_SHIFT_5:
          doStyle(start, end, "textDecoration", "line-through");
          settings.getInstrumentor().record(Action.SHORTCUT_LINETHROUGH);
          return true;
      }

      return false;
    }

    @Override
    public boolean handleCollapsedKeyCombo(EditorEvent event, Point<ContentNode> caret) {
      // Handle events with carets
      // TODO(user): handle lots more events here
      KeyCombo combo = EventWrapper.getKeyCombo(event.asEvent());

      // TODO(patcoleman): separate collapsed and normal, maybe also incorporate handled flag.
      if (keyBindings.hasAction(combo)) {
        keyBindings.getAction(combo).execute(EditorImpl.this);
        return true;
      }

      switch (combo) {
        // NOTE(user): Ideally, these should be changed to ORDER_B, ORDER_I,
        // ORDER_U. However, Safari on OSX does not emit any key events for
        // those combos, so we're allowing both on mac.
        case CTRL_SPACE:
          // TODO(danilatos): Factor out configuration of popup key combo
          suggestionsManager.showSuggestionsNearestTo(caret);
          settings.getInstrumentor().record(Action.SHORTCUT_OPENNEARBYPOPUP);
          return true;
        case CTRL_B:
        case ORDER_B:
          doCollapsedStyle(caret, "fontWeight", "bold");
          settings.getInstrumentor().record(Action.SHORTCUT_BOLD);
          return true;
        case CTRL_I:
        case ORDER_I:
          doCollapsedStyle(caret, "fontStyle", "italic");
          settings.getInstrumentor().record(Action.SHORTCUT_ITALIC);
          return true;
        case CTRL_U:
        case ORDER_U:
          doCollapsedStyle(caret, "textDecoration", "underline");
          settings.getInstrumentor().record(Action.SHORTCUT_UNDERLINE);
          return true;
        case CTRL_SHIFT_5:
        case ORDER_SHIFT_5:
          doCollapsedStyle(caret, "textDecoration", "line-through");
          settings.getInstrumentor().record(Action.SHORTCUT_LINETHROUGH);
          return true;
      }

      return false;
    }

    @Override
    public void handleDomMutation(SignalEvent event) {
      domMutationReverter.handleMutationEvent(event);
    }

    @Override
    public boolean handleCommand(EditorEvent event) {
      if (event.isUndoCombo()) {
        assert responsibility.withinDirectSequence();
        settings.getInstrumentor().record(Action.UNDO);
        editorUndoManager.undo();
        return true;
      }

      if (event.isRedoCombo()) {
        assert responsibility.withinDirectSequence();
        settings.getInstrumentor().record(Action.REDO);
        editorUndoManager.redo();

        return true;
      }

      KeyCombo combo = new EventWrapper(event.asEvent()).getKeyCombo();
      switch (combo) {
        // TODO(user): deprecate CTRL_ALT_D in favour of CTRL_ALT_G, ctrl alt d
        // is a bad combo for linux as it minimizes the window in many
        // window managers.
        case CTRL_ALT_D:
        case CTRL_ALT_G:
          debugToggleDebugDialog();
          return true;
      }

      return false;
    }

    /**
     * Handles tab behaviour.
     *
     * @param start
     * @param end
     * @param shiftDown
     */
    private void handleTab(Point<ContentNode> start, Point<ContentNode> end, boolean shiftDown) {
      ContentNode node = start.getContainer();
      while (node != null) {
        if (isTabTarget(node)) {
          break;
        }
        node = node.getParentElement();
      }

      // If we're not in a caption, tab = indent/outdent
      if (node == null) {
        applyParagraphIndent(start, end, shiftDown);
        settings.getInstrumentor().record(shiftDown ?
            Action.SHORTCUT_TABOUTDENT : Action.SHORTCUT_TABINDENT);
      } else {
        settings.getInstrumentor().record(Action.SHORTCUT_TABFIELDS);
        // traverse until we find the next caption:
        do {
          node = DocHelper.getNextOrPrevNodeDepthFirst(mutable(), node, null, true, !shiftDown);
          if (isTabTarget(node)) {
            break;
          }
        } while (node != null);

        // found the next caption, set the selection:
        if (node != null) {
          Point<ContentNode> fixedStart = Point.start(mutable(), (ContentElement) node);
          Point<ContentNode> fixedEnd = Point.end(node);
          passiveSelectionHelper.setSelectionPoints(fixedStart, fixedEnd);
          // NOTE(patcoleman): scroll into view should happen automagically?
        }
      }
    }

    public boolean isTabTarget(ContentNode node) {
      if (node == null || node.asElement() == null) {
        return false;
      }
      ContentElement elem = node.asElement();
      String nodeTagName = elem.getTagName();
      return TAB_TARGETS.contains(nodeTagName);
    }

    @Override
    public boolean handleCut(EditorEvent event) {
      editorUndoManager.maybeCheckpoint();

      EditorStaticDeps.logger.trace().log("handling cut");
      return pasteExtractor.handleCutEvent(EditorImpl.this);
    }

    @Override
    public boolean handlePaste(EditorEvent event) {
      editorUndoManager.maybeCheckpoint();
      EditorStaticDeps.logger.trace().log("handling paste");
      return pasteExtractor.handlePasteEvent(currentSelectionBias);
    }

    @Override
    public boolean handleCopy(EditorEvent event) {
      EditorStaticDeps.logger.trace().log("handling copy");
      return pasteExtractor.handleCopyEvent(EditorImpl.this);
    }

    /**
     * Set a style over a range, or clear it if the range is already entirely
     * covered by the given value for that style.
     */
    private void doStyle(Point<ContentNode> start, Point<ContentNode> end, String key,
        String value) {
      key = StyleAnnotationHandler.key(key);
      applyPaint(mapper().getLocation(start), mapper().getLocation(end), key, value);
    }

    /**
     * Set a style over a caret, or clear it if the style is already set
     * by the given value for that style.
     */
    private void doCollapsedStyle(Point<ContentNode> caret,
        String key, String value) {
      key = StyleAnnotationHandler.key(key);
      if (caretStyles.isAnnotated(key, value)) {
        caretStyles.setAnnotation(key, null);
      } else {
        caretStyles.setAnnotation(key, value);
      }
      scheduleUpdateNotification(false, false, true, false);
    }

    private void applyPaint(int startLocation, int endLocation, String key, String value) {

      boolean isFullyStyled =
          mutable().firstAnnotationChange(startLocation, endLocation, key, value) == -1;

      editorUndoManager.maybeCheckpoint(startLocation, endLocation);

      mutable().setAnnotation(startLocation, endLocation, key,
          isFullyStyled ? null : value);
    }

    /**
     * Apply paragraph indent/outdent
     *
     * @param start
     * @param end
     * @param outdent true to indent/false to outdent
     */
    private void applyParagraphIndent(Point<ContentNode> start, Point<ContentNode> end,
        boolean outdent) {
      // Indent paragraph:
      LocationMapper<ContentNode> m = mapper();
      Paragraph.traverse(m, m.getLocation(start), m.getLocation(end), outdent ? Paragraph.OUTDENTER
          : Paragraph.INDENTER);
    }
  }

  /**
   * Our event handling logic
   *
   * Package private for testing
   */
  EditorEventHandler eventHandler = null;

  private Text webkitEndOfLinkHackTextNode = null;

  private Responsibility.Manager responsibility = null;

  private EditorUndoManager editorUndoManager = null;

  /**
   * Resolves annotations by checking what the annotation of the character on the side of
   * the cursor bias is.
   */
  AnnotationResolver annotationResolver = new AnnotationResolver() {
    public String getAnnotation(String key) {
      FocusedRange browserSelection = passiveSelectionHelper.getSelectionRange();
      if (browserSelection == null) {
        EditorStaticDeps.logger.error().log(
            "No selection when resolving editor annotations.");
        return null; // safely consume for now...
      } else if (!browserSelection.isCollapsed()) {
        EditorStaticDeps.logger.error().log("Resolving selection annotations is only supported "
            + "while the browser selection is collapsed");
        return null;
      }

      // TODO(patcoleman): optimise by caching the selection?
      int at = browserSelection.getFocus();
      boolean biasLeft = (currentSelectionBias != BiasDirection.RIGHT); // default to left
      return Annotations.getAlignedAnnotation(mutable(), at, key, biasLeft);
    }
  };

  /// Editor instrumentation

  /** Time for key process (bundled to be reported with a post-key process time). */
  private double processKeyPressTimer = 0.0;

  /** Timer for tracking the rendering speed after a keypress. */
  private Duration postKeyPressTimer = null;

  /** Task scheduled to run after the layout/paint after an input event. */
  private final Task instrumentationTask = new Task() {
    @Override
    public void execute() {
      settings.getInstrumentor().recordDuration(
          TimedAction.INPUT_PROCESS, processKeyPressTimer);
      settings.getInstrumentor().recordDuration(
          TimedAction.INPUT_POSTPROCESS, postKeyPressTimer.elapsedMillis());
    }
  };

  /** Instrumentation for repairing */
  // TODO(patcoleman): extract out instrumentation logic into separate class
  private final RepairListener repairListener = new RepairListener() {
    @Override
    public void onFullDocumentRevert(
        ReadableDocument<ContentNode, ContentElement, ContentTextNode> doc) {
      settings.getInstrumentor().record(Action.FULL_REPAIR);
    }

    @Override
    public void onRangeRevert(El<ContentNode> start, El<ContentNode> end) {
      settings.getInstrumentor().record(Action.PARTIAL_REPAIR);
    }
  };

  /**
   * Restricted interface that allows the event handler to interact with the editor.
   */
  private class EditorInteractorImpl implements EditorInteractor {
    @Override
    public void forceFlush() {
      EditorImpl.this.flushSynchronous();
    }

    @Override
    public FocusedContentRange getSelectionPoints() {
      return getAggressiveSelectionHelper().getSelectionPoints();
    }

    @Override
    public boolean selectionIsOrdered() {
      return NativeSelectionUtil.isOrdered();
    }

    @Override
    public FocusedPointRange<Node> getHtmlSelection() {
      return EditorImpl.this.getHtmlSelection();
    }

    @Override
    public boolean hasContentSelection() {
      return isTyping() ||
          (SelectionUtil.filterNonContentSelection(getHtmlSelection()) != null);
    }

    @Override
    public Point<ContentNode> normalizePoint(Point<ContentNode> caret) {
      // left align caret within full, then filter it to hard, then normalise within hard:
      caret = DocHelper.normalizePoint(caret, content.getSelectionFilter());
      caret = DocHelper.leftAlign(caret, full(), content.getSelectionFilter());
      caret = DocHelper.getFilteredPoint(content.getSelectionFilter(), caret);
      return caret;
    }

    @Override
    public boolean notifyListeners(SignalEvent event) {
      boolean handled = false;

      // Fire keyboard event to listeners but cancel their bubbling
      // but only if the event is actually relevant to editor (i.e,
      // in editing mode or target is in form element)
      if (event.isKeyEvent()) {
        if (editorRelevantEvent(event)) {
          handled = fireKeyboardEvent(event);
        }
//        // Hack(user): special case submit because currently the owner
//        // may turn the editor to display mode from submit. Rethink
//        // where + how submits get handled
//        if (isEditing() && event.isCombo(' ', KeyModifier.SHIFT)) {
//          return true;
//        }
      }

      return handled;
    }

    @Override
    public boolean shouldIgnoreMutations() {
      return EditorStaticDeps.shouldIgnoreMutations();
    }

    @Override
    public boolean isExpectingMutationEvents() {
      return EditorImpl.this.isTyping();
    }

    @Override
    public boolean isTyping() {
      return EditorImpl.this.isTyping();
    }

    @Override
    public boolean notifyTypingExtractor(Point<ContentNode> caret, boolean useHtmlCaret,
        boolean isReplace) {
      Point<Node> htmlCaret;
      if (caret != null) {
        assert !useHtmlCaret;
        htmlCaret = getNodeManager().wrapperPointToNodeletPoint(caret);
      } else {
        if (useHtmlCaret) {
          // HACK(danilatos): Just use the html caret.
          // See NOTE #XYZ in EditorEventHandler
          NativeSelectionUtil.cacheClear();
          FocusedPointRange<Node> htmlSelection = NativeSelectionUtil.get();
          htmlCaret = htmlSelection != null ? htmlSelection.getFocus() : null;
        } else {
          // The caret might be null for some other reason, such as it's in a
          // text input box in a doodad, or something... leave it as null.
          // Only use the HTML caret if explicitly told to do so.
          htmlCaret = null;
        }
      }

      if (htmlCaret != null) {
        // NOTE(patcoleman): caret == null shows we're in the middle of typing, so it is correct
        // to not supplement annotations (they should have been supplemented on typing start).
        if (!isReplace && caret != null) {
          annotationLogic.supplementAnnotations(mutable().getLocation(caret),
              currentSelectionBias, ContentType.PLAIN_TEXT);
        }
        EditorImpl.this.notifyTypingExtractor(htmlCaret);
        return false;
      } else {
        EditorStaticDeps.logger.error().logPlainText(
            "Null html caret in EditorImpl's notifyTypingExtractor, content caret: " + caret);

        // allow - probably that weird IME issue.
        // or maybe it's in a funky text input box in some doodad. who knows.
        // hopefully not in a broken place.
        return false;
      }
    }

    @Override
    public void setCaret(Point<ContentNode> caret) {
      getAggressiveSelectionHelper().setCaret(caret);
    }

    @Override
    public Point<ContentNode> deleteRange(Point<ContentNode> first, Point<ContentNode> second,
        boolean isReplace) {
      if (isReplace) {
        annotationLogic.supplementAnnotations(mutable().getLocation(first), BiasDirection.RIGHT,
            ContentType.PLAIN_TEXT);
      }
      return mutable().deleteRange(getFilteredPoint(first), getFilteredPoint(second)).getFirst();
    }

    @Override
    public Point<ContentNode> insertText(Point<ContentNode> at, String text, boolean isReplace) {
      return EditorImpl.this.insertText(at, text, isReplace);
    }

    @Override
    public ContentElement findElementWrapper(Element target) {
      return getNodeManager().findElementWrapper(target);
    }

    @Override
    public boolean isEditing() {
      return EditorImpl.this.isEditing();
    }

    @Override
    public void noteWebkitEndOfLinkHackOccurred(Text textNode) {
      webkitEndOfLinkHackTextNode = textNode;
    }

    public void clearCaretAnnotations() {
      caretStyles.clear();
    }

    @Override
    public void deleteWordEndingAt(Point<ContentNode> caret) {
      Point<ContentNode> end = normalizePoint(caret);
      Point<ContentNode> start = caretMoveHelper.getWordBoundary(false);
      if (start != null) {
        mutable().deleteRange(getFilteredPoint(start), getFilteredPoint(end));
        rebiasSelection(CursorDirection.FROM_RIGHT);
      }
    }

    @Override
    public void deleteWordStartingAt(Point<ContentNode> caret) {
      Point<ContentNode> start = normalizePoint(caret);
      Point<ContentNode> end = caretMoveHelper.getWordBoundary(true);
      if (end != null) {
        mutable().deleteRange(getFilteredPoint(start), getFilteredPoint(end));
        rebiasSelection(CursorDirection.FROM_LEFT);
      }
    }

    @Override
    public void compositionStart(Point<ContentNode> caret) {
      // NOTE(danilatos): Is it safe to have the start & end ignore mutations in this
      // manner? Or should we ignore based on the event handler state? I guess it's
      // the same effect.
      EditorStaticDeps.startIgnoreMutations();
      if (caret != null) {
        imeExtractor.activate(content.getContext(), caret);
      }
      annotationLogic.supplementAnnotations(mutable().getLocation(caret), currentSelectionBias,
          ContentType.PLAIN_TEXT);
    }

    @Override
    public void compositionUpdate() {
      // TODO(danilatos): Some event or other, so e.g. we can show other users
      // the composition state.
    }

    @Override
    public FocusedContentRange compositionEnd() {
      try {

        if (!imeExtractor.isActive()) {
          EditorStaticDeps.logger.error().log(
              "Composition end called with inactive ImeExtractor! "
                  + "Maybe caret was null initially?");
          return null;
        }

        // HACK(danilatos): prevent CC from sending the insertion before the annotation update.
        // TODO(zdwang/danilatos): Implement batching in CcBasedWavelet, or something, and send
        // out in a deferred command, so synchronously generated ops always go in the same
        // delta, whether or not CC is waiting for an unacknowledged op.
        mutable().hackConsume(new Nindo.Builder().build());

        String composition = imeExtractor.getContent();
        assert composition != null : "Composition should not be null with active IME extractor";
        Point<ContentNode> contentPoint = imeExtractor.deactivate(content.getAnnotatableContent());
        Point<ContentNode> caret = insertText(contentPoint, composition, true);
        aggressiveSelectionHelper.setCaret(caret);
        rebiasSelection(CursorDirection.FROM_LEFT);

        // HACK(danilatos): Flush updates, so that listeners to the ime state get an immediate
        // update, to synchronously clear the composition state from any selection annotations,
        // so that there's no instant where the other side sees both the inserted text and
        // the last composed bit.
        // This can be avoided by keeping track of the selection annotations directly in the
        // editor, something worth considering.
        responsibility.startIndirectSequence();
        updateEvent.flushUpdates();
        responsibility.endIndirectSequence();

        return passiveSelectionHelper.getSelectionPoints();
      } finally {
        EditorStaticDeps.endIgnoreMutations();
      }
    }

    @Override
    public void checkpoint(FocusedContentRange currentRange) {
      if (currentRange != null) {
        editorUndoManager.maybeCheckpoint(mutable().getLocation(currentRange.getFocus()), mutable()
            .getLocation(currentRange.getAnchor()));
      } else {
        editorUndoManager.maybeCheckpoint();
      }
    }

    @Override
    public void rebiasSelection(CursorDirection lastDirection) {
      EditorImpl.this.rebiasSelection(lastDirection);
    }
  }

  /**
   * Rebias the user's selection to whichever side is desired by annotation / document state.
   * NOTE(patcoleman): occurs in a deferred command so the selection is correct when retrieved.
   */
  private void rebiasSelection(final CursorDirection lastDirection) {
    // short-cut for extraction
    if (isTyping()) {
      currentSelectionBias = BiasDirection.LEFT;
      return;
    }

    SchedulerInstance.getHighPriorityTimer().schedule(new Task() {
      @Override
      public void execute() {
        CursorDirection current = lastDirection;
        if (!settings.useFancyCursorBias()) {
          current = CursorDirection.FROM_LEFT;
        }

        FocusedRange focused = getSelectionHelper().getSelectionRange();
        if (focused != null) {
          Range range = focused.asRange();
          currentSelectionBias = annotationLogic.rebias(range.getStart(), range.getEnd(), current);
        } else {
          // no selection, so have default bias
          currentSelectionBias = BiasDirection.LEFT;
        }
      }
    });
  }

  /**
   * Utility that inserts some text into the document
   * @param isReplace Whether this insertion is the second half of a replacement.
   */
  private Point<ContentNode> insertText(Point<ContentNode> at, String text, boolean isReplace) {
    text = PermittedCharacters.BLIP_TEXT.coerceString(text);
    int location = mutable().getLocation(getFilteredPoint(at));

    if (!isReplace) {
      annotationLogic.supplementAnnotations(location, currentSelectionBias, ContentType.PLAIN_TEXT);
    }

    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(location);
    caretStyles.buildAnnotationStarts(builder);
    builder.characters(text);
    caretStyles.buildAnnotationEnds(builder, true);
    mutable().hackConsume(builder.build());
    return mutable().locate(location + text.length());
  }

  /**
   * Internal utility that handles document deletion and text insertion, writing the
   * result to a builder, including extra annotation logic.
   * NOTE(patcoleman): *does not* delete the content if it is not all text,
   *   due to being unable to do that in an op.
   *
   * @param at Location in the document to delete then insert.
   * @param deletionSize Size of content to delete.
   * @param text Text to insert at the location.
   * @return Builder that when built and
   */
  private Nindo generateReplaceTextOp(int at, int deletionSize, String text) {
    Nindo.Builder builder = new Nindo.Builder();
    builder.skip(at);

    // Delete only when necessary
    if (deletionSize > 0) {
      // Desired behaviour: inherit styles from the left of the deleted range:
      annotationLogic.supplementAnnotations(at, BiasDirection.RIGHT, ContentType.PLAIN_TEXT);
      builder.deleteCharacters(deletionSize); // NOTE(patcoleman): everything deleted must be text.
    }

    // Insert only when necessary
    if (text.length() > 0) {
      caretStyles.buildAnnotationStarts(builder);
      builder.characters(text);
      caretStyles.buildAnnotationEnds(builder, true);
    }
    return builder.build();
  }

  private Point<ContentNode> getFilteredPoint(Point<ContentNode> unfiltered) {
    return DocHelper.getFilteredPoint(persistent(), unfiltered);
  }

  @Override
  public Manager getResponsibilityManager() {
    return responsibility;
  }

  @Override
  public void undoableSequence(Runnable cmd) {
    editorUndoManager.maybeCheckpoint();
    responsibility.startDirectSequence();
    try {
      cmd.run();
    } finally {
      responsibility.endDirectSequence();
    }
  }

  private UniversalPopup debugPopup = null;

  static {
    StyleInjector.inject(css.getText(), true);
    EditorJsniHelpers.nativeSetupWebDriverTestPins();
    NativeSelectionUtil.setTransientMutationListener(new NativeSelectionUtil.MutationListener() {
      @Override
      public void startTransientMutations() {
        EditorStaticDeps.startIgnoreMutations();
      }

      @Override
      public void endTransientMutations() {
        EditorStaticDeps.endIgnoreMutations();
      }
    });
  }

  /**
   * Method used to trigger the static initializer on this class.
   */
  public static void init() {
  }

  /**
   * Determines the lifecycle relationship between this editor and the document
   * it edits. If true, this editor owns the document, so it is responsible for
   * creating it and destroying it (i.e., the lifetime of the document is within
   * the lifetime of the editor). If false, this editor does not own the
   * document, so it must not create or destroy it, and must leave it in the
   * same state as it found it (i.e., the lifetime of the editor is within the
   * lifetime of the document).
   */
  private final boolean ownsDocument;

  /**
   * Constructor
   */
  protected EditorImpl(boolean ownsDocument, Element div) {
    this.ownsDocument = ownsDocument;
    this.div = div;
    setElement(div);
  }

  private final IdentitySet<HandlerReference> domHandlers = CollectionUtils.createIdentitySet();

  private void registerDomEventHandling() {
    for (String event : BrowserEvents.HANDLED_EVENTS) {
      domHandlers.add(DomHelper.registerEventHandler(getDocumentHtmlElement(), event, this));
    }
  }

  private void unregisterDomEventHandling() {
    domHandlers.each(new Proc<HandlerReference>() {
      @Override
      public void apply(HandlerReference handler) {
        handler.unregister();
      }
    });
    domHandlers.clear();
  }

  @Override
  public Widget getWidget() {
    return this;
  }

  @Override
  public void init(Registries registries, KeyBindingRegistry bindings, EditorSettings settings) {
    Preconditions.checkState(
        ownsDocument == (registries != null), "Can only set registries on owned documents");
    this.registries = registries;
    this.keyBindings = bindings;
    this.settings = settings;

    eventHandler = new EditorEventHandler(
        new EditorInteractorImpl(), eventsSubHandler, NodeEventRouter.INSTANCE,
        settings.useWhitelistInEditor(), settings.useWebkitCompositionEvents());

    setEditing(false);
  }

  @Override
  public void reset() {
    // TODO(danilatos): only bring this back once all the jobs associated with the editor (e.g.
    //   painter) have been reimplemented such that:
    //     a) they notice that an editor has been reset, and cancel themselves; or
    //     b) an editor can cancel jobs associated with it here on reset(); or
    //     c) happy/sad/ecstatic packages have only immutable state, rather than pulling out
    //        mutable editor state that can be reset here.
    // factoryRegistry = null;

    // GWT widget cleanup
    // clear();

    updateEvent.clear();
    elementsWithDisplayEditModes.clear();

    Iterator<Widget> i = iterator();
    while (i.hasNext()) {
      Widget w = i.next();
      doOrphan(w);
      // Resume iteration.
      i = iterator();
    }

    repairer = null;
    keyBindings.clear();
    caretStyles = null;
    clearContent();
    annotationLogic = null;
  }

  private void clearContent() {
    if (content != null) {
      updateDocumentEditState(false);
      EditorStaticDeps.startIgnoreMutations();
      try {
        unregisterDomEventHandling();

        if (ownsDocument) {
          div.removeChild(getDocumentHtmlElement());
        } else {
          // Restore the document's original sink and DOM state.
          Object oldSink = content.replaceOutgoingSink(innerOutputSink);
          if (oldSink != outgoingOperationSink) {
            throw new RuntimeException("Document had a mysterious sink.  Restoration is unsafe");
          }
          div.removeClassName("editor");
        }
      } finally {
        EditorStaticDeps.endIgnoreMutations();
      }
    }

    content = null;
  }

  @Override
  public void cleanup() {
    reset();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setOutputSink(final SilentOperationSink<DocOp> sink) {
    this.innerOutputSink = sink;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void clearOutputSink() {
    this.innerOutputSink = null;
  }

  /**
   * Causes all pending operations to be fired as events.
   */
  @VisibleForTesting
  void flushSynchronous() {
    if (content != null) {
      typing.flush();
    }
  }

  private boolean flush(Runnable resume) {
    if (!canApplyIncomingOperations()) {
      EditorStaticDeps.logger.trace().log("Deferring incoming operation");
      consistencyQueue.schedule(resume);
      return false;
    } else {
      // In case this event cycle is running before the -continuation command queue's runner,
      // and there are previously queued resume commands.
      consistencyQueue.execute();
      return true;
    }
  }

  @Override
  public ContentView getPersistentDocument() {
    return content.getPersistentView();
  }

  @Override
  public void setContent(final DocInitialization op, DocumentSchema schema) {
    Preconditions.checkState(ownsDocument, "Can not replace content not owned");
    setContent(new ContentDocument(registries, op, schema));
  }

  @Override
  public ContentDocument getContent() {
    return content;
  }

  @Override
  public ContentDocument removeContent() {
    ContentDocument oldDoc = content;

    clearContent();

    if (ownsDocument) {
      oldDoc.setRendering();
      oldDoc.replaceOutgoingSink(SilentOperationSink.Void.get());
    } else {
      oldDoc.setInteractive();
    }

    // TODO(danilatos): Clear all the stuff initialised in setContent()
    return oldDoc;
  }

  @Override
  public ContentDocument removeContentAndUnrender() {
    ContentDocument oldDoc = content;

    clearContent();
    oldDoc.setShelved();

    return oldDoc;
  }

  @Override
  public void setContent(ContentDocument newDoc) {
    flushSynchronous();
    EditorStaticDeps.startIgnoreMutations();
    try {
      if (suggestionsManager != null) {
        suggestionsManager.clear();
      }
      elementsWithDisplayEditModes.clear();
      clearContent();

      content = newDoc;

      if (ownsDocument) {
        // Attach the document to the editor.
        content.replaceOutgoingSink(outgoingOperationSink);
      } else {
        // Attach the editor to the document.
        innerOutputSink = content.replaceOutgoingSink(outgoingOperationSink);
      }

      /////////////////////////////
      /////////////////////////////
      if (!content.getLevel().isAtLeast(Level.RENDERED)) {
        // TODO(danilatos): Use setRenderingFast() once it also initialises nodeManager, etc.
        content.setRendering();
      }

      repairer = content.getRepairer();

      nodeManager = content.getNodeManager();

      htmlSelectionHelper = new HtmlSelectionHelperImpl(getDocumentHtmlElement());
      passiveSelectionHelper =
          new PassiveSelectionHelper(htmlSelectionHelper, content.getNodeManager(),
              content.getRenderedView(), content.getLocationMapper());

      suggestionsManager = new InteractiveSuggestionsManager(
          passiveSelectionHelper, settings.closeSuggestionsMenuDelayMs());

      aggressiveSelectionHelper =
          new AggressiveSelectionHelper(htmlSelectionHelper, nodeManager, content.getRenderedView(),
              content.getLocationMapper(), content.getMutableDoc()) {
            @Override
            protected void flushForUnextractedText() {
              flushSynchronous();
              // TODO(danilatos): Schedule another typing pass since we're forcing a flush?
              // Do something else?
            }
          };

      caretMoveHelper = UserAgent.isWebkit() ? new CaretMovementHelperWebkitImpl(nodeManager)
          : new CaretMovementHelperImpl(persistent(), passiveSelectionHelper);


      final OperationSequencer<Nindo> sequencer = content.getOpSequencer();

      typing = new TypingExtractor(typingSink, nodeManager,
          content.getFilteredHtmlView(), content.getRenderedView(), repairer,
          new SelectionSource() {
            /** {@inheritDoc} */
            public Point<Node> getSelectionStart() {
              PointRange<Node> range = getOrderedHtmlSelection();
              return range == null ? null : range.getFirst();
            }

            /** {@inheritDoc} */
            public Point<Node> getSelectionEnd() {
              PointRange<Node> range = getOrderedHtmlSelection();
              return range == null ? null : range.getSecond();
            }
          });

      responsibility = new ResponsibilityManagerImpl();

      if (settings.undoEnabled()) {
        editorUndoManager = new EditorUndoManagerImpl(
            UndoManagerFactory.createUndoManager(),
            new SilentOperationSink<DocOp>() {
              @Override
              public void consume(DocOp op) {
                // This applies to the content document as well as sending
                // it out remotely.
                content.sourceNindo(Nindo.fromDocOp(op, true));
              }
            }, passiveSelectionHelper);

      } else {
        editorUndoManager = EditorUndoManager.NOP_IMPL;
      }

      OperationSequencer<Nindo> undoingSequencer = new UndoableSequencer(sequencer, responsibility);

      CMutableDocument undoableDocument = content.createSequencedDocumentWrapper(undoingSequencer);

      pasteExtractor = new PasteExtractor(CommandQueue.HIGH_PRIORITY,
          aggressiveSelectionHelper,
          undoableDocument,
          content.getRenderedView(),
          content.getPersistentView(),
          content.getRegistries().getAnnotationHandlerRegistry(),
          undoingSequencer,
          content.getValidator(),
          settings.getInstrumentor(),
          settings.useSemanticCopyPaste());

      final Scheduler.Task revertTask = new Scheduler.Task() {
        public void execute() {
          domMutationReverter.flush();
        }
      };

      domMutationReverter = new DomMutationReverter(new RevertListener() {
        @Override
        public void scheduleRevert() {
          if (!isTyping()) {
            EditorStaticDeps.logger.trace().log(
                "WARNING: Dom removal outside of known typing context");
            ScheduleCommand.addCommand(revertTask);
          }
        }
      });

      repairer.hideDeath(full().getDocumentElement());

      // set up empty styles
      caretStyles = new CaretAnnotations();
      caretStyles.setAnnotationResolver(annotationResolver);

      // Force rendering of annotations
      // TODO(danilatos): Make this only apply to the current editor.
      AnnotationPainter.repaintNow(content.getContext());

      savedSelection = new RangeTracker(content.getLocalAnnotations(), "savedsel");
      passiveSelectionHelper.setSelectionTracker(savedSelection);

      // initialise the annotation behavioural logic:
      annotationLogic = new AnnotationBehaviourLogic<ContentNode>(
          ROOT_ANNOTATION_REGISTRY, content.getMutableDoc(), caretStyles);


//      /////////////////////////////
//      /////////////////////////////

      content.attachEditor(editorPackage, ownsDocument ? this : null);

      Element docDiv = getDocumentHtmlElement();
      if (ownsDocument) {
        div.appendChild(docDiv);
        // TODO(danilatos): Is this style name needed anymore? It was needed
        // in the past for webdriver, not anymore. Anything else?
        docDiv.addClassName("document");
      } else {
        Preconditions.checkArgument(div == docDiv.getParentElement(), "wrong content document");
      }
      docDiv.setAttribute(EDITABLE_DOC_MARKER, "true");
      DomHelper.setNativeSpellCheck(docDiv, false);

      // setup event handling
      registerDomEventHandling();

      // Initialise editing state related things
      setEditing(isEditing());

      // TODO(danilatos): If this is necessary, add comment why.  Also consider
      // scheduling with delay or at low priority, so as not to slow down initial
      // rendering.
      // scheduleUpdateNotification();

      // Check health if in debug build
      debugCheckHealth();
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
  }

  /**
   * Location Mapper for use by editor classes (only exposed on EditorImpl)
   */
  public LocationMapper<ContentNode> mapper() {
    return content.getLocationMapper();
  }

  /**
   * Full Content View for use by editor classes (only exposed on EditorImpl)
   */
  public ContentView full() {
    return content.getFullContentView();
  }

  /**
   * Mutable Docuemnt for use by editor classes (only exposed on EditorImpl)
   */
  public CMutableDocument mutable() {
    return content.getMutableDoc();
  }

  /**
   * Peristent Content View for use by editor classes (only exposed on EditorImpl)
   */
  public ContentView persistent() {
    return content.getPersistentView();
  }

  /**
   * NodeManager for use by editor classes (only exposed on EditorImpl)
   */
  public NodeManager getNodeManager() {
    return nodeManager;
  }

  /**
   * AggressiveSelectionHelper for use by editor classes (only exposed on EditorImpl)
   */
  public AggressiveSelectionHelper getAggressiveSelectionHelper() {
    return aggressiveSelectionHelper;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isEditing() {
    return editing;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setEditing(final boolean editing) {
    this.editing = editing;
    if (content != null) {
      updateDocumentEditState(editing);

      editorUndoManager.maybeCheckpoint();

      // there should be any edit notification when we are not editing.  There was a bug
      // where we modifies the document after stopEditing is called.
      if (editing) {
        if (innerOutputSink == null) {
          EditorStaticDeps.logger.error().log("Scheduling update with no inner output sink...");
        }
        scheduleUpdateNotification(false, true, false, false);
      }
    }
  }

  /**
   * Removes the various editor bits on the document
   */
  private void updateDocumentEditState(boolean editing) {
    Element topLevel = getDocumentHtmlElement();
    // Set property to some arbitrary non-null value if we're in editing mode.
    full().getDocumentElement().setProperty(AnnotationPainter.DOCUMENT_MODE, editing);

    topLevel.removeClassName(WAVE_EDITOR_EDIT_ON);
    topLevel.removeClassName(WAVE_EDITOR_EDIT_OFF);
    topLevel.addClassName(editing ? WAVE_EDITOR_EDIT_ON : WAVE_EDITOR_EDIT_OFF);

    AnnotationPainter.maybeScheduleRepaint(content.getContext(), 0, mutable().size());
    DomHelper.setContentEditable(topLevel, editing, true);

    for (ContentElement element : elementsWithDisplayEditModes) {
      if (element.getParentElement() != null) {
        DisplayEditModeHandler.onEditModeChange(element, editing);
      } else {
        elementsWithDisplayEditModes.remove(element);
      }
    }
  }

  private void maybeSaveAncestorScrollPositions(Element e) {
    if (QuirksConstants.ADJUSTS_SCROLL_TOP_WHEN_FOCUSING) {
      ancestorScrollTops = CollectionUtils.createIdentityMap();
      while (e != null) {
        ancestorScrollTops.put(e, e.getScrollTop());
        e = e.getParentElement();
      }
    }
  }

  private void maybeRestoreAncestorScrollPositions(Element e) {
    if (QuirksConstants.ADJUSTS_SCROLL_TOP_WHEN_FOCUSING && ancestorScrollTops != null) {
      ancestorScrollTops.each(new IdentityMap.ProcV<Element, Integer>() {
        public void apply(Element e, Integer i) {
          e.setScrollTop(i);
        }
      });
      ancestorScrollTops = null;
    }
  }

  /**
   * TODO(user): use content document to set caret, and issue operation
   *
   * TODO(danilatos): This stuff seems out of date...
   *
   * TODO(danilatos): Make this method trivially idempotent
   *
   * {@inheritDoc}
   */
  @Override
  public void focus(boolean collapsed) {
    if (!isAttached()) {
      EditorStaticDeps.logger.error().log("Shouldn't focus a detached editor");
      return;
    }

    // focus document
    if (isEditing() && content != null) {
      // first, handle DOM focus
      FocusedPointRange<Node> htmlSelection = getHtmlSelection(); // save before focusing.

      // element causes webkit based browsers to automatically scroll the element into view
      // In wave, we want to be in charge of how things move, so we cancel this behaviour
      // here by first recording the scrollTops of all the editor's ancestors, and
      // then resetting them after calling focus.
      Element docElement = getDocumentHtmlElement();
      maybeSaveAncestorScrollPositions(docElement);
      FocusImpl.getFocusImplForWidget().focus(DomHelper.castToOld(docElement));
      maybeRestoreAncestorScrollPositions(docElement);

      // then, handle the case when selection already existed inside the element:
      if (htmlSelection != null) {
        // NOTE(patcoleman): we may have killed it with the DOM focusing above, so restore
        NativeSelectionUtil.set(htmlSelection);

        if (!collapsed) {
          // if we have selection, and we're not forcibly collapsing it, then nothing needs doing.
          return;
        } else {
          // Otherwise, we might need to adjust it if we're collapsing it. So we'll fall through to
          // the manual selection-restore-with-collapse, but first we save what we have anyway.
          EditorStaticDeps.logger.trace().log("Saving...");
          doSaveSelection();
        }
      }

      // finally, make sure selection is correct:
      safelyRestoreSelection(aggressiveSelectionHelper, collapsed);
      scheduleUpdateNotification(true, true, false, false);
    }
  }

  @Override
  public boolean hasDocument() {
    return content != null;
  }

  @Override
  public Element getDocumentHtmlElement() {
    assert content != null : "getDocumentHtmlElement: content is null";
    return content.getFullContentView().getDocumentElement().getImplNodelet();
  }

  /**
   * Small helper that sets the selection safely by altering the selection
   * if the initial selections are invalid.
   */
  private void safelyRestoreSelection(SelectionHelper selectionHelper, boolean collapsed) {
    assert savedSelection != null;

    boolean selectionRestored = false;

    FocusedRange sel = savedSelection.getFocusedRange();
    if (sel != null) {
      EditorStaticDeps.logger.trace().log("Focusing, set selection at: " + sel.getFocus());

      // either set to the focus point (to give visual edit cue) or keep entire selection
      if (collapsed) {
        selectionHelper.setCaret(sel.getFocus());
      } else {
        selectionHelper.setSelectionRange(sel);
      }

      // Check if it successfully is restored, even in the content view.
      selectionRestored = selectionHelper.getSelectionPoints() != null;
    }

    if (!selectionRestored) {
      EditorStaticDeps.logger.trace().log("Focusing at last valid point as a catch-all");
      // Either we didn't have a saved selection, or the saved selection
      // ended up being invalid and we didn't set it. In that case,
      // just set it to the end of the document.
      selectionHelper.setCaret(selectionHelper.getLastValidSelectionPoint());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void blur() {
    if (content != null) {
      // The typing extractor may have a pending flush(), which requires a selection to exist.
      // We must therefore force the flush to happen now, because we're about to remove the
      // selection.
      flushSynchronous();

      // NOTE(mtsui: Save selection whenever we stop editing (i.e. submit). This
      // allows us to return to the spot when we reedit the wave. However, this
      // doesn't work if the user clicks on another wave.

      if (NativeSelectionUtil.get() != null) {
        DomHelper.blur(getDocumentHtmlElement());
        NativeSelectionUtil.clear();
      }

      scheduleUpdateNotification(false, true, false, false);
    }
  }


  // Selection saving code
  // TODO(danilatos): Pull this out into a helper class

  private RangeTracker savedSelection;
  private final Scheduler.Task selectionSavingTask = new Scheduler.Task() {
    public void execute() {
      doSaveSelection();
    }
  };

  private void doSaveSelection() {
    if (passiveSelectionHelper != null) {
      assert savedSelection != null;
      FocusedRange range = passiveSelectionHelper.getSelectionRange();
      if (range != null) {
        // We don't want to clear it - only remember the last existing selection
        savedSelection.trackRange(range);
      }
    }
  }

  @Override
  public void flushSaveSelection() {
    doSaveSelection();
    SchedulerInstance.getHighPriorityTimer().cancel(selectionSavingTask);
  }

  private void scheduleSaveSelection() {
    SchedulerInstance.getHighPriorityTimer().schedule(selectionSavingTask);
  }

  // End selection saving code

  // Note: This class implements HtmlSelectionHelper for historic reasons.
  // Components that still use an editor object as their HtmlSelectionHelper
  // impl can assume that that selection helper has the lifetime of the editor.
  // However, the htmlSelectionHelper object, because it is constructed on a
  // content document's doc element, only has the lifetime of an
  // editor<->content association, and it gets replaced in setContent.
  @Override
  public FocusedPointRange<Node> getHtmlSelection() {
    // NOTE(user): If content is null, we shoudln't be trying to get the
    // selection in the first place. However, we often don't know when an
    // editor is closed, such as from inside defered commands. i.e.
    // PasteExtractor, AnnotationPainter.
    return hasDocument() ? htmlSelectionHelper.getHtmlSelection() : null;
  }

  @Override
  public PointRange<Node> getOrderedHtmlSelection() {
    return htmlSelectionHelper.getOrderedHtmlSelection();
  }

  /**
   * TODO(user): consider relaying this question to the ContentElemtn owning
   * the HTML target rather than hard-coding the logic here..
   *
   * @param event
   * @return true if the target of the event is editable in display mode, e.g.,
   *   an input field or radio button
   */
  protected boolean isTargetEditableInDisplayMode(SignalEvent event) {
    Element target = event.getTarget();
    if (DomHelper.isTextNode(target)) {
      target = target.getParentElement();
    }
    return DomHelper.isEditable(target);
  }

  /**
   * @param event
   * @return true if the editor has cause to care about this event at all
   */
  protected boolean editorRelevantEvent(SignalEvent event) {
    // TODO(danilatos): Investigate if the second check is innefficient, and if so,
    // do something smarter (such as, remember if we are in an editable context
    // as long as it doesn't change, rather than check each time).
    return isEditing() || isTargetEditableInDisplayMode(event) ||
      event.isCopyEvent(); // Handle copy event even when editor is not in edit mode.
  }

  /**
   * {@inheritDoc}
   */
  public void onJavaScriptEvent(String name, Event rawEvent) {
    double processTiming = 0;
    Duration instrumentedDuration = null;
    // These events should never be stopped by the editor since other handlers
    // rely on getting to handle them.
    // TODO(danilatos): clean this mess up.
    boolean hackEditorNeverConsumes = name.equals("contextmenu") || name.equals("click")
        || name.equals("mousedown");

    try {
      EditorEvent event = SignalEventImpl.create(EditorEventImpl.FACTORY,
          rawEvent, !hackEditorNeverConsumes);

      try {
        if (UserAgent.isMac() &&
            rawEvent.getCtrlKey() && rawEvent.getAltKey()
            && rawEvent.getTypeInt() == Event.ONKEYPRESS) {
          // In mac safari, Ctrl+Alt+something often inserts weird invisible
          // characters in the dom!!! Always cancel the event, no matter what
          // (We can still handle it programmatically if we wish)

          // Cancel for FF as well just in case - but if this causes an issue
          // it should be OK to enable for FF. We get keypress events, but
          // no dom munge.

          // Don't cancel it on Windows, because that prevents AltGr combos
          // from working in european languages

          EditorStaticDeps.logger.trace().log("Cancelling dangerous: " + rawEvent.getType());

          rawEvent.preventDefault();
        }
      } catch (JavaScriptException e) {
        // If this fails, swallow it. Seems to cause issues with dom mutation
        // events when setting contentEditable with JUnit tests.
      }

      if (event == null) {
        if (!hackEditorNeverConsumes) {
          try {
            rawEvent.stopPropagation();
          } catch (JavaScriptException e) {
            // If this fails, swallow it. Seems to cause issues with dom mutation
            // events when setting contentEditable with JUnit tests.
          }
        }
        return;
      }

      boolean cancel = false;

      if (!hackEditorNeverConsumes) {
        if (editorRelevantEvent(event)) {
          try {
            event.stopPropagation();
          } catch (JavaScriptException e) {
            // If this fails, swallow it. Seems to cause issues with dom mutation
            // events when setting contentEditable with JUnit tests.
          }
        } else {
          return;
        }
      }

      boolean isMutationEvent = event.isMutationEvent();

      if (!isMutationEvent || !EditorStaticDeps.shouldIgnoreMutations()) {
        EditorStaticDeps.startIgnoreMutations();
        try {
          if (debugDisabled) {
            cancel = false;
          } else if (!canHandleBrowserEvents()) {
            // If we're too busy for it to be safe to do anything at all,
            // we cancel the event altogether
            EditorStaticDeps.logger.trace().log("Too busy to handle: ", event);
            cancel = true;
          } else {
            // Cache the selection across multiple calls to Selection.get(), for the
            // duration of this event handler. We turn caching off again in the finally
            // block below.
            NativeSelectionUtil.cacheOn();

            // Normal event handling
            responsibility.startDirectSequence();
            try {
              if (settings.getInstrumentor().shouldInstrument(event)) {
                Duration timer = new Duration();
                cancel = eventHandler.handleEvent(event);
                processTiming = timer.elapsedMillis();
                instrumentedDuration = new Duration();
              } else {
                cancel = eventHandler.handleEvent(event);
              }
            } finally {
              responsibility.endDirectSequence();
            }

            // Alert others that something has happened:
            // NOTE(user): We notify with "user-input" true if we believe this to
            // be a navigation or input event that moves the selection. Events such
            // as mouse scroll are not classified as "user-input" because a user
            // might want to scroll and look at other blips while in edit mode, we
            // shouldn't change the viewport back to the cursor if the user is
            // trying to look at another page
            if (!isMutationEvent) {
              // TODO(user): Remove this check
              boolean trackCursor = shouldTrackCursor(event);

              scheduleSaveSelection();
              scheduleUpdateNotification(trackCursor, trackCursor, false, false);
            }
          }

          if (cancel && !isMutationEvent) {
            EditorStaticDeps.logger.trace().log("Prevent default: ", event);

            // The above code handled the event, so we prevent the browser's
            // default action
            rawEvent.preventDefault();
            // Check health if in debug build
            debugCheckHealth();
          }
        } finally {
          EditorStaticDeps.endIgnoreMutations();
        }
      }

    // NOTE(danilatos): Comment this block for decreased reliability
    // but increased ease of bug finding
    } catch (OperationRuntimeException e) {
      EditorStaticDeps.logger.error().log("Operation Exception - probably an invalid operation -> "
          + "All bets are off!!! Not even going to try to repair!!");

      GWT.getUncaughtExceptionHandler().onUncaughtException(e);
      repairer.showDeath(full().getDocumentElement());
      rawEvent.preventDefault();
      throw e;
    } catch (LocalOperationException e) {
      // Propagate exception if assertions are turned on.
      try {
        assert false;
        EditorStaticDeps.logger.error().logPlainText("Invalid local operation swallowed " + e);
      } catch (AssertionError ae) {
        throw e;
      }
    } catch (RuntimeException e) {
      try {
        // We'll try to repair even though we're re-throwing, because some users keep
        // using the client after it shinies, and some clients using the editor might
        // have a different uncaught exception behaviour.
        EditorStaticDeps.logger.error().log("Repairing: " + e);

        // TODO(danilatos): This is a bit coarse, do more accurate/user friendly handling
        ContentElement el = Element.is(rawEvent.getEventTarget())
            ? nodeManager.findElementWrapper(Element.as(rawEvent.getEventTarget())) : null;
        if (el == null) {
          repairListener.onFullDocumentRevert(mutable());

          // Destroy all rendering
          ContentDocument savedDoc = removeContent();
          savedDoc.setShelved();

          // Re-insert document to re-render from scratch
          setContent(savedDoc);

          repairer.flashShowRepair(full().getDocumentElement());
        } else {
          repairer.revert(
              Point.inElement(el, el.getFirstChild()),
              Point.inElement(el, (ContentNode)null));
        }

        rawEvent.preventDefault();
      } finally {
        // Rethrow on to GWT's uncaught exception handler.
        // Placed in a finally block to guarantee the original, more
        // interesting exception is re-thrown, not an exception potentially
        // thrown by this catch block.

        // if (true) workaround for an eclipse warning about a finally block
        // not completing normally.
        if (true) {
          throw e;
        }
      }

    } finally {
      NativeSelectionUtil.cacheOff();
    }

    if (instrumentedDuration != null) { // was instrumented
      // schedule, executing the previous one first if it hasn't been reported yet.
      if (SchedulerInstance.getHighPriorityTimer().isScheduled(instrumentationTask)) {
        instrumentationTask.execute();
      }
      // set up and instrument
      processKeyPressTimer = processTiming;
      postKeyPressTimer = instrumentedDuration;
      SchedulerInstance.getHighPriorityTimer().schedule(instrumentationTask);
    }
  }

  // TODO(user): Remove this code, it does not belong in the editor.
  private boolean shouldTrackCursor(SignalEvent event) {
    if (event.isMouseButtonEvent()) {
      return true;
    }
    if (event.isKeyEvent()) {
      KeySignalType keySignalType = event.getKeySignalType();
      // The cursor location should move if the user either has modified the
      // content (typed or delete), or move the cursor deliberately.  However, page up/down
      // doesn't actually move the cursor, so we don't want to move the view port
      int keyCode = event.getKeyCode();
      return keySignalType == KeySignalType.INPUT ||
            keySignalType == KeySignalType.DELETE ||
            keySignalType == KeySignalType.NAVIGATION && (
               keyCode != KeyCodes.KEY_PAGEDOWN &&
               keyCode != KeyCodes.KEY_PAGEUP);
    }
    return false;
  }

  /**
   * @param evt
   */
  private boolean fireKeyboardEvent(SignalEvent evt) {
    boolean handled = false;
    if (keySignalListeners != null) {
      for (KeySignalListener l : keySignalListeners) {
        // "|| handled" at end of line to avoid short circuiting
        handled = l.onKeySignal(this, evt) || handled;
      }
    }
    return handled;
  }

  private boolean debugDisabled = false;

  /**
   * If disabled, the editor will essentially just turn into a content editable div.
   * It will still pass events to listeners in any case.
   * @param isDisabled
   */
  public void debugSetDisabled(boolean isDisabled) {
    debugDisabled = isDisabled;
  }

  /**
   * @return Whether or not the editor is in a disabled state
   */
  public boolean debugIsDisabled() {
    return debugDisabled;
  }

  /**
   * @return a reference to the update event implementation for further debugging
   */
  public EditorUpdateEventImpl debugGetUpdateEventImpl() {
    return updateEvent;
  }

//  public boolean debug

  //
  // "Syntatic" (interpretation-free) editor states.
  //

  private boolean isPasting() {
    return pasteExtractor != null && pasteExtractor.isBusy();
  }

  private boolean isTyping() {
    // if typing != null, then domMutationReverter != null, so that's an adequate check.
    // if typing == null, then domMutationReverter will most likely also be null.
    assert (typing == null) == (domMutationReverter == null);
    return typing !=  null && (typing.isBusy() || domMutationReverter.hasPendingReverts());
  }

  //
  // Intermediate semantic (interpretation-full) states.  These states should be defined in terms
  // of either the syntactic states above, or other intermediate semantic states.
  //

  /**
   * @return whether the editor is handling a browser-event, and the handling
   *         lgoic does not require other events to be blocked.
   */
  private boolean isHandlingNonblockingEvent() {
    return isTyping();
  }

  /**
   * @return whether the editor is handling a browser-event, where the
   *         handling logic requires all future browser-events to be cancelled
   *         (prevented) until the handling of this event has completed.
   */
  private boolean isHandlingBlockingEvent() {
    return isPasting();
  }

  /**
   * Tests whether this editor's document includes the intent of all past
   * browser-events.  The document may become inconsistent because some
   * document mutations are caused by delayed-inspection of mutations performed
   * by the browser, and some other mutations are carried out in a time-sliced
   * asynchronous manner.
   *
   * @return whether the editor's document is consistent with all past
   *         browser-events.
   */
  private boolean isConsistentWithEventHistory() {
    return !isHandlingBlockingEvent() && !isHandlingNonblockingEvent();
  }

  /**
   * Tests whether this editor's document is in a consistent state with
   * respect to the operation stream to which it pushes operations.
   * Specifically, this means that the current document state is precisely
   * that which would be produced by applying all operations that this editor
   * has output to the state with which this editor was
   * {@link #setContent(DocInitialization, DocumentSchema) initialized}.
   *
   * @return whether the editor's document is consistent with the outgoing
   *         operation stream.
   */
  private boolean isConsistentWithOutgoingOperationStream() {
    // NOTE(danilatos): Used to be false sometimes when the editor used to
    // queue ops. Clean this logic up later.
    return true;
  }

  /**
   * Tests whether the editor's document is in a consistent state.
   *
   * Consistency is defined in terms of the stack in which the editor sits,
   * with an operation stream beneath it and a (browser-)event generator on
   * top.  This editor's document is in a consistent state if it includes the
   * intent of all browser-events that have happened before now (i.e., is
   * consistent with the state from the layer above), and has pushed out
   * operations to the operation stream that reflect those mutations (i.e.,
   * is also consistent with the layer below).
   *
   * Pragmatically, consistency means that if the editor's browser-event stream
   * is stopped, then the editor can be discarded without loss of information.
   *
   * @return whether the editor's document is consistent.
   */
  public boolean isConsistent() {
    if (content == null) {
      return true;
    }
    return isConsistentWithEventHistory() && isConsistentWithOutgoingOperationStream();
  }

  //
  // Top-level semantic states.  These should be defined only in terms of intermediate semantic
  // states (or syntactic states) in order to keep top-level concerns separated.
  //

  /**
   * Tests whether this editor is in a state where it can handle an arbitrary
   * browser-event.
   *
   * @return whether the editor can handle browser events.
   */
  private boolean canHandleBrowserEvents() {
    return !isHandlingBlockingEvent();
  }

  /**
   * Tests whether this editor is in a state where it is safe to apply an
   * incoming operation to its document.
   *
   * @return whether an incoming operation can be applied to this editor's
   *         document.
   */
  private boolean canApplyIncomingOperations() {
    return permitOperations && isConsistent();
  }

  private UniversalPopup getDebugDialog() {
    if (debugPopup == null) {
      debugPopup = DebugPopupFactory.create(this);
    }
    return debugPopup;
  }

  @Override
  public void debugToggleDebugDialog() {
    if (settings.hasDebugDialog()) {
      UniversalPopup popup = getDebugDialog();
      if (popup.isShowing()) {
        popup.hide();
      } else {
        popup.show();
      }
    }
  }

  private void notifyTypingExtractor(Point<Node> htmlCaret) {
    Preconditions.checkNotNull(htmlCaret, "Notifying typing extractor with invalid selection");

    try {
      typing.somethingHappened(htmlCaret);
    } catch (HtmlMissing e) {
      repairer.handleMissing(e);
    } catch (HtmlInserted e) {
      repairer.handleInserted(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addKeySignalListener(KeySignalListener listener) {
    if (keySignalListeners == null) {
      keySignalListeners = new HashSet<KeySignalListener>();
    }
    keySignalListeners.add(listener);
  }

  @Override
  public void removeKeySignalListener(KeySignalListener listener) {
    if (keySignalListeners != null) {
      keySignalListeners.remove(listener);
    }
  }

  private void onIncomingOp(DocOp operation) {
    EditorStaticDeps.logger.trace().logLazyObjects("Incoming operation", operation);

    if (!hasDocument()) {
      // This is potentially recoverable, consider throwing an exception that
      // can be handled by the wave client.
      throw new IllegalStateException("Cannot apply op to uninitialized editor");
    }

    try {
      editorUndoManager.nonUndoableOp(operation);
      debugCheckHealth();
    } finally {
      // alert others on change:
      // TODO(mtsui/danilatos): We sometimes want incoming ops to notify that the
      // selection coordinates might have changed - if the user cares about the
      // focus of their caret. If they've scrolled away though, this could be annoying.
      // Therefore, we actually need some stateful logic to handle this,
      scheduleUpdateNotification(false, false, true, false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DocInitialization getDocumentInitialization() {
    return content.asOperation();
  }

  /**
   * Asserts that editor is healthy
   */
  private void debugAssertHealthy() {
    assert content.debugCheckHealthy2();
    //TODO(danilatos): Re-implement something equivalent
//    Assert.assertEquals("Content should match shadow",
//        ContentXmlString.createChildren(shadow), ContentXmlString.createChildren(content));
//    content.debugAssertHealthy();
  }

  /**
   * Checks health if in debug build + logs errors
   */
  @VisibleForTesting
  void debugCheckHealth() {
    if (LogLevel.showErrors()) {
      try {
        debugAssertHealthy();
      } catch (Throwable t) {
        GWT.getUncaughtExceptionHandler().onUncaughtException(t);
      }
    }
  }

  @Override
  protected void onAttach() {
    super.onAttach();
    EditorWebDriverUtil.register(this, div);
  }

  @Override
  protected void onDetach() {
    EditorWebDriverUtil.unregister(div);
    super.onDetach();
  }

  /// Editor updates listening system

  private void scheduleUpdateNotification(
      boolean selectionCoordsChanged,
      boolean selectionLocationChanged,
      boolean contentChanged,
      boolean userDirectlyChangedContent) {
    updateEvent.scheduleUpdateNotification(
        selectionCoordsChanged, selectionLocationChanged, contentChanged,
        userDirectlyChangedContent);
  }

  @Override
  public void addUpdateListener(EditorUpdateEvent.EditorUpdateListener listener) {
    updateEvent.addUpdateListener(listener);
  }

  @Override
  public void removeUpdateListener(EditorUpdateEvent.EditorUpdateListener listener) {
    updateEvent.removeUpdateListener(listener);
  }

  @Override
  public void flushUpdates() {
    updateEvent.flushUpdates();
  }


  public EditorEventHandler debugGetEventHandler() {
    return eventHandler;
  }

  ////// Editor context methods
  @Override
  public String getImeCompositionState() {
    return imeExtractor.getContent();
  }

  @Override
  public CMutableDocument getDocument() {
    checkContextConsistency();
    return mutable();
  }

  @Override
  public CaretAnnotations getCaretAnnotations() {
    Preconditions.checkState(caretStyles != null,
        "Using the caret annotations of an editor not set up.");
    checkContextConsistency();
    return caretStyles;
  }

  @Override
  public SelectionHelper getSelectionHelper() {
    Preconditions.checkState(passiveSelectionHelper != null,
        "Using the selection helper of an editor not set up.");
    checkContextConsistency();
    return passiveSelectionHelper;
  }

  /** Called when the editor should be consistent - forces it, and logs if update needed. */
  private void checkContextConsistency() {
    if (!isConsistentWithEventHistory()) {
      flushSynchronous();
      EditorStaticDeps.logger.error().log(
          "Editor context methods called while editor is not consistent.");
    }
  }

  /**
   * Only use for unit tests!!
   */
  public PasteExtractor debugGetPasteExtractor() {
    return pasteExtractor;
  }

  /**
   * Disconnects or connects the editor from the CC stack by rejecting all
   * incoming operations (forcing them to defer until later) and bufferring all
   * outgoing operations.
   *
   * It is safe to reconnect the editor at a later time after disconnecting, by
   * calling method.
   *
   * @param isConnected true to enable suppression, false to disable
   */
  public void debugConnectOpSinks(boolean isConnected) {
    if (permitOperations == isConnected) {
      return;
    }
    permitOperations = isConnected;
    if (permitOperations) {
      for (DocOp op : suppressedOutgoingOps) {
        outgoingOperationSink.consume(op);
      }
      suppressedOutgoingOps = null;
      ScheduleCommand.addCommand(consistencyQueue);
    } else {
      suppressedOutgoingOps = new ArrayList<DocOp>();
    }
  }

  /**
   * @see #debugConnectOpSinks(boolean)
   */
  public boolean debugIsConnected() {
    return permitOperations;
  }

  @Override
  public void flushAnnotationPainting() {
    content.flushAnnotationPainting();
  }


  @Override
  public String toString() {
    return "Editor: [Content: " + content + "]";
  }

  public static final Renderer SIMPLE_RENDERER = new Renderer() {
    @Override
    public Element createDomImpl(Renderable element) {
      return element.setAutoAppendContainer(Document.get().createElement(element.getTagName()));
    }
  };
}
