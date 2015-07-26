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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.RestrictedRange;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.ContentView;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlInserted;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.impl.HtmlView;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.SchedulerTimerService;
import org.waveprotocol.wave.client.scheduler.TimerService;

import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * This class extracts operations from one or more typing changes made by the
 * native editor. It relies on the various EditorImpl implementations to call
 * its {@link #somethingHappened(Point)} method appropriately.
 *
 * The extractor may at any time have outstanding changes queued up for several
 * ranges of text, each comprising multiple text nodelets and up to 2
 * ContentTextNodes one text node. Call {@link #flush()} to ensure all such
 * outstanding changes have been extracted
 */
public class TypingExtractor {

  /**
   * The interface used by the typing extractor to express what typing changes
   * have occurred.
   */
  public interface TypingSink {

    /**
     * Called just before the flush code executes
     */
    void aboutToFlush();

    /**
     * Replacement of text occurred at the given location.
     *
     * The deleted area must only encompass text.
     *
     * @param start Place where replacement begins
     * @param length Length of replaced text
     * @param text Replacement text
     * @param range Range bounding the typing sequence
     */
    void typingReplace(Point<ContentNode> start, int length, String text,
        RestrictedRange<ContentNode> range);
  }

  /**
   * An interface that provides information about the current user selection
   * in the raw html.
   *
   * Useful to provide a fake implementation for testing.
   */
  public interface SelectionSource {
    /**
     * @return The start point of the selection in the raw html
     */
    Point<Node> getSelectionStart();

    /**
     * @return The end point. It could be the same as the start point.
     */
    Point<Node> getSelectionEnd();
  }

  /** Our "output" */
  private final TypingSink sink;

  /** Needed to get the wrapper for a given html node */
  private final NodeManager manager;

  /** How we know what the current selection is */
  private final SelectionSource selectionSource;


  /** Areas of changing text we are tracking */
  private final List<TypingState> statePool = new ArrayList<TypingState>();

  /** Number of states being tracked */
  private int numStates = 0;

  /** html */
  private final HtmlView filteredHtmlView;

  /** corresponding content */
  private final ContentView renderedContentView;

  /** when things go wrong */
  private final Repairer repairer;

  /**
   * To prevent infinite recursion when searching for nearby areas of text.
   * We only need to search once, so a boolean suffices.
   */
  private boolean searchingForAdjacentArea = false;

  /**
   * Mutating State
   * Tracks text changing under a single ContentTextNode (possibly
   * multiple text nodelets). Sometimes we need to track more than
   * one of these at a time.
   */
  private class TypingState {

    /**
     * The wrapper nodes whose html text node(s) are being changed. If null,
     * we don't have a wrapper associated with the current action (meaning we are
     * inserting new text in an empty element, or between two elements).
     * Currently, firstWrapper will either be equal to or adjacent to lastWrapper.
     * We need two when the typing occurs at a boundary between content text nodes.
     */
    private ContentTextNode firstWrapper = null, lastWrapper = null;

    /** The range being changed from the content view */
    private RestrictedRange<ContentNode> contentRange = null;

    /** The range being changed from the html view */
    private RestrictedRange<Node> htmlRange = null;

    /**
     * The smallest length before the selection across
     * all versions of the text under the restricted range
     */
    private int minpre = 0;

    /**
     * The last text node we saw in a previous call to
     * {@link #somethingHappened(Point)}. Usually it will be the same node,
     * so we can speed things up by avoiding more expensive checks to see if we
     * are typing in the same place
     */
    private Text lastTextNode = null;

    /**
     * clears all state
     */
    private void clear() {
      contentRange = null;
      firstWrapper = null;
      lastWrapper = null;
      htmlRange = null;
      minpre = 0;
      lastTextNode = null;
    }

    /**
     * @return true if this state can be reused
     */
    private boolean isClear() {
      return contentRange == null;
    }

    private boolean isPartOfThisState(Point<Node> point) {

      checkRangeIsValid();

      Text node = point.isInTextNode()
        ? point.getContainer().<Text>cast()
        : null;

      if (node == null) {
        // If we're not in a text node - i.e. we just started typing
        // either in an empty element, or between elements.
        if (htmlRange.getNodeAfter() == point.getNodeAfter()
            && htmlRange.getContainer() == point.getContainer()) {
          return true;
        } else if (point.getNodeAfter() == null) {
          return false;
        } else {
          return partOfMutatingRange(point.getNodeAfter());
        }
      }

      // The first check is redundant but speeds up the general case
      return node == lastTextNode || partOfMutatingRange(node);
    }

    private boolean partOfMutatingRange(Node node) {
      return htmlRange.contains(filteredHtmlView, node);
    }

    /**
     * Specify that a typing sequence might be starting between two elements,
     * or in an empty element. If there is a text node at the cursor,
     * {@link #startTypingSequence(Point.Tx)} should be called instead
     * @param point A point between nodes
     */
    private void startTypingSequence(Point.El<Node> point) {
      htmlRange = RestrictedRange.collapsedAt(filteredHtmlView, point);
      assert htmlRange.getContainer() != null;
      assert
          (htmlRange.getNodeBefore() == null || !DomHelper.isTextNode(htmlRange.getNodeBefore())) &&
          (htmlRange.getNodeAfter() == null || !DomHelper.isTextNode(htmlRange.getNodeAfter()));

      contentRange = RestrictedRange.<ContentNode>boundedBy(
          NodeManager.getBackReference(htmlRange.getContainer().<Element>cast()),
          NodeManager.getBackReference(htmlRange.getNodeBefore().<Element>cast()),
          NodeManager.getBackReference(htmlRange.getNodeAfter().<Element>cast()));
    }

    /**
     * Start a typing sequence in a text node, even if the text node has no
     * ContentTextNode wrapper.
     * @param previousSelectionStart The selection just before the text changes. However
     *   it's not the end of the world if it's the selection after the text changed,
     *   as often happens with some international input methods.
     * @throws HtmlMissing When something is abnormal
     * @throws HtmlInserted When an element got inserted. We won't throw this
     *   for text nodes, instead we'll assume they're new and part of this
     *   typing sequence.
     */
    private void startTypingSequence(Point.Tx<Node> previousSelectionStart)
        throws HtmlMissing, HtmlInserted {
      Text node = previousSelectionStart.getContainer().cast();
      ContentView renderedContent = renderedContentView;
      HtmlView filteredHtml = filteredHtmlView;
      try {
        // This might throw an exception
        ContentTextNode wrapper = manager.findTextWrapper(node, true);

        // No exception -> already a wrapper for this node (we're editing some existing text)
        firstWrapper = wrapper;
        lastWrapper = wrapper;

        checkNeighbouringTextNodes(previousSelectionStart);

        contentRange = RestrictedRange.around(renderedContent, firstWrapper, lastWrapper);

        // Ensure methods we call on the text node operate on the same view as us
        assert wrapper.getFilteredHtmlView() == filteredHtml;

        Node htmlNodeBefore = filteredHtml.getPreviousSibling(firstWrapper.getImplNodelet());
        Element htmlParent = filteredHtml.getParentElement(node);
        ContentNode cnodeAfter = contentRange.getNodeAfter();
        Node htmlNodeAfter = cnodeAfter == null ? null : cnodeAfter.getImplNodelet();
        htmlRange = RestrictedRange.between(
            htmlNodeBefore, Point.inElement(htmlParent, htmlNodeAfter));

        if (partOfMutatingRange(filteredHtml.asText(previousSelectionStart.getContainer()))) {
          // This must be true if getWrapper worked correctly. Program error
          // otherwise (not browser error)
          assert firstWrapper.getImplNodelet() == htmlRange.getStartNode(filteredHtml);

          // NOTE(danilatos): We are asking the firstWrapper to give us the offset of
          // a nodelet that might not actually belong to it, but to its next sibling.
          // This is ok, because we tell it what node to stop the search at, and it
          // doesn't know any better.
          minpre = previousSelectionStart.getTextOffset() +
            firstWrapper.getOffset(node, htmlNodeAfter);
        }


      } catch (HtmlInserted e) {
        // Exception caught -> no wrapper for this node (we're starting a new chunk of text)
        Node nodeAfter = e.getHtmlPoint().getNodeAfter();
        if (!DomHelper.isTextNode(nodeAfter)) {
          throw e;
        }
        node = nodeAfter.cast();

        contentRange = RestrictedRange.collapsedAt(renderedContent, e.getContentPoint());
        Node before = contentRange.getNodeBefore() == null
            ? null : contentRange.getNodeBefore().getImplNodelet();
        Node after = contentRange.getNodeAfter() == null
            ? null : contentRange.getNodeAfter().getImplNodelet();
        htmlRange    = RestrictedRange.between(before,
            Point.inElement(contentRange.getContainer().getImplNodelet(), after));
      }
    }

    /**
     * Continue tracking an existing typing sequence, after we have determined
     * that this selection is indeed part of an existing one
     * @param previousSelectionStart
     * @throws HtmlMissing
     * @throws HtmlInserted
     */
    private void continueTypingSequence(Point<Node> previousSelectionStart)
        throws HtmlMissing, HtmlInserted {

      if (firstWrapper != null) {

        // minpost is only needed if we allow non-typing actions (such as moving
        // with arrow keys) to stay as part of the same typing sequence. otherwise,
        // minpost should always correspond to the last cursor position.
        // TODO(danilatos): Ensure this is the case
        updateMinPre(previousSelectionStart);

        // TODO(danilatos): Is it possible to ever need to check neighbouring
        // nodes if we're not in a text node now? If we're not, we are almost
        // certainly somewhere were there are no valid neighbouring text nodes,
        // otherwise the selection should have been reported as in one of
        // those nodes.....
        checkNeighbouringTextNodes(previousSelectionStart);
      }
    }

    /**
     * Checks to see if we need to expand the current state to cover a larger
     * area, or add a new state to track inside a neighbouring element.
     * @param previousSelectionStart
     * @throws HtmlMissing
     * @throws HtmlInserted
     */
    private void checkNeighbouringTextNodes(Point<Node> previousSelectionStart)
        throws HtmlMissing, HtmlInserted {
      // Note that this method is called before most of the member variables are
      // initialised, so therefore don't use them! However we assert that we
      // have first & last wrapper, which is what we care about here.
      assert firstWrapper != null && lastWrapper != null;
      if (searchingForAdjacentArea) {
        return;
      }
      try {
        searchingForAdjacentArea = true;

        HtmlView filteredHtml = filteredHtmlView;
        ContentView renderedContent = renderedContentView;

        // Is this method slow? we need it often enough, but not in 95% of scenarios,
        // so there is room to optimise.

        // See if there are other text nodes we should check
        Text selNode = previousSelectionStart.getContainer().cast();
        int selOffset = previousSelectionStart.getTextOffset();

        // TODO(patcoleman): see if being zero here is actually a problem.
        // assert selNode.getLength() > 0;

        if (selOffset == 0 && firstWrapper.getImplNodelet() == selNode) {
          // if we are at beginning of mutating node
          ContentNode prev = renderedContent.getPreviousSibling(firstWrapper);
          if (prev != null && prev.isTextNode()) {
            firstWrapper = (ContentTextNode)prev;
          }
        } else {
          ContentNode nextNode = renderedContent.getNextSibling(lastWrapper);
          Node nextNodelet = nextNode != null ? nextNode.getImplNodelet() : null;
          if (selOffset == selNode.getLength() &&
              filteredHtml.getNextSibling(selNode) == nextNodelet) {
            // if we are at end of mutating node
            if (nextNode != null && nextNode.isTextNode()) {
              lastWrapper = (ContentTextNode)nextNode;
            }
          }
        }
      } finally {
        searchingForAdjacentArea = false;
      }
    }

    /**
     * @return The current value of the text in the html, within our tracked range
     */
    private String calculateNewValue() {
      HtmlView filteredHtml = filteredHtmlView;
      Text fromIncl = htmlRange.getStartNode(filteredHtml).cast();
      Node toExcl = htmlRange.getPointAfter().getNodeAfter();

      return ContentTextNode.sumTextNodes(fromIncl, toExcl, filteredHtml);
    }

    /**
     * Set the last text node we looked at, to optimise the general case of
     * checking if a text node is part of the given typing sequence.
     */
    private void setLastTextNode(Text node) {
      lastTextNode = node;
    }

    /**
     * Outputs any pending operations and reset state
     */
    public void flush() {

      try {
        // Return if no operations are pending
        if (isClear()) {
          return;
        }

        checkRangeIsValid();

        String newValue = calculateNewValue();
        if (firstWrapper == null) {
          if (newValue.length() > 0) {
            // point after and point before should be identical, point after is
            // a simple getter though, rather than involving a calculation.
            sink.typingReplace(contentRange.getPointAfter(), 0, newValue, contentRange);
          }
        } else {
          // TODO(danilatos): Avoid calculating all of impl data
          // NOTE(danilatos): Assume that our range contains at most 2 wrappers
          String originalValue = firstWrapper.getData() +
              (firstWrapper != lastWrapper ? lastWrapper.getData() : "");

          Point<Node> selectionStart = selectionSource.getSelectionStart();
          Point<Node> selectionEnd = selectionSource.getSelectionEnd();

          // TODO(danilatos): Use some old selection value rather than forcing
          // checking the whole node?
          if (selectionStart != null) {
            updateMinPre(selectionStart);
          } else {
            minpre = 0;
          }

          int minpost;
          if (selectionEnd != null) {
            int endOffset = getAbsoluteOffset(selectionEnd);
            minpost = (endOffset == -1) ? 0 : newValue.length() - endOffset;

//            // XXX(danilatos): Figure out why this might ever happen, instead of just
//            // stupidly guarding the condition
//            if (minpost > originalValue.length() || minpost > newValue.length()) {
//              minpost = 0;
//            }
          } else {
            minpost = 0;
          }

          assert minpre >= 0 && minpost >= 0 : "minpre/minpost outside valid range, minpre: " +
              minpre + " minpost: " + minpost;

          if (minpre < 0) minpre = 0;
          if (minpost < 0) minpost = 0;

          // Compute what has been deleted and what been inserted
          // based solely on minpre and minpost
          int deleteEndIndex = originalValue.length() - minpost;
          int insertEndIndex = newValue.length() - minpost;
          int startIndex = Math.min(minpre, deleteEndIndex);

          // Try to expand/contract the region of change
          // Expanding sometimes happens with multilanguage input.
          // E.g. when typing with pinyin, you can type a whole bunch of roman
          // characters, then trigger them all to be converted at once by
          // pressing space
          while (startIndex > 0
              && originalValue.charAt(startIndex - 1) != newValue.charAt(startIndex - 1)) {
            startIndex--;
          }
          while (startIndex < deleteEndIndex && startIndex < insertEndIndex
              && originalValue.charAt(startIndex) == newValue.charAt(startIndex)) {
            startIndex++;
          }

          int minpostLeft = minpost;
          while (minpostLeft > 0
              && originalValue.charAt(deleteEndIndex) != newValue.charAt(insertEndIndex)) {
            deleteEndIndex++;
            insertEndIndex++;
            minpostLeft--;
          }
          while (startIndex < deleteEndIndex && startIndex < insertEndIndex
              && originalValue.charAt(deleteEndIndex - 1) ==
                newValue.charAt(insertEndIndex - 1)) {
            deleteEndIndex--;
            insertEndIndex--;
          }

          assert startIndex <= deleteEndIndex : "startIndex larger than deleteEndIndex, " +
              "startIndex: " + startIndex + " deleteEndIndex: " + deleteEndIndex;
          assert startIndex <= insertEndIndex : "startIndex larger than insertEndIndex, " +
              "startIndex: " + startIndex + " insertEndIndex: " + insertEndIndex;

          // Check whether we need to do a delete or an insert now, as
          // we might be modifying these variables shortly.
          boolean deleting = startIndex < deleteEndIndex;
          boolean inserting = startIndex < insertEndIndex;

          int deleteSize = deleteEndIndex - startIndex;

          // Figure out what node the start point lies in
          ContentTextNode startNode = firstWrapper, deleteEndNode = firstWrapper;
          if (firstWrapper.getLength() < startIndex) {
            assert firstWrapper != lastWrapper : "first wrapper != lastWrapper";
            startIndex -= firstWrapper.getLength();
            startNode = lastWrapper;
          }
          Point<ContentNode> start = Point.<ContentNode>inText(startNode, startIndex);

          if (deleting || inserting) {
            sink.typingReplace(start, deleteSize, newValue.substring(startIndex, insertEndIndex),
                contentRange);
          }

        }
      } catch (RuntimeException e) { // TODO(danilatos): Is this the best type & place to catch?
        EditorStaticDeps.logger.error().log(e);
        EditorStaticDeps.logger.trace();
        tryRepair();
      } finally {
        // Clear all state
        clear();
      }
    }

    private void tryRepair() {
      if (contentRange == null) {
        // no range, so revert everything
        repairer.revert(renderedContentView, renderedContentView.getDocumentElement());
      } else {
        repairer.revert(
            contentRange.getPointBefore(renderedContentView),
            contentRange.getPointAfter());
      }
      clear();
    }

    /**
     * Updates minpre
     *
     * @param selectionStart
     */
    private void updateMinPre(Point<Node> selectionStart) {

      if (selectionStart == null) {
        minpre = 0;
        return;
      }

      int newVal = getAbsoluteOffset(selectionStart);
      if (newVal == -1) {
        newVal = 0;
      }
      minpre = Math.min(minpre, newVal);
    }

    /**
     * @param point
     * @return The offset of the point with respect to the start of our typing
     *    sequence, not with respect to its container text node. Returns -1
     *    if the point's container node isn't one of the impl nodes of our
     *    mutatingNode.
     */
    private int getAbsoluteOffset(Point<Node> point) {
      // This method is used to calculate minpre and minpost.
      // We shouldn't need this method if mutatingNode is null, because
      // in such cases minpre and minpost are both trivially zero.
      assert firstWrapper != null;

      if (partOfMutatingRange(point.getContainer())) {
        // TODO(danilatos): check for mutatingNodeOwns duplicates a loop which
        // is done in getOffset
        Text toFind = point.getContainer().<Text>cast();
        HtmlView filteredHtml = filteredHtmlView;

        return ContentTextNode.getOffset(
            toFind,
            htmlRange.getStartNode(filteredHtml).<Text>cast(),
            htmlRange.getNodeAfter(),
            filteredHtml) + point.getTextOffset();
      }

      return -1;
    }

  }

  /**
   * Use this to schedule a future flush unless one is already pending
   */
  private final Scheduler.Task flushCmd;

  private final TimerService timerService;

  /**
   * @param sink
   * @param manager
   * @param selectionSource
   */
  public TypingExtractor(TypingSink sink, NodeManager manager,
      HtmlView filteredHtmlView, ContentView renderedContentView,
      Repairer repairer, SelectionSource selectionSource) {
    // TYPING EXTRACTOR MUST ALWAYS BE CRITICAL PRIORITY
    // NOTHING ELSE CAN BE CRITICAL
    this(sink, manager,
        new SchedulerTimerService(SchedulerInstance.get(), Scheduler.Priority.CRITICAL),
        filteredHtmlView, renderedContentView, repairer, selectionSource);
  }

  TypingExtractor(TypingSink sink, NodeManager manager, TimerService service,
      HtmlView filteredHtmlView, ContentView renderedContentView, Repairer repairer,
      SelectionSource selectionSource) {
    this.sink = sink;
    this.manager = manager;
    this.selectionSource = selectionSource;
    this.timerService = service;
    this.filteredHtmlView = filteredHtmlView;
    this.renderedContentView = renderedContentView;
    this.repairer = repairer;
    this.flushCmd = new  Scheduler.Task() {
      @Override
      public void execute() {
        flush();
      }
    };
  }

  /**
   *
   * TODO: use isSameRange. currently, it'll just start a new typing sequence
   * @param previousSelectionStart Where the selection start is,
   *    before the result of typing
   * @throws HtmlMissing
   * @throws HtmlInserted
   */
  public void somethingHappened(Point<Node> previousSelectionStart)
      throws HtmlMissing, HtmlInserted {
    Preconditions.checkNotNull(previousSelectionStart,
        "Typing extractor notified with null selection");
    Text node = previousSelectionStart.isInTextNode()
      ? previousSelectionStart.getContainer().<Text>cast()
      : null;

    // Attempt to associate our location with a node
    // This should be a last resort, ideally we should be given selections
    // in the correct text node, when the selection is at a text node boundary
    if (node == null) {
      HtmlView filteredHtml = filteredHtmlView;
      Node nodeBefore = Point.nodeBefore(filteredHtml, previousSelectionStart.asElementPoint());
      Node nodeAfter = previousSelectionStart.getNodeAfter();
      //TODO(danilatos): Usually we would want nodeBefore as a preference, but
      // not always...
      if (nodeBefore != null && DomHelper.isTextNode(nodeBefore)) {
        node = nodeBefore.cast();
        previousSelectionStart = Point.<Node>inText(node, 0);
      } else if (nodeAfter != null && DomHelper.isTextNode(nodeAfter)) {
        node = nodeAfter.cast();
        previousSelectionStart = Point.<Node>inText(node, node.getLength());
      }
    }

    TypingState t = findTypingState(previousSelectionStart);

    if (t == null) {
      t = getFreshTypingState();
      if (node != null) {
        // check the selection is in a text point, and start the sequence
        Preconditions.checkNotNull(previousSelectionStart.asTextPoint(),
            "previousSelectionStart must be a text point");
        t.startTypingSequence(previousSelectionStart.asTextPoint());
      } else {
        // otherwise make sure we're not in a text point, and start the sequence
        Preconditions.checkState(!previousSelectionStart.isInTextNode(),
            "previousSelectionStart must not be a text point");
        t.startTypingSequence(previousSelectionStart.asElementPoint());
      }
    } else {
      t.continueTypingSequence(previousSelectionStart);
    }

    t.setLastTextNode(node);
    timerService.schedule(flushCmd);
  }

  /**
   * Outputs any pending operations and reset all states
   */
  public void flush() {
    sink.aboutToFlush();
    for (int i = 0; i < numStates; i++) {
      TypingState t = statePool.get(i);
      if (t.isClear()) {
        continue;
      }
      t.flush();
    }
    numStates = 0;
  }

  /**
   * @return true if we are in the middle of extracting
   */
  public boolean isBusy() {
    for (int i = 0; i < numStates; i++) {
      TypingState t = statePool.get(i);
      if (t.isClear()) {
        continue;
      }
      return true;
    }
    return false;
  }

  /**
   * @param selection
   * @return The typing state that the given text node is a part of, or null if none
   */
  private TypingState findTypingState(Point<Node> selection) {
    for (int i = 0; i < numStates; i++) {
      TypingState t = statePool.get(i);
      if (t.isClear()) {
        continue;
      }
      if (t.isPartOfThisState(selection)) {
        return t;
      }
    }
    return null;
  }

  /**
   * @return A clear typing state from our pool of states
   */
  private TypingState getFreshTypingState() {
    //TODO(danilatos): In the background, reduce numStates
    numStates++;
    if (numStates > statePool.size()) {
      TypingState t = new TypingState();
      statePool.add(t);
      return t;
    } else {
      assert statePool.get(numStates - 1).isClear();
      return statePool.get(numStates - 1);
    }
  }


  /**
   * Ensures the html range is valid. If it isn't, it can repair it in some
   * circumstances. In others, it will throw an exception.
   */
  private void checkRangeIsValid() {
    //TODO(danilatos) I haven't encountered this issue in practice yet, but it's a
    //potential hole that needs to be plugged.


    //if (htmlRange.getNodeBefore())
  }


}

