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

package org.waveprotocol.wave.client.widget.button.text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Panel;

import org.waveprotocol.wave.client.widget.button.ButtonDisplay;
import org.waveprotocol.wave.client.widget.button.MouseListener;
import org.waveprotocol.wave.client.widget.button.StyleAxis;

/**
 * Template for a button that is implemented as some text with separate images
 * for the left and right sides, and the x-repeat image behind it.
 *
 * +------------+-----------------------+-------------+
 * |  leftElem  |       middleElem      |  rightElem  |
 * |            |       (xrepeat)       |             |
 * +------------+-----------------------+-------------+
 *
 */
public class TextButton extends Composite implements ButtonDisplay {

  interface Binder extends UiBinder<Panel, TextButton> {}

  private static Binder binder = GWT.create(Binder.class);

  @UiField protected Element middle;

  public interface Resources extends ClientBundle {
    /* REGULAR_BUTTON images */
    @Source("button_left.png")
    @ImageOptions(flipRtl = true)
    ImageResource regularLeftImage();

    @Source("button_middle.png")
    @ImageOptions(repeatStyle=RepeatStyle.Horizontal)
    ImageResource regularMiddleImage();

    @Source("button_right.png")
    @ImageOptions(flipRtl = true)
    ImageResource regularRightImage();

