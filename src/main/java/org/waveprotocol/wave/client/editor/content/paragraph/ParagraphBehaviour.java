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

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.ValueUtils;

/**
 * The many behaviours of the "paragraph" element type
 *
 * It should probably not be called paragraph any more...
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
enum ParagraphBehaviour {

  /** Normal paragraph */
  DEFAULT,
  /** Heading of some importance level or other */
  HEADING,
  /** List item */
  LIST;

  private static final StringMap<ParagraphBehaviour> subtypeMappings =
      CollectionUtils.createStringMap();

  static {
    subtypeMappings.put("p", DEFAULT);
    for (int i = 1; i <= Paragraph.NUM_HEADING_SIZES; i++) {
      subtypeMappings.put("h" + i, HEADING);
    }
    subtypeMappings.put(Paragraph.LIST_TYPE, LIST);
  }

  /**
   * @param subtype value of a paragraph's subtype attribute
   * @return the corresponding behaviour
   */
  public static ParagraphBehaviour of(String subtype) {
    ParagraphBehaviour b = subtypeMappings.get(ValueUtils.valueOrDefault(subtype, ""));
    return ValueUtils.valueOrDefault(b, DEFAULT);
  }
}
