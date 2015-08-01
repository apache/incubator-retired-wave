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

package org.waveprotocol.wave.client.editor.sugg;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.impl.FocusImpl;
import org.waveprotocol.wave.client.common.util.LinkedPruningSequenceMap;
import org.waveprotocol.wave.client.common.util.PruningSequenceMap;
import org.waveprotocol.wave.client.common.util.SequenceElement;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.SchedulerTimerService;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.client.widget.popup.PopupEventListener;
import org.waveprotocol.wave.client.widget.popup.PopupEventSourcer;
import org.waveprotocol.wave.client.widget.popup.RelativePopupPositioner;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;
import org.waveprotocol.wave.model.document.util.FocusedRange;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Interactive implementation with real UI.
 *
 * TODO(user): Add a unit test for this class.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class InteractiveSuggestionsManager implements SuggestionsManager, RelativePopupPositioner,
    PopupEventListener {

  /**
   * Handles events fired by SuggestionMenu
   */
  public interface SuggestionMenuHandler {
    void handleItemSelected();
    void handleLeftRight(boolean b);
    void beforeItemClicked();
    void handleMouseOut();
    void handleMouseOver();
  }

  private final SuggestionMenuHandler handler = new SuggestionMenuHandler() {
    @Override
    public void handleItemSelected() {
      // NOTE(user): The previous behaviour here moves to the next item, but we
      // don't want to do that as it disrupts the user flow. Perhaps enable it
      // for rare cases.
      // moveToNextItem();
      popupCloser.closeImmediately();
    }

    /**
     * Resets current to the next (or previous, if isRight is false) element.
     * Wraps to the beginning (or end), unless there is only one element in the
     * sequence, in which case current is set to null.
     */
    @Override
    public void handleLeftRight(boolean isRight) {
      SequenceElement<HasSuggestions> newCurrent = isRight ? current.getNext() : current.getPrev();
      if (newCurrent == current) {
        newCurrent = null;
      }
      setCurrent(newCurrent);
    }

    @Override
    public void beforeItemClicked() {
      if (savedSelection != null) {
        try {
          selectionHelper.setSelectionRange(savedSelection);
        } finally {
          savedSelection = null;
        }
      }
    }

    @Override
    public void handleMouseOut() {
      popupCloser.scheduleClose(null);
    }

    @Override
    public void handleMouseOver() {
      popupCloser.cancelScheduledClose();
    }
  };

  /**
   * Manages the scheduling of hiding the popup. The asynchronous logic enables
   * us to close the menu a short period after the user mouses off the menu,
   * but to cancel the scheduled close if the mouse returns onto the menu.
   */
  private final class PopupCloser {
    private final TimerService timerService;

    private final Scheduler.Task task = new Scheduler.Task() {
      @Override
      public void execute() {
        closeImmediately();
      }
    };

    private Command callback;

    private PopupCloser(TimerService timerService) {
      this.timerService = timerService;
      this.callback = null;
    }

    private void closeImmediately() {
      popup.hide();
      if (callback != null) {
        callback.execute();
        callback = null;
      }
    }

    private void scheduleClose(Command callback) {
      this.callback = callback;
      timerService.scheduleDelayed(task, closeSuggestionMenuDelayMs);
    }

    private void cancelScheduledClose() {
      timerService.cancel(task);
      callback = null;
    }
  }

  /** Singleton suggestion menu per manager */
  private final SuggestionMenu menu = new SuggestionMenu(handler);

  private final UniversalPopup popup;

  /** The popup appears relative to this element. */
  private Element popupAnchor;

  // TODO(danilatos): Implement a binary tree implementation instead of LL.
  private final PruningSequenceMap<ContentNode, HasSuggestions> suggestables =
      LinkedPruningSequenceMap.<ContentNode, HasSuggestions>create();

  private final SelectionHelper selectionHelper;

  private FocusedRange savedSelection = null;

  private SequenceElement<HasSuggestions> current = null;

  private PopupCloser popupCloser = new PopupCloser(
      new SchedulerTimerService(SchedulerInstance.get()));

  private final int closeSuggestionMenuDelayMs;

  /** Constructor */
  public InteractiveSuggestionsManager(
      SelectionHelper selectionHelper, int closeSuggestionMenuDelayMs) {
    popup = EditorStaticDeps.createPopup(null, this, true, false, menu, this);
    this.closeSuggestionMenuDelayMs = closeSuggestionMenuDelayMs;
    this.selectionHelper = selectionHelper;
  }

  @Override
  public void clear() {
    suggestables.clear();
  }

  @Override
  public void registerElement(HasSuggestions element) {
    suggestables.put(element.getSuggestionElement(), element);
  }

  @Override
  public boolean showSuggestionsNearestTo(Point<ContentNode> location) {
    popupCloser.cancelScheduledClose();
    SequenceElement<HasSuggestions> newCurrent = suggestables.findBefore(location.getContainer());

    if (newCurrent == null) {
      // If null, cursor is before the first one, try the first one.
      newCurrent = suggestables.getFirst();
    } else if (!suggestables.isLast(newCurrent)) {
      // If it's not null and not the last one, we are between "current" and
      // the next suggestable. see which is closer.
      newCurrent.getNext();

      // TODO(danilatos):
      // if (pixel distance to next < dist to current) { current = next; }
    }

    if (newCurrent == null) {
      return false;
    }

    setCurrent(getFromKeyboard(newCurrent, false));
    return newCurrent != null;
  }

  @Override
  public void showSuggestionsFor(HasSuggestions suggestable) {
    popupCloser.cancelScheduledClose();
    ContentElement element = suggestable.getSuggestionElement();
    // NOTE(user): If content is not attached, then at the moment, we don't
    // bring up any suggestions. In the future, we may decide to look for other
    // suggestions that are sufficiently near.
    if (element.isContentAttached()) {
      setCurrent(suggestables.getElement(element));
    }
  }

  /**
   * Schedule the closing of the suggestions menu. The closing may not actually
   * happen if the user mouses onto the menu before it is scheduled to close.
   */
  @Override
  public void hideSuggestions(Command callback) {
    popupCloser.scheduleClose(callback);
  }

  /**
   * Logic for setting the current suggestiable, given a seq element.
   * The flow of these related methods looks like this:
   *
   * {@code
   * showSuggestionsFor        -->  setCurrent  --> showSuggestionsForInner
   * showSuggestionsNearestTo             ^
   *          ^                           |
   * Outside _|            Local Methods _|
   * }
   */
  private void setCurrent(SequenceElement<HasSuggestions> newCurrent) {
    //logic for hiding old one
    boolean alreadyShown = false;
    if (current != null) {
      if (newCurrent == null) {
        popupCloser.closeImmediately();
      } else {
        changeAwayFromCurrent();
      }
      alreadyShown = true;
    }

    // logic for setting up and showing new one
    if (newCurrent != null) {

      current = newCurrent;

      HasSuggestions suggestable = current.value();
      menu.clearItems();
      suggestable.populateSuggestionMenu(menu);
      suggestable.handleShowSuggestionMenu();
      // HACK(danilatos): I had to patch MenuBar to make this method public.
      // Getting more and more tempting to write own menu class...
      // Calling this makes the first item in the menu selected by default,
      // so just pressing enter will choose it.
      menu.moveSelectionDown();
      ContentElement element = suggestable.getSuggestionElement();

      popupAnchor = element.getImplNodelet();
      // If savedSelection is null, it should be the first time we are showing a popup (not moving
      // around). So, we save the selection because it becomes null later when we lose focus,
      // at least in IE.
      if (savedSelection == null) {
        savedSelection = selectionHelper.getSelectionRange();
      }

      if (alreadyShown) {
        popup.move();
      } else {
        popup.show();
      }
    }
  }

  private SequenceElement<HasSuggestions> getFromKeyboard(
      SequenceElement<HasSuggestions> el, boolean rightWardsFirst) {
    SequenceElement<HasSuggestions> found =
        getFromKeyboardSpecifiedDirectionOnly(el, rightWardsFirst);
    if (found == null) {
      getFromKeyboardSpecifiedDirectionOnly(el, !rightWardsFirst);
    }
    return found;
  }

  private SequenceElement<HasSuggestions> getFromKeyboardSpecifiedDirectionOnly(
      SequenceElement<HasSuggestions> el, boolean rightWards) {
    assert el != null;

    SequenceElement<HasSuggestions> start = el;

    // NOTE(user): SequenceElement.getNext() and getPrev() never returns null if
    // there are any elements in the SequenceMap at all. These methods return null
    // if the getNext()/getPrev() returns the original element.
    SequenceElement<HasSuggestions> seen = null;
    while (true) {
      // Went through the entire list, didn't find anything we
      // should show suggestions for.
      if (el == seen) {
        el = null;
        break;
      }

      assert el != null : "Sequence element contract does't allow this.";

      if (el.value().isAccessibleFromKeyboard()) {
        break;
      }

      if (seen == null) {
        seen = el;
      }

      if (rightWards) {
        el = el.getNext();
      } else {
        el = el.getPrev();
      }
    }

    return el;
  }

  @Override
  public void onHide(PopupEventSourcer source) {
    // Restore selection that we lost
    // TODO(danilatos): Transform this against operations that came in the meantime
    try {
      if (savedSelection != null) {
        selectionHelper.setSelectionRange(savedSelection);
      }
    } finally {
      savedSelection = null;
      if (current != null) {
        changeAwayFromCurrent();
        current = null;
      }
    }
  }

  @Override
  public void onShow(PopupEventSourcer source) {
    // NOTE(user): Clear selection so that it doesn't get forcibly restored
    // when applying operations. In Firefox, that would take focus away from the
    // suggestion menu.
    selectionHelper.clearSelection();
    FocusImpl.getFocusImplForPanel().focus(menu.getElement());
  }

  @Override
  public void setPopupPositionAndMakeVisible(Element reference, final Element popup) {
    Style popupStyle = popup.getStyle();

    // TODO(danilatos): Do something more intelligent than arbitrary constants (which might be
    // susceptible to font size changes, etc)
    popupStyle.setLeft(popupAnchor.getAbsoluteLeft() - popup.getOffsetWidth() + 26, Unit.PX);
    popupStyle.setTop(popupAnchor.getAbsoluteBottom() + 5, Unit.PX);

    popupStyle.setVisibility(Visibility.VISIBLE);
  }

  private void changeAwayFromCurrent() {
    current.value().handleHideSuggestionMenu();
  }
}
