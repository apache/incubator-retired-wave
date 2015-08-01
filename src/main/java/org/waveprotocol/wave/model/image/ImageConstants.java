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

package org.waveprotocol.wave.model.image;

/**
 * Common image constants.
 */

public final class ImageConstants {
  private ImageConstants() {} // Non-instantiable.

  /** Image tag */
  public static final String TAGNAME ="image";

  /** Image Attachment Attribute. */
  public static final String ATTACHMENT_ATTRIBUTE = "attachment";

  /** Image Style Attribute. */
  public static final String STYLE_ATTRIBUTE = "style";


  /** Caption element tag. */
  public static final String CAPTION_TAGNAME = "caption";

  /** Gadget element tag. */
  public static final String GADGET_TAGNAME = "gadget";

  /** Full style attribute value */
  public static final String STYLE_FULL_VALUE = "full";

  /** Source Prefs Attribute. */
  public static final String[] ATTRIBUTE_NAMES =
      {ATTACHMENT_ATTRIBUTE, STYLE_ATTRIBUTE};
}
