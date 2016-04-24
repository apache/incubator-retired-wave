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

package org.waveprotocol.wave.client.editor.event;

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.event.dom.client.KeyCodes;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.EventWrapper;
import org.waveprotocol.wave.client.common.util.KeyCombo;
import org.waveprotocol.wave.client.common.util.QuirksConstants;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.common.util.SignalEvent.KeyModifier;
import org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType;
import org.waveprotocol.wave.client.common.util.SignalEvent.MoveUnit;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.constants.BrowserEvents;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentPoint;
import org.waveprotocol.wave.client.editor.content.FocusedContentRange;
import org.waveprotocol.wave.client.editor.content.NodeEventRouter;
import org.waveprotocol.wave.client.editor.event.CompositionEventHandler.CompositionListener;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.SchedulerTimerService;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.CursorDirection;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Central event handler for the editor, encapsulating the core logic for event
 * routing and handling. Application specific handling for combos, etc are done via a
 * subhandler.
 *
 * TODO(user): Remove gwt dependencies so that this is junit testable.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author mtsui@google.com (Mark Tsui)
 */
public final class EditorEventHandler {

  /**
   * States the event handler may be in.
   */
  // TODO(danilatos): Consider separating out other states from normal, such as
  // TYPING, CLIPBOARD, etc, when we are in these transient states.
  enum State {

    /** Normal state */
    NORMAL,

    /** IME composition state */
    COMPOSITION
  }

  /** Reduces the times selection logging is sent to eye3, reporting seems linear with users. */
  private static final int SELECTION_LOG_CULL_FACTOR = 100; // 1/100 sent

  private static final LoggerBundle logger = EditorStaticDeps.logger;

  /**
   * Sets whether unsafe key events are cancelled (set to false for testing)
   */
  private static boolean cancelUnsafeKeyEvents = true;

  private final CompositionListener<EditorEvent> compositionListener =
      new CompositionListener<EditorEvent>() {

        @Override
        public void compositionStart(EditorEvent event) {
          EditorEventHandler.this.compositionStart(event);
        }

        @Override
        public void compositionUpdate() {
          EditorEventHandler.this.compositionUpdate();
        }

        @Override
        public void compositionEnd() {
          EditorStaticDeps.startIgnoreMutations();
          try {
            EditorEventHandler.this.compositionEnd();
          } finally {
            EditorStaticDeps.endIgnoreMutations();
          }
        }
  };

  private final boolean weirdComposition =
      QuirksConstants.MODIFIES_DOM_AND_FIRES_TEXTINPUT_AFTER_COMPOSITION;

  private final boolean useCompositionEvents;

  /**
   * Sets whether we use whitelisting or blacklisting to potentially cancel
   * unhandled keycombos.
   */
  private final boolean useWhiteListing;

  /**
   * Current selection. Ensure this is always set correctly, especially
   * if it's changed or invalidated.
   */
  private FocusedContentRange cachedSelection;

  /**
   * Interact with the editor through this interface.
   */
  private final EditorInteractor editorInteractor;

  private final NodeEventRouter router;

  /**
   * We keep track of whether selection affinity is up to date. When we receive
   * an event, we assume that the event will invalidate the selection affinity,
   * thus we set selectionAffinityMaybeChanged to true. If we later find out
   * that the event does not modify selection affinity, we set
   * selectionAffinityMaybeChanged to false.
   *
   * If at the end of the event loop, selectionAffinityMaybeChanged
   */
  private boolean needToSetSelectionAffinity = true;
  private boolean selectionAffinityMaybeChanged = true;

  /** Tracks whether there was selection at the start of an event handling run. */
  private boolean hadInitialSelection;

  private State state = State.NORMAL;

  /**
   * Handler for higher level, application specific event handling.
   */
  private final EditorEventsSubHandler subHandler;

  private final CompositionEventHandler<EditorEvent> compositionHandler;

  /**
   * @param editorInteractor
   * @param subHandler
   */
  public EditorEventHandler(EditorInteractor editorInteractor, EditorEventsSubHandler subHandler,
      NodeEventRouter router,
      boolean useWhiteListFlag, boolean useWebkitCompositionFlag) {
    this(new SchedulerTimerService(SchedulerInstance.get(), Scheduler.Priority.CRITICAL),
        editorInteractor, subHandler, router,
        useWhiteListFlag,
        // We may want to turn off composition events for webkit if something goes wrong...
        QuirksConstants.SUPPORTS_COMPOSITION_EVENTS &&
            (UserAgent.isWebkit() ? useWebkitCompositionFlag : true));
  }

