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

import com.google.common.annotations.VisibleForTesting;

import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * This class is used to generate Wave and Wavelet ids.
 *
 * The id field is structured as a sequence of '+'-separated tokens.
 * The id field is case sensitive.
 *
 * A wavelet is hosted by a single wave service provider, which may differ from
 * the service provider which allocated the wave id. Common examples are private
 * replies and user-data wavelets for users from a federated domain. Thus, the
 * service provider specified in a wavelet id may differ from the service
 * provider of the wave to which it belongs.
 *
 * @author zdwang@google.com (David Wang)
 */
public class IdGeneratorImpl implements IdGenerator, IdConstants {
  /**
   * An object capable of providing a seed value. The seed value may (but need
   * not) change during the lifetime of the using generator.
   */
  public interface Seed {
    /**
     * Gets the current seed value. The seed value may not contain "*".
     *
     * TODO(anorth): remove "*" restriction after migrating legacy documents.
     *
     * @return seed value.
     */
    String get();
  }

  private final String defaultDomain;
  private final Seed idSeed;

  /**
   * A simple counter over all the id's we've issued to ensure uniqueness.
   * Access to the counter is protected by synchronizing on {@code this}.
   */
  protected int counter = 0;

  public IdGeneratorImpl(String domain, Seed idSeed) {
    this.defaultDomain = domain;
    this.idSeed = idSeed;
  }

  @Override
  @Deprecated
  public String peekBlipId() {
    return build(IdConstants.BLIP_PREFIX, peekUniqueToken());
  }

  //NOTE: These are _NOT_ federation happy. Ensure that your caller is!
  @Override
  public WaveId newWaveId() {
    return WaveId.of(defaultDomain, newId(WAVE_PREFIX));
  }

  @Override
  public WaveletId newConversationRootWaveletId() {
    return WaveletId.of(defaultDomain, CONVERSATION_ROOT_WAVELET);
  }

  @Override
  public WaveletId newConversationWaveletId() {
    return WaveletId.of(defaultDomain, newId(CONVERSATION_WAVELET_PREFIX));
  }

  @Override
  public WaveletId buildConversationRootWaveletId(WaveId waveId) {
    return WaveletId.of(waveId.getDomain(), CONVERSATION_ROOT_WAVELET);
  }

  @Override
  public WaveletId newUserDataWaveletId(String address) {
    // TODO(anorth): Take ParticipantId as a parameter after moving it
    // into model.id package.
    String userDomain = ParticipantId.ofUnsafe(address).getDomain();
    return WaveletId.of(userDomain, build(USER_DATA_WAVELET_PREFIX, address));
  }

  @Override
  public String newDataDocumentId() {
    String newId = newUniqueToken();
    if (IdUtil.isBlipId(newId)) {
      Preconditions.illegalState("generated data document id '" + newId + "' is a blip id");
    }
    return newId;
  }

  @Override
  public String newBlipId() {
    String newId = newId(BLIP_PREFIX);
    if (!IdUtil.isBlipId(newId)) {
      Preconditions.illegalState("generated blip id '" + newId + "' is not a blip id");
    }
    return newId;
  }

  @Override
  public String newId(String namespace) {
    String newId = build(namespace, newUniqueToken());
    if (namespace.equals(BLIP_PREFIX) || namespace.equals(GHOST_BLIP_PREFIX)) {
      if (!IdUtil.isBlipId(newId)) {
        Preconditions.illegalState("generated blip id '" + newId + "' is not a blip id");
      }
    } else {
      if (IdUtil.isBlipId(newId)) {
        Preconditions.illegalState("generated data document id '" + newId + "' is a blip id");
      }
    }
    return newId;
  }

  @Override
  public String newUniqueToken() {
    int unique;
    synchronized (this) {
      unique = counter++;
    }
    return idSeed.get() + base64Encode(unique);
  }

  /**
   * @return a string from a list of tokens by using
   *         {@link IdConstants#TOKEN_SEPARATOR} as delimiters.
   */
  protected static String build(String... tokens) {
    return SimplePrefixEscaper.DEFAULT_ESCAPER.join(TOKEN_SEPARATOR, tokens);
  }

  /**
   * Retrieve the domain component of a user's address.
   *
   * @param address legal address.
   * @throw IllegalArgumentException if the address does not contain a '@' sign.
   */
  protected static String getDomain(String address) {
    int pos = address.indexOf('@');
    if (pos < 0) {
      throw new IllegalArgumentException("Invalid address '" + address + "'");
    }
    return address.substring(pos+1);
  }


  /** The 64 valid web-safe values. */
  private static final char[] WEB64_ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
      .toCharArray();

  /**
   * Base-64 encodes a non-negative integer value in a minimum-length string.
   */
  @SuppressWarnings("fallthrough")
  @VisibleForTesting
  public
  static String base64Encode(int intValue) {
    assert intValue >= 0;
    int numEncodedBytes;
    if (intValue == 0) {
      numEncodedBytes = 1;
    } else {
      numEncodedBytes = (int) Math.ceil((32 - Integer.numberOfLeadingZeros(intValue)) / 6.0);
    }
    StringBuilder encoded = new StringBuilder(numEncodedBytes);
    switch (numEncodedBytes) {
      // Fall through all cases to append additional chars.
      case 6:
        encoded.append(WEB64_ALPHABET[(intValue >> 30) & 0x3F]);
      case 5:
        encoded.append(WEB64_ALPHABET[(intValue >> 24) & 0x3F]);
      case 4:
        encoded.append(WEB64_ALPHABET[(intValue >> 18) & 0x3F]);
      case 3:
        encoded.append(WEB64_ALPHABET[(intValue >> 12) & 0x3F]);
      case 2:
        encoded.append(WEB64_ALPHABET[(intValue >> 6) & 0x3F]);
      default:
        encoded.append(WEB64_ALPHABET[intValue & 0x3F]);
    }
    return encoded.toString();
  }

  /**
   * @return the next result that will be obtained from calling
   *         {@link #newUniqueToken()}.
   */
  protected String peekUniqueToken() {
    int unique;
    synchronized (this) {
      unique = counter;
    }
    return idSeed.get() + base64Encode(unique);
  }

  @Override
  public String getDefaultDomain() {
    return defaultDomain;
  }
}
