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
public interface IdGenerator {

  /**
   * Creates a new unique wave id.
   *
   * Conversational waves (all the waves we have today) are specified by a leading
   * token 'w' followed by a pseudo-random string, e.g. w+3dKS9cD.
   */
  WaveId newWaveId();

  /**
   * Creates a new unique wavelet id.
   *
   * Conversational wavelets (those expected to render in a wave client) are
   * specified by a leading token "conv" followed by a psuedorandom string,
   * e.g. conv+3sG7. The distinguished root conversation wavelet is
   * identified by conv+root.
   */
  WaveletId newConversationWaveletId();

  /**
   * Create a new conversation root wavelet id.
   *
   * The conversation root wavelet has id "conv+root".
   */
  WaveletId newConversationRootWaveletId();

  /**
   * Creates/gets a root wavelet id for the given waveId
   * (Federation happy version of newConversationRootWaveletId)
   */

  WaveletId buildConversationRootWaveletId(WaveId waveId);

  /**
   * Creates a user data wavelet id.
   *
   * Per-user data wavelets are specified by a leading token "user" followed by
   * a service-provider specific user identifier, e.g.
   * "user+michael@initech.com".
   *
   * @param address address of the user that the user data wavelet belongs.
   */
  WaveletId newUserDataWaveletId(String address);

  /**
   * Creates an id for a document that is a blip. Generated blip ids take the
   * form "b+[random-token]".
   */
  String newBlipId();

  /**
   * Peeks at what the next blip id will be, providing no other ids are
   * generated.
   *
   * HACK(user): this is required by some legacy client code.
   */
  @Deprecated
  String peekBlipId();

  /**
   * Creates an id for a document that is used to store arbitrary data.
   */
  String newDataDocumentId();

  /**
   * Creates a short, globally unique string token. The token will be distinct
   * from any produced in wave/wavelet/blip/document ids from any instance of
   * this generator.
   */
  String newUniqueToken();

  /**
   * Creates a unique string id with an initial namespacing token. Generated ids
   * take the form "[namespace]+[random-token]".
   *
   * @param namespace initial token for the id
   */
  String newId(String namespace);

  /**
   * @return the wave service provider domain name (e.g. google.com) to
   *     associate with generated ids.
   */
  String getDefaultDomain();
}