  EditorEventHandler(TimerService criticalTimerService, EditorInteractor interactor,
     EditorEventsSubHandler subHandler, NodeEventRouter router,
     boolean useWhiteListing, boolean useCompositionEvents) {
    this.editorInteractor = interactor;
    this.subHandler = subHandler;
    this.router = router;
    this.useWhiteListing = useWhiteListing;
    this.compositionHandler = new CompositionEventHandler<EditorEvent>(
        criticalTimerService, compositionListener, logger, weirdComposition);
    this.useCompositionEvents = useCompositionEvents;
  }

  /** Visible for testing */
  State getState() {
    return state;
  }

  static int selectionLogCullRotation = 0;

  /**
   * @param signal
   * @return true if its handled
   */
  public boolean handleEvent(EditorEvent signal) {
    if (editorInteractor.notifyListeners(signal)) {
      // The listeners themselves can cancel the event if they wish.
      return false;
    }

    // Wraps handleEventInner to update the selectionAffinity variables.
    selectionAffinityMaybeChanged = true;
    hadInitialSelection = editorInteractor.hasContentSelection();
    boolean retVal = true;

    try {
      retVal = handleEventInner(signal);
    } catch (SelectionLostException e) {
      if (e.hasLostSelection() &&
          (LogLevel.showDebug() || (selectionLogCullRotation++ % SELECTION_LOG_CULL_FACTOR) == 0)) {
        EditorStaticDeps.logger.error().log(e);
      }

      // NOTE(patcoleman): we assume that if there was no selection to start with, that the
      // html selection is inside a part with no corresponding content node (e.g. inside doodad
      // or textbox). In this case it's not cancelled, so the browser can deal with it.
      retVal = e.hasLostSelection();
    }

    if (selectionAffinityMaybeChanged) {
      needToSetSelectionAffinity = true;
    }
    return retVal;
  }

  private boolean handleEventInner(EditorEvent event) throws SelectionLostException {
    // TODO(danilatos): IE IME keycode thingy!!
    invalidateSelection();

    // NOTE(patcoleman): special cases FTW!
    // 1) click can be while the editor isn't editing, so needs to avoid needing content selection.
    if (event.isMouseEvent()) {
      // Flush because the selection location may have changed to somewhere
      // else in the same text node. We MUST handle mouse down events for
      // this.
      editorInteractor.forceFlush();
      ContentElement node = editorInteractor.findElementWrapper(event.getTarget());
      event.setCaret(new ContentPoint(node, null));
      if (node != null && event.isClickEvent()) {
        router.handleClick(node, event);
        editorInteractor.clearCaretAnnotations();
        editorInteractor.rebiasSelection(CursorDirection.NEUTRAL);
        return !event.shouldAllowBrowserDefault();
      } else {
        return false;
      }
    }

    // 2) Only update selection if we know it's needed:
    if (checkIfValidSelectionNeeded(event)) {
      refreshEditorWithCaret(event);
      if (cachedSelection == null) {
        // disallow events if we don't know where the selection is - probably something's botched
        // lars: only in editing mode; otherwise we block, e.g., keyboard manipulation
        // of radio buttons
        return editorInteractor.isEditing();
      }
    }

    if (weirdComposition && state == State.COMPOSITION) {
      if (!event.isCompositionEvent()) {
        compositionHandler.handleOtherEvent();
      }
    }

    // Handle:
    if (event.isKeyEvent()) {
      return handleKeyEvent(event);
    } else if (event.isCompositionEvent()) {
      if (useCompositionEvents) {
        return handleCompositionEvent(event);
      } else {
        return false;
      }
    } else if (event.isClipboardEvent()) {
      if (event.isPasteEvent()) {
        return subHandler.handlePaste(event);
      } else if (event.isCutEvent()) {
        return subHandler.handleCut(event);
      } else if (event.isCopyEvent()) {
        return subHandler.handleCopy(event);
      } else {
        // These are onbeforecopy/onbeforepaste etc.. We are not currently
        // interested, and they are harmless so just allow.
        return false;
      }
    } else if (event.isMutationEvent()) {
      selectionAffinityMaybeChanged = false;
      if (!editorInteractor.isExpectingMutationEvents()) {
        if (DomHelper.isTextNode(event.getTarget())) {
          cachedSelection = editorInteractor.getSelectionPoints();
          if (cachedSelection != null) {
            if (!cachedSelection.isCollapsed()) {
              logger.trace().logPlainText("WARNING: Probable IME input on non-collapsed " +
                  "range not handled!!!");
              // TODO(dan/patcoleman): Yeargh, IME killing a range!!! Nooo!!!!
              // Handle eeet
            }
            logger.trace().logPlainText("Notifying typing extractor for " +
                "probable IME-caused mutation event");
            // Nothing to do with the return value of this method, as mutation
            // events are not cancellable.
            editorInteractor.notifyTypingExtractor(cachedSelection.getFocus(), false, false);
          }
        }
      }
      if (QuirksConstants.LIES_ABOUT_CARET_AT_LINK_END_BOUNDARY) {
        checkForWebkitEndOfLinkHack(event);
      }
      subHandler.handleDomMutation(event);
      return false;
    } else if (event.isFocusEvent()) {
      return false;
    } else {
      // cancel anything we don't know about
      logger.trace().log("Cancelling: " + event.getType());
      return true;
    }
  }

