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

package org.waveprotocol.wave.model.gadget;

/**
 * Common gadget constants.
 *
 */
public final class GadgetConstants {
  private GadgetConstants() {} // Non-instantiable.

  /** Gadget tag */
  public static final String TAGNAME ="gadget";

  /** Gadget ID Attribute. */
  public static final String ID_ATTRIBUTE = "id";

  /** Source Url Attribute. */
  public static final String URL_ATTRIBUTE = "url";

  /** Gadget User Preferences Attribute. */
  public static final String PREFS_ATTRIBUTE = "prefs";

  /** Gadget Title Attribute. */
  public static final String TITLE_ATTRIBUTE = "title";

  /** Wave Gadget State Attribute. */
  public static final String STATE_ATTRIBUTE = "state";

  /** Wave Gadget Author (user who added gadget to wave) Attribute */
  public static final String AUTHOR_ATTRIBUTE = "author";

  /** Last known height of the gadget iframe (for smoother wave reopening). */
  public static final String LAST_KNOWN_HEIGHT_ATTRIBUTE = "height";

  /** Last known width of the gadget iframe. */
  public static final String LAST_KNOWN_WIDTH_ATTRIBUTE = "width";

  /** Cached value of the gadget iframe URL. */
  public static final String IFRAME_URL_ATTRIBUTE = "ifr";

  /** Snippet for wave digest. */
  public static final String SNIPPET_ATTRIBUTE = "snippet";

  /**
   * Reference to extension manifest of the extension that installed the gadget.
   */
  public static final String EXTENSION_ATTRIBUTE = "extension";

  /** Name element tag. */
  public static final String CATEGORY_TAGNAME = "category";

  /** Title element tag. */
  public static final String TITLE_TAGNAME = "title";

  /** State element tag. */
  public static final String STATE_TAGNAME = "state";

  /** Pref element tag. */
  public static final String PREF_TAGNAME = "pref";

  /** Name Attribute used in inner nodes. */
  public static final String KEY_ATTRIBUTE = "name";

  /** Name Attribute used in inner nodes. */
  public static final String VALUE_ATTRIBUTE = "value";

  /** Source Prefs Attribute. */
  public static final String[] ATTRIBUTE_NAMES =
      {URL_ATTRIBUTE, PREFS_ATTRIBUTE, TITLE_ATTRIBUTE, STATE_ATTRIBUTE, AUTHOR_ATTRIBUTE};
}
