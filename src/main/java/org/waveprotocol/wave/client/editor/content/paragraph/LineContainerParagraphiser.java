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

package org.waveprotocol.wave.client.editor.content.paragraph;

import static org.waveprotocol.wave.client.editor.content.paragraph.LineRendering.isLineElement;
import static org.waveprotocol.wave.model.document.util.LineContainers.PARAGRAPH_FULL_TAGNAME;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.NodeMutationHandlerImpl;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentDocument.PermanentMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.model.document.util.DocumentContext;

/**
 * Maps line nodes in a line container to local paragraph nodes within the same
 * document.
 *
 * Wraps all the nodes corresponding to a line in a local paragraph,
 * synchronously in response to mutation events.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class LineContainerParagraphiser extends NodeMutationHandlerImpl<ContentNode, ContentElement>
   implements PermanentMutationHandler {

  public class LineHandler extends RenderingMutationHandler
      implements PermanentMutationHandler {

    @Override
    public Element createDomImpl(Renderable element) {
      return null;
    }

    @Override
    public void onActivationStart(ContentElement element) {
      fanoutAttrs(element);
    }

    @Override
    public void onAttributeModified(ContentElement element, String name, String oldValue,
        String newValue) {
      if (Line.fromLineElement(element) == null) {
        // not yet initialised
        return;
      }
      element.getContext().annotatableContent().transparentSetAttribute(
          Line.fromLineElement(element).getParagraph(), name, newValue);
    }

    @Override
    public void onAddedToParent(ContentElement element, ContentElement oldParent) {
      // Get the parent in the persistent view
      handleNewChild(element.getMutableDoc().getParentElement(element), element);
    }
  }

  private final LineHandler lineHandler = new LineHandler();

  public static boolean USE_STRONG_CHECK = false;

  public LineHandler getLineHandler() {
    return lineHandler;
  }

  @Override
  public void onChildAdded(ContentElement container, ContentNode child) {
    if (!isLineElement(child)) {
      handleNewChild(container, child);
    }
  }

  boolean nested = false;

  /**
   * Similar to onChildAdded, but container may not be the immediate parent of
   * child in the full document view (a paragraph might be in the way)
   *
   * @param container
   * @param child
   */
  public void handleNewChild(ContentElement container, ContentNode child) {
    if (nested) {
      return;
    }

    nested = true;
    try {

      DocumentContext<ContentNode, ContentElement, ContentTextNode> cxt =
        container.getContext();

      ContentElement element = child.asElement();
      if (element != null && isLineElement(element)) {
        Line line = new Line(cxt, element);
        ContentNode previousDirectSibling = child.getPreviousSibling();
        ContentNode previousPersistentSibling = container.getMutableDoc().getPreviousSibling(child);
        if (previousPersistentSibling == null) {
          insertAsFirstLine(container, line);
        } else {
          Line previousLine;
          if (element.getParentElement() == container) {
            // TODO(danilatos): Handle the case where this assertion fails.
            assert previousDirectSibling != null &&
                LineRendering.isLocalParagraph(previousDirectSibling);
            previousLine = Line.fromParagraph(previousDirectSibling.asElement());
          } else {
            previousLine = Line.fromParagraph(element.getParentElement());
          }

          // TODO(danilatos): Handle the case where this assertion fails too.
          assert previousLine != null;

          insertAfterLine(previousLine, line);
        }

        Line nextLine = line.next();
        ContentElement paragraph = line.getParagraph();

        if (element.getParentElement() != container) {
          cxt.annotatableContent().transparentMove(paragraph, paragraph.getNextSibling(),
              null, null);
          cxt.annotatableContent().transparentMove(container, paragraph, null,
              element.getParentElement().getNextSibling());
          cxt.annotatableContent().transparentMove(container, element, null,
              paragraph);
        }

      } else {
        ContentNode before = child.getPreviousSibling();
        ContentElement paragraph = before == null ? null : asParagraph(before);
        if (paragraph != null) {
          cxt.annotatableContent().transparentMove(paragraph, child, child.getNextSibling(), null);
        }
      }

    } finally {
      nested = false;
    }
  }

  @Override
  public void onChildRemoved(ContentElement element, ContentNode child) {
    handleRemovedChild(element, child);
  }

  public void handleRemovedChild(ContentElement container, ContentNode child) {
    if (nested) {
      return;
    }

    if (child.isContentAttached()) {
      EditorStaticDeps.logger.error().log("Node was moved and not removed?");
      return;
    }

    nested = true;
    try {
      DocumentContext<ContentNode, ContentElement, ContentTextNode> cxt =
        container.getContext();


      ContentElement element = child.asElement();
      if (element != null && isLineElement(element)) {
        Line line = Line.fromLineElement(element);
        assert line != null;

        // move only if there's something to move:
        if (line.getParagraph().getFirstChild() != null) {
          Line previousLine = line.previous();
          if (previousLine != null) {
            cxt.annotatableContent().transparentMove(previousLine.getParagraph(),
                line.getParagraph().getFirstChild(), null, null);
          } else {
            cxt.annotatableContent().transparentUnwrap(line.getParagraph());
          }
        }

        line.remove();
      } else {
        // do nothing
      }

    } finally {
      nested = false;
    }
  }

  static ContentElement asParagraph(ContentNode node) {
    ContentElement e = node.asElement();
    return e != null && e.getTagName().equals(PARAGRAPH_FULL_TAGNAME) ? e : null;
  }

  void insertAsFirstLine(ContentElement container, Line line) {
    Line previousFirst = Line.getFirstLineOfContainer(container);
    if (previousFirst != null) {
      line.insertBefore(previousFirst);
    }
    Line.setFirstLineOfContainer(container, line);
  }

  void insertAfterLine(Line previousLine, Line line) {
    line.insertAfter(previousLine);
  }

  private static void errorLogAndThrow(String message) {
    EditorStaticDeps.logger.error().logPlainText(message);
    assert false : message;
  }

  /**
   * Aggressively checks for a healthy state.
   *
   * Note that this method can fail if the right op is applied, even though the state is
   * still consistent - so this method should be used to test that the event handling code didn't
   * create any incorrect ops as well.
   *
   * TODO(danilatos): A weaker check, that checks consistency regardless of ops applied, and
   * robustness against malicious ops.
   *
   * @param container
   * @return true if healthy, false otherwise
   */
  public static boolean containerIsHealthyStrong(ContentElement container) {
    ContentNode firstChild = container.getFirstChild();
    Line line = Line.getFirstLineOfContainer(container);
    if (line == null) {
      errorLogAndThrow("Empty container - must have at least one child");
      if (firstChild != null) {
        return false;
      } else {
        return true;
      }
    }
    if (line.previous() != null) {
      errorLogAndThrow("First line must have no previous sibling");
      return false;
    }
    if (!isLineElement(firstChild)) {
      errorLogAndThrow("First child not a line element");
      return false;
    }

    ContentElement element = firstChild.asElement();
    boolean first = true;

    while (true) {
      if (line.getParagraph().getPreviousSibling() != line.getLineElement()) {
        errorLogAndThrow("Junk between line token and its paragraph");
        return false;
      }

      ContentNode node;
      for (node = line.getParagraph().getFirstChild(); node != null; node = node.getNextSibling()) {

        ContentElement e = node.asElement();
        if (e != null) {
          if (isLineElement(e)) {
            errorLogAndThrow("Line element stuck inside rendering paragraph: " + e);
            return false;
          }
        }
      }

      node = line.getParagraph().getNextSibling();
      if (node == null) {
        if (line.next() != null) {
          errorLogAndThrow("Supposed to have another line, but no more nodes");
          return false;
        } else {
          return true;
        }
      }

      if (line.getParagraph().getImplNodelet() != null) {
        // Only check this when rendered.
        if (line.getParagraph().getImplNodelet().getNextSibling() !=
            line.next().getParagraph().getImplNodelet()) {
          errorLogAndThrow("Junk in html between paragraph nodelets");
          return false;
        }
      }

      if (!isLineElement(node)) {
        errorLogAndThrow("Junk after rendering paragraph "
            + line.getParagraph() + ", junk: " + node);
        return false;
      }

      element = node.asElement();
      Line nextLine = Line.fromLineElement(element);
      if (line.next() != nextLine) {
        errorLogAndThrow("Next line doesn't correspond to next line element");
        return false;
      }

      if (nextLine.previous() != line) {
        errorLogAndThrow("Line linked list corruption: nextLine.previous != line");
        return false;
      }

      line = nextLine;
      first = false;
    }
  }

  static Node endingNodelet(Element paragraph) {
    return ParagraphHelper.INSTANCE.getEndingNodelet(paragraph);
  }
}
