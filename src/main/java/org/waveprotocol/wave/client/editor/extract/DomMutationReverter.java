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
import org.waveprotocol.wave.client.common.util.SignalEvent;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.impl.NodeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for recording unwanted destructive dom mutation events
 * into a stack, and reverting the changes.
 *
 * TODO(danilatos): GWTTestCase this. Quite browser specific though... hard.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class DomMutationReverter {

  public interface RevertListener {
    void scheduleRevert();
  }

  interface Undoable {
    void undo();
    void cleanup();
  }

  private static class RemovalEntry implements Undoable {
    Node removedNode;
    Element oldParent;
    Node oldNodeAfter;
    boolean removeAfter;

    public RemovalEntry(Node removedNode, Element oldParent, Node oldNodeAfter,
        boolean removeAfter) {
      if (oldParent == null) {
        throw new IllegalArgumentException("Old parent cannot be null");
      }
      this.removeAfter = removeAfter;
      this.removedNode = removedNode;
      this.oldParent = oldParent;
      this.oldNodeAfter = oldNodeAfter;
    }

    @Override
    public void undo() {
      oldParent.insertBefore(removedNode, oldNodeAfter);
    }

    @Override
    public void cleanup() {
      if (removeAfter) {
        EditorStaticDeps.logger.trace().log("REVERT CLEANUP: " + (DomHelper.isTextNode(removedNode)
            ? removedNode.<Text>cast().getData() : removedNode.<Element>cast().getTagName()));
        removedNode.removeFromParent();
      }
    }
  }

  private final RevertListener listener;

  private final List<Undoable> entries = new ArrayList<Undoable>();

  public DomMutationReverter(RevertListener listener) {
    this.listener = listener;
  }

  /**
   * Deals with a dom mutation event, deciding if it is damaging or not.
   *
   * WARNING: This method should NOT be called for mutation events that are a
   * result of programmatic changes - only for changes that the browser did by
   * itself and we need to investigate. Doing otherwise will result in
   * programmatic changes being reverted!
   *
   * @param event a dom mutation event
   */
  public void handleMutationEvent(SignalEvent event) {
    // TODO(user): Do we care about other types of events?
    if (event.getType().equals("DOMNodeRemoved")) {
      Node target = event.getTarget();
      boolean ignorableWhenEmpty = DomHelper.isTextNode(target)
          || !NodeManager.hasBackReference(target.<Element>cast());
      if (ignorableWhenEmpty && entries.isEmpty()) {
        // If it's a text node, or a non-backreferenced element,
        // and we don't already have entries, then we just ignore it as regular typing. Ok.
      } else {

        EditorStaticDeps.logger.trace().log("REVERT REMOVAL: " + (DomHelper.isTextNode(target)
            ? target.<Text>cast().getData() : target.<Element>cast().getTagName()));

        addEntry(new RemovalEntry(target, target.getParentElement(), target.getNextSibling(),
            ignorableWhenEmpty));
      }
    }
  }

  private void addEntry(Undoable entry) {
    entries.add(entry);
    listener.scheduleRevert();
  }

  public void flush() {
    if (entries.size() > 0) {
      EditorStaticDeps.logger.trace().log("DomMutation Flush of " + entries.size() + " mutations.");
    }
    for (int i = entries.size() - 1; i >= 0; i--) {
      entries.get(i).undo();
    }
    for (int i = 0; i < entries.size(); i++) {
      entries.get(i).cleanup();
    }
    clear();
  }

  public void clear() {
    entries.clear();
  }

  public boolean hasPendingReverts() {
    return !entries.isEmpty();
  }
}
