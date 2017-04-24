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

package org.waveprotocol.wave.model.conversation.testing;

import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.TitleHelper;

import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * Utility functions used by conversation tests.
 *
 */
public final class BlipTestUtils {

  // Non-instantiatable class
  private BlipTestUtils() {
  }

  /**
   * Returns the position of the body element in an initial, empty, blip.
   */
  public static int getInitialBlipBodyPosition() {
    IndexedDocument<Node, Element, Text> d = DocProviders.POJO.build(
        TitleHelper.emptyDocumentWithTitle(),
        DocumentSchema.NO_SCHEMA_CONSTRAINTS);
    return getBodyPosition(d);
  }


  /**
   * Returns the position of the body element of the given blip.
   */
  public static int getBodyPosition(ConversationBlip blip) {
    return getBodyPosition(blip.getContent());
  }

  /**
   * Returns the position of the body element of the given document or 0 if the
   * document has no body.
   */
  public static <N, E extends N> int getBodyPosition(ReadableWDocument<N, E, ?> doc) {
    N body = DocHelper.getElementWithTagName(doc, Blips.BODY_TAGNAME);
    if (body == null) return 0;
    return doc.getLocation(body);
  }

  /**
   * Wrap the given xml string containing a blip body in an XmlStringBuilder
   * and prepend an empty head.
   */
  public static XmlStringBuilder prependHead(String body) {
    return XmlStringBuilder.createEmpty()
      .append(Blips.INITIAL_HEAD)
      .append(XmlStringBuilder.createFromXmlString(body));
  }

  /**
   * Returns a blip whose contents are the given lines.
   */
  public static String debugBlipWrap(String ... lines) {
    return XmlStringBuilder.createEmpty()
        .append(Blips.INITIAL_HEAD)
        .append(XmlStringBuilder.createFromXmlString(LineContainers.debugContainerWrap(lines)))
        .toString();
  }
}