  void checkForWebkitEndOfLinkHack(SignalEvent signal) {
    // If it's inserting text
    if (DomHelper.isTextNode(signal.getTarget()) &&
        (signal.getType().equals(BrowserEvents.DOMCharacterDataModified) ||
         signal.getType().equals(BrowserEvents.DOMNodeInserted))) {

      Text textNode = signal.getTarget().cast();
      if (textNode.getLength() > 0) {
        Node e = textNode.getPreviousSibling();
        if (e != null && !DomHelper.isTextNode(e)
            && e.<Element>cast().getTagName().toLowerCase().equals("a")) {

          FocusedPointRange<Node> selection =  editorInteractor.getHtmlSelection();
          if (selection.isCollapsed() && selection.getFocus().getTextOffset() == 0) {
            editorInteractor.noteWebkitEndOfLinkHackOccurred(textNode);
          }
        }
      }
    }
  }

  private boolean handleKeyEvent(EditorEvent event) throws SelectionLostException {
    KeySignalType keySignalType = event.getKeySignalType();

    switch (state) {
      case NORMAL:
        if (isAccelerator(event)) {
          refreshEditorWithCaret(event);
          if (subHandler.handleCommand(event)
              || subHandler.handleBlockLevelCommands(event,
                  cachedSelection.asOrderedRange(editorInteractor.selectionIsOrdered()))) {
            return true;
          }

          if (cachedSelection.isCollapsed()) {
            if (subHandler.handleCollapsedKeyCombo(event, cachedSelection.getFocus())) {
              return true;
            }
          } else {
            if (subHandler.handleRangeKeyCombo(event,
                cachedSelection.asOrderedRange(editorInteractor.selectionIsOrdered()))) {
              return true;
            }
          }
          return shouldCancelAcceleratorBrowserDefault(event);
        }

        switch(keySignalType) {
          case INPUT:
          case DELETE:
            return handleInputOrDeleteKeyEvent(event, keySignalType);
          case NAVIGATION:
            return handleNavigationKeyEvents(event);
          case NOEFFECT:
            return false;
        }
        throw new RuntimeException("Unhandled signal type");

      case COMPOSITION:
        // NOTE(danilatos): From my investigations, during IME composition, the browser itself
        // pretty much disables all the combos. Or, it has its own strange buggy behaviour
        // without us doing anything. Therefore, we can pretty much ignore key events during
        // composition mode.
        return false;
      default:
        throw new RuntimeException("Unhandled state");
    }
  }

  private boolean handleCompositionEvent(EditorEvent event) {
    return compositionHandler.handleCompositionEvent(event, event.getType());
  }

