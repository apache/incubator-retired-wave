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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.EditorRuntimeException;
import org.waveprotocol.wave.client.editor.content.SelectionMaintainer.TextNodeChangeType;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException;
import org.waveprotocol.wave.client.editor.extract.InconsistencyException.HtmlMissing;
import org.waveprotocol.wave.client.editor.extract.Repairer;
import org.waveprotocol.wave.client.editor.impl.HtmlView;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.indexed.NodeType;
import org.waveprotocol.wave.model.document.raw.RawDocument;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Content text node
 *
 * Wrapper that tracks multiple implementation text nodelets.
 * This is because we can't trust the browser not to unpredictably split
 * our text nodelets, so we simply allow & expect it.
 * TODO(danilatos): Thorough details of how this stuff works. For now,
 * NodeManager has some more information.
 *
 * See {@link ContentDocument} for more...
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author lars@google.com (Lars Rasmussen)
 */
public class ContentTextNode extends ContentNode implements Doc.T {

  private String data = "";

  public ContentTextNode(String data, ExtendedClientDocumentContext bundle) {
    this(data, null, bundle);
  }

  /**
   * Constructor should only be used for testing
   *
   * @param wrapped
   * @param bundle
   */
  public ContentTextNode(Text wrapped, ExtendedClientDocumentContext bundle) {
    this(wrapped.getData(), wrapped, bundle);
  }

  protected ContentTextNode(String data, Text wrapped, ExtendedClientDocumentContext bundle) {
    super(wrapped, bundle);
    this.data = data;
  }

  @Override
  public ContentElement asElement() {
    return null;
  }

  @Override
  public ContentTextNode asText() {
    return this;
  }

  /** {@inheritDoc} */
  @Override
  public Text getImplNodelet() {
    return super.getImplNodelet().cast();
  }

  /**
   * Prefer this type safety (only allow Text nodelet)
   */
  public void setTextNodelet(Text nodelet) {
    setImplNodelet(nodelet);
  }

  void setRendering(boolean isRendering) {
    if ((getImplNodelet() != null) == isRendering) {
      return;
    }

    if (isRendering) {
      setImplNodelet(Document.get().createTextNode(data));
    } else {
      setImplNodelet(null);
    }
  }

  /**
   * @return The wrapper text node's character data
   */
  public String getData() {
    return data;
  }

  private void setContentData(String newData) {
    if (!newData.equals(this.data)) {
      this.data = newData;
      ContentElement parent = getParentElement();
      if (parent != null) {
        parent.notifyChildrenMutated();
      }
    }
  }

  /**
   * @see RawDocument#insertData(Object, int, String)
   */
  void insertData(int offset, String arg, boolean affectImpl) {
    String data = getData();
    setContentData(
        data.substring(0, offset) +
        arg +
        data.substring(offset, data.length()));

    if (affectImpl) {
      // NOTE(user): There is an issue here. When findNodeletWihOffset causes a
      // repair, the data may get inserted twice. The repairer may set the DOM
      // node to reflect the updated content data (which already has the data
      // inseretd). Then, when insertData is called, the data is inserted again.
      findNodeletWithOffset(offset, nodeletOffsetOutput, getRepairer());
      Text nodelet = nodeletOffsetOutput.getNode().<Text>cast();
      int nodeletOffset = nodeletOffsetOutput.getOffset();
      nodelet.insertData(nodeletOffset, arg);
      getExtendedContext().editing().textNodeletAffected(
          nodelet, nodeletOffset, arg.length(), TextNodeChangeType.DATA);
    }
  }

  /**
   * @see RawDocument#deleteData(Object, int, int)
   */
  void deleteData(int offset, int count, boolean affectImpl) {
    String data = getData();

    setContentData(
        data.substring(0, offset) +
        data.substring(offset + count, data.length()));

    if (affectImpl) {
      if (isImplAttached()) {
        findNodeletWithOffset(offset, nodeletOffsetOutput, getRepairer());
        Text nodelet = nodeletOffsetOutput.getNode().cast();
        int subOffset = nodeletOffsetOutput.getOffset();

        if (nodelet.getLength() - subOffset >= count) {
          // Handle the special case where the delete is in a single text nodelet
          // carefully, to avoid splitting it
          nodelet.deleteData(subOffset, count);
          getExtendedContext().editing().textNodeletAffected(
              nodelet, subOffset, -count, TextNodeChangeType.DATA);
        } else {
          // General case
          Node toExcl = implSplitText(offset + count);
          Node fromIncl = implSplitText(offset);

          HtmlView filteredHtml = getFilteredHtmlView();
          for (Node node = fromIncl; node != toExcl && node != null;) {
            Node next = filteredHtml.getNextSibling(node);
            node.removeFromParent();
            node = next;
          }
        }
      } else {
        // TODO(user): have these assertion failure fixed (b/2129931)
        // assert getImplNodelet().getLength() == getLength() :
        //    "text node's html impl not normalised while not attached to html dom";
        getImplNodelet().deleteData(offset, count);
      }
    }
  }

