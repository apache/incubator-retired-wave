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

import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.ValueUtils;

/**
 * An editable paragraph element
 *
 */
public class Paragraph {

  /**
   * Tag name for regular paragraphs
   */
  public static final String TAGNAME = "p";

  /**
   * Number of headings, h1...h{NUM_HEADING_SIZES}
   */
  public static final int NUM_HEADING_SIZES = 4;

  /**
   * Subtype attribute, for things like headings, bullet points, etc.
   */
  public static final String SUBTYPE_ATTR = "t";

  /**
   * Indentation attribute. Integral values, in "indentation units"
   */
  public static final String INDENT_ATTR = "i";

  public static final int MAX_INDENT = 50;

  /**
   * Text alignment, left, right, center, justified
   */
  public static final String ALIGNMENT_ATTR = "a";

  /**
   * Text direction, ltr, rtl
   */
  public static final String DIRECTION_ATTR = "d";

  /**
   * Text alignment, left, right, center, justified
   */
  public enum Alignment implements ContentElement.Action, LineStyle {
    /***/ LEFT("l", "left"),
    /***/ RIGHT("r", "right"),
    /***/ CENTER("c", "center"),
    /***/ JUSTIFY("j", "justify");

    private static final StringMap<Alignment> map = CollectionUtils.createStringMap();
    static {
      for (Alignment a : Alignment.values()) {
        map.put(a.value, a);
      }
    }

    public static Alignment fromValue(String v) {
      return v != null ? map.get(v) : null;
    }

    static String cssFromValue(String v) {
      Alignment a = fromValue(v);
      return a != null ? a.cssValue() : "";
    }

    final String value, css;
    private Alignment(String value, String css) {
      this.value = value;
      this.css = css;
    }

    @Override public void execute(ContentElement e) {
      apply(e, true);
    }

    @Override public void apply(ContentElement e, boolean on) {
      e.getMutableDoc().setElementAttribute(e, ALIGNMENT_ATTR,
          this == LEFT ? null : (on ? value : null));
    }

    @Override public boolean isApplied(ContentElement e) {
      Alignment val = fromValue(e.getAttribute(ALIGNMENT_ATTR));
      return this == (val == null ? LEFT : val);
    }

    /**
     * @return CSS value for the "text-align" property
     */
    public String cssValue() {
      return css;
    }

    /** Convert css value back to an enum value, null if not found. */
    public static Alignment fromCssValue(String css) {
      for (Alignment a : Alignment.values()) {
        if (a.css.equals(css)) {
          return a;
        }
      }
      return null;
    }
  }

  /**
   * Text direction, ltr vs rtl
   */
  public enum Direction implements ContentElement.Action, LineStyle {
    /***/ LTR("l", "ltr", Alignment.LEFT, Alignment.RIGHT),
    /***/ RTL("r", "rtl", Alignment.RIGHT, Alignment.LEFT);

    private static final StringMap<Direction> map = CollectionUtils.createStringMap();
    static {
      for (Direction a : Direction.values()) {
        map.put(a.value, a);
      }
    }

    static Direction fromValue(String v) {
      return v != null ? map.get(v) : null;
    }

    static String cssFromValue(String v) {
      Direction a = fromValue(v);
      return a != null ? a.cssValue() : "";
    }

    final String value, css;
    final Alignment alignment, oppositeAlignment;
    private Direction(String value, String css, Alignment alignment, Alignment oppositeAlignment) {
      this.value = value;
      this.css = css;
      this.alignment = alignment;
      this.oppositeAlignment = oppositeAlignment;
    }

    @Override public void execute(ContentElement e) {
      apply(e, true);
    }

    @Override public void apply(ContentElement e, boolean on) {
      e.getMutableDoc().setElementAttribute(e, DIRECTION_ATTR,
          this == LTR ? null : (on ? value : null));
      if (on && oppositeAlignment.isApplied(e)) {
        alignment.apply(e, true);
      }
    }

    @Override public boolean isApplied(ContentElement e) {
      Direction val = fromValue(e.getAttribute(DIRECTION_ATTR));
      return this == (val == null ? LTR : val);
    }

    /**
     * @return HTML value for the "dir" attribute
     */
    public String cssValue() {
      return css;
    }
  }

  /**
   * Type for list element mode.
   */
  public static final String LIST_TYPE = "li";