  private void compositionStart(EditorEvent event) {
    if (state == State.COMPOSITION) {
      logger.error().log("State was already IME during a compositionstart event!");
    }

    Point<ContentNode> caret;
    if (cachedSelection == null) {
      logger.error().log("No selection during a composition start event? Maybe it's " +
          "deep inside some doodad's html?");
      caret = null;
    } else if (cachedSelection.isCollapsed()) {
      caret = cachedSelection.getFocus();
    } else {
      caret = deleteCachedSelectionRangeAndInvalidate(true);
    }

    state = State.COMPOSITION;
    editorInteractor.compositionStart(caret);
  }


  private void compositionUpdate() {
    editorInteractor.compositionUpdate();
  }


  private void compositionEnd() {
    // We update the cached selection because sometimes we'll immediately get called back
    // into compositionStart()
    cachedSelection = editorInteractor.compositionEnd();
    state = State.NORMAL;
  }


  private boolean handleInputOrDeleteKeyEvent(EditorEvent event, KeySignalType keySignalType)
      throws SelectionLostException {
    // !!!!!!!!!
    // TODO(danilatos): This caret is in the wrong (full) view, and can die when
    // applied to mutable doc!!!! Only OK right now out of sheer luck.
    // !!!!!!!!!
    Point<ContentNode> caret;

    boolean isCollapsed = editorInteractor.getHtmlSelection() != null &&
        editorInteractor.getHtmlSelection().isCollapsed();
    boolean isReplace = false;

    if (isCollapsed) {
      MoveUnit moveUnit = event.getMoveUnit();
      if (moveUnit != MoveUnit.CHARACTER) {
        if (event.getMoveUnit() == MoveUnit.WORD) {
          if (event.getKeyCode() == KeyCodes.KEY_BACKSPACE) {
            refreshEditorWithCaret(event);
            caret = cachedSelection.getFocus();
            editorInteractor.deleteWordEndingAt(caret);
          } else if (event.getKeyCode() == KeyCodes.KEY_DELETE){
            refreshEditorWithCaret(event);
            caret = cachedSelection.getFocus();
            editorInteractor.deleteWordStartingAt(caret);
          }
        }
        // TODO(user): Manually handle line/other etc. deletes, because
        // they might contain formatting, etc. For now, cancelling for safety.
        return true;
      } else {
        // HACK(danilatos/patcoleman): We don't want the caret to get set here,
        // because it is not safe unless we continually flush the typing extractor
        // which is undesirable.
        // NOTE #XYZ (this comment referenced from elsewhere)
        // To fix this properly, we need to restructure the control flow, and
        // possibly change the types of caret we pass around.
        caret = null;
      }

    } else {
      refreshEditorWithCaret(event);

      // NOTE: at this point, should be either INPUT or DELETE
      boolean isDelete = (keySignalType == KeySignalType.DELETE);

      if (event.isImeKeyEvent()) {
        // Semi-HACK(danilatos): sometimes during composition, the selection will be reported
        // as a range. We want to leave this alone, not delete it. Since we're not handling
        // ranged deletions with non-FF ime input properly anyway, this will do.
        caret = cachedSelection.getFocus();
      } else {
        caret = deleteCachedSelectionRangeAndInvalidate(!isDelete); // keep annotations on insert
      }

      if (isDelete) {
        return true; // Did a range delete already. Do not go on to typing extractor.
      } else {
        isReplace = true;
      }
    }

    if (keySignalType == KeySignalType.DELETE) {
      refreshEditorWithCaret(event);
      caret = cachedSelection.getFocus();
      ContentNode node = caret.getContainer();

      editorInteractor.checkpoint(new FocusedContentRange(caret));

      switch (EventWrapper.getKeyCombo(event)) {
        case BACKSPACE:
        case SHIFT_BACKSPACE:
          editorInteractor.rebiasSelection(CursorDirection.FROM_RIGHT);
          return router.handleBackspace(node, event);
        case SHIFT_DELETE:
          if (!QuirksConstants.HAS_OLD_SCHOOL_CLIPBOARD_SHORTCUTS) {
            // On a mac, shift+delete is the same as regular delete.
            editorInteractor.rebiasSelection(CursorDirection.FROM_LEFT);
            return router.handleDelete(node, event);
          } else {
            // On windows & linux, shift+delete is cut
            // It should have been caught earlier by the isAccelerator check
            throw new RuntimeException("Shift delete should have been caught"
                + "as an accelerator event!");
          }
        case DELETE:
          editorInteractor.rebiasSelection(CursorDirection.FROM_LEFT);
          return router.handleDelete(node, event);
      }
    } else if (handleEventsManuallyOnNode(event, caret)){
      return true;
    }

    return handleNormalTyping(event, caret, isReplace);
  }

