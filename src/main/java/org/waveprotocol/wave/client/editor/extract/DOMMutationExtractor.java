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
import com.google.gwt.user.client.Command;
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentRange;
import org.waveprotocol.wave.client.editor.impl.NodeManager;
import org.waveprotocol.wave.client.editor.selection.content.SelectionHelper;
import org.waveprotocol.wave.client.scheduler.CommandQueue;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringSet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Class for handling DOM mutation. This currently reverts all unexpected DOM
 * mutations without causing the red flash.
 *
 * NOTE(user): Consider trigger red flash for unexpected DOM mutations other
 * than Apple+B
 *
 */
public final class DOMMutationExtractor {

  private static final StringSet DOM_EVENTS_IGNORE = CollectionUtils.createStringSet();

  private final CommandQueue deferredCommands;

  private final SelectionHelper passiveSelectionHelper;

  /**
   * List of content elements queued for revert.
   */
  private final List<ContentElement> toRevert = new ArrayList<ContentElement>();

  /**
   * Is set to true while we are reverting.
   */
  private boolean isReverting;

  /**
   * The content range prior to DOM mutation event.
   */
  private ContentRange previousContentRange;

  static {
    for (String e : new String[] {"DOMSubtreeModified", "DOMNodeRemovedFromDocument",
        "DOMNodeInsertedIntoDocument", "DOMAttrModified", "DOMCharacterDataModified",
        "DOMElementNameChanged", "DOMAttributeNameChanged"}) {
      DOM_EVENTS_IGNORE.add(e);
    }
  }

  private final Command revertCmd = new Command() {
    public void execute() {
      isReverting = true;
      domMutationLog("nodes to revert: " + toRevert.size());
      for (ContentElement e : toRevert) {
        revertElement(e);
      }
      toRevert.clear();
      // Set the selection to the end.
      if (previousContentRange != null) {
        passiveSelectionHelper.setSelectionPoints(previousContentRange.getFirst(),
            previousContentRange.getSecond());
      }
      isReverting = false;
    }

    private void revertElement(ContentElement e) {
      if (e == null || !e.isContentAttached()) {
        return;
      }
      e.revertImplementation();
    }
  };

  /**
   * Creates a DOMMutationExtractor.
   */
  public DOMMutationExtractor(CommandQueue deferredCommands,
      SelectionHelper passiveSelectionHelper) {
    this.deferredCommands = deferredCommands;
    this.passiveSelectionHelper = passiveSelectionHelper;
  }

  /**
   * Handles DOM mutation events.
   * @param event
   * @param contentRange  last known selection
   */
  public void handleDOMMutation(SignalEvent event, ContentRange contentRange) {
    // Early exit if non-safari or non-mac
    if (!(UserAgent.isSafari() && UserAgent.isMac())) {
      return;
    }

    // We don't care about DOMMutations that we generate while we are reverting.
    if (isReverting) {
      return;
    }

    previousContentRange = contentRange;

    Node n = event.getTarget();
    if (n.getNodeType() == Node.ELEMENT_NODE) {
      Element e = Element.as(event.getTarget());
      if (DOM_EVENTS_IGNORE.contains(event.getType())) {
        // ignore
        return;
      } else if (event.getType().equals("DOMNodeInserted") && handleDOMNodeInserted(e)) {
        return;
      } else if (event.getType().equals("DOMNodeRemoved") && handleDOMNodeRemoved(e)) {
        return;
      }
    }
    return;
  }

  private boolean handleDOMNodeInserted(final Element e) {
    if (e.getTagName().equals("SPAN") && e.hasAttribute("class")
        && e.getAttribute("class").equals("Apple-style-span")) {
       scheduleElementForRevert(e.getParentElement());
    }
    return true;
  }

  private boolean handleDOMNodeRemoved(final Element e) {
    if (e.getTagName().equals("B")) {
      scheduleElementForRevert(e.getParentElement());
    }
    return false;
  }

  private void scheduleElementForRevert(Element e) {
    // We get the back reference of the target's parent, because even if this
    // element is just inserted, the parent should have a corresponding content node.
    final ContentElement content = NodeManager.getBackReference(e);
    if (content == null) {
      return;
    }

    Iterator<ContentElement> i = toRevert.iterator();
    while (i.hasNext()) {
      ContentElement current = i.next();
      if (isAncestorOf(current, content)) {
        // Ancestor has already been scheduled for revert, we can return early.
        return;
      } else if (isAncestorOf(content, current)){
        // We no longer need to revert his node, as we will revert the ancestor
        i.remove();
      }
    }
    toRevert.add(content);

    schedule();
  }

  private void schedule() {
    deferredCommands.addCommand(revertCmd);
  }

  private void domMutationLog(String msg) {
    EditorStaticDeps.logger.trace().log("DOMMutationExtractor: " + msg);
  }

  /**
   * Returns true iff a is b or an ancestor of b
   * @param a
   * @param b
   */
  public static boolean isAncestorOf(ContentElement a, ContentElement b) {
    while (b != null) {
      if (a == b) {
        return true;
      }
      b = b.getParentElement();
    }
    return false;
  }
}
