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

import static org.waveprotocol.wave.client.editor.content.paragraph.constants.ParagraphRenderingConstants.INDENT_UNIT_SIZE_PX;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.FontWeight;
import com.google.gwt.dom.client.Style.Unit;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.HasImplNodelets;
import org.waveprotocol.wave.client.editor.content.Renderer;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.Alignment;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.Direction;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class DefaultParagraphHtmlRenderer implements ParagraphHtmlRenderer {
  // TODO(danilatos): Use CssResource
  private static final String NUMBERED_CLASSNAME = "numbered";

  private static final String[] BULLET_CLASSNAMES = new String[] {
    "bullet-type-0",
    "bullet-type-1",
    "bullet-type-2"
  };

  private static String bulletClassName(int indent) {
    return BULLET_CLASSNAMES[indent % 3];
  }

  /**
   * Size of the largest heading
   */
  public static final double MAX_HEADING_SIZE_EM = 1.75;

  /**
   * Size of the smallest heading
   */
  public static final double MIN_HEADING_SIZE_EM = 1.0;

  /**
   * Tag name to use for html implementation for paragraph element mode.
   */
  public static final String PARAGRAPH_IMPL_TAGNAME = "div";

  /**
   * Tag name to use for html implementation for list element mode.
   */
  public static final String LIST_IMPL_TAGNAME = "li";

  private final String implTagName;

  public DefaultParagraphHtmlRenderer() {
    this(PARAGRAPH_IMPL_TAGNAME);
  }

  public DefaultParagraphHtmlRenderer(String implTagName) {
    this.implTagName = implTagName;
  }

  /**
   * Unless very specific behaviour is wanted, it is better to override
   * {@link #createNodelet(Renderable)} if a different nodelet from a simple
   * named element is desired.
   */
  @Override
  public Element createDomImpl(Renderable element) {
    Element nodelet = createNodelet(element);

    assert nodelet.getFirstChild() == null : "was not given an empty nodelet";
    ParagraphHelper.INSTANCE.onEmpty(nodelet);

    return element.setAutoAppendContainer(nodelet);
  }

  @Override
  public void updateRendering(HasImplNodelets element,
      String type, String listStyle,
      int indent, Alignment alignment, Direction direction) {

    Element implNodelet = element.getImplNodelet();
    ParagraphBehaviour behaviour = ParagraphBehaviour.of(type);
    boolean toListItem = behaviour == ParagraphBehaviour.LIST;
    boolean isListItem = implNodelet.getTagName().equalsIgnoreCase(LIST_IMPL_TAGNAME);

    if (isListItem != toListItem) {
      Element newNodelet = createNodeletInner(toListItem);
      DomHelper.replaceElement(implNodelet, newNodelet);
      element.setBothNodelets(newNodelet);
      // Ideally onRepair shouldn't require a ContentElement
      ParagraphHelper.INSTANCE.onRepair((ContentElement) element);

      implNodelet = newNodelet;
    }


    //// Type logic ////

    double fontSize = -1;
    FontWeight fontWeight = null;
    implNodelet.removeClassName(NUMBERED_CLASSNAME);

    switch (behaviour) {
      case LIST:
        if (Paragraph.LIST_STYLE_DECIMAL.equals(listStyle)) {
          implNodelet.addClassName(NUMBERED_CLASSNAME);
        }
        break;
      case HEADING:
        fontWeight = FontWeight.BOLD;
        double headingNum = Integer.parseInt(type.substring(1));
        // Do this with CSS instead.
        // h1 -> 1.75, h4 -> 1, others linearly in between.
        double factor = 1 - (headingNum - 1) / (Paragraph.NUM_HEADING_SIZES - 1);
        fontSize = MIN_HEADING_SIZE_EM + factor * (MAX_HEADING_SIZE_EM - MIN_HEADING_SIZE_EM);
        break;
    }


    //// Indent logic ////

    for (String bulletType : BULLET_CLASSNAMES) {
      implNodelet.removeClassName(bulletType);
    }

    if (behaviour == ParagraphBehaviour.LIST) {
      if (listStyle == null) {
        implNodelet.addClassName(bulletClassName(indent));
      }
      indent++;
    }

    int margin = indent * INDENT_UNIT_SIZE_PX;

    //// Update actual values ////

    // NOTE(danilatos): For these, it might be more efficient to check that the
    // value has changed before changing it. This is not currently  known.

    Style style = implNodelet.getStyle();

    if (fontSize != -1) {
      style.setFontSize(fontSize, Unit.EM);
    } else {
      style.clearFontSize();
    }

    if (fontWeight != null) {
      style.setFontWeight(fontWeight);
    } else {
      style.clearFontWeight();
    }

    if (alignment != null) {
      style.setProperty("textAlign", alignment.cssValue());
    } else {
      style.clearProperty("textAlign");
    }

    if (direction != null) {
      style.setProperty("direction", direction.cssValue());
    } else {
      style.clearProperty("direction");
    }

    if (margin == 0) {
      style.clearMarginLeft();
      style.clearMarginRight();
    } else {
      if (direction == Direction.RTL) {
        style.setMarginRight(margin, Unit.PX);
        style.clearMarginLeft();
      } else {
        style.setMarginLeft(margin, Unit.PX);
        style.clearMarginRight();
      }
    }
  }

  @Override
  public void updateListValue(HasImplNodelets element, int value) {
    element.getImplNodelet().setAttribute("value", String.valueOf(value));
  }

  /**
   * Override this method to use something other than a simple named element.
   * (The tag name is parametrisable via the constructor).
   *
   * @param element See {@link Renderer#createDomImpl(Renderable)}
   * @return the nodelet to use for rendering, must not contain any children
   */
  protected Element createNodelet(Renderable element) {
    return createNodeletInner(false);
  }

  private Element createNodeletInner(boolean isListItem) {
    return Document.get().createElement(isListItem ? LIST_IMPL_TAGNAME : implTagName);
  }
}