  private Point<ContentNode> deleteCachedSelectionRangeAndInvalidate(boolean isReplace) {
    // !!!!!!!!!
    // TODO(danilatos): This caret is in the wrong (full) view, and can die when
    // applied to mutable doc!!!! Only OK right now out of sheer luck.
    // !!!!!!!!!
    editorInteractor.checkpoint(cachedSelection);

    Point<ContentNode> start;
    Point<ContentNode> end;
    if (editorInteractor.selectionIsOrdered()) {
      start = cachedSelection.getAnchor();
      end = cachedSelection.getFocus();
    } else {
      end = cachedSelection.getAnchor();
      start = cachedSelection.getFocus();
    }

    Point<ContentNode> caret = null;
    caret = editorInteractor.deleteRange(start, end, isReplace);
    setCaret(caret);

    assert cachedSelection == null;
    return caret;
  }

  private boolean handleNormalTyping(EditorEvent event, Point<ContentNode> caret, boolean isReplace)
      throws SelectionLostException  {

    // Note that caret may be null if this is called during typing extraction

    // Normal typing
    selectionAffinityMaybeChanged = false;

    // NOTE(danilatos): We can't tell if a key event is IME in firefox, so
    // we just always do typing extraction instead.
    // Additionally, even for normal key strokes, firefox has strange
    // behaviour when handling them programmatically. The cursor appears
    // to lag a character behind, and there are selection half-disappearing
    // issues when deleting around annotation boundaries.
    boolean useTypingExtractor = event.isImeKeyEvent() || UserAgent.isFirefox();

    if (useTypingExtractor) {
      // Just normal typing. Send to typing extractor.
      if (editorInteractor.isTyping()) {
        // NOTE(patcoleman): Do not change affinity while normal typing, our affinity should
        // remain consistent across normal typing.
        logger.trace().log("Not notifying typing extractor, already notified");
      } else {
        if (UserAgent.isFirefox()) {
          // NOTE(user): This is one way of handling the affinity problem.
          // The other method is to detect where the selection is, and modify
          // the behaviour of typing extractor/document such that when the
          // typing is extracted, the formatting applied to the content doc
          // matches the html impl.
          // TODO(user): This doesn't handle the case for persistent inline
          // elements where the browser may automatically place the cursor. We
          // don't currently have such elements, but we'll need to consider
          // this case in the future.
          refreshEditorWithCaret(event);
          caret = maybeSetSelectionLeftAffinity(event.getCaret().asPoint());
          event.setCaret(ContentPoint.fromPoint(caret));
        } else {
          // Caret might be null
        }

        logger.trace().log("Notifying typing extractor");
        return editorInteractor.notifyTypingExtractor(caret, caret == null, isReplace);
      }
      return false;
    } else {
      char c = (char) event.getKeyCode();
      refreshEditorWithCaret(event);
      caret = cachedSelection.getFocus(); // Is it safe to delete this line?
      caret = editorInteractor.insertText(caret, String.valueOf(c), isReplace);
      caret = editorInteractor.normalizePoint(caret);
      setCaret(caret);
      editorInteractor.rebiasSelection(CursorDirection.FROM_LEFT);
      return true;
    }
  }

  private boolean handleEventsManuallyOnNode(EditorEvent event, Point<ContentNode> caret)
      throws SelectionLostException {
    // Note that caret may be null if this is called during typing extraction

    // Always handle enter specially, and always cancel the default action.
    // TODO(danilatos): This is still a slight anomaly, to call a
    // node.handleXYZ method here.
    if (event.isOnly(KeyCodes.KEY_ENTER)) {
      refreshEditorWithCaret(event);
      caret = event.getCaret().asPoint();
      editorInteractor.checkpoint(new FocusedContentRange(caret));
      router.handleEnter(caret.getContainer(), event);
      editorInteractor.rebiasSelection(CursorDirection.FROM_LEFT);
      return true;
    } else if (event.isCombo(KeyCodes.KEY_ENTER, KeyModifier.SHIFT)) {
      // shift+enter inserts a "newline" (such as a <br/>) by default
      // TODO(danilatos): Form elements want to handle this.
      return true;
    }
    return false;
  }

