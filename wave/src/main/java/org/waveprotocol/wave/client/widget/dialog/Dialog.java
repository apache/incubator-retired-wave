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

package org.waveprotocol.wave.client.widget.dialog;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import org.waveprotocol.wave.client.wavepanel.view.dom.full.WavePanelResourceLoader;

/**
 * Resources and common functions library for dialog widgets.
 *
 * @author Denis Konovalchik (dyukon@gmail.com)
 */
public class Dialog {
  /** Resources used by this widget. */
  public interface Resources extends ClientBundle {
    @ClientBundle.Source("Dialog.css")
    Css css();
  }

  /** CSS for this widget. */
  public interface Css extends CssResource {
    String errorLabel();
    String warningLabel();
    String infoLabel();
    String dialogButtonPanel();
    String dialogButton();
    String glassPanel();
  }

  private static Css css;

  public static Css getCss() {
    if (css == null) {
      css = WavePanelResourceLoader.getDialog().css();
    }
    return css;
  }
}