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

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.StyleInjector;

import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.RestrictedRange;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.ContentView;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlInserted;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.impl.HtmlView;
import org.waveprotocol.wave.client.scheduler.ScheduleTimer;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Repairs inconsistencies between content and html, in both directions.
 *
 * TODO(danilatos): This class is just a loosely related collection of methods,
 * and does not encapsulate any state other than a reference to its bundle. Perhaps
 * break it up, or convert to static methods?
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class Repairer {

  /** For testing/debug purposes, treat repairs as fatal errors */
  @VisibleForTesting public static boolean debugRepairIsFatal = getAssertionsOn();

  private static boolean getAssertionsOn() {
    try {
      assert false;
    } catch (AssertionError e) {
      return true;
    }
    return false;
  }

  /**
   * Resources for repairing & problem finding
   */
  public static final ExtractResources resources;
  static {
    if (GWT.isClient()) {
      resources = GWT.create(ExtractResources.class);
      StyleInjector.inject(resources.css().getText(), true);
    } else {
      resources = null;
    }
  }

  private final ContentView persistentView, renderedView;
  private final HtmlView strippingView;
  private final RepairListener listener;


  /**
   * @param persistentView
   * @param renderedView
   * @param strippingView
   */
  public Repairer(ContentView persistentView, ContentView renderedView, HtmlView strippingView,
      RepairListener listener) {
    this.persistentView = persistentView;
    this.renderedView = renderedView;
    this.strippingView = strippingView;
    this.listener = listener;
  }

  /**
   * Generic handler for any inconsistency exception
   * @param error
   */
  public void handle(InconsistencyException error) {
    // Do something nicer (visitor?) if we end up with more
    // inconsistency exception types
    if (error instanceof HtmlInserted) {
      handleInserted((HtmlInserted) error);
    } else {
      handleMissing((HtmlMissing) error);
    }
  }

  /**
   * Specific handler for {@link HtmlInserted}
   * Currently just reverts the HTML
   * @param error
   */
  public void handleInserted(HtmlInserted error) {
    EditorStaticDeps.logger.error().log("handleInserted: ", error);
    assert false : "Repairer triggered, handleInserted";
    revert(error.getContentPoint(), null);
  }

  /**
   * Specific handler for {@link HtmlMissing}
   * Currently just reverts the HTML
   * @param error
   */
  public void handleMissing(HtmlMissing error) {
    EditorStaticDeps.logger.error().log("handleMissing: ", error);
    assert false : "Repairer triggered, handleMissing";
    revert(Point.before(renderedView, error.getBrokenNode()), null);
  }

  /**
   * Revert the HTML implementation between two node-bounded points.
   * The points must be in the container element.
   * TODO(danilatos): Change interface to use RestrictedRange, or improve
   * implementation to not require restriction.
   *
   * @param start
   * @param end if null, keep going until consistency is found
   */
  @SuppressWarnings("deprecation")
  private void revertWithoutNotification(Point.El<ContentNode> start, Point.El<ContentNode> end) {
    if (debugRepairIsFatal) {
      throw new RuntimeException("Repair is fatal");
    }

    if (start.getContainer() == null) {
      revert(renderedView, renderedView.getDocumentElement());
      return;
    }

    EditorStaticDeps.startIgnoreMutations();
    try {

      // TODO(danilatos): Log all calls to here, & find causes.
      // In the ideal universe, there would be none!
      EditorStaticDeps.logger.error().logPlainText("REPAIRING!! " + start.getContainer());
      flashShowRepair((ContentElement) start.getContainer());

      try {
        revertInner(start, end);
        return;
      } catch (RuntimeException t1) {
        EditorStaticDeps.logger.error().logPlainText("exception while revertInner: " + t1);
        for (ContentNode attempt = start.getContainer(); attempt != null; attempt =
            attempt.getParentElement()) {
          try {
            attempt.revertImplementation();
            return;
          } catch (RuntimeException t2) {
            EditorStaticDeps.logger.error().logPlainText(
                "Exception while revertImplementation: " + t2);
            // iterate

            if (attempt == persistentView.getDocumentElement()) {
              // We cannot reconstruct the document. Re-throw the original exception from
              // the first repair attempt so we get as meaningful an error message and
              // stacktrace as possible.
              throw t1;
            }
          }
        }
      }
    } finally {
      EditorStaticDeps.endIgnoreMutations();
    }
    EditorStaticDeps.logger.trace().logPlainText("Revert successful");
  }

  /**
   * Same as {@link #revertWithoutNotification(Point.El, Point.El)}, but first notifies listener.
   */
  public void revert(Point.El<ContentNode> start, Point.El<ContentNode> end) {
    // notify and fix
    listener.onRangeRevert(start, end);
    revertWithoutNotification(start, end);
  }

  /**
   * Reverts the entire content of a particular element
   * @param doc The document containing the element to revert
   * @param element The element to revert
   */
  public void revert(ReadableDocument<ContentNode, ContentElement, ContentTextNode> doc,
      ContentElement element) {
    // find start and end as points
    Point.El<ContentNode> start = Point.start(doc, element);
    Point.El<ContentNode> end = Point.end((ContentNode) element);

    // notify and fix.
    if (element == doc.getDocumentElement()) {
      listener.onFullDocumentRevert(doc);
    } else {
      listener.onRangeRevert(start, end);
    }
    revertWithoutNotification(start, end);
  }

  /**
   * Briefly give a visual cue that the given element has been repaired
   * @param regionNode
   */
  public void flashShowRepair(final ContentElement regionNode) {
    EditorStaticDeps.logger.error().log("repairing region: " + regionNode);
    if (!LogLevel.showErrors()) {
      return;
    }

    if (regionNode.getImplNodelet() == null) {
      return;
    }
    regionNode.getImplNodelet().addClassName(resources.css().repaired());
    ScheduleTimer t = new ScheduleTimer() {
      @Override
      public void run() {
        regionNode.getImplNodelet().removeClassName(resources.css().repaired());
      }
    };
    t.schedule(800);
  }

  /**
   * Mark a region (probably everything) as not recoverable
   * @param regionNode
   */
  public void showDeath(ContentElement regionNode) {
    if (!LogLevel.showErrors()) {
      return;
    }
    if (regionNode.getImplNodelet() != null) {
      regionNode.getImplNodelet().addClassName(resources.css().dead());
    }
  }

  /**
   * Clear death marking. Probably because we are calling setContent and
   * totally refreshing everything.
   *
   * @param regionNode
   */
  public void hideDeath(ContentElement regionNode) {
    if (!LogLevel.showErrors()) {
      return;
    }
    if (regionNode.getImplNodelet() != null) {
      regionNode.getImplNodelet().removeClassName(resources.css().dead());
    }
  }

  /**
   * Same as {@link #revertWithoutNotification(Point.El, Point.El)}, but without runtime exception
   * handling
   */
  void revertInner(Point.El<ContentNode> start, Point.El<ContentNode> end) {
    // TODO(danilatos): This naive implementation has three main problems
    // 1. It does more work than it needs to, by throwing everything away and
    //    redoing it
    // 2. It blatantly disregards transparent nodes. It should "work" with them
    //    around, but it might mess them up.
    // 3. The code is too complicated

    ContentView renderedContent = renderedView;

    assert end == null || end.getContainer() == start.getContainer() :
        "No reverting across elements";

    ContentNode before = Point.nodeBefore(renderedContent, start);
    Node nodeletBefore = before == null ? null : before.getImplNodelet();
    Element parentNodelet;
    if (nodeletBefore == null) {
      parentNodelet = renderedContent.getVisibleNode(start.getContainer()).getImplNodelet().cast();
    } else {
      parentNodelet = nodeletBefore.getParentElement();
    }

    ContentNode first = start.getNodeAfter();
    ContentNode last = end == null ? null : end.getNodeAfter();
    assert renderedContent.getVisibleNode(first) == first;
    assert renderedContent.getVisibleNode(last) == last;
    assert last == null
        || renderedContent.getParentElement(first) == renderedContent.getParentElement(last)
        : "First and last are expected to have same parent";
    {
      ContentNode node;
      for (node = first; node != last; node = renderedContent.getNextSibling(node)) {
        // If node is consistent, we assume the ones after are ok.
        if (end == null && node.isConsistent()) {
          break;
        }

        node.revertImplementation();
      }
      last = node;
    }

    // TODO(danilatos): Actually use some view that strips unknown nodes out. or do something
    // better.
    Node nodeletAfter = last == null ? null : last.getImplNodelet();

    reattachImplChildren(parentNodelet, nodeletBefore, nodeletAfter, first, last);
  }

  private void reattachImplChildren(Node parentNodelet, Node nodeletBefore, Node nodeletAfter,
      ContentNode first, ContentNode last) {
    // TODO(danilatos): Replace this hairy code with pre-order traversal
    // getters, once they exist
    while (true) {
      Node nodelet = nodeletBefore == null
          ? strippingView.getFirstChild(parentNodelet)
          : strippingView.getNextSibling(nodeletBefore);

      if (nodelet == null || nodelet == nodeletAfter) {
        break;
      }

      if (nodelet.getParentElement() != null) {
        nodelet.removeFromParent();
      }
    }

    for (ContentNode node = first; node != last; node = renderedView.getNextSibling(node)) {
      parentNodelet.insertBefore(node.getImplNodelet(), nodeletAfter);
    }
  }

  /**
   * Zip between the given range
   * @see ContentElement#zipChildren(ContentNode, ContentNode, Node)
   * @param range
   * @param userSelection Node within which the user has their selection. If
   *    zipping ends up splitting it, we will return true, so we know if we
   *    need to restore the selection.
   *   HACK(danilatos): This is a bit nasty. Better way? (issue can probably
   *   be avoided altogether if we can have some control on which text nodes
   *   operations affect, when there is a choice).
   * @return true if html in the user's selection was affected
   *    (e.g. splitting a text node)
   */
  public boolean zipRange(RestrictedRange<ContentNode> range, Node userSelection) {
    try {
      // nodeAfter is 1 further than we need, but meh
      return ((ContentElement)range.getContainer()).zipChildrenExcludingFrom(
          range.getNodeBefore(), range.getNodeAfter(), userSelection);
    } catch (RuntimeException t) {
      // Do the more expensive, but robust action
      // TODO(danilatos): Should this be content or renderedContent?
      revert(range.getPointBefore(persistentView), range.getPointAfter());
      return true;
    }
  }

}
