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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.junit.client.GWTTestCase;

import org.waveprotocol.wave.client.common.util.EventWrapper;
import org.waveprotocol.wave.client.common.util.FakeSignalEvent;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.common.util.SignalEvent.KeyModifier;
import org.waveprotocol.wave.client.common.util.SignalEvent.KeySignalType;
import org.waveprotocol.wave.client.common.util.SignalKeyLogic;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentPoint;
import org.waveprotocol.wave.client.editor.content.ContentRange;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.FocusedContentRange;
import org.waveprotocol.wave.client.editor.content.NodeEventRouter;
import org.waveprotocol.wave.client.editor.testing.FakeEditorEvent;
import org.waveprotocol.wave.client.scheduler.testing.FakeTimerService;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.CursorDirection;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for EditorEventHandler.
 *
 * TODO(user): Use jmock when this no longer needs to be a gwt test case.
 *
 */

public class EditorEventHandlerGwtTest
//  extends JavaGWTTestCase {
  extends GWTTestCase {
  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.editor.event.Tests";
  }

//  @Override protected void setUp() throws Exception {
//    super.setUp();
//    super.getBrowserEmulator().setGWTcreateClass(
//        UserAgentStaticProperties.class,
//        UserAgentStaticProperties.FirefoxImpl.class);
//    super.getBrowserEmulator().setIsClient(false);
//  }

  private EditorEventHandler createEditorEventHandler(FakeRouter router,
      FakeEditorInteractor interactor, EditorEventsSubHandler subHandler) {
    return new EditorEventHandler(interactor, subHandler, router, true, true);
  }

  private EditorEventHandler createEditorEventHandler(
      FakeEditorInteractor interactor, EditorEventsSubHandler subHandler) {
    return new EditorEventHandler(interactor, subHandler, new FakeRouter(), true, true);
  }

  /**
   * Set up a FakeEditorInteractor with some default expectations.
   *
   * - allow any number of calls to getSelectionPoints, forceFlush, isEditing,
   * setCaret, normalizePoint and notifyingListeners
   *
   * - set default return values for some of these
   *
   * - normalizePoint returns the start of selection. This might be unexpected,
   * but is convenient for most cases.
   *
   * @param selection
   */
  private FakeEditorInteractor setupFakeEditorInteractor(FocusedContentRange selection) {
    FakeEditorInteractor interactor = new FakeEditorInteractor();
    interactor.call(FakeEditorInteractor.GET_SELECTION_POINTS).anyOf().returns(selection);
    interactor.call(FakeEditorInteractor.FORCE_FLUSH).anyOf();
    interactor.call(FakeEditorInteractor.IS_EDITING).anyOf().returns(true);
    interactor.call(FakeEditorInteractor.SET_CARET).anyOf();
    interactor.call(FakeEditorInteractor.NOTIFYING_LISTENERS).anyOf().returns(false);
    interactor.call(FakeEditorInteractor.CHECKPOINT).anyOf();
    interactor.call(FakeEditorInteractor.HAS_CONTENT_SELECTION).anyOf().returns(true);
    interactor.call(FakeEditorInteractor.REBIAS_SELECTION).anyOf();

    FocusedPointRange<Node> htmlSelection = null;
    if (selection != null) {
      interactor.call(FakeEditorInteractor.NORMALIZE_POINT).anyOf().returns(selection.getFocus());
      htmlSelection = new FocusedPointRange<Node>(
          toNodePoint(selection.getAnchor()), toNodePoint(selection.getFocus()));
    }
    interactor.call(FakeEditorInteractor.GET_HTML_SELECTION).anyOf().returns(htmlSelection);
    return interactor;
  }


  private Point<Node> toNodePoint(Point<ContentNode> content) {
    if (content == null) {
      return null;
    } else {
      if (content.isInTextNode()) {
        return Point.inText(content.getContainer().getImplNodelet(), content.getTextOffset());
      } else {
        Node post = content.getNodeAfter() == null ? null : content.getNodeAfter().getImplNodelet();
        return Point.inElement(content.getContainer().getImplNodelet(), post);
      }
    }
  }

  private ContentElement newParaElement() {
    return new ContentElement("p", Document.get().createPElement(), null);
  }

  /**
   * Ensure that the event handler normalises the selection when necessary
   * Note that this is currently just for firefox.
   */
  public void testNormalisesSelection() {
    FakeEditorEvent fakeEvent = FakeEditorEvent.create(KeySignalType.INPUT, 'a');
    final Point<ContentNode> caret =
        Point.<ContentNode> end(newParaElement());

    EditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    FakeEditorInteractor interactor = setupFakeEditorInteractor(new FocusedContentRange(caret));
    final Point<ContentNode> newCaret = Point.<ContentNode>inText(
        new ContentTextNode(Document.get().createTextNode("hi"), null), 2);
    interactor.call(FakeEditorInteractor.NORMALIZE_POINT).returns(newCaret);
    interactor.call(FakeEditorInteractor.SET_CARET).nOf(1).withArgs(newCaret);
    interactor.call(FakeEditorInteractor.NOTIFYING_TYPING_EXTRACTOR).nOf(1).withArgs(
        newCaret, false);
    EditorEventHandler handler = createEditorEventHandler(interactor, subHandler);

    handler.handleEvent(fakeEvent);
    interactor.checkExpectations();
  }

  /**
   * Firefox specific-
   *
   * Test that typing extractor is notifed of the correct caret location.
   */
  public void testNormalTypingNotifiesExtractor() {
    FakeEditorEvent fakeEvent = FakeEditorEvent.create(KeySignalType.INPUT, 'a');
    final Point<ContentNode> caret =
        Point.<ContentNode> end(newParaElement());

    EditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    FakeEditorInteractor interactor = setupFakeEditorInteractor(new FocusedContentRange(caret));
    EditorEventHandler handler = createEditorEventHandler(interactor, subHandler);

    interactor.call(FakeEditorInteractor.NOTIFYING_TYPING_EXTRACTOR).nOf(1).withArgs(caret, false);
    boolean cancel = handler.handleEvent(fakeEvent);
    assertFalse("Should allow typing event", cancel);
    interactor.checkExpectations();
  }

  /**
   * Tests that events handled by listeners exits the EditorEventHandler and
   * does not continue triggering other handler methods.
   */
  public void testEventsHandledByListenerExitsHandler() {
    FakeEditorEvent fakeEvent = FakeEditorEvent.create(KeySignalType.INPUT, 'a');
    final Point<ContentNode> caret =
        Point.<ContentNode> end(newParaElement());

    FakeEditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    FakeEditorInteractor interactor = setupFakeEditorInteractor(new FocusedContentRange(caret));
    EditorEventHandler handler = createEditorEventHandler(interactor, subHandler);

    interactor.call(FakeEditorInteractor.NOTIFYING_LISTENERS).nOf(1).withArgs(fakeEvent).returns(
        true);
    boolean cancel = handler.handleEvent(fakeEvent);
    assertFalse("Should not cancel events even if handled by listeners", cancel);
    interactor.checkExpectations();
    subHandler.checkExpectations();
  }

  ContentElement newElement() {
    return new ContentElement("p", Document.get().createPElement(), null);
  }

  /**
   * Tests mouse event triggers on node.
   */
  public void testMouseEventsTriggeredOnNode() {
    EditorEvent mouseSignal = FakeSignalEvent.createClick(FakeEditorEvent.ED_FACTORY, null);
    ContentElement fakeContentElement = newElement();
    final Point<ContentNode> caret = Point.<ContentNode> end(fakeContentElement);

    FakeRouter router = new FakeRouter();
    FakeEditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    FakeEditorInteractor interactor = setupFakeEditorInteractor(new FocusedContentRange(caret));
    EditorEventHandler handler = createEditorEventHandler(router, interactor, subHandler);

    interactor.call(FakeEditorInteractor.FIND_ELEMENT_WRAPPER).nOf(1).withArgs(
        mouseSignal.getTarget()).returns(fakeContentElement);
    router.ctx.call(FakeRouter.HANDLE_CLICK).nOf(1).withArgs(mouseSignal)
        .returns(true);
    interactor.call(FakeEditorInteractor.CLEAR_ANNOTATIONS).nOf(1);

    boolean cancel = handler.handleEvent(mouseSignal);
    router.ctx.checkExpectations();
    interactor.checkExpectations();
    subHandler.checkExpectations();
    assertEquals("Allow iff event allows browser default", mouseSignal.shouldAllowBrowserDefault(),
        !cancel);
  }

  /**
   * Tests that an unhandled accelerator should trigger subhandlers and cancel
   * browser default if unhandled and non-whitelisted.
   */
  public void testUnhandledAcceleratorKeys() {
    // Always cancel INS key
    EditorEvent insKeyEvent = FakeEditorEvent.create(
        KeySignalType.NOEFFECT, EventWrapper.KEY_INSERT);
    testUnhandledAcceleratorHelper(insKeyEvent, true, true);

    // test tab, a non-metesque accelerator
    EditorEvent tabPressEvent = createTabSignal();
    testUnhandledAcceleratorHelper(tabPressEvent, true, true);

    // test an allowable combo
    EditorEvent ctrlInsert = createCtrlComboKeyPress(KeySignalType.NOEFFECT,
        EventWrapper.KEY_INSERT);
    if (UserAgent.isLinux() || UserAgent.isWin()) {
      testUnhandledAcceleratorHelper(ctrlInsert, true, false);
    } else {
      testUnhandledAcceleratorHelper(ctrlInsert, true, true);
    }

    // test something that is not whitelisted
    EditorEvent ctrlY = createCtrlComboKeyPress(KeySignalType.INPUT, 'Y');
    testUnhandledAcceleratorHelper(ctrlY, true, true);
    testUnhandledAcceleratorHelper(ctrlY, false, false);
  }

  private void testUnhandledAcceleratorHelper(EditorEvent editorEvent,
      boolean cancelNonWhitelistedCombos,
      boolean expectCancel) {
    final Point<ContentNode> start =
        Point.<ContentNode> end(newParaElement());
    final Point<ContentNode> end =
        Point.<ContentNode> end(newParaElement());

    FocusedContentRange selection = new FocusedContentRange(start, end);
    ContentRange range = selection.asOrderedRange(true);
    FakeEditorInteractor interactor = setupFakeEditorInteractor(selection);
    FakeEditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    FakeRouter router = new FakeRouter();
    EditorEventHandler handler = new EditorEventHandler(new FakeTimerService(),
        interactor, subHandler, router, cancelNonWhitelistedCombos, true);

    subHandler.call(FakeEditorEventsSubHandler.HANDLE_COMMAND).nOf(1).withArgs(editorEvent)
        .returns(false);
    subHandler.call(FakeEditorEventsSubHandler.HANDLE_BLOCK_LEVEL_COMMANDS).nOf(1).withArgs(
        editorEvent, range).returns(false);
    subHandler.call(FakeEditorEventsSubHandler.HANDLE_RANGE_KEY_COMBO).nOf(1).withArgs(editorEvent,
        range).returns(false);

    boolean cancel = handler.handleEvent(editorEvent);
    interactor.checkExpectations();
    subHandler.checkExpectations();
    assertEquals("Cancel does not match expected", expectCancel, cancel);
  }


  /**
   * Test that handleLeft/Right are triggered on the correct node.
   */
  public void testHandleLeftRightTriggeredOnNode() {
    FakeEditorEvent fakeEvent = FakeEditorEvent.create(KeySignalType.NAVIGATION, KeyCodes.KEY_LEFT);
    FakeRouter router = new FakeRouter();
    ContentElement fakeContentElement = newElement();
    final Point<ContentNode> caret = Point.<ContentNode> end(fakeContentElement);

    EditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    FakeEditorInteractor interactor = setupFakeEditorInteractor(new FocusedContentRange(caret));
    EditorEventHandler handler = createEditorEventHandler(router, interactor, subHandler);

    router.ctx.call(FakeRouter.HANDLE_LEFT).nOf(1).withArgs(fakeEvent).returns(
        true);
    interactor.call(FakeEditorInteractor.CLEAR_ANNOTATIONS).nOf(1);

    boolean cancel1 = handler.handleEvent(fakeEvent);

    router.ctx.checkExpectations();
    assertEquals(!fakeEvent.shouldAllowBrowserDefault(), cancel1);


    router.ctx.reset();

    FakeEditorEvent fakeEvent2 =
        FakeEditorEvent.create(KeySignalType.NAVIGATION, KeyCodes.KEY_RIGHT);
    router.ctx.call(FakeRouter.HANDLE_RIGHT).nOf(1).withArgs(fakeEvent2)
        .returns(true);
    boolean cancel2 = handler.handleEvent(fakeEvent2);

    assertEquals(!fakeEvent.shouldAllowBrowserDefault(), cancel2);
    router.ctx.checkExpectations();
  }

  public void testDeleteWithRangeSelectedDeletesRange() {
    FakeEditorEvent fakeEvent = FakeEditorEvent.create(KeySignalType.DELETE, KeyCodes.KEY_LEFT);

    //Event event = Document.get().createKeyPressEvent(
    //    false, false, false, false, KeyCodes.KEY_BACKSPACE, 0).cast();

    Text input = Document.get().createTextNode("ABCDE");
    ContentNode node = new ContentTextNode(input, null);

    final Point<ContentNode> start = Point.inText(node, 1);
    final Point<ContentNode> end = Point.inText(node, 4);
    FakeEditorInteractor interactor = setupFakeEditorInteractor(
        new FocusedContentRange(start, end));
    EditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    EditorEventHandler handler = createEditorEventHandler(interactor, subHandler);

    interactor.call(FakeEditorInteractor.DELETE_RANGE).nOf(1).withArgs(
        start, end, false).returns(start);

    handler.handleEvent(fakeEvent);
    interactor.checkExpectations();
  }

  /**
   * Firefox specific- Test that typing with selection deletes the selected
   * range and reports correct cursor to typing extractor.
   */
  public void testTypingWithRangeSelectedDeletesRangeAndNotifiesExtrator() {
    FakeEditorEvent fakeEvent = FakeEditorEvent.create(KeySignalType.INPUT, 'a');
    final Point<ContentNode> start =
        Point.<ContentNode> end(newParaElement());
    final Point<ContentNode> end =
        Point.<ContentNode> end(newParaElement());

    PointRange<ContentNode> deleteRangeReturnValue = new PointRange<ContentNode>(end);

    FakeEditorInteractor interactor = setupFakeEditorInteractor(
        new FocusedContentRange(start, end));
    EditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    EditorEventHandler handler = createEditorEventHandler(interactor, subHandler);

    interactor.call(FakeEditorInteractor.DELETE_RANGE).nOf(1).withArgs(
        start, end, true).returns(start);
    interactor.call(FakeEditorInteractor.NOTIFYING_TYPING_EXTRACTOR).nOf(1).withArgs(
        deleteRangeReturnValue.getSecond(), true);

    boolean cancel = handler.handleEvent(fakeEvent);
    interactor.checkExpectations();
    assertFalse(cancel);
  }

  /**
   * Tests backspace, shift backspace and delete.
   */
  public void testBackspaceDeleteVariants() {
    // normal backspace handled/unhandled
    testBackspaceDeleteHelper(true, true, true);
    testBackspaceDeleteHelper(true, false, true);

    // shift backspace handled/unhandled
    testBackspaceDeleteHelper(true, true, false);
    testBackspaceDeleteHelper(true, false, false);

    // normal delete handled/unhandled
    testBackspaceDeleteHelper(false, true, false);
    testBackspaceDeleteHelper(false, false, false);

    // TODO(user): test shift delete elsewhere. Shift-delete is a special case,
    // as it means cut in windows/linux and normal delete on mac.
  }

  private void testBackspaceDeleteHelper(boolean isBackspace, boolean handled,
      boolean isShiftDown) {
    EditorEvent signal = FakeSignalEvent.createKeyPress(FakeEditorEvent.ED_FACTORY,
        KeySignalType.DELETE, isBackspace ? KeyCodes.KEY_BACKSPACE : KeyCodes.KEY_DELETE,
        isShiftDown ? EnumSet.of(KeyModifier.SHIFT) : null);

    FakeRouter router = new FakeRouter();
    ContentElement fakeContentElement = newElement();
    final Point<ContentNode> caret = Point.<ContentNode> end(fakeContentElement);

    FakeEditorInteractor interactor = setupFakeEditorInteractor(new FocusedContentRange(caret));
    EditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    EditorEventHandler handler = createEditorEventHandler(router, interactor, subHandler);

    // Because we cannot override handleBackspace in ContentElement, we test at
    // handleBackspaceAtBeginning instead. We have to be sure that the caret is
    // at beginning or it wouldn't work, so this is a bit fragile.
    assertTrue(ContentPoint.fromPoint(caret).isAtBeginning());
    if (isBackspace) {
      router.ctx.call(FakeRouter.HANDLE_BACKSPACE_AT_BEGINNING).nOf(1)
          .withArgs(signal).returns(handled);
    } else {
      router.ctx.call(FakeRouter.HANDLE_DELETE).nOf(1).withArgs(signal)
          .returns(handled);
    }
    boolean cancel = handler.handleEvent(signal);
    router.ctx.checkExpectations();
    assertEquals("Backspace should be cancelled if handled", handled, cancel);
  }

  /**
   * Test that collapsed keycombos gets routed to block level handling unless it
   * is caught by handleCommand
   */
  public void testRouteToCollapsedKeyCombo() {
    testRouteToCollapsedKeyComboHelper(createTabSignal());
    testRouteToCollapsedKeyComboHelper(createCtrlComboKeyPress(KeySignalType.INPUT, 'b'));
  }

  private EditorEvent createTabSignal() {
    return FakeSignalEvent.createKeyPress(FakeEditorEvent.ED_FACTORY,
        KeySignalType.INPUT, KeyCodes.KEY_TAB, null);
  }

  private EditorEvent createCtrlComboKeyPress(KeySignalType type, int c) {
    return FakeSignalEvent.createKeyPress(FakeEditorEvent.ED_FACTORY,
        type, c, EnumSet.of(KeyModifier.CTRL));
  }

  private void testRouteToCollapsedKeyComboHelper(EditorEvent signal) {
    testRouteToCollapsedKeyComboHelperInner(signal, false, true);
    testRouteToCollapsedKeyComboHelperInner(signal, true, false);
    testRouteToCollapsedKeyComboHelperInner(signal, false, false);
  }

  private void testRouteToCollapsedKeyComboHelperInner(EditorEvent tabSignal,
      boolean isHandledCommand, boolean isHandledBlockLevelCommand) {
    final Point<ContentNode> caret =
        Point.<ContentNode> end(newParaElement());

    FocusedContentRange selection = new FocusedContentRange(caret);
    ContentRange range = selection.asOrderedRange(true);
    FakeEditorInteractor interactor = setupFakeEditorInteractor(selection);
    FakeEditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    EditorEventHandler handler = createEditorEventHandler(interactor, subHandler);

    subHandler.call(FakeEditorEventsSubHandler.HANDLE_COMMAND).nOf(1).withArgs(tabSignal).returns(
        isHandledCommand);
    if (!isHandledCommand) {
      subHandler.call(FakeEditorEventsSubHandler.HANDLE_BLOCK_LEVEL_COMMANDS).nOf(1).withArgs(
          tabSignal, range).returns(isHandledBlockLevelCommand);

      if (!isHandledBlockLevelCommand) {
        // Stop it here by returning true, test lower down commands in other
        // methods.
        subHandler.call(FakeEditorEventsSubHandler.HANDLE_COLLAPSED_KEY_COMBO).nOf(1).withArgs(
            tabSignal, selection.getFocus()).returns(true);
      }
    }
    boolean cancel = handler.handleEvent(tabSignal);
    interactor.checkExpectations();
    subHandler.checkExpectations();

    assertTrue("Handled commands should be cancelled", cancel);
  }

  /**
   * Test that ranged combos are routed correctly.
   */
  public void testRouteToRangeKeyCombo() {
    EditorEvent editorEvent = createCtrlComboKeyPress(KeySignalType.INPUT, 'b');

    final Point<ContentNode> start =
        Point.<ContentNode> end(newParaElement());
    final Point<ContentNode> end =
        Point.<ContentNode> end(newParaElement());

    FocusedContentRange selection = new FocusedContentRange(start, end);
    ContentRange range = selection.asOrderedRange(true);
    FakeEditorInteractor interactor = setupFakeEditorInteractor(selection);
    FakeEditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    EditorEventHandler handler = createEditorEventHandler(interactor, subHandler);

    subHandler.call(FakeEditorEventsSubHandler.HANDLE_COMMAND).nOf(1).withArgs(editorEvent)
        .returns(false);
    subHandler.call(FakeEditorEventsSubHandler.HANDLE_BLOCK_LEVEL_COMMANDS).nOf(1).withArgs(
        editorEvent, range).returns(false);
    subHandler.call(FakeEditorEventsSubHandler.HANDLE_RANGE_KEY_COMBO).nOf(1).withArgs(editorEvent,
        range).returns(true);

    boolean cancel = handler.handleEvent(editorEvent);
    interactor.checkExpectations();
    subHandler.checkExpectations();
    assertTrue(cancel);
  }

  /**
   * TODO(user): Test this for mac, mac has alt-backspace and alt-delete
   * instead.
   *
   * NOTE(user): This isn't testing the final intended behaviour. We'd
   * like ctrl-backspace and ctrl-delete to actually delete words. However,
   * for now we are ensuring that we cancel to avoid editor becoming inconsistent.
   */
  public void testNonCharacterMoveUnitIsCancelled() {
    // test ctrl-backspace
    EditorEvent ctrlBackspaceEvent = createCtrlComboKeyPress(KeySignalType.DELETE,
        KeyCodes.KEY_BACKSPACE);
    testNonCharacterMoveUnitIsCancelledHelper(ctrlBackspaceEvent, true);

    // test ctrl-delete
    EditorEvent ctrlDeleteEvent = createCtrlComboKeyPress(KeySignalType.DELETE,
        KeyCodes.KEY_DELETE);
    testNonCharacterMoveUnitIsCancelledHelper(ctrlDeleteEvent, false);
  }

  private void testNonCharacterMoveUnitIsCancelledHelper(EditorEvent event, boolean backspace) {
    final Point<ContentNode> caret =
        Point.<ContentNode> end(newParaElement());

    FocusedContentRange selection = new FocusedContentRange(caret);
    FakeEditorInteractor interactor = setupFakeEditorInteractor(selection);
    FakeEditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    EditorEventHandler handler = createEditorEventHandler(interactor, subHandler);

    if (backspace) {
      interactor.call(FakeEditorInteractor.DELETE_WORD_ENDING_AT).withArgs(caret).anyOf();
    } else {
      interactor.call(FakeEditorInteractor.DELETE_WORD_STARTING_AT).withArgs(caret).anyOf();
    }
    boolean cancel = handler.handleEvent(event);
    interactor.checkExpectations();
    subHandler.checkExpectations();
    assertEquals("Cancel does not match expected", true, cancel);
  }

  /**
   * Test that paste events are routed correctly.
   */
  public void testRoutePasteEvent() {
    final Point<ContentNode> caret =
      Point.<ContentNode> end(newParaElement());
    FocusedContentRange selection = new FocusedContentRange(caret);

    FakeEditorEvent pasteEvent = FakeEditorEvent.createPasteEvent();
    FakeEditorInteractor interactor = setupFakeEditorInteractor(selection);
    FakeEditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    EditorEventHandler handler = createEditorEventHandler(interactor, subHandler);

    subHandler.call(FakeEditorEventsSubHandler.HANDLE_PASTE).nOf(1).withArgs(pasteEvent).returns(
        false);
    boolean cancel = handler.handleEvent(pasteEvent);
    interactor.checkExpectations();
    subHandler.checkExpectations();
    assertFalse(cancel);
  }

  public void testCompositionEventsChangeState() {
    FakeEditorEvent[] events = FakeEditorEvent.compositionSequence(2);

    FakeEditorEvent keyEvent1 = FakeEditorEvent.create(KeySignalType.INPUT, 'a');
    FakeEditorEvent keyEvent2 = FakeEditorEvent.create(KeySignalType.INPUT,
        SignalKeyLogic.IME_CODE);

    final Point<ContentNode> caret =
        Point.<ContentNode> end(newParaElement());

    EditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    FakeEditorInteractor interactor = setupFakeEditorInteractor(new FocusedContentRange(caret));
    EditorEventHandler handler = createEditorEventHandler(interactor, subHandler);

    interactor.call(FakeEditorInteractor.COMPOSITION_START).nOf(1);
    interactor.call(FakeEditorInteractor.COMPOSITION_UPDATE).nOf(2);
    interactor.call(FakeEditorInteractor.COMPOSITION_END).nOf(1);
    interactor.call(FakeEditorInteractor.NOTIFYING_TYPING_EXTRACTOR).nOf(1).anyArgs();

    assertEquals(EditorEventHandler.State.NORMAL, handler.getState());

    boolean cancel;
    cancel = handler.handleEvent(events[0]);
    assertFalse("Should allow composition start event", cancel);

    assertEquals(EditorEventHandler.State.COMPOSITION, handler.getState());

    cancel = handler.handleEvent(keyEvent1);
    assertFalse("Should allow regular keycode key event", cancel);

    cancel = handler.handleEvent(events[1]);
    assertFalse("Should allow composition update event", cancel);

    cancel = handler.handleEvent(keyEvent2);
    assertFalse("Should allow ime keycode key event", cancel);

    cancel = handler.handleEvent(events[2]);
    assertFalse("Should allow 2nd composition update event", cancel);

    assertEquals(EditorEventHandler.State.COMPOSITION, handler.getState());

    cancel = handler.handleEvent(events[3]);
    assertFalse("Should allow composition end event", cancel);

    assertEquals(EditorEventHandler.State.NORMAL, handler.getState());

    cancel = handler.handleEvent(keyEvent1);
    assertFalse("Should allow regular keycode key event", cancel);

    // Note: explicitly should only call the key event handling code
    // (resulting in notifying the typing extractor when in a normal
    // state, not during composition).
    interactor.checkExpectations();
  }

  public void testIsAccelerator() {
    // Test alt+input and alt+shift+input keys - These are normal input on mac,
    // and accelerators on
    // other platforms
    for (int c = 'a'; c <= 'z'; c++) {
      SignalEvent signal = FakeSignalEvent.createKeyPress(KeySignalType.INPUT, c,
          EnumSet.of(KeyModifier.ALT));
      // mac
      assertFalse(EditorEventHandler.isAcceleratorInner(signal, true, false));
      assertFalse(EditorEventHandler.isAcceleratorInner(signal, true, true));
      // other platforms
      assertTrue(EditorEventHandler.isAcceleratorInner(signal, false, false));
      assertTrue(EditorEventHandler.isAcceleratorInner(signal, false, true));
    }

    // Test a few others such as `, - and ;
    // mac
    String otherInputKeys = "`-;";
    for (char c : otherInputKeys.toCharArray()) {
      SignalEvent signal = FakeSignalEvent.createKeyPress(KeySignalType.INPUT, c,
          EnumSet.of(KeyModifier.ALT));
      assertFalse(EditorEventHandler.isAcceleratorInner(signal, true, false));
      assertFalse(EditorEventHandler.isAcceleratorInner(signal, true, true));

      // with alt+shift
      SignalEvent altShift = FakeSignalEvent.createKeyPress(KeySignalType.INPUT, c,
          EnumSet.of(KeyModifier.ALT, KeyModifier.SHIFT));
      assertFalse(EditorEventHandler.isAcceleratorInner(altShift, true, false));
      assertFalse(EditorEventHandler.isAcceleratorInner(altShift, true, true));
    }


    // Test ctrl/meta keys- these are accelerators unless they are navigation
    // related
    assertIsAcceleratorInner(FakeSignalEvent.createKeyPress(KeySignalType.INPUT, 'c',
        EnumSet.of(KeyModifier.CTRL)), true);
    assertIsAcceleratorInner(FakeSignalEvent.createKeyPress(KeySignalType.INPUT, 'c',
        EnumSet.of(KeyModifier.META)), true);
    assertIsAcceleratorInner(FakeSignalEvent.createKeyPress(KeySignalType.NAVIGATION,
        KeyCodes.KEY_LEFT, EnumSet.of(KeyModifier.CTRL)), false);

    // Test shift-delete is an accelerator on windows/linux
    assertTrue(EditorEventHandler.isAcceleratorInner(FakeSignalEvent.createKeyPress(
        KeySignalType.DELETE, KeyCodes.KEY_DELETE, EnumSet.of(KeyModifier.SHIFT)), false, true));
  }

  private void assertIsAcceleratorInner(SignalEvent evt, boolean expected) {
    assertEquals(expected, EditorEventHandler.isAcceleratorInner(evt, false, false));
    assertEquals(expected, EditorEventHandler.isAcceleratorInner(evt, false, true));
    assertEquals(expected, EditorEventHandler.isAcceleratorInner(evt, true, false));
    assertEquals(expected, EditorEventHandler.isAcceleratorInner(evt, true, true));
  }

  /**
   * Checks that events are cancelled/permitted properly when selection is lost,
   * both when there was initial content selection, and when there wasn't.
   */
  public void testSelectionLostCancelling() {
    /// part one - start with no content selection
    FakeEditorInteractor interactor = new FakeEditorInteractor();
    EditorEventsSubHandler subHandler = new FakeEditorEventsSubHandler();
    EditorEventHandler handler = createEditorEventHandler(interactor, subHandler);

    // should check for selection, the flush, realise selection is lost, and get a null selection.
    interactor.call(FakeEditorInteractor.NOTIFYING_LISTENERS).nOf(1).returns(false);
    interactor.call(FakeEditorInteractor.HAS_CONTENT_SELECTION).nOf(1).returns(false);
    interactor.call(FakeEditorInteractor.FORCE_FLUSH).nOf(1);
    interactor.call(FakeEditorInteractor.GET_SELECTION_POINTS).nOf(1).returns(null);

    // didn't have content selection, so don't cancel.
    FakeEditorEvent keyEvent = FakeEditorEvent.createPasteEvent();
    assertFalse(handler.handleEvent(keyEvent));

    /// part two - now have content selection

    // should check for selection, the flush, realise selection is lost, and get a null selection.
    interactor.call(FakeEditorInteractor.NOTIFYING_LISTENERS).nOf(1).returns(false);
    interactor.call(FakeEditorInteractor.HAS_CONTENT_SELECTION).nOf(1).returns(true);
    interactor.call(FakeEditorInteractor.FORCE_FLUSH).nOf(1);
    interactor.call(FakeEditorInteractor.GET_SELECTION_POINTS).nOf(1).returns(null);

    // and again, this time cancel!
    assertTrue(handler.handleEvent(keyEvent));
  }

  private static class FakeEditorInteractor extends MockContext implements EditorInteractor {
    public static final MethodID DELETE_RANGE = new MethodID("deleteRange");
    public static final MethodID INSERT_TEXT = new MethodID("insertText");
    public static final MethodID FIND_ELEMENT_WRAPPER = new MethodID("findElementWrapper");
    public static final MethodID FORCE_FLUSH = new MethodID("forceFlush");
    public static final MethodID GET_SELECTION_POINTS = new MethodID("getSelectionPoints");
    public static final MethodID HAS_CONTENT_SELECTION = new MethodID("hasContentSelection");
    public static final MethodID IS_EDITING = new MethodID("isEditing");
    public static final MethodID NORMALIZE_POINT = new MethodID("normalizePoint");
    public static final MethodID NOTIFYING_LISTENERS = new MethodID("notifyingListeners");
    public static final MethodID NOTIFYING_TYPING_EXTRACTOR =
        new MethodID("notifyingTypingExtractor");
    public static final MethodID SET_CARET = new MethodID("setCaret");
    public static final MethodID CLEAR_ANNOTATIONS = new MethodID("clearCaretAnnotations");
    public static final MethodID NOTE_WEOLHO = new MethodID("noteWebkitEndOfLineHackOccurred");
    public static final MethodID GET_HTML_SELECTION = new MethodID("getHtmlSelection");
    private static final MethodID DELETE_WORD_ENDING_AT = new MethodID("deleteWordEndingAt");
    private static final MethodID DELETE_WORD_STARTING_AT = new MethodID("deleteWordStartingAt");
    private static final MethodID COMPOSITION_START = new MethodID("compositionStart");
    private static final MethodID COMPOSITION_END = new MethodID("compositionEnd");
    private static final MethodID COMPOSITION_UPDATE = new MethodID("compositionUpdate");
    private static final MethodID CHECKPOINT = new MethodID("checkpoint");
    private static final MethodID REBIAS_SELECTION = new MethodID("rebiasSelection");

    @Override
    public Point<ContentNode> deleteRange(Point<ContentNode> fst, Point<ContentNode> second,
        boolean isReplace) {
      return methodCalledHelper(DELETE_RANGE, fst, second, isReplace);
    }

    @Override
    public Point<ContentNode> insertText(Point<ContentNode> at, String text,
        boolean isReplace) {
      return methodCalledHelper(INSERT_TEXT, at, text, isReplace);
    }

    @Override
    public ContentElement findElementWrapper(Element target) {
      return methodCalledHelper(FIND_ELEMENT_WRAPPER, target);
    }

    @Override
    public void forceFlush() {
      methodCalledHelper(FORCE_FLUSH);
    }

    @Override
    public FocusedContentRange getSelectionPoints() {
      return methodCalledHelper(GET_SELECTION_POINTS);
    }

    @Override
    public boolean selectionIsOrdered() {
      // TODO(danilatos)
      return true;
    }

    @Override
    public boolean isExpectingMutationEvents() {
      // TODO(user)
      return false;
    }

    @Override
    public boolean shouldIgnoreMutations() {
      // TODO(user)
      return false;
    }

    @Override
    public boolean isTyping() {
      // TODO(user)
      return false;
    }

    @Override
    public FocusedPointRange<Node> getHtmlSelection() {
      return methodCalledHelper(GET_HTML_SELECTION);
    }

    @Override
    public boolean hasContentSelection() {
      return methodCalledHelperBoolean(HAS_CONTENT_SELECTION);
    }

    @Override
    public boolean isEditing() {
      return methodCalledHelperBoolean(IS_EDITING);
    }

    @Override
    public Point<ContentNode> normalizePoint(Point<ContentNode> caret) {
      return methodCalledHelper(NORMALIZE_POINT, caret);
    }

    @Override
    public boolean notifyListeners(SignalEvent event) {
      return methodCalledHelperBoolean(NOTIFYING_LISTENERS, event);
    }

    @Override
    public boolean notifyTypingExtractor(Point<ContentNode> caret, boolean useHtmlCaret,
        boolean isReplace) {
      Boolean ret = methodCalledHelperBoolean(NOTIFYING_TYPING_EXTRACTOR, caret, isReplace);
      return ret != null ? ret : false;
    }

    @Override
    public void setCaret(Point<ContentNode> caret) {
      methodCalledHelper(SET_CARET, caret);
    }

    @Override
    public void noteWebkitEndOfLinkHackOccurred(Text textNode) {
      methodCalledHelper(NOTE_WEOLHO, textNode);
    }

    @Override
    public void clearCaretAnnotations() {
      methodCalledHelper(CLEAR_ANNOTATIONS);
    }

    @Override
    public void deleteWordEndingAt(Point<ContentNode> caret) {
      methodCalledHelper(DELETE_WORD_ENDING_AT, caret);
    }

    @Override
    public void deleteWordStartingAt(Point<ContentNode> caret) {
      methodCalledHelper(DELETE_WORD_STARTING_AT, caret);
    }

    @Override
    public void compositionUpdate() {
      methodCalledHelper(COMPOSITION_UPDATE);
    }

    @Override
    public FocusedContentRange compositionEnd() {
      return methodCalledHelper(COMPOSITION_END);
    }

    @Override
    public void compositionStart(Point<ContentNode> caret) {
      methodCalledHelper(COMPOSITION_START);
    }

    @Override
    public void checkpoint(FocusedContentRange selection) {
      methodCalledHelper(CHECKPOINT);
    }

    @Override
    public void rebiasSelection(CursorDirection defaultDirection) {
      methodCalledHelper(REBIAS_SELECTION);
    }
  }

  private static class FakeEditorEventsSubHandler extends MockContext implements
      EditorEventsSubHandler {
    public static final MethodID HANDLE_BLOCK_LEVEL_COMMANDS =
        new MethodID("handleBlockLevelCommands");
    public static final MethodID HANDLE_COLLAPSED_KEY_COMBO =
        new MethodID("handleCollapsedKeyCombo");
    public static final MethodID HANDLE_COMMAND = new MethodID("handleCommand");
    public static final MethodID HANDLE_CUT = new MethodID("handleCut");
    public static final MethodID HANDLE_COPY = new MethodID("handleCopy");
    public static final MethodID HANDLE_DOM_MUTATION = new MethodID("handleDOMMutation");
    public static final MethodID HANDLE_PASTE = new MethodID("handlePaste");
    public static final MethodID HANDLE_RANGE_KEY_COMBO = new MethodID("handleRangeKeyCombo");
    public static final MethodID HANDLE_SUBMIT = new MethodID("handleSubmit");

    @Override
    public boolean handleBlockLevelCommands(EditorEvent event, ContentRange selection) {
      return methodCalledHelperBoolean(HANDLE_BLOCK_LEVEL_COMMANDS, event, selection);
    }

    @Override
    public boolean handleCollapsedKeyCombo(EditorEvent event, Point<ContentNode> caret) {
      return methodCalledHelperBoolean(HANDLE_COLLAPSED_KEY_COMBO, event, caret);
    }

    @Override
    public boolean handleCommand(EditorEvent event) {
      return methodCalledHelperBoolean(HANDLE_COMMAND, event);
    }

    @Override
    public boolean handleCut(EditorEvent event) {
      return methodCalledHelperBoolean(HANDLE_CUT, event);
    }

    @Override
    public void handleDomMutation(SignalEvent event) {
      methodCalledHelperBoolean(HANDLE_DOM_MUTATION, event);
    }

    @Override
    public boolean handlePaste(EditorEvent event) {
      return methodCalledHelperBoolean(HANDLE_PASTE, event);
    }

    @Override
    public boolean handleRangeKeyCombo(EditorEvent event, ContentRange selection) {
      return methodCalledHelperBoolean(HANDLE_RANGE_KEY_COMBO, event, selection);
    }

    @Override
    public boolean handleCopy(EditorEvent event) {
      return methodCalledHelperBoolean(HANDLE_COPY, event);
    }
  }

  private static class FakeRouter extends NodeEventRouter {
    public static final MethodID HANDLE_CLICK = new MethodID("handleClick");
    public static final MethodID HANDLE_BACKSPACE_AT_BEGINNING =
        new MethodID("handleBackspaceAtBeginning");
    public static final MethodID HANDLE_DELETE = new MethodID("handleDelete");
    public static final MethodID HANDLE_LEFT = new MethodID("handleLeft");
    public static final MethodID HANDLE_RIGHT = new MethodID("handleRight");

    public MockContext ctx = new MockContext();

    @Override
    public boolean handleClick(ContentNode node, EditorEvent event) {
      return ctx.methodCalledHelperBoolean(HANDLE_CLICK, event);
    }

    @Override
    public boolean handleBackspaceAtBeginning(ContentNode node, EditorEvent event) {
      return ctx.methodCalledHelperBoolean(HANDLE_BACKSPACE_AT_BEGINNING, event);
    }

    @Override
    public boolean handleDelete(ContentNode node, EditorEvent event) {
      return ctx.methodCalledHelperBoolean(HANDLE_DELETE, event);
    }

    @Override
    public boolean handleLeft(ContentNode node, EditorEvent event) {
      return ctx.methodCalledHelperBoolean(HANDLE_LEFT, event);
    }

    @Override
    public boolean handleRight(ContentNode node, EditorEvent event) {
      return ctx.methodCalledHelperBoolean(HANDLE_RIGHT, event);
    }
  }

  /**
   * Poor man's jmock (Can't use jmock in gwt as that relies on reflection):
   *
   * These are probably generally useful for cases where jUnit tests are not
   * possible without huge refactoring.
   *
   * It seems like this might be generally useful, but I'd rather keep it here
   * until I can doc and polish it.
   */
  private static class MethodContext {
    private Object returnValue;

    private boolean ignoreCallCount = true;
    private int expectedCalls;
    private int actualCalls;

    // Ignore args for this method.
    private boolean ignoreArgs = true;
    private final List<Object> actualArgs = new ArrayList<Object>();
    private List<Object> expectedArgs;

    public MethodContext() {
    }

    public MethodContext nOf(int n) {
      this.ignoreCallCount = false;
      this.expectedCalls = n;
      return this;
    }

    public MethodContext anyOf() {
      this.ignoreCallCount = true;
      return this;
    }

    public MethodContext withArgs(Object... args) {
      this.ignoreArgs = false;
      expectedArgs = Arrays.asList(args);
      return this;
    }

    public MethodContext anyArgs() {
      this.ignoreArgs = true;
      return this;
    }

    public MethodContext returns(Object returnValue) {
      this.returnValue = returnValue;
      return this;
    }
  }

  private static class MethodID {
    public final String name;

    private MethodID(String name) {
      this.name = name;
    }
  }

  private static class MockContext {
    Map<MethodID, MethodContext> methodContexts = new HashMap<MethodID, MethodContext>();

    /**
     * NOTE(user): We need this because javac cannot infer type correctly.
     *
     * @param methodId
     * @param args
     * @return the predefined return value.
     */
    Boolean methodCalledHelperBoolean(MethodID methodId, Object... args) {
      return methodCalledHelper(methodId, args);
    }

    @SuppressWarnings("unchecked")
    <T> T methodCalledHelper(MethodID methodId, Object... args) {
      MethodContext ctx = methodContexts.get(methodId);
      assertNotNull("no calls to " + methodId.name + " expected", ctx);
      ctx.actualCalls++;

      for (Object arg : args) {
        ctx.actualArgs.add(arg);
      }

      return (T) ctx.returnValue;
    }

    /**
     * Expect a call to method corresponding to methodId
     *
     * @param methodId
     */
    MethodContext call(MethodID methodId) {
      MethodContext retVal = new MethodContext();
      methodContexts.put(methodId, retVal);
      return retVal;
    }

    /**
     * Check that the defined expectations are satisfied.
     */
    public void checkExpectations() {
      for (MethodID method : methodContexts.keySet()) {
        MethodContext m = methodContexts.get(method);
        if (!m.ignoreCallCount) {
          assertEquals(method.name
              + ": Expected number of calls does not match actual number of calls for method: ",
              m.expectedCalls, m.actualCalls);
        }
        if (!m.ignoreArgs) {
          assertEquals(method.name + ": args do not match", m.expectedArgs, m.actualArgs);
        }
      }
    }

    /**
     * Resets expectations and count/args of methods called.
     */
    public void reset() {
      methodContexts.clear();
    }
  }
}
