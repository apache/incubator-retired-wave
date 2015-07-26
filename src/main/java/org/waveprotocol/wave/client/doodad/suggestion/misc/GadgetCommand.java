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

package org.waveprotocol.wave.client.doodad.suggestion.misc;

import com.google.gwt.user.client.Command;

import org.waveprotocol.wave.client.gadget.GadgetXmlUtil;
import org.waveprotocol.wave.client.gadget.StateMap;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.document.util.RangeTracker;

/**
 * Command for a menu, replaces the Content Element with a Gadget.
 *
 *
 * @param <N>
 * @param <E>
 * @param <T>
 */
public class GadgetCommand<N, E extends N, T extends N> implements Command {
  // TODO(user): Consider moving this into another package.

  private final String gadgetUrl;
  private final StateMap params;
  private final String keyToClear;
  private final MutableDocument<N, E, T> doc;
  private final RangeTracker replacementRangeProvider;

  /**
   *
   * @param gadgetUrl url of the gadget xml
   * @param params parameter as json to be passed the gadget
   * @param doc mutable doc
   * @param keyToClear
   * @param replacementRangeProvider
   */
  public GadgetCommand(String gadgetUrl, StateMap params, MutableDocument<N, E, T> doc,
      String keyToClear, RangeTracker replacementRangeProvider) {
    this.gadgetUrl = gadgetUrl;
    this.params = params;
    this.doc = doc;
    this.keyToClear = keyToClear;
    this.replacementRangeProvider = replacementRangeProvider;
  }

  @Override
  public void execute() {
    replaceWithGadget(gadgetUrl, params);
  }

  /** Replaces the contentElement with a gadget. */
  private void replaceWithGadget(String url, StateMap stateMap) {
    Range range = replacementRangeProvider.getRange();
    if (range == null) {
      return;
    }

    // NOTE(user): Clear the annotation to schedule repaint. Is there a better
    // way?
    doc.setAnnotation(range.getStart(), range.getEnd(), keyToClear, null);
    doc.deleteRange(doc.locate(range.getStart()), doc.locate(range.getEnd()));

    // TODO: Plumb the login name here
    doc.insertXml(doc.locate(range.getStart()),
        GadgetXmlUtil.constructXml(url, stateMap, "anonymous@example.com" /* Fix this */));
  }
}
