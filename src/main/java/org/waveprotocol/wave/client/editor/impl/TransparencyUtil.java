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

package org.waveprotocol.wave.client.editor.impl;

import com.google.gwt.dom.client.Element;
import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.model.document.util.FilteredView.Skip;

import java.util.List;

/**
 * Utility methods
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class TransparencyUtil {

  /**
   * Helper to remove a list of transparent nodes from the document
   * @param elements
   */
  public static void clear(List<Element> elements) {
    for (Element e : elements) {
      if (e.getParentElement() != null) {
        if (NodeManager.getTransparency(e) == Skip.DEEP) {
          e.removeFromParent();
        } else {
          DomHelper.unwrap(e);
        }
      }
      // Break circular references in browsers with poor gc
      NodeManager.setTransparentBackref(e, null);
    }

    elements.clear();
  }
}