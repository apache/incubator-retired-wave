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

package org.waveprotocol.wave.client.widget.button;

import org.waveprotocol.wave.client.widget.button.ClickButton.ClickButtonListener;
import org.waveprotocol.wave.client.widget.button.icon.IconButtonTemplate.IconButtonStyle;
import org.waveprotocol.wave.client.widget.button.text.TextButton.TextButtonStyle;

/**
 * Use this to creates buttons if you don't like long parameter lists.
 * This will create an extra instance which IEs garbage collector does
 * not like, so don't use in a tight loop (please GWT can we get the
 * compiler to be able optimize this sort of stuff away!).
 *
 * TODO(tirsen): support all the other buttons and styles
 *
 */
public class ButtonBuilder {

  /**
   * Properties supported by all types of buttons.
   */
  @SuppressWarnings({"unchecked", "rawtypes"}) // we're doing some clever tricks in here
  public abstract static class Base<T extends Base> {
    protected String text;
    protected String tooltip;
    protected String debugClass;
    protected String styleName;

    public T label(String label) {
      this.text = label;
      return (T) this;
    }

    public T tooltip(String tooltip) {
      this.tooltip = tooltip;
      return (T) this;
    }

    public T debugClass(String debugClass) {
      this.debugClass = debugClass;
      return (T) this;
    }

    public T styleName(String style) {
      this.styleName = style;
      return (T) this;
    }
  }

  public static class Click extends Base<Click> {
    private ClickButtonListener listener;
    private TextButtonStyle textStyle = TextButtonStyle.REGULAR_BUTTON;
    private IconButtonStyle iconStyle = null;

    public Click listener(ClickButtonListener listener) {
      this.listener = listener;
      return this;
    }

    public ClickButtonWidget build() {
      ClickButtonWidget result;
      if (text != null) {
        result = ButtonFactory.createTextClickButton(text, textStyle, tooltip, listener);
      } else {
        result = ButtonFactory.createIconClickButton(iconStyle, tooltip, listener);
      }
      if (styleName != null) {
        result.addStyleName(styleName);
      }
      if (debugClass != null) {
        // DebugClassHelper.addDebugClass(result, debugClass);
      }
      return result;
    }

    public Click iconStyle(IconButtonStyle style) {
      this.text = null;
      this.iconStyle = style;
      return this;
    }

    public Click textStyle(TextButtonStyle style) {
      this.textStyle = style;
      return this;
    }
  }

  public Click clickButton() {
    return new Click();
  }
}