  private boolean handleNavigationKeyEvents(EditorEvent event) {
    editorInteractor.checkpoint(null);
    editorInteractor.clearCaretAnnotations();
    ContentNode node = cachedSelection.getFocus().getContainer();
    logger.trace().log("Navigation event");

    // Not using key combo, because we want to handle left key with
    // any modifiers also applying.
    // TODO(danilatos): MoveUnit, and holding down shift for selection.
    if (event.getKeyCode() == KeyCodes.KEY_LEFT) {
      router.handleLeft(node, event);
      editorInteractor.rebiasSelection(CursorDirection.FROM_RIGHT);
      return !event.shouldAllowBrowserDefault();
    } else if (event.getKeyCode() == KeyCodes.KEY_RIGHT) {
      router.handleRight(node, event);
      editorInteractor.rebiasSelection(CursorDirection.FROM_LEFT);
      return !event.shouldAllowBrowserDefault();
    } else {
      editorInteractor.rebiasSelection(CursorDirection.NEUTRAL);
    }
    return false;
  }

  private Point<ContentNode> maybeSetSelectionLeftAffinity(Point<ContentNode> caret) {
    if (!needToSetSelectionAffinity) {
      return caret;
    }
    needToSetSelectionAffinity = false;

    Point<ContentNode> newCaret = editorInteractor.normalizePoint(caret);
    if (newCaret != caret) {
      editorInteractor.setCaret(newCaret);
    }
    return newCaret;
  }

  /**
   * Tells us if this key event is an "accelerator" key event.
   *
   * For lack of a better word, basically this means keys & combos that aren't
   * used for basic input, deletion, and navigation. See the implementation
   * comments for details.
   *
   * @param event Must be a key event!
   * @return true if this event is an accelerator key sequence.
   */
  static boolean isAccelerator(SignalEvent event) {
    return isAcceleratorInner(event, UserAgent.isMac(),
        QuirksConstants.HAS_OLD_SCHOOL_CLIPBOARD_SHORTCUTS);
  }

  /**
   * Parameterised to allow testing different browser/os permuations
   * @param event
   * @param isMac
   * @param quirksHasOldSchoolClipboardShortcuts
   */
  @VisibleForTesting
  static boolean isAcceleratorInner(SignalEvent event, boolean isMac,
      boolean quirksHasOldSchoolClipboardShortcuts) {
    switch (event.getKeySignalType()) {
      case INPUT:
        // Alt on its own is a simple modifier, like shift, on OSX
        boolean maybeAltKey = !isMac && event.getAltKey();

        // NOTE(user): Perhaps we should create a registry in
        // EditorEventSubHandler of non-metesque like command keys such as TAB.
        // For now TAB is our only special case, but we may need to allow
        // implementers to define arbitrary keys as accelerators.
        return event.getCtrlKey() || event.getMetaKey() || event.getKeyCode() == KeyCodes.KEY_TAB
            || maybeAltKey;
      case DELETE:
        if (quirksHasOldSchoolClipboardShortcuts &&
            event.getKeyCode() == KeyCodes.KEY_DELETE && KeyModifier.SHIFT.check(event)) {

          // shift+delete on windows/linux is cut
          // (shift+insert and ctrl+insert are other clipboard alternatives,
          // but that's handled below).

          return true;
        } else {
          return false;
        }
      case NAVIGATION:
        // All navigation does not count
        return false;
      case NOEFFECT:
        // Random special keys like ESC, F7, TAB, INS, etc count
        return true;
    }
    throw new RuntimeException("Unknown KeySignal type");
  }

  /**
   * @param acceleratorEvent Must be a key event AND isAccelerator(event) == true
   * @return whether we should cancel the browser's default action
   */
  private boolean shouldCancelAcceleratorBrowserDefault(SignalEvent acceleratorEvent) {
    // (more verbose name in argument to remind us of the constraint).
    SignalEvent event = acceleratorEvent;

    // First, handle non-combo events (here they should only be "NOEFFECT" keys)
    // We use blacklisting for these.
    // TODO(danilatos/mtsui): Switch to whitelisting as well?
    if (KeyModifier.NONE.check(event)) {
      if (event.getKeyCode() == EventWrapper.KEY_INSERT) {
        // Cancel INSERT to prevent overwrite mode, for now
        // (Happens in IE).
        return true;
      } else {
        // Other things like ESC, TAB, function keys, etc are OK.
        return cancelUnsafeKeyEvents;
      }
    }

    if (isAllowableCombo(event)) {
      // We can safely ignore
      logger.trace().log("Allowing event");
      return false;
    }

    if (logger.trace().shouldLog()) {
      logger.trace().log("unsafe combo: ", event.getType(), event.getKeyCode());
    }
    return cancelUnsafeKeyEvents;
  }

