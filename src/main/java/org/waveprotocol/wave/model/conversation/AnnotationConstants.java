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

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;

/**
 * Constants useful for annotations
 * Refer to the conversation specification for more detailed information.
 */
public class AnnotationConstants {
  // Style

  /** Prefix for style annotations (follows CSS). */
  public static final String STYLE_PREFIX = "style";

  public static final String STYLE_BG_COLOR = STYLE_PREFIX + "/backgroundColor";

  public static final String STYLE_COLOR = STYLE_PREFIX + "/color";

  public static final String STYLE_FONT_FAMILY = STYLE_PREFIX + "/fontFamily";

  public static final String STYLE_FONT_SIZE = STYLE_PREFIX + "/fontSize";

  public static final String STYLE_FONT_STYLE = STYLE_PREFIX + "/fontStyle";

  public static final String STYLE_FONT_WEIGHT = STYLE_PREFIX + "/fontWeight";

  public static final String STYLE_TEXT_DECORATION = STYLE_PREFIX + "/textDecoration";

  public static final String STYLE_VERTICAL_ALIGN = STYLE_PREFIX + "/verticalAlign";

  public static final ReadableStringSet STYLE_KEYS = CollectionUtils.newStringSet(
      AnnotationConstants.STYLE_BG_COLOR,
      AnnotationConstants.STYLE_COLOR,
      AnnotationConstants.STYLE_FONT_FAMILY,
      AnnotationConstants.STYLE_FONT_SIZE,
      AnnotationConstants.STYLE_FONT_STYLE,
      AnnotationConstants.STYLE_FONT_WEIGHT,
      AnnotationConstants.STYLE_TEXT_DECORATION,
      AnnotationConstants.STYLE_VERTICAL_ALIGN
    );

  public static final ReadableStringSet DELETED_STYLE_KEYS = CollectionUtils.newStringSet(
      AnnotationConstants.STYLE_COLOR,
      AnnotationConstants.STYLE_FONT_FAMILY,
      AnnotationConstants.STYLE_FONT_SIZE,
      AnnotationConstants.STYLE_FONT_STYLE,
      AnnotationConstants.STYLE_FONT_WEIGHT,
      AnnotationConstants.STYLE_VERTICAL_ALIGN
    );

  // User

  /** Prefix for user annotations. */
  public static final String USER_PREFIX = "user";

  /** The range of text selected by the user. */
  public static final String USER_RANGE = USER_PREFIX + "/r/";

  /** The user's selection focus, always extends to the document end. */
  public static final String USER_END = USER_PREFIX + "/e/";

  /** User activity annotation, always covers the whole document.  */
  public static final String USER_DATA = USER_PREFIX + "/d/";

  // Links

  /** Prefix for link annotations. */
  public static final String LINK_PREFIX = "link";

  /** Used to denote automatically created links. (e.g. Linky) */
  public static final String LINK_AUTO = LINK_PREFIX + "/auto";

  /** Denotes a user-created link. */
  public static final String LINK_MANUAL = LINK_PREFIX + "/manual";

 /** A link to another wave */
  public static final String LINK_WAVE = LINK_PREFIX + "/wave";

  // Other

  /** Prefix for spelling annotations. (e.g. Spelly) */
  public static final String SPELLY_PREFIX = "spell";

  /** Prefix for language annotations. */
  public static final String LANGUAGE_PREFIX = "lang";

  /** Prefix for translation annotations. (e.g. Rosy) */
  public static final String ROSY_PREFIX = "tr";

  /** An automatically translated section. */
  public static final String ROSY_AUTO = ROSY_PREFIX + "/1";
}