  /**
   * Splits this text node at the given offset.
   * If the offset is zero, no split occurs, and the current node is returned.
   * If the offset is equal to or greater than the length of the text node, no split
   * occurs, and null is returned.
   *
   * @see RawDocument#splitText(Object, int)
   */
  ContentTextNode splitText(int offset, boolean affectImpl) {
    if (offset == 0) {
      return this;
    } else if (offset >= getLength()) {
      return null;
    }

    Text nodelet = null;

    if (affectImpl) {
      nodelet = implSplitText(offset);
    } else {
      nodelet = Document.get().createTextNode("");
    }

    String first = getData().substring(0, offset);
    String second = getData().substring(offset);

    ContentTextNode sibling = new ContentTextNode(second, nodelet, getExtendedContext());
    setContentData(first);

    // Always false for affecting the impl, as it's already been done
    getParentElement().insertBefore(sibling, getNextSibling(), false);

    return sibling;
  }

  /**
   * Splits and returns the second.
   * If split point at a node boundary, doesn't split, but returns the next nodelet.
   */
  private Text implSplitText(int offset) {
    findNodeletWithOffset(offset, nodeletOffsetOutput, getRepairer());
    Text text = nodeletOffsetOutput.getNode().<Text>cast();
    if (text.getLength() == nodeletOffsetOutput.getOffset()) {
      return text.getNextSibling().cast();
    } else if (nodeletOffsetOutput.getOffset() == 0) {
      return text;
    } else {
      int nodeletOffset = nodeletOffsetOutput.getOffset();
      Text ret = text.splitText(nodeletOffset);
      // -10000 because the number should be ignored in the splitText case,
      // so some large number to trigger an error if it is not ignored.
      getExtendedContext().editing().textNodeletAffected(
          text, nodeletOffset, -10000, TextNodeChangeType.SPLIT);
      return ret;
    }
  }

  /**
   * @return Length of the character data
   */
  public int getLength() {
    return getData().length();
  }

  /**
   * @return the calculated character data in the html
   * @throws HtmlMissing
   */
  public String getImplData() throws HtmlMissing {
    Node next = checkNodeAndNeighbourReturnImpl(this);
    HtmlView filteredHtml = getFilteredHtmlView();

    return sumTextNodes(getImplNodelet(), next, filteredHtml);
  }

  @Override
  public void onAddedToParent(ContentElement previousParent) {
    if (!isImplAttached()) {
      simpleNormaliseImpl();
    }
  }

  /**
   * @return length of character data in the html
   * @throws HtmlMissing
   */
  public int getImplDataLength() throws HtmlMissing {
    Node next = checkNodeAndNeighbourReturnImpl(this);
    HtmlView filteredHtml = getFilteredHtmlView();

    return sumTextNodesLength(getImplNodelet(), next, filteredHtml);
  }

  /** {@inheritDoc} */
  @Override
  public void revertImplementation() {
    setImplNodelet(Document.get().createTextNode(getData()));
  }

  // TODO(danilatos): A lot of these methods have roughly the same 4-5 lines,
  // iterating over the nodelets and doing something with them. There is a
  // lot of repeated logic, but I can't see an easy way to factor it out
  // without resorting to callbacks. Try using callbacks and see if GWT
  // optimises it out.

  /**
   * Compacts the multiple impl text nodelets into one
   * @throws HtmlMissing
   */
  public void normaliseImplThrow() throws HtmlMissing {
    // TODO(danilatos): Some code in line container depends on the isImplAttached() check,
    // but sometimes it might not be attached but should, and so should throw an exception.
    if (!isContentAttached() || !isImplAttached()) {
      simpleNormaliseImpl();
    }

    Text first = getImplNodelet();
    if (first.getLength() == getLength()) {
      return;
    }

    ContentNode next = checkNodeAndNeighbour(this);
    HtmlView filteredHtml = getFilteredHtmlView();

    //String sum = "";
    Node nextImpl = (next == null) ? null : next.getImplNodelet();
    for (Text nodelet = first; nodelet != nextImpl && nodelet != null;
        nodelet = filteredHtml.getNextSibling(first).cast()) {
      //sum += nodelet.getData();
      if (nodelet != first) {
        getExtendedContext().editing().textNodeletAffected(
            nodelet, -1000, -1000, TextNodeChangeType.REMOVE);
        nodelet.removeFromParent();
      }
    }

    getExtendedContext().editing().textNodeletAffected(
        first, -1000, -1000, TextNodeChangeType.REPLACE_DATA);
    first.setData(getData());
  }