  private boolean isAllowableCombo(SignalEvent sEvent) {
    // Detect inconsistency between whitelist and blacklist.
    checkBlackWhiteListConsistency(sEvent);
    if (isWhiteListedCombo(sEvent)) {
      return true;
    }

    if (useWhiteListing) {
      // If we are using whitelisting, disallow all events that didn't pass the
      // above check.
      return false;
    } else {
      // TODO(user): Log a sample of these combos to the server, so we can
      // analyse these and perhaps add a class of keys to the whitelist. Also
      // store this string somewhere so in case an exception is thrown later, it
      // can be associated with this event.
      if (logger.trace().shouldLog()) {
        logger.trace().log("not in whitelist: ", sEvent);
      }

      // Otherwise return allow events that are not in the blacklist.
      return !isBlackListedCombo(sEvent);
    }
  }

  private boolean checkBlackWhiteListConsistency(SignalEvent sEvent) {
    boolean isConsistent = !(isWhiteListedCombo(sEvent) && isBlackListedCombo(sEvent));
    if (!isConsistent) {
      String message =
          "Combo both whitelisted and blacklisted! " + sEvent.getKeyCode();
      assert false : message;
      logger.error().logPlainText(message);
    }
    return isConsistent;
  }

  /**
   * These key combos can be safely ignored. They don't directly modify the
   * editable region, but may perform something useful on the browser so we
   * don't want to cancel them. i.e. copy/cut/paste key events.
   *
   * Combos listed here should be accompanied with a comment stating the reason.
   *
   * Maintaining this whitelist is quite an effort, but at least we shouldn't
   * get the browser blowing up if the user entered some keycombo we don't know
   * about.
   *
   *
   * References:
   * http://support.mozilla.com/en-US/kb/Keyboard+shortcuts
   * http://docs.info.apple.com/article.html?artnum=42951
   * http://www.microsoft.com/windows/products/winfamily/ie/quickref.mspx
   *
   * @return true if it is safe to ignore, or false which will result in further
   *         handling.
   */
  private boolean isWhiteListedCombo(SignalEvent signal) {
    KeyCombo keyCombo = EventWrapper.getKeyCombo(signal);
    switch (keyCombo) {
      // Edit actions:
      // Allow cut/copy/paste combos and handle the actual clipboard events
      // later.
      case ORDER_C: // copy
      case ORDER_X: // cut
      case ORDER_V: // paste
      case ORDER_A: // select all
      case ORDER_P: // print
      case ORDER_L: // navigate to url box

      // Page navigation
      // On safari, delete/backspace is normally used to go back as well, but
      // of course in the editor we won't allow that.
      case META_LEFT: // back
      case META_RIGHT: // forward
      case META_HOME: // home
      case ORDER_O: // open file
      case ORDER_R: // reload
      case ORDER_SHIFT_R: // reload (override cache)

      // Search
      case ORDER_F: // find
      case ORDER_G: // find again

      // tools
      case ORDER_D: // bookmark this page

     // Window and tabs
      case ORDER_N: // new window
      case ORDER_T: // new tab
      case ORDER_W: // close window
      case ORDER_Q: // quit
        return true;

      default:
    }

    if (QuirksConstants.HAS_OLD_SCHOOL_CLIPBOARD_SHORTCUTS) {
      if (isAlternateClipboardCombo(signal)) {
        return true;
      }
    }

    if (UserAgent.isSafari() && UserAgent.isMac()) {
      // Navigation events for Mac Safari only.
      switch (keyCombo) {
        case CTRL_A:
        case CTRL_B:
        case CTRL_E:
        case CTRL_F:
          return true;
        default:
      }
    }

    return false;
  }

