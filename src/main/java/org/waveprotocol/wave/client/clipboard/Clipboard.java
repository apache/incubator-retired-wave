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

package org.waveprotocol.wave.client.clipboard;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.PointRange;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * A html clipboard that supports wave metadata: wave xml and annotations
 *
 * TODO(user): Add high level methods for extracting semantic content from
 * clipboard.
 *
 */
public class Clipboard {
  private static final String MAGIC_CLASSNAME = "__wave_paste";
  private static final String WAVE_XML_ATTRIBUTE = "data-wave-xml";
  private static final String WAVE_ANNOTATIONS_ATTRIBUTE = "data-wave-annotations";

  public static final LoggerBundle LOG = new DomLogger("clipboard");

  /**
   * Singleton instance of clipboard, there should only be 1 per client.
   */
  private static final Clipboard INSTANCE = new Clipboard(PasteBufferImpl.create());

  /**
   * Underlying pasteBuffer.
   */
  private final PasteBufferImpl pasteBuffer;

  public static final Clipboard get() {
    return INSTANCE;
  }

  private Clipboard(PasteBufferImpl pasteBuffer) {
    this.pasteBuffer = pasteBuffer;
  }

  /**
   * Fill the PasteBuffer with the given content.
   *
   * This fills the PasteBuffer and sets the selection over the corresponding
   * html on the buffer. To fill the clipboard, this needs to be called inside a
   * copy event handler, or follow this with an execCommand(copy)
   *
   * @param htmlFragment
   * @param selection
   * @param waveXml
   * @param normalizedAnnotation
   */
  public void fillBufferAndSetSelection(Node htmlFragment, PointRange<Node> selection,
      XmlStringBuilder waveXml, Iterable<RangedAnnotation<String>> normalizedAnnotation,
      boolean restoreSelection) {
    final FocusedPointRange<Node> oldSelection = restoreSelection ? NativeSelectionUtil.get() : null;

    // Clear this node and append the cloned fragment.
    pasteBuffer.setContent(htmlFragment);

    assert htmlFragment.isOrHasChild(selection.getFirst().getContainer()) :
      "first not attached before hijack";
    assert htmlFragment.isOrHasChild(selection.getSecond().getContainer()) :
      "second not attached before hijack";

    if (waveXml != null && normalizedAnnotation != null) {
      selection =
          hijackFragment(waveXml.toString(), AnnotationSerializer
              .serializeAnnotation(normalizedAnnotation), selection);
    }
    assert htmlFragment.isOrHasChild(selection.getFirst().getContainer()) :
      "first not attached after hijack";
    assert htmlFragment.isOrHasChild(selection.getSecond().getContainer()) :
      "second not attached after hijack";
    NativeSelectionUtil.set(selection.getFirst(), selection.getSecond());

    if (restoreSelection && oldSelection != null) {
      DeferredCommand.addCommand(new Command() {
        @Override
        public void execute() {
          NativeSelectionUtil.set(oldSelection);
        }
      });
    }
  }

  /**
   * {@link #fillBufferAndSetSelection(Node, PointRange, XmlStringBuilder, Iterable, boolean)
   * same as above, but with the entire fragment. }
   *
   * @param htmlFragment
   * @param waveXml
   * @param normalizedAnnotation
   */
  public void fillBufferAndSetSelection(Node htmlFragment, XmlStringBuilder waveXml,
      Iterable<RangedAnnotation<String>> normalizedAnnotation, boolean restoreSelection) {
    Element parent = Document.get().createDivElement();
    parent.appendChild(htmlFragment);
    PointRange<Node> selection =
        new PointRange<Node>(Point.inElement(parent, htmlFragment), Point.<Node> end(parent));
    fillBufferAndSetSelection(parent, selection, waveXml, normalizedAnnotation, restoreSelection);
  }

