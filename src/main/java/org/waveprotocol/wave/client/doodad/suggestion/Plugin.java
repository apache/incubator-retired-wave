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

package org.waveprotocol.wave.client.doodad.suggestion;

import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.sugg.Menu;
import org.waveprotocol.wave.model.document.util.RangeTracker;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.Map;

/**
 * Interface for class that populates the suggestions depending on the annotations.
 *
 */
public interface Plugin {
  /**
   * Fill in the relevant attributes if the annotations match conditions.
   */
  public void maybeFillAttributes(Map<String, Object> before, final StringMap<String> attributes);

  /**
   * Given a set of attributes, populate the suggestion menu with relevant actions.
   *
   * @param menu the menu object that will be populated.
   * @param replacementRangeHelper the range that may be replaced if this
   *     suggestion is used.
   * @param mutableDocument the document containing
   * @param element the actual element used to render the suggestion, useful
   *     for retrieving properties.
   */
  void populateSuggestionMenu(Menu menu, RangeTracker replacementRangeHelper,
      CMutableDocument mutableDocument, ContentElement element);
}
