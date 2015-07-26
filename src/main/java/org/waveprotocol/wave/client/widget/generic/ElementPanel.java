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

package org.waveprotocol.wave.client.widget.generic;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A complex panel that simply adds its children to an element.
 */
public class ElementPanel extends ComplexPanel {

  /**
   * Creates a panel that adds its children to a div.
   */
  public ElementPanel() {
    this(Document.get().createDivElement());
  }

  /**
   * Creates a panel that adds its children to the given element.
   */
  public ElementPanel(Element elem) {
    setElement(elem);
  }

  public ElementPanel(String style) {
    this();
    setStyleName(style);
  }

  @Override
  public void add(Widget w) {
    add(w, getElement());
  }

  public void insert(Widget w, int beforeIndex) {
    super.insert(w, getElement(), beforeIndex, true);
  }
}