  /**
   * Extract waveXml from this container.
   *
   * @param srcContainer
   */
  public String maybeGetWaveXml(Element srcContainer) {
    String waveXml = maybeGetAttributeFromContainer(srcContainer, WAVE_XML_ATTRIBUTE);
    String x = srcContainer.getInnerHTML();
    if (waveXml != null) {
      LOG.trace().logPlainText("found serialized waveXml: " + waveXml);

      // NOTE(user): Ensure waveXml does not contain any new line characters.
      // FF36+ adds random new lines the attributes
      // https://bugzilla.mozilla.org/show_bug.cgi?id=540979
      // We filter on all browsers, so they can accept content from FF36.
      waveXml = waveXml.replace("\n", "");
    }

    return waveXml;
  }

  /**
   * Extract serialized annotations from this container.
   *
   * @param srcContainer
   */
  public String maybeGetAnnotations(Element srcContainer) {
    String annotations = maybeGetAttributeFromContainer(srcContainer, WAVE_ANNOTATIONS_ATTRIBUTE);
    if (annotations != null) {
      LOG.trace().log("found serialized annotations: " + annotations);
    }
    return annotations;
  }

  private String maybeGetAttributeFromContainer(Element srcContainer, String attribName) {
    NodeList<Element> elementsByClassName =
        DomHelper.getElementsByClassName(srcContainer, MAGIC_CLASSNAME);
    if (elementsByClassName != null && elementsByClassName.getLength() > 0) {
      return elementsByClassName.getItem(0).getAttribute(attribName);
    }
    return null;
  }

  /**
   * Return the underlying paste buffer.
   *
   * NOTE(user): Paste buffer functionality should be abstracted into this
   * class.
   */
  public PasteBufferImpl getPasteBuffer() {
    return pasteBuffer;
  }

  /**
   * Hijacks the paste fragment by hiding a span with metadata at the end of the
   * fragment.
   *
   * @param xmlInRange The xml string to pass into the magic span element
   * @param annotations The annotation string to pass into the magic span
   *        element
   * @param origRange the current range. The span element will be inserted
   *        before the start
   *
   * @return The new adjusted selection. The end will be adjusted such that it
   *         encloses the original selection and the span with metadata
   */
  private PointRange<Node> hijackFragment(String xmlInRange, String annotations,
      PointRange<Node> origRange) {
    Point<Node> origStart = origRange.getFirst();
    Point<Node> origEnd = origRange.getSecond();
    SpanElement spanForXml = Document.get().createSpanElement();
    spanForXml.setAttribute(WAVE_XML_ATTRIBUTE, xmlInRange);
    spanForXml.setAttribute(WAVE_ANNOTATIONS_ATTRIBUTE, annotations);
    spanForXml.setClassName(MAGIC_CLASSNAME);

    LOG.trace().log("original point: " + origStart);

    // NOTE(user): An extra span is required at the end for Safari, otherwise
    // the span with the metadata may get discarded.
    SpanElement trailingSpan = Document.get().createSpanElement();
    trailingSpan.setInnerHTML("&nbsp;");

    if (origEnd.isInTextNode()) {
      Text t = (Text) origEnd.getContainer();
      t.setData(t.getData().substring(0, origEnd.getTextOffset()));
      origEnd.getContainer().getParentElement().insertAfter(spanForXml, t);
      origEnd.getContainer().getParentElement().insertAfter(trailingSpan, spanForXml);
    } else {
      origEnd.getContainer().insertAfter(spanForXml, origEnd.getNodeAfter());
      origEnd.getContainer().insertAfter(trailingSpan, spanForXml);
    }


    Point<Node> newEnd =
      Point.<Node> inElement(spanForXml.getParentElement(), trailingSpan.getNextSibling());
    LOG.trace().log("new point: " + newEnd);
    LOG.trace().logPlainText("parent: " + spanForXml.getParentElement().getInnerHTML());
    assert newEnd.getNodeAfter() == null
        || newEnd.getNodeAfter().getParentElement() == newEnd.getContainer() : "inconsistent point";
    return new PointRange<Node>(origStart, newEnd);
  }
}
