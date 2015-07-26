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

package org.waveprotocol.wave.client.wavepanel.view;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.common.util.StringCodec;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Provides an isomorphic mapping between model objects and strings.
 *
 */
public final class ModelIdMapperImpl implements ModelIdMapper {

  // Because of the loosely defined alphabets of input strings (conversation
  // ids, blip ids, participant ids), this code assumes that all characters are
  // permitted in those strings. This means that constructing product values
  // (pairs, triples etc) must be done with linear-cost quoting.

  private final static StringCodec CODEC = StringCodec.INSTANCE;

  /** Component separator, as provided by StringCodec. */
  private final static String SEP = CODEC.free().substring(0, 1);

  /** Model context in which this mapper is mapping. */
  private final ConversationView model;

  /** Prefix for short ids, used for namespacing. */
  private final String prefix;

  /** Counter for the next short id. */
  private int nextId;

  /**
   * 2 String map that maps between longId and shortId.
   * There are 2 invariants that hold
   * longIdToShortIdMap.containKey(longId) ->
   *    shortIdToLongIdMap.get(longIdToShortIdMap.get(longId)).equals(longId)
   *
   * shortIdToLongIdMap.containKey(shortId) ->
   *    longIdToShortIdMap.get(shortIdToLongIdMap.get(shortId)).equals(shortId)
   */
  private final StringMap<String> shortIdToLongIdMap = CollectionUtils.createStringMap();
  private final StringMap<String> longIdToShortIdMap = CollectionUtils.createStringMap();

  @VisibleForTesting
  ModelIdMapperImpl(ConversationView model, String prefix, int nextId) {
    this.model = model;
    this.prefix = prefix;
    this.nextId = nextId;
  }

  /**
   * Creates a new mapper.
   *
   * @param model model universe
   * @param prefix prefix on all short ids, for namespacing
   */
  public static ModelIdMapperImpl create(ConversationView model, String prefix) {
    return new ModelIdMapperImpl(model, prefix, 0);
  }

  /**
   * Creates a new mapper.
   *
   * @param model model universe
   * @param prefix prefix on all short ids, for namespacing
   * @param nextId initial counter for id generation
   */
  public static ModelIdMapperImpl create(ConversationView model, String prefix, int nextId) {
    return new ModelIdMapperImpl(model, prefix, nextId);
  }

  @Override
  public String conversationsId(ConversationView cs) {
    Preconditions.checkArgument(model.equals(cs));
    return prefix;
  }

  @Override
  public String convId(Conversation c) {
    return shorten(c.getId());
  }

  @Override
  public String participantId(Conversation c, ParticipantId p) {
    return shorten(longConvId(c) + SEP + CODEC.encode(p.getAddress()));
  }

  @Override
  public String threadId(ConversationThread t) {
    return shorten(longConvId(t.getConversation()) + SEP + CODEC.encode(t.getId()));
  }

  @Override
  public String blipId(ConversationBlip b) {
    return shorten(longConvId(b.getConversation()) + SEP + CODEC.encode(b.getId()));
  }

  private String longConvId(Conversation c) {
    return CODEC.encode(c.getId());
  }

  @Override
  public ConversationBlip locateBlip(String modelId) {
    String longId = restoreId(modelId);
    String[] parts = split(longId);

    if (parts.length != 2) {
      throw new IllegalArgumentException("Not a blip model id: " + modelId);
    } else {
      Conversation c = model.getConversation(parts[0]);
      return c != null ? c.getBlip(parts[1]) : null;
    }
  }

  @Override
  public ConversationThread locateThread(String modelId) {
    String longId = restoreId(modelId);
    String[] parts = split(longId);

    if (parts.length != 2) {
      throw new IllegalArgumentException("Not a thread model id: " + modelId);
    } else {
      Conversation c = model.getConversation(parts[0]);
      return c != null ? c.getThread(parts[1]) : null;
    }
  }

  @Override
  public Conversation locateConversation(String modelId) {
    return model.getConversation(restoreId(modelId));
  }

  @Override
  public Pair<Conversation, ParticipantId> locateParticipant(String modelId) {
    String longId = restoreId(modelId);
    String[] parts = split(longId);

    if (parts.length != 2) {
      throw new IllegalArgumentException("Not a participant model id: " + modelId);
    } else {
      Conversation c = model.getConversation(parts[0]);
      ParticipantId p = new ParticipantId(parts[1]);
      return Pair.of(c, p);
    }
  }

  private static String[] split(String domId) {
    // Split with -1, rather than 0, to preserve empty strings, which are
    // legitimated model ids (e.g., the root thread of a conversation has an
    // empty id).
    String [] parts = domId.split(SEP, -1);
    for (int i = 0; i < parts.length; i++) {
      parts[i] = CODEC.decode(parts[i]);
    }
    return parts;
  }

  /**
   * Shortens a long id into a short id. If there is already a mapping for
   * {@code longId}, the existing short id is returned. Otherwise, a new short
   * id is generated and returned.
   */
  @VisibleForTesting
  public String shorten(String longId) {
    String shortId = longIdToShortIdMap.get(longId);
    if (shortId == null) {
      shortId = prefix + nextId++;
      registerIdPair(shortId, longId);
    }
    return shortId;
  }

  /**
   * Restores a shortId back to a longId.
   */
  @VisibleForTesting
  public String restoreId(String shortId) {
    return shortIdToLongIdMap.get(shortId);
  }

  /**
   * Registers an id pair.
   *
   * @param shortId short id (arbitrary)
   * @param longId long id (must conform to the grammar for model ids).
   */
  public void registerIdPair(String shortId, String longId) {
    Preconditions.checkArgument(!shortIdToLongIdMap.containsKey(shortId) &&
        !longIdToShortIdMap.containsKey(longId), "Id already exist");
    shortIdToLongIdMap.put(shortId, longId);
    longIdToShortIdMap.put(longId, shortId);
  }

  /**
   * Applies a callback to each pair (shortId, longId) in the mapping.
   *
   * @param callback  callback to invoke on each pair.
   */
  public void forEachPair(ProcV<? super String> callback) {
    shortIdToLongIdMap.each(callback);
  }

  /** @return the counter to use for the next short id. */
  public int getNextId() {
    return nextId;
  }

  /** @return the prefix to prepend on the next short id. */
  public String getPrefix() {
    return prefix;
  }
}
