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

package org.waveprotocol.wave.client.editor.examples.img;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.DomHelper.JavaScriptEventListener;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.RenderingMutationHandler;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.misc.ChunkyElementHandler;
import org.waveprotocol.wave.client.editor.content.misc.DisplayEditModeHandler;
import org.waveprotocol.wave.client.editor.content.misc.LinoTextEventHandler;
import org.waveprotocol.wave.client.editor.content.misc.UpdateContentEditable;
import org.waveprotocol.wave.client.editor.content.paragraph.ParagraphRenderer;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.client.editor.gwt.GwtRenderingMutationHandler;
import org.waveprotocol.wave.model.document.util.Point;

/**
 * Demonstration doodad, with a few different variations of rendering and
 * interactive behavior.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class MyDoodad {
  public static String TAGNAME = "mydoodad";
  public static String CAPTION_TAGNAME = "mycaption";

  public static String REF_ATTR = "ref";

  public static void register(ElementHandlerRegistry registry) {

    //// For <mydoodad> ////

    // Alternatives with different behaviors are commented out.

//    SimpleRenderer renderer = new SimpleRenderer();
    CaptionedRenderer renderer = new CaptionedRenderer();

//    NodeEventHandler handler = ChunkyElementHandler.get();
//    SimpleEventHandler handler = new SimpleEventHandler();
//    GwtEventHandler handler = new GwtEventHandler(renderer);
    CaptionedEventHandler handler = new CaptionedEventHandler(renderer);


    //// For <mycaption> ////

    // The next line Creates a renderer that uses a "div" for the HTML, and
    // handles all the logic for correctly rendering a line
    // of text. See the implementation for details.
    ParagraphRenderer captionRenderer = ParagraphRenderer.create("div");
    NodeEventHandler captionHandler = new CaptionEventHandler();


    ////

    registry.registerRenderingMutationHandler(TAGNAME, renderer);
    registry.registerRenderingMutationHandler(CAPTION_TAGNAME, captionRenderer);
    registry.registerEventHandler(TAGNAME, handler);
    registry.registerEventHandler(CAPTION_TAGNAME, captionHandler);
  }

  /**
   * A trivial renderer that keeps the image's src attribute up-to-date with the
   * model's ref attribute.
   */
  static class SimpleRenderer extends RenderingMutationHandler {

    @Override
    public Element createDomImpl(Renderable element) {
      Element imgTag = Document.get().createImageElement();
      DomHelper.setContentEditable(imgTag, false, false);
      return imgTag;
    }

    @Override
    public void onActivatedSubtree(ContentElement element) {
      fanoutAttrs(element);
    }

    @Override
    public void onAttributeModified(
        ContentElement element, String name, String oldValue, String newValue) {

      if (REF_ATTR.equals(name)) {
        element.getImplNodelet().setAttribute("src", newValue);
      }
    }
  }

  /**
   * Simple event handler that allows the user to change the image.
   *
   * This demonstrates adding custom event handling to the doodad.
   *
   * We extend ChunkyElementHandler that takes care of special editor events
   * that make our element have sensible behaviour with respect to arrow keys
   * and delete/backspace.
   */
  static class SimpleEventHandler extends ChunkyElementHandler {

    @Override
    public void onActivated(final ContentElement element) {
      Helper.registerJsHandler(
          element, element.getImplNodelet(), "click", new JavaScriptEventListener() {
            @Override
            public void onJavaScriptEvent(String name, Event event) {
              promptNewRef(element);
            }
          });
    }

    @Override
    public void onDeactivated(ContentElement element) {
      // Cleanup
      Helper.removeJsHandlers(element);
    }
  }

  static void promptNewRef(ContentElement element) {
    String newRef = Window.prompt("New Ref", element.getAttribute(REF_ATTR));
    if (newRef != null) {
      // Get the document view for mutating the persistent state, then update it
      element.getMutableDoc().setElementAttribute(element, REF_ATTR, newRef);
    }
  }

  /**
   * Renderer that adds a caption and some fancier DOM rendering.
   *
   *  Demonstrates using a GWT widget for rendering, and fancier features like
   * having a concurrently-editable caption sub-element.
   */
  static class CaptionedRenderer extends GwtRenderingMutationHandler {

    public CaptionedRenderer() {
      super(Flow.INLINE);
    }

    /** Gwt renderer equivalent of {@link #createDomImpl(Renderable)} */
    @Override
    protected CaptionedImageWidget createGwtWidget(Renderable element) {
      return new CaptionedImageWidget();
    }

    /**
     * Specify where the HTML DOM of child XML elements goes. Our widget's
     * getContainer() method returns the inner 'div' where we would like to put
     * the caption. We use this as the "container nodelet" so that when the
     * 'mycaption' element gets added to 'mydoodad' (in the model XML), the
     * caption's main 'div' nodelet automatically gets added to our doodad's
     * inner container nodelet (in the render HTML).
     *
     * So our DOM will end up looking like this:
     *
     * <pre>{@literal
     *
     * <div class='top'>           <!-- this is <mydoodad>'s top level "impl nodelet" -->
     *   <img src='...'/>          <!-- the image inside the tag -->
     *   <div class='container>    <!-- this is the container nodelet -->
     *
     *     <div>                   <!-- this is <mycaption>'s top level impl nodelet -->
     *       caption text
     *       <br/>                 <!-- This br gets inserted by the paragraph renderer
     *     </div>                       and is needed on some browsers. we don't have to
     *                                  worry about it, it's taken care of for us -->
     *   </div>
     * </div>
     *
     * }</pre>
     */
    @Override
    protected Element getContainerNodelet(Widget w) {
      return ((CaptionedImageWidget) w).getContainer();
    }

    @Override
    public void onActivatedSubtree(ContentElement element) {
      super.onActivatedSubtree(element);
      fanoutAttrs(element);
    }

    @Override
    public void onAttributeModified(
        ContentElement element, String name, String oldValue, String newValue) {
      super.onAttributeModified(element, name, oldValue, newValue);

      if (REF_ATTR.equals(name)) {
        getWidget(element).setImageSrc(newValue);
      }
    }

    /** Convenience getter */
    CaptionedImageWidget getWidget(ContentElement e) {
      return ((CaptionedImageWidget) getGwtWidget(e));
    }
  }

  static class GwtEventHandler extends ChunkyElementHandler {
    private final CaptionedRenderer renderer;

    GwtEventHandler(CaptionedRenderer renderer) {
      this.renderer = renderer;
    }

    @Override
    public void onActivated(final ContentElement element) {
      renderer.getWidget(element).setListener(new CaptionedImageWidget.Listener() {
          @Override public void onClickImage() {
            promptNewRef(element);
          }
        });
    }
  }

  static class CaptionedEventHandler extends GwtEventHandler {
    CaptionedEventHandler(CaptionedRenderer renderer) {
      super(renderer);
    }

    /**
     * Handles a left arrow that occurred with the caret immediately
     * after this node, by moving caret to end of caption
     */
    @Override
    public boolean handleLeftAfterNode(ContentElement element, EditorEvent event) {
      ContentElement caption = getCaption(element);

      if (caption != null) {
        // If we have a caption, move the selection into the caption
        element.getSelectionHelper().setCaret(
            Point.<ContentNode> end(getCaption(element)));
        return true;
      } else {
        // If we don't have a caption, use the default behavior
        return super.handleLeftAfterNode(element, event);
      }
    }

    /**
     * Similar to {@link #handleLeftAfterNode(ContentElement, EditorEvent)}
     */
    @Override
    public boolean handleRightBeforeNode(ContentElement element, EditorEvent event) {
      ContentElement caption = getCaption(element);

      if (caption != null) {
        // If we have a caption, move the selection into the caption
        element.getSelectionHelper().setCaret(
            Point.start(element.getRenderedContentView(), caption));
        return true;
      } else {
        // If we don't have a caption, use the default behavior
        return super.handleRightBeforeNode(element, event);
      }
    }

    /**
     * Handles a left arrow at the beginning of the caption, moving the
     * selection out of the whole doodad. We receive this event because the
     * caption doesn't handle it and it bubbles outwards to our handler here.
     */
    @Override
    public boolean handleLeftAtBeginning(ContentElement element, EditorEvent event) {
      // NOTE: The use of location mapper will normalise into text nodes.
      element.getSelectionHelper().setCaret(element.getLocationMapper().getLocation(
          Point.before(element.getRenderedContentView(), element)));
      return true;
    }

    /**
     * Similar to {@link #handleLeftAtBeginning(ContentElement, EditorEvent)}
     */
    @Override
    public boolean handleRightAtEnd(ContentElement element, EditorEvent event) {
      // NOTE: The use of location mapper will normalise into text nodes.
      element.getSelectionHelper().setCaret(element.getLocationMapper().getLocation(
          Point.after(element.getRenderedContentView(), element)));
      return true;
    }

    private ContentElement getCaption(ContentElement element) {
      return (ContentElement) element.getFirstChild();
    }
  }

  /**
   * Event handler for our caption. Demonstrates two things:
   * 1. Subclassing LinoTextEventHandler, which provides sane behavior for,
   *    well, a line-of-text. (See its code for details)
   * 2. Use of utility to synchronise editability of caption region with main
   *    editor region.
   */
  static class CaptionEventHandler extends LinoTextEventHandler {
    @Override
    public void onActivated(ContentElement element) {
      super.onActivated(element);

      // Add a listener to edit mode changes.
      // We use an existing one that does exactly what we want: updates the editability of
      // our element's container as a result.
      DisplayEditModeHandler.setEditModeListener(element, UpdateContentEditable.get());
    }
  }
}