  public static final String LIST_STYLE_ATTR = "listyle";

  public static final String LIST_STYLE_DECIMAL = "decimal";

  /**
   * Registers handlers for paragraphs
   */
  public static void register(ElementHandlerRegistry registry) {
    register(TAGNAME, registry);
  }

  /**
   * Default renderer that responds to mutation events
   */
  public static final ParagraphRenderer DEFAULT_RENDERER = new ParagraphRenderer(
      new DefaultParagraphHtmlRenderer());

  /**
   * Default NiceHtmlRenderer for paragraphs.
   */
  public static final ParagraphNiceHtmlRenderer DEFAULT_NICE_HTML_RENDERER =
      new ParagraphNiceHtmlRenderer();

  /**
   * Default handler for user events
   */
  public static final NodeEventHandler DEFAULT_EVENT_HANDLER = new ParagraphEventHandler();

  /**
   * Registers paragraph handlers for any provided tag names / type attributes.
   */
  public static void register(String tagName, ElementHandlerRegistry registry) {
    registry.registerEventHandler(tagName, DEFAULT_EVENT_HANDLER);
    registry.registerRenderingMutationHandler(tagName, DEFAULT_RENDERER);
  }

  /**
   * Indents paragraphs
   */
  public static final ContentElement.Action INDENTER = new ContentElement.Action() {
    public void execute(ContentElement e) {
      ParagraphEventHandler.indent(e, 1);
    }
  };

  /**
   * Outdents paragraphs
   */
  public static final ContentElement.Action OUTDENTER = new ContentElement.Action() {
    public void execute(ContentElement e) {
      ParagraphEventHandler.indent(e, -1);
    }
  };

  private static class RegularStyler implements LineStyle {
    private final String type;
    RegularStyler(String type) {
      this.type = type;
    }

    @Override public void apply(ContentElement e, boolean isOn) {
      e.getMutableDoc().setElementAttribute(e, SUBTYPE_ATTR, isOn ? type : null);
      e.getMutableDoc().setElementAttribute(e, LIST_STYLE_ATTR, null);
    }

    @Override public boolean isApplied(ContentElement e) {
      return ValueUtils.equal(type, e.getAttribute(SUBTYPE_ATTR));
    }
  }

  private static class ListStyler implements LineStyle {
    private final String type;
    ListStyler(String type) {
      this.type = type;
    }

    @Override public void apply(ContentElement e, boolean isOn) {
      e.getMutableDoc().setElementAttribute(e, SUBTYPE_ATTR, isOn ? LIST_TYPE : null);
      e.getMutableDoc().setElementAttribute(e, LIST_STYLE_ATTR, isOn ? type : null);
    }

    @Override public boolean isApplied(ContentElement e) {
      return Paragraph.LIST_TYPE.equals(e.getAttribute(SUBTYPE_ATTR))
          && ValueUtils.equal(type, e.getAttribute(LIST_STYLE_ATTR));
    }
  }

  public static LineStyle regularStyle(String type) {
    Preconditions.checkArgument(!LIST_TYPE.equals(type),
      "Don't use regularStyle() for list styles, use listStyle()");
    return new RegularStyler(type);
  }

  public static LineStyle listStyle(String listStyleType) {
    return new ListStyler(listStyleType);
  }

  /**
   * TODO(danilatos): Get rid of most if not all uses of ContentElement.Action
   * as they are better replaced by LineStyle. For now, this adapter.
   */
  public static ContentElement.Action asAction(final LineStyle style, final boolean isOn) {
    return new ContentElement.Action() {
      @Override public void execute(ContentElement e) {
        style.apply(e, isOn);
      }
    };
  }

  public static Line getFirstLine(LocationMapper<ContentNode> mapper, int start) {
    Point<ContentNode> point = mapper.locate(start);
    CMutableDocument doc = point.getContainer().getMutableDoc();
    LineContainers.checkNotParagraphDocument(doc);

    // get line element we are in:
    ContentNode first = LineContainers.getRelatedLineElement(doc, point);

    if (first == null) {
      return null;
    }

    // go through the lines one by one:
    return Line.fromLineElement(first.asElement());
  }

