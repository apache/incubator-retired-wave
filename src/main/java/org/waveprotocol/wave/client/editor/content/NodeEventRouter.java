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

package org.waveprotocol.wave.client.editor.content;

import com.google.gwt.dom.client.Node;

import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlInserted;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Routes event handling to the right handler, and does default handling.
 *
 * We may want to consider eventually pulling out all the hard-coded default
 * handling into something more generalized, but keeping in mind that much of
 * this (especially the text node stuff) has to do with low level text-editing
 * concerns that have been carefully crafted and would severely break basic
 * editing if damaged.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class NodeEventRouter {

  public static final NodeEventRouter INSTANCE = new NodeEventRouter();

  private static NodeEventHandler nodeEventHandler(ContentElement e) {
    return ((AgentAdapter) e).nodeEventHandler;
  }

  public boolean handleBackspace(ContentNode node, EditorEvent event) {
    return event.getCaret().isAtBeginning() ?
        handleBackspaceAtBeginning(node, event) :
        handleBackspaceNotAtBeginning(node, event);
  }

  public boolean handleBackspaceAfterNode(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleBackspaceAfterNode(element, event)) {
      return true;
    } else {
      /*
       * Handles a backspace after node by moving caret inside element (at end),
       * and calling handleBackspace, or deleting self it is an empty element.
       */
      if (element != null && element.getFirstChild() == null) {
        // The default handling of an empty element is to delete it.
        // This is preferable than allowing the browser to do it.
        maybeDelete(element);
        return true;
      } else {
        // Otherwise, perform default behaviour for this text node, or contents
        // inside this element (depending).
        event.getCaret().setToEnd(node);
        return handleBackspace(node, event);
      }
    }
  }

  public boolean handleBackspaceAtBeginning(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleBackspaceAtBeginning(element, event)) {
      return true;
    } else {
      /*
       * Handles a backspace at beginning by moving caret outside before the element,
       * and passing on the event to parent.
       */
      return handleBackspace(event.getCaret().setToBefore(node).getContainer(), event);
    }
  }

  public boolean handleBackspaceNotAtBeginning(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element == null) {
      return handleTextNodeDeleteAction(node.asText(), event, true);
    } else if (nodeEventHandler(element).handleBackspaceNotAtBeginning(element, event)) {
      return true;
    } else {
      /*
       * Handles a backspace not at beginning by passing it onto the node
       * immediately before the caret, if such a node exists.
       */
      ContentNode childBefore = event.getCaret().getNodeBefore(FullContentView.INSTANCE);
      return childBefore == null ? false:
          handleBackspaceAfterNode(childBefore, event);
    }
  }

  public boolean handleClick(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleClick(element, event)) {
      return true;
    } else {
      ContentElement parent = node.getParentElement();
      if (parent != null && handleClick(parent, event)) {
        return true;
      } else {
        event.allowBrowserDefault();
        return false;
      }
    }
  }

  public boolean handleDelete(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleDelete(element, event)) {
      return true;
    } else {
      /*
       * Handles a delete in node by calling handleDeleteAtEnd or
       * handleDeleteNotAtEnd as appropriate
       */
      return event.getCaret().isAtEnd() ?
          handleDeleteAtEnd(node, event) :
          handleDeleteNotAtEnd(node, event);
    }
  }

  public boolean handleDeleteAtEnd(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleDeleteAtEnd(element, event)) {
      return true;
    } else {
      /*
       * Handles a delete at end by moving caret outside after the element,
       * and passing on the event to parent.
       */
      return handleDelete(event.getCaret().setToAfter(node).getContainer(), event);
    }
  }

  public boolean handleDeleteBeforeNode(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleDeleteBeforeNode(element, event)) {
      return true;
    } else {
      /*
       * Handles a delete before node by moving caret inside element (at beginning),
       * and calling handleDelete, or deleting self it is an empty element.
       */
      if (element != null && node.getFirstChild() == null) {
        // The default handling of an empty element is to delete it.
        // This is preferable than allowing the browser to do it.
        maybeDelete(element);
        return true;
      } else {
        // Otherwise, perform default behaviour for this text node, or contents
        // inside this element (depending).
        event.getCaret().setToBeginning(node);
        return handleDelete(node, event);
      }
    }
  }

  public boolean handleDeleteNotAtEnd(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element == null) {
      return handleTextNodeDeleteAction(node.asText(), event, false);
    } else if (nodeEventHandler(element).handleDeleteNotAtEnd(element, event)) {
      return true;
    } else {
      /*
       * Handles a delete not at end by passing it onto the node
       * immediately after the caret, if such a node exists.
       */
      ContentNode child = event.getCaret().getNodeAfter();
      return child != null ?
          handleDeleteBeforeNode(child, event) : false;
    }
  }

  public boolean handleEnter(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleEnter(element, event)) {
      return true;
    } else {
      /*
       * Default behavior simply passes event to parent.
       */
      ContentElement parent = node.getParentElement();
      // Move the caret out of its current node if at either end-point,
      // for example <p>a<i>|bc</i>d</p>, to avoid creating empty elements
      if (parent != null && event.getCaret().isIn(node)) {
        event.getCaret().maybeMoveOut();
      }
      return parent != null ? handleEnter(parent, event) : false;
    }
  }

  public boolean handleLeft(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleLeft(element, event)) {
      return true;
    } else {
      /*
       * Handles a left arrow by calling either handleLeftAtBeginning or
       * handleLeftAfterNode as appropriate
       */
      ContentPoint caret = event.getCaret();
      if (caret.isAtBeginning()) {
        // Caret is at beginning of node: pass event to node containing caret
        if (handleLeftAtBeginning(caret.getContainer(), event)) {
          return true;
        } else {
          event.allowBrowserDefault();
          return false;
        }
      } else {
        // Caret is not at beginning: pass event to node before caret
        ContentNode child = caret.getNodeBefore(FullContentView.INSTANCE);
        if (child != null && handleLeftAfterNode(child, event)) {
          return true;
        } else {
          event.allowBrowserDefault();
          return false;
        }
      }
    }
  }

  public boolean handleLeftAfterNode(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleLeftAfterNode(element, event)) {
      return true;
    } else {
      event.allowBrowserDefault();
      return false;
    }
  }

  public boolean handleLeftAtBeginning(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleLeftAtBeginning(element, event)) {
      return true;
    } else {
      /*
       * Delegates handling to the node before this one.
       */
      if (handleLeft(event.getCaret().setToBefore(node).getContainer(), event)) {
        return true;
      } else {
        event.allowBrowserDefault();
        return false;
      }
    }
  }

  public boolean handleRight(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleRight(element, event)) {
      return true;
    } else {
      /*
       * Handles a right arrow by calling either handleRightAtEnd or
       * handleRightBeforeNode as appropriate
       */
      ContentPoint caret = event.getCaret();
      if (caret.isAtEnd()) {
        // Caret is at end of node: pass event to node containing caret
        if (handleRightAtEnd(caret.getContainer(), event)) {
          return true;
        } else {
          event.allowBrowserDefault();
          return false;
        }
      } else if (!node.isTextNode()) {
        // Caret is not at end: pass event to node after caret
        ContentNode child = caret.getNodeAfter();
        if (child != null && handleRightBeforeNode(child, event)) {
          return true;
        } else {
          event.allowBrowserDefault();
          return false;
        }
      } else {
        event.allowBrowserDefault();
        return false;
      }
    }
  }

  public boolean handleRightAtEnd(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleRightAtEnd(element, event)) {
      return true;
    } else {
      // Symmetric logic to handleLeftAtBeginning
      if (handleRight(event.getCaret().setToAfter(node).getContainer(), event)) {
        return true;
      } else {
        event.allowBrowserDefault();
        return false;
      }
    }
  }

  public boolean handleRightBeforeNode(ContentNode node, EditorEvent event) {
    ContentElement element = node.asElement();
    if (element != null && nodeEventHandler(element).handleRightBeforeNode(element, event)) {
      return true;
    } else {
      event.allowBrowserDefault();
      return false;
    }
  }

  private static boolean handleTextNodeDeleteAction(ContentTextNode node,
      EditorEvent event, boolean isBackSpace) {
    int implDataLength = -1;
    try {
      // Flush if the impl data is ahead of us
      // TODO(danilatos): Is this an expensive check??? Without it, you can
      // definitely trigger errors if you backspace too quickly.
      implDataLength = node.getImplDataLength();
      if (implDataLength <= 1) {
        node.getTypingExtractor().flush();
      }
    } catch (HtmlMissing e1) {
      node.getRepairer().handleMissing(e1);
      return true;
    }

    // In case we got detached by the result of the flush
    if (!node.isContentAttached()) {
      return true;
    }

    if (node.getLength() <= 1) {
      // In Safari at least, but for now just for safety do it always:
      // Sometimes the browser will decide it's a good idea to obliterate
      // a whole bunch of inline elements if you delete the last remaining
      // character from the text they contain. This is basically catastrophic.
      // So if this is the last remaining character, we just handle it
      // manually ourselves. This will result in a slightly degraded user
      // experience with IMEs in very contrived circumstances.
      // TODO(danilatos): Solution?

      node.getMutableDoc().deleteRange(
          Point.<ContentNode>inText(node, 0), Point.<ContentNode>inText(node, 1));

    } else {
      assert implDataLength != -1;

      if (!UserAgent.isFirefox()) {
        Point<ContentNode> caret = event.getCaret().asPoint();
        Point<ContentNode> a, b;
        if (isBackSpace) {
          a = Point.inText(caret.getContainer(), caret.getTextOffset() - 1);
          b = caret;
        } else {
          a = caret;
          b = Point.inText(caret.getContainer(), caret.getTextOffset() + 1);
        }
        node.getMutableDoc().deleteRange(a, b);
      } else {
        try {
          ContentPoint caret = event.getCaret();
          // Adjust caret if the offset extends beyond the length of impl text node.
          if (caret.isInTextNode() && caret.getTextOffset() > implDataLength) {
            caret.setTextOffset(implDataLength);
          }

          Point<Node> htmlCaret = node.getExtendedContext().rendering().getNodeManager()
              .wrapperPointToNodeletPoint(caret.asPoint());

          if (htmlCaret != null) {
            node.getTypingExtractor().somethingHappened(
                htmlCaret);

            // allow default
            return false;
          } else {
            ContentNode.logger.error().logPlainText(
                "Null html caret in ContentTextNode, content caret: " + caret);
            // cancel default for safety
            return true;
          }

        } catch (HtmlMissing e) {
          node.getRepairer().handleMissing(e);
        } catch (HtmlInserted e) {
          node.getRepairer().handleInserted(e);
        }
      }
    }

    return true;
  }

  static void maybeDelete(ContentElement e) {
    if (e.isPersistent()) {
      e.getMutableDoc().deleteNode(e);
    }
  }
}