  void simpleNormaliseImpl() {
    if (getImplNodelet() == null || getImplNodelet().getLength() != getLength()) {
      setImplNodelet(Document.get().createTextNode(getData()));
    }
    return;
  }

  /**
   * Same as {@link #normaliseImplThrow()}, but uses a repairer to fix problems
   * rather than throw an exception
   */
  @Override
  public Text normaliseImpl() {
    Repairer repairer = getRepairer();
    for (int i = 0; i < MAX_REPAIR_ATTEMPTS; i++) {
      try {
        normaliseImplThrow();
        return getImplNodelet();
      } catch (HtmlMissing e) {
        repairer.handle(e);
      } catch (RuntimeException e) {
        // Safe to catch runtime exception - no stateful code should be affected,
        // just browser DOM has been munged which we repair
        repairer.revert(Point.before(getRenderedContentView(), this), null);
      }
    }

    Text nodelet = getImplNodelet();
    getExtendedContext().editing().textNodeletAffected(
        nodelet, -1000, -1000, TextNodeChangeType.REPLACE_DATA);
    nodelet.setData(getData());
    return nodelet;
  }

  /**
   * Helper function to concatenate the character data of a group of
   * adjacent text nodes.
   * Important: Does not do any consistency checking. It assumes this has
   * already been done.
   *
   * @param fromIncl Start from this node, inclusive
   * @param toExcl Go until this node, exclusive
   * @param filteredHtml Html view to use
   * @return the summed impl data
   */
  public static String sumTextNodes(Text fromIncl, Node toExcl, HtmlView filteredHtml) {
    // TODO(danilatos): This could potentially be slow if there are many nodelets. In
    // practice this shouldn't be an issue, as we should be normalising them when
    // they get too many.
    String data  = "";
    // TODO(danilatos): Some assumptions about validity here. Could they fail?
    for (Text n = fromIncl; n != toExcl && n != null;
        n = filteredHtml.getNextSibling(n).cast()) {
      data += n.getData();
    }

    return getNodeValueFromHtmlString(data);
  }

  private static int sumTextNodesLength(Text fromIncl, Node toExcl, HtmlView filteredHtml) {
    int length = 0;
    for (Text n = fromIncl; n != toExcl && n != null;
        n = filteredHtml.getNextSibling(n).cast()) {
      length += n.getLength();
    }

    return length;
  }
  /**
   * Check whether the given text nodelet is one of the nodelets owned by this
   * wrapper.
   * @param textNodelet
   * @return true if textNodelet is owned by this wrapper
   * @throws HtmlMissing
   */
  public boolean owns(Text textNodelet) throws HtmlMissing {
    ContentNode next = checkNodeAndNeighbour(this);
    HtmlView filteredHtml = getFilteredHtmlView();

    Node nextImpl = (next == null) ? null : next.getImplNodelet();
    for (Text nodelet = getImplNodelet(); nodelet != nextImpl;
        nodelet = filteredHtml.getNextSibling(nodelet).cast()) {
      if (nodelet == textNodelet) {
        return true;
      }
    }

    return false;
  }

  /**
   * Finds the character offset of the given text node. For example, if this
   * wrapper were tracking 3 nodelets with data "abc", "de", "fghi", and we
   * asked for the offset of the third, we'd get back 5.
   *
   * @param textNodelet
   * @return The character offset of the given node
   * @throws HtmlMissing If the nodelet isn't owned by this wrapper
   */
  public int getOffset(Text textNodelet) throws HtmlMissing {
    try {
      return getOffset(textNodelet, checkNodeAndNeighbourReturnImpl(this));
    } catch (Exception t) {
      // is this a missing or inserted error?
      throw new HtmlMissing(this, getRenderedContentView().getParentElement(this).getImplNodelet());
    }
  }

  /**
   * Same as {@link #getOffset(Text)}, but we also provide the nodelet where
   * we should stop looking. This method assumes the correctness of its input
   * and the document state, and doesn't do any inconsistency checking.
   *
   * NOTE(danilatos): It is possible to provide an "incorrect" nextImpl
   * that is much further down. This is OK as long as you know what you are doing.
   *
   * @param nextImpl The first impl nodelet of the next wrapper.
   */
  public int getOffset(Text textNodelet, Node nextImpl) {
    return getOffset(textNodelet, getImplNodelet(), nextImpl, getFilteredHtmlView());
  }

  /**
   * Implementation of {@link #getOffset(Text, Node)} that is not bound to any
   * specific ContentTextNode
   */
  public static int getOffset(Text textNodelet, Text startNode, Node nextImpl, HtmlView view) {
    int offset = 0;
    for (Text nodelet = startNode; nodelet != nextImpl;
        nodelet = view.getNextSibling(nodelet).cast()) {
      if (nodelet == textNodelet) {
        return offset;
      }
      offset += nodelet.getLength();
    }

    // Programming error, this method assumes the input state was valid,
    // unlike other methods which don't
    throw new EditorRuntimeException("Didn't find text nodelet to get offset for");
  }