    @Source("button_left_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource regularLeftDownImage();

    @Source("button_middle_down.png")
    @ImageOptions(repeatStyle=RepeatStyle.Horizontal)
    ImageResource regularMiddleDownImage();

    @Source("button_right_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource regularRightDownImage();

    /* PRIMARY_BUTTON images */
    @Source("primary_button_left.png")
    @ImageOptions(flipRtl = true)
    ImageResource primaryLeftImage();

    @Source("primary_button_middle.png")
    @ImageOptions(repeatStyle=RepeatStyle.Horizontal)
    ImageResource primaryMiddleImage();

    @Source("primary_button_right.png")
    @ImageOptions(flipRtl = true)
    ImageResource primaryRightImage();

    @Source("primary_button_left_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource primaryLeftDownImage();

    @Source("primary_button_middle_down.png")
    @ImageOptions(repeatStyle=RepeatStyle.Horizontal)
    ImageResource primaryMiddleDownImage();

    @Source("primary_button_right_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource primaryRightDownImage();

    /* ADD_BUTTON images */
    @Source("add_button_left.png")
    @ImageOptions(flipRtl = true)
    ImageResource addButtonLeftImage();

    @Source("add_button_middle.png")
    @ImageOptions(repeatStyle=RepeatStyle.Horizontal)
    ImageResource addButtonMiddleImage();

    @Source("add_button_right.png")
    @ImageOptions(flipRtl = true)
    ImageResource addButtonRightImage();

    /* SYSTEM_BUTTON images */
    @Source("system_button_left.png")
    @ImageOptions(flipRtl = true)
    ImageResource systemButtonLeftImage();

    @Source("system_button_middle.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource systemButtonMiddleImage();

    @Source("system_button_right.png")
    @ImageOptions(flipRtl = true)
    ImageResource systemButtonRightImage();

    @Source("system_button_left_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource systemLeftDownImage();

    @Source("system_button_middle_down.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource systemMiddleDownImage();

    @Source("system_button_right_down.png")
    @ImageOptions(flipRtl = true)
    ImageResource systemRightDownImage();


    @Source("TextButton.css")
    Css css();

    interface Css extends CssResource {
      String addButton();
      String primaryButton();
      String regularButton();
      String systemButton();

      String fullWidth();

      String full();
      String left();
      String middle();
      String middletd();
      String right();

      String down();
      String hover();
      String disabled();

      String bold();
      String cursorPointer();
    }
  }

  /**
   * The styles that this kind of button can have.
   *
   */
  public enum TextButtonStyle {
    REGULAR_BUTTON(res.css().regularButton()),
    PRIMARY_BUTTON(res.css().primaryButton()),
    ADD_BUTTON(res.css().addButton()),
    SYSTEM_BUTTON(res.css().systemButton());

    private final String style;

    private TextButtonStyle(String style) {
      this.style = style;
    }

    public String getStyleName() {
      return style;
    }
  }

  /** The singleton instance of our resources. */
  @UiField(provided=true)
  static final Resources res = GWT.create(Resources.class);

  static {
    StyleInjector.inject(res.css().getText());
  }

  TextButtonStyle style;

  /**
   * Creates a {@link TextButton} with the given text, style and logic.
   *
   * @param text The text that the button should have.
   * @param style The style that the button should have.
   * @param tooltip The tooltip for this button.
   */
  @UiConstructor
  public TextButton(String text, TextButtonStyle style, String tooltip) {
    this.style = style;
    initWidget(binder.createAndBindUi(this));
    addStyleName(style.getStyleName());
    setText(text);
    setTooltip(tooltip);

    boldStyle = new StyleAxis(getElement());
    cursorStyle = new StyleAxis(getElement());
    fullWidthStyle = new StyleAxis(getElement());

    cursorStyle.setStyle(res.css().cursorPointer());
  }

  // TODO(schuck,jameskozianski): Combine 0-arg constructor + init() method into
  //     the single 3-arg constructor. Remove 0-arg constr + init().
  public TextButton() {
    this("", TextButtonStyle.REGULAR_BUTTON, "");
  }

  /**
   * Constructs a TextButton instance.
   *
   * @param text The text that the button should have.
   * @param style The style that the button should have.
   * @param tooltip The tooltip for this button.
   */
  public void init(String text, TextButtonStyle style, String tooltip) {
    removeStyleName(this.style.getStyleName());
    addStyleName(style.getStyleName());
    this.style = style;
    setText(text);
    setTooltip(tooltip);
  }

  private final StyleAxis boldStyle;
  private final StyleAxis cursorStyle;
  private final StyleAxis fullWidthStyle;

  public void setBold(boolean isBold) {
    boldStyle.setStyle(isBold ? res.css().bold() : null);
  }

  public void setFullWidth(boolean isFullWidth) {
    fullWidthStyle.setStyle(isFullWidth ? res.css().fullWidth() : null);
  }

  /**
   * The listener for mouse events on this widget.
   */
  private MouseListener mouseListener;

  /**
   * The CSS class that we have currently applied to the element because of the
   * hover / normal / down state.
   */
  private String stateStyleName;

  /**
   * Whether or not we should call stopPropagation() on click events.
   */
  private boolean stopPropagation = false;

  /** {@inheritDoc} */
  public void setUiListener(MouseListener mouseListener) {
    sinkEvents(Event.MOUSEEVENTS);
    sinkEvents(Event.ONCLICK);

    this.mouseListener = mouseListener;
  }

  @Override
  public void setTooltip(String tooltip) {
    setTitle(tooltip);
  }

  /** {@inheritDoc} */
  public void setState(ButtonState state) {
    if (stateStyleName != null) {
      removeStyleName(stateStyleName);
    }
    switch (state) {
      case DISABLED:
        addStyleName(stateStyleName = res.css().disabled());
        break;
      case DOWN:
        addStyleName(stateStyleName = res.css().down());
        break;
      case HOVER:
        addStyleName(stateStyleName = res.css().hover());
        break;
      case NORMAL:
        stateStyleName = null;
        break;
    }
  }

  @Override
  public void onBrowserEvent(Event event) {
    if (mouseListener == null) {
      super.onBrowserEvent(event);
      return;
    }

    switch (event.getTypeInt()) {
      case Event.ONMOUSEDOWN:
        mouseListener.onMouseDown();
        break;
      case Event.ONMOUSEOUT:
        mouseListener.onMouseLeave();
        break;
      case Event.ONMOUSEOVER:
        mouseListener.onMouseEnter();
        break;
      case Event.ONMOUSEUP:
        mouseListener.onMouseUp();
        break;
      case Event.ONCLICK:
        mouseListener.onClick();
        if (stopPropagation) {
          event.stopPropagation();
        }
        break;
    }
    super.onBrowserEvent(event);
  }

  @Override
  protected void onDetach() {
    super.onDetach();
    if (mouseListener != null) {
      mouseListener.onMouseLeave();
    }
  }

  /**
   * @return The middle element which has an xrepeated background.
   */
  public Element getMiddle() {
    return middle;
  }

  /**
   * @param stopPropagation Whether or not this button stops propagation when a
   *        click event is received.
   */
  public void setStopPropagation(boolean stopPropagation) {
    this.stopPropagation = stopPropagation;
  }

  /**
   * Sets the text of this text button.
   *
   * @param text Text to show in the button.
   */
  public void setText(String text) {
    middle.setInnerText(text);
  }

  public void setHtml(String html) {
    middle.setInnerHTML(html);
  }

  /**
   * Makes this button assume the given style.
   *
   * @param style The new style for this button.
   */
  public void changeStyle(TextButtonStyle style) {
    removeStyleName(this.style.getStyleName());
    this.style = style;
    addStyleName(this.style.getStyleName());
  }
}
