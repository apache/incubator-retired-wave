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

package org.waveprotocol.wave.model.conversation;


import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * Collection of blip utility functions and constants.
 *
 */
public class Blips {

  /**
   * Returns the unique body element of the given document or null if none exists.
   */
  public static <N, E extends N> E getBody(ReadableDocument<N, E, ?> doc) {
    return DocHelper.getElementWithTagName(doc, LineContainers.topLevelContainerTagname());
  }

  /**
   * All annotations related to the conversation model
   *
   * This is an implementation detail, but it is public, given the
   * inspectability of the wavelet & document data
   */
  public static final String ANNOTATION_PREFIX = "conv";

  /**
   * The tag name for the visible, user-editable element in a conversation
   * document This is NOT the root element.
   *
   * It is also a line container.
   */
  public static final String BODY_TAGNAME = "body";
  static {
    LineContainers.setTopLevelContainerTagname(BODY_TAGNAME);
  }

  /**
   * The tag name for the blip head.
   */
  public static final String HEAD_TAGNAME = "head";

  /**
   * The tag name for blip timestamps.
   */
  public static final String TIMESTAMP_TAGNAME = "timestamp";

  /**
   * The tag name for last modification time.
   */
  public static final String LAST_MODIFICATION_TIME_TAGNAME = "lmt";

  /**
   * XML representing the initial state of a blip head.
   *
   * TODO(user): Add blip head.
   */
  public static final XmlStringBuilder INITIAL_HEAD = XmlStringBuilder.createEmpty();

  /**
   * Adds a blip head to a DocInitializationBuilder.
   */
  public static void buildBlipHead(DocInitializationBuilder b) {
    // Will be implemented when adding blip heads.
  }

  /**
   * Adds a blip body to a DocInitializationBuilder.
   */
  public static void buildEmptyBlipBody(DocInitializationBuilder b) {
    b.elementStart(BODY_TAGNAME, AttributesImpl.EMPTY_MAP)
     .elementStart(LineContainers.LINE_TAGNAME, AttributesImpl.EMPTY_MAP)
     .elementEnd()
     .elementEnd();
  }

  /**
   * XML representing the initial state of a blip body.
   */
  public static final XmlStringBuilder INITIAL_BODY = XmlStringBuilder.createEmpty()
      .wrap(LineContainers.LINE_TAGNAME)
      .wrap(Blips.BODY_TAGNAME);

  /**
   * XML representing the initial contents of a blip document.
   */
  public static final XmlStringBuilder INITIAL_CONTENT = XmlStringBuilder.createEmpty()
      .append(Blips.INITIAL_HEAD)
      .append(Blips.INITIAL_BODY);

  /**
   * Tag name for an inline reply anchor.
   */
  public static final String THREAD_INLINE_ANCHOR_TAGNAME = "reply";

  /**
   * Attribute name for an inline reply anchor's thread id attribute.
   */
  public static final String THREAD_INLINE_ANCHOR_ID_ATTR = "id";

  /**
   * Inline reply location signifying a deleted anchor element.
   */
  public static final int INVALID_INLINE_LOCATION = -1;

  /**
   * Currently just a dummy method to ensure the constants are initialised
   */
  public static void init() {
  }
}
