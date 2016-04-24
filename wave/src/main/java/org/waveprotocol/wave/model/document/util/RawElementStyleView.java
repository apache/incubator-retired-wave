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

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;

/**
 * Implements style accessors for POJO elements.
 *
 */
public class RawElementStyleView extends IdentityView<Node, Element, Text>
    implements ElementStyleView<Node, Element, Text> {
  /**
   * Constructor.
   * @param inner
   */
  public RawElementStyleView(ReadableDocument<Node, Element, Text> inner) {
    super(inner);
  }

  /** {@inheritDoc} */
  @Override
  public String getStylePropertyValue(Element element, String name) {
    // TODO(user): This is a highly non-optimal solution, but this is just for
    // testing. Did not want to pollute the raw Element class with this.
    String styles = element.getAttribute("style");
    if (styles != null && styles.contains(name)) {
      for (String stylePair : styles.split(";")) {
        int index = stylePair.indexOf(':');
        if (index >= 0 && index < stylePair.length() - 1) {
          String key = stylePair.substring(0, index).trim();
          String value = stylePair.substring(index + 1);
          if (key.equalsIgnoreCase(name)) {
            return value.trim();
          }
        }
      }
    }
    return null;
  }
}
