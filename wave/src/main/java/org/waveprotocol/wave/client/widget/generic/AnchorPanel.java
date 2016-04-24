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

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * A complex panel that simply adds its children to an anchor tag.
 *
 */
public class AnchorPanel extends SimplePanel {
  private AnchorElement anchor;
  /**
   * Create a new anchor panel
   */
  public AnchorPanel() {
    super(Document.get().createAnchorElement());
    anchor = (AnchorElement)getElement().cast();
  }

  /**
   * Set the target field of the anchor tag.
   * @param t
   */
  public void setTarget(String t) {
    anchor.setTarget(t);
  }

  /**
   * Set the HREF of the anchor tag.
   */
  public void setHref(String href) {
    anchor.setHref(href);
  }
}