  private boolean isBlackListedCombo(SignalEvent event) {
    KeyCombo keyCombo = EventWrapper.getKeyCombo(event);
    switch (keyCombo) {
      // Disallow undo
      case ORDER_Z:
        return true;
    }

    if (UserAgent.isMac()) {
      switch (keyCombo) {
        case CTRL_D: // Deletes a character, needs to be handled manually
        case CTRL_H: // Deletes a character backwards
        case CTRL_K: // Deletes to end of line, needs to be handled manually
          return true;
      }

      if (UserAgent.isFirefox()) {
        switch (keyCombo) {
          case CTRL_W: // Deletes a word backwards
            return true;
          case CTRL_U: // Kills line
            // NOTE(user): Implement this when Firefox updates their selection API.
            return true;
        }
      }

      if (UserAgent.isWebkit()) {
        switch (keyCombo) {
          case CTRL_O: // Inserts a new line
            return true;
        }
      }
    }

    if (QuirksConstants.PLAINTEXT_PASTE_DOES_NOT_EMIT_PASTE_EVENT
        && keyCombo == KeyCombo.ORDER_ALT_SHIFT_V) {
      return true;
    }

    return false;
  }

  private boolean isAlternateClipboardCombo(SignalEvent signal) {
    switch (EventWrapper.getKeyCombo(signal)) {
      // Edit actions:
      // Allow cut/copy/paste combos and handle the actual clipboard events
      // later.
      case SHIFT_DELETE: // cut (win + linux only)
      case CTRL_INSERT: // copy (win + linux only)
      case SHIFT_INSERT: // paste (win + linux only)
        return true;
      default:
        return false;
    }
  }

  // If any of these abstract methods return true, we stop processing the signal
  // We also prevent default for those named with "handleXYZ" if true is returned

  private void setCaret(Point<ContentNode> caret) {
    invalidateSelection();
    editorInteractor.setCaret(caret);
  }

  private void invalidateSelection() {
    cachedSelection = null;
  }

  /**
   * Flushes the editor, and updates the caret of the event to be the new start of selection.
   */
  private void refreshEditorWithCaret(EditorEvent event) throws SelectionLostException {
    // NOTE(patcoleman): don't call interactor's flush outside here - it is possible the rest of the
    // event states will not be updated correctly.
    editorInteractor.forceFlush();
    cachedSelection = editorInteractor.getSelectionPoints();
    if (cachedSelection != null) {
      event.setCaret(ContentPoint.fromPoint(cachedSelection.getFocus()));
    } else {
      throw new SelectionLostException("Null selection after force flushing editor, "
          + "event = " + event.getType(), hadInitialSelection);
    }
  }

  /**
   * A check extracted out, to see whether a particular event requires a valid refreshed selection.
   */
  private boolean checkIfValidSelectionNeeded(EditorEvent event) {
    if (event.isMutationEvent() || event.isFocusEvent()) {
      return false; // mutations or focus don't mutate the document at this stage.
    } else if (event.isKeyEvent() && state == State.NORMAL) {
      if (event.isImeKeyEvent()) {
        return false; // ime typing can be extracted not on firefox
      } else if(event.getKeySignalType() == KeySignalType.INPUT) {
        return false; // normal typing can be extracted on firefox
      }
    }
    return true;
  }

  /**
   * This may not be always correct, but may be useful when the selection is
   * not otherwise available, i.e. when the editor is blurred.
   */
  public FocusedContentRange getCachedSelection() {
    return cachedSelection;
  }

  /**
   * Sets whether unsafe combos are cancelled.
   */
  public static void setCancelUnsafeCombos(boolean shouldCancel) {
    cancelUnsafeKeyEvents = shouldCancel;
  }

  /**
   * Gets whether unsafe combos are cancelled.
   */
  public static boolean getCancelUnsafeCombos() {
    return cancelUnsafeKeyEvents;
  }

  /**
   * Checked exception for finding any places the editor unexpectedly
   * has no selection - as this probably indicates a bug.
   */
  private static class SelectionLostException extends Exception {
    private final boolean lostSelection;
    public SelectionLostException(String message, boolean lost) {
      super(message + ". Selection was " + (lost ? "" : "not ") + "lost.");
      this.lostSelection = lost;
    }
    public boolean hasLostSelection() {
      return lostSelection;
    }
  }
}
