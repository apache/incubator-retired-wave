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

import org.waveprotocol.wave.model.wave.ParticipantId;


/**
 * This class holds useful utilities for ids.
 *
 * @author zdwang@google.com (David Wang)
 */
public class IdUtil implements IdConstants {

  protected IdUtil() {
  }

  /**
   * Tests whether a wavelet id is of a conversation root wavelet.
   */
  public static boolean isConversationRootWaveletId(WaveletId waveletId) {
    return waveletId.getId().equals(CONVERSATION_ROOT_WAVELET)
        || isLegacyConversationRootWaveletId(waveletId);
  }

  /**
   * Tests whether a wavelet id is a legacy conversation root wavelet.
   */
  private static boolean isLegacyConversationRootWaveletId(WaveletId id) {
    return id.getId().equals("conversation/root");
  }

  /**
   * Tests whether an wavelet id is of a conversation wavelet.
   */
  public static boolean isConversationalId(WaveletId waveletId) {
    return getInitialToken(waveletId.getId()).equals(CONVERSATION_WAVELET_PREFIX)
        || isLegacyConversationalId(waveletId);
  }

  /**
   * Tests whether a wavelet id is of a legacy conversational wavelet.
   */
  private static boolean isLegacyConversationalId(WaveletId id) {
    // NOTE(user): Legacy private-replies have random ids, and do not
    // necessarily follow the id schema of starting with the conversation
    // prefix. Therefore, an id without a prefix is considered to be
    // conversational.
    return getInitialToken(id.getId()).isEmpty();
  }

  /**
   * Tests whether a document id is a blip id. A legacy "ghost" blip id is a
   * blip id.
   */
  public static boolean isBlipId(String id) {
    return hasInitialTokenUnescaped(id, BLIP_PREFIX) || isGhostBlipId(id) || isLegacyBlipId(id);
  }

  /**
   * Tests whether a document id is a legacy blip id.
   */
  private static final boolean isLegacyBlipId(String id) {

    // NOTE(anorth): legacy blips have random ids containing "*"; data
    // documents from this time have a few well-known prefixes.
    return id.contains("*") && !id.startsWith("m/") && !id.startsWith("attach+") && !id
        .startsWith("spell+");
  }

  /**
   * Tests whether a document id is a ghost blip document id.
   */
  public static final boolean isGhostBlipId(String id) {
    return hasInitialTokenUnescaped(id, GHOST_BLIP_PREFIX);
  }

  /**
   * Tests whether a document id is an attachment data document id.
   */
  public static boolean isAttachmentDataDocument(String id) {
    return hasInitialTokenUnescaped(id, ATTACHMENT_METADATA_PREFIX)
        || isLegacyAttachmentDataDocumentId(id);
  }

  private static boolean isLegacyAttachmentDataDocumentId(String id) {
    return id.startsWith("m/attachment/");
  }

  /**
   * Tests whether a document id is a manifest document ID.
   */
  public static boolean isManifestDocument(String id) {
    return id.equals(MANIFEST_DOCUMENT_ID);
  }

  /**
   * Tests whether a document id is a tags document ID.
   */
  public static boolean isTagsDataDocument(String id) {
    return id.equals(TAGS_DOC_ID);
  }

  /**
   * Tests whether a document id is a robot document id.
   */
  public static boolean isRobotDocId(String documentId) {
    return hasInitialTokenUnescaped(documentId, ROBOT_PREFIX);
  }

  /**
   * Builds user data wavelet id.
   */
  public static WaveletId buildUserDataWaveletId(ParticipantId participant) {
    WaveletId udwId =
        WaveletId.of(participant.getDomain(),
            IdUtil.join(IdConstants.USER_DATA_WAVELET_PREFIX, participant.getAddress()));
    return udwId;
  }

  /**
   * Gets the address that a user data wavelet ID is for.
   *
   * @return UDW address, or {@code null} if the ID is not a user data wavelet
   *         ID.
   */
  public static final String getUserDataWaveletAddress(WaveletId waveletId) {
    String[] parts = split(waveletId.getId());
    if ((parts.length != 2) || !parts[0].equals(USER_DATA_WAVELET_PREFIX)) {
      return null;
    }
    return parts[1];
  }

  /**
   * Tests if a wavelet id is the id of a user's user-data wavelet.
   *
   * @param waveletId
   * @return true if {@code waveletId} is the waveletId of an user-data wavelet.
   */
  public static final boolean isUserDataWavelet(WaveletId waveletId) {
    return hasInitialTokenUnescaped(waveletId.getId(), USER_DATA_WAVELET_PREFIX);
  }

  /**
   * Tests if a wavelet id is the id of a user's user-data wavelet.
   *
   * @return true if {@code waveletId} is the waveletId of the user-data
   *         wavelet of {@code address}.
   */
  public static final boolean isUserDataWavelet(String address, WaveletId waveletId) {
    return address.equals(getUserDataWaveletAddress(waveletId));
  }

  /**
   * Extracts the initial token of an id followed by the
   * {@link IdConstants#TOKEN_SEPARATOR} or an empty string if the separator was
   * not found.
   *
   * @param id
   * @return the initial token of the id, or an empty string if no separator was
   *         found.
   */
  public static String getInitialToken(String id) {
    String[] tokens = SimplePrefixEscaper.DEFAULT_ESCAPER.split(TOKEN_SEPARATOR, id);
    if (tokens.length > 1) {
      return tokens[0];
    } else {
      return "";
    }
  }

  /**
   * Tests whether a string has an initial token followed by
   * {@link IdConstants#TOKEN_SEPARATOR} without un-escaping the token first.
   * The token must not contain the separator.
   */
  public static boolean hasInitialTokenUnescaped(String id, String token) {
    // Ensure token doesn't contain the separator so startsWith is safe.
    assert token.indexOf(TOKEN_SEPARATOR) == -1;
    return id.startsWith(token + TOKEN_SEPARATOR);
  }

  /** Splits an id into an array of tokens on {@link IdConstants#TOKEN_SEPARATOR}. */
  public static String[] split(String id) {
    try {
      return SimplePrefixEscaper.DEFAULT_ESCAPER.split(TOKEN_SEPARATOR, id);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Joins an array of tokens with {@link IdConstants#TOKEN_SEPARATOR} into an
   * id string.
   */
  public static String join(String... tokens) {
    return SimplePrefixEscaper.DEFAULT_ESCAPER.join(TOKEN_SEPARATOR, tokens);
  }
}