  public interface LineStyle {
    /**
     * Applies an action to a line based on a button state.
     *
     * @param e line element
     * @param isOn true if the style should be applied, false if it should be removed.
     */
    void apply(ContentElement e, boolean isOn);

    /**
     * @param e line element
     * @return true if the style is considered applied to the given element.
     */
    boolean isApplied(ContentElement e);
  }

  public static void toggle(LocationMapper<ContentNode> mapper,
                           int start, int end, LineStyle style) {
    traverse(mapper, start, end, asAction(style, !appliesEntirely(mapper, start, end, style)));
  }

  public static void apply(LocationMapper<ContentNode> mapper,
      int start, int end, LineStyle style, boolean isOn) {
    traverse(mapper, start, end, asAction(style, isOn));
  }

  public static boolean appliesEntirely(LocationMapper<ContentNode> mapper,
      int start, int end, final LineStyle style) {
    final boolean[] applied = new boolean[]{true, false};
    traverse(mapper, start, end, new ContentElement.Action() {
      @Override public void execute(ContentElement e) {
        applied[0] &= style.isApplied(e);
        applied[1] = true; // to make sure it ran at all
      }
    });

    return applied[0] && applied[1];
  }

  /**
   * Apply an action over all paragraphs entirely or partially contained by the
   * given range.
   *
   * Does not deep traverse to find all paragraphs, assumes they are siblings of
   * each other.
   *
   * @param mapper for getting nodes from the range
   * @param start start of range
   * @param end end of range
   * @param action action to perform on paragraphs (other nodes ignored)
   */
  public static void traverse(LocationMapper<ContentNode> mapper,
      int start, int end, ContentElement.Action action) {

    // go through the lines one by one:
    Line lineAt = getFirstLine(mapper, start);
    if (lineAt == null) {
      return;
    }

    while (lineAt != null) {
      ContentElement lineTag = lineAt.getLineElement();

      // apply if it's ok
      int position = mapper.getLocation(lineTag);
      if (position >= end) { // equals implies selection is before line tag, so don't apply.
        break;
      }

      // traverse to the next one
      action.execute(lineTag);
      lineAt = lineAt.next();
    }
  }

  /** Gets the previous local paragraph sibling, or null if there is one. */
  public static ContentNode getLocalParagraphBackwards(ContentNode at) {
    while (at != null && !LineRendering.isLocalParagraph(at)) {
      at = at.getPreviousSibling();
    }
    return at;
  }

  /**
   * @param node
   * @return true iff the given node is a list item.
   */
  // TODO(user): Handle line elements
  public static boolean isListItem(ContentNode node) {
    return LineRendering.isLocalParagraph(node)
        && Paragraph.LIST_TYPE.equals(node.asElement().getAttribute(SUBTYPE_ATTR));
  }

  /**
   * @param node
   * @return true iff the given node is a heading
   */
  public static boolean isHeading(ContentNode node) {
    String tagName = node.asElement().getAttribute(SUBTYPE_ATTR);
    if (tagName != null && tagName.length() == 2) {
      if (tagName.charAt(0) == 'h' || tagName.charAt(0) == 'H') {
        int size = tagName.charAt(1) - '0';
        if (size >= 1 && size <= 4) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Returns the heading side of the given node (Assumes node is a heading.)
   *
   * @param node
   */
  public static int getHeadingSize(ContentNode node) {
    assert node.asElement() != null;
    String tagName = node.asElement().getAttribute(SUBTYPE_ATTR);
    assert tagName != null && tagName.length() == 2;
    int size = tagName.charAt(1) - '0';
    assert size >= 1 && size <= 4;
    return size;
  }

  static int getIndent(String str) {
    if (str == null) {
      return 0;
    }
    try {
      return Math.min(Math.max(0, Integer.parseInt(str)), Paragraph.MAX_INDENT);
    } catch (NumberFormatException e) {
      // Would only happen if the schema were invalidated
      return 0;
    }
  }

  public static boolean isDecimalListItem(ContentElement e) {
    return
        Paragraph.LIST_TYPE.equals(e.getAttribute(Paragraph.SUBTYPE_ATTR)) &&
        Paragraph.LIST_STYLE_DECIMAL.equals(e.getAttribute(Paragraph.LIST_STYLE_ATTR));
  }

  static int getIndent(ContentElement p) {
    return getIndent(p.getAttribute(INDENT_ATTR));
  }
}