  /**
   * Useful to use as the output from {@link #findNodeletWithOffset(int, HtmlPoint)}
   * and friends. Instead of everyone creating their own singleton, this is a handy
   * common one to use.
   */
  public static final HtmlPoint nodeletOffsetOutput = new HtmlPoint(null, 0);

  /**
   * Given an offset, finds the nodelet that corresponds to that offset, and
   * the remaining offset within that nodelet.
   * @param offset
   * @param output The nodelet and offset result are placed in this parameter
   * @throws HtmlMissing
   */
  public void findNodeletWithOffset(int offset, HtmlPoint output) throws HtmlMissing {
    findNodeletWithOffset(offset, output, checkNodeAndNeighbourReturnImpl(this));
  }

  /**
   * Same as {@link #findNodeletWithOffset(int, HtmlPoint)}, but provide a repairer
   * to handle the checked exception, instead of this method throwing it.
   */
  public void findNodeletWithOffset(int offset, HtmlPoint output, Repairer repairer) {
    // HACK(danilatos): Nicer way?
    for (int tries = 0; tries < 3; tries++) {
      try {
        findNodeletWithOffset(offset, output);
        return;
      } catch (HtmlMissing e) {
        getRepairer().handle(e);
      }
    }
    throw new EditorRuntimeException("Tried to repair and it just wouldn't work");
  }

  /**
   * Same as {@link #findNodeletWithOffset(int, HtmlPoint)}, but does not do
   * a consistency check. Instead it accepts the impl nodelet of the next
   * wrapper (or null) and assumes everthing is consistent.
   */
  public void findNodeletWithOffset(int offset, HtmlPoint output, Node nextImpl) {
    HtmlView filteredHtml = getFilteredHtmlView();

    int sum = 0;
    int prevSum;
    for (Text nodelet = getImplNodelet(); nodelet != nextImpl;
        nodelet = filteredHtml.getNextSibling(nodelet).cast()) {
      prevSum = sum;
      sum += nodelet.getLength();
      if (sum >= offset) {
        output.setNode(nodelet);
        output.setOffset(offset - prevSum);
        return;
      }
    }

    output.setNode(null);
  }

  /**
   * Same as {@link #checkNodeAndNeighbour(ContentTextNode)}, but returns the
   * impl nodelet of the wrapper found.
   */
  private static Node checkNodeAndNeighbourReturnImpl(ContentTextNode node) throws HtmlMissing {
    ContentNode next = checkNodeAndNeighbour(node);
    return next == null ? null : next.getImplNodelet();
  }

  /**
   * Return the given node's next sibling, after doing consistency checks to ensure both are
   * consistent.
   */
  private static ContentNode checkNodeAndNeighbour(ContentTextNode node) throws HtmlMissing {

    // TODO(danilatos): How do we fare without these checks?
    // Worth the performance hit or not?

    ContentView renderedContent = node.getRenderedContentView();

    if (!node.isImplAttached()) {
      Element parentElement = renderedContent.getParentElement(node).getImplNodelet();
      throw new InconsistencyException.HtmlMissing(node, parentElement);
    }

    ContentNode next = renderedContent.getNextSibling(node);
    if (next != null && !next.isImplAttached()) {
      Element parentElement = renderedContent.getParentElement(node).getImplNodelet();
      throw new InconsistencyException.HtmlMissing(next, parentElement);
    }

    return next;
  }


  /** {@inheritDoc} */
  @Override
  public short getNodeType() {
    return NodeType.TEXT_NODE;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isElement() {
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isTextNode() {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isConsistent() {
    try {
      return isImplAttached() && getImplData().equals(getData());
    } catch (HtmlMissing e) {
      return false;
    } catch (Throwable t) {
      return false;
    }
  }

  /**
   * TODO(user): for now translate all &nbsp; chars to regular spaces
   * such that only regular spaces go on the wire. Consider doing
   * the translation closer to the wire to avoid the danger of accidentally
   * using a translated string internally, e.g., in split or join.
   *
   * @param value a node value string from an html text node
   * @return the content text node's interpretations of value
   */
  public static String getNodeValueFromHtmlString(String value) {
    return value.replace('\u00a0', ' ');
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void debugAssertHealthy() {

    // Assert ContentTextnode has a text nodelet
    assert DomHelper.isTextNode(getImplNodelet()) :
        "ContentTextNode's implNodelet should be a text node";

    // Assert it has no children
    assert null == getFirstChild() : "ContentTextNode should be childless";

    super.debugAssertHealthy();
  }
}
