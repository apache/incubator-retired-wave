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

package org.waveprotocol.wave.model.id;

/**
 * Constants useful for the context of ids.
 *
 *
 */
public interface IdConstants {
  /** Conventional separator for tokens in an id. */
  public static final char TOKEN_SEPARATOR = '+';

  // Waves.

  /** Scheme for wave and wavelet URIs. */
  public static final String WAVE_URI_SCHEME = "wave";

  /** Prefix for conversational wave ids. */
  public static final String WAVE_PREFIX = "w";

  /** Prefix for profile wave ids. */
  public static final String PROFILE_WAVE_PREFIX = "prof";

  // Wavelets.

  /** Prefix for conversational wavelet ids. */
  public static final String CONVERSATION_WAVELET_PREFIX = "conv";

  /** Conventional conversation root wavelet id. */
  public static final String CONVERSATION_ROOT_WAVELET =
      CONVERSATION_WAVELET_PREFIX + TOKEN_SEPARATOR + "root";

  /** Prefix for user-data wavelet ids. */
  public static final String USER_DATA_WAVELET_PREFIX = "user";

  // Documents.

  /** Prefix for blip document ids. */
  public static final String BLIP_PREFIX = "b";

  /** Document id of the conversation manifest. */
  public static final String MANIFEST_DOCUMENT_ID = "conversation";

  /** Name of the data document that contains tags information. */
  public static final String TAGS_DOC_ID = "tags";

  /** Prefix for ghost blip document ids. Ghost blips aren't rendered. */
  public static final String GHOST_BLIP_PREFIX = "g";

  /** Prefix of the name of the attachment metadata data document. */
  public static final String ATTACHMENT_METADATA_PREFIX = "attach";

  /** Old metadata document id. TODO: remove. */
  public static final String OLD_METADATA_DOC_ID = "m/metadata";
  
  /** Prefix for robot data document ids. */
  public static final String ROBOT_PREFIX = "r";
  
  /** Document id for wavelet level role assignments. */
  public static final String ROLES_DATA_DOC_ID = "roles";
  
  /** Document id for wavelet-level indexability assignments. */
  public static final String INDEXABILITY_DATA_DOC_ID = "listing";
}
