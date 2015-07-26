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

package org.waveprotocol.box.server.robots;

import com.google.common.collect.ImmutableSet;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.event.Event;

import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.box.common.Receiver;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.Map;

/**
 * Context for performing robot operations.
 *
 * <p>
 * {@link OperationContext} throws {@link InvalidRequestException} because it is
 * expected to be used together with an {@link OperationRequest}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public interface OperationContext {

  /** Marks temporary wave and blip ID's since V2 */
  final String TEMP_ID_MARKER = "TBD_";

  /**
   * @return true iff this context is bound to a wavelet.
   */
  boolean isBound();

  /**
   * @param operationId the id of the robot operation.
   * @return True iff a response has been set for the given id.
   */
  boolean hasResponse(String operationId);

  /**
   * Constructs a response with the given data in its payload field.
   *
   * @param data the data to be put in the repsonse.
   */
  void constructResponse(OperationRequest operation, Map<ParamsProperty, Object> data);

  /**
   * Constructs and stores a response signifying an error to be put in the
   * context.
   *
   * @param errorMessage the error message to be put in the response.
   */
  void constructErrorResponse(OperationRequest operation, String errorMessage);

  /**
   * Processes the event and sets the proper response.
   *
   * @param event the event to process.
   * @throws InvalidRequestException If the event could not be properly
   *         processed.
   */
  void processEvent(OperationRequest operation, Event event) throws InvalidRequestException;

  /**
   * Stores a reference from a temporary wavelet id to a real wavelet id. If the
   * given id is not a temporary id no reference will be stored.
   *
   * @param waveId the wave id.
   * @param waveletId the wavelet id.
   * @param newWavelet the new wavelet to remember.
   */
  void putWavelet(WaveId waveId, WaveletId waveletId, RobotWaveletData newWavelet);

  /**
   * Opens a wavelet for the given wave id and wavelet id. Note: Usually if the
   * wavelet for specified wavelet id doesn't exist - the method returns
   * null. However, for user data wavelets the method will create a new empty one
   * and return it.
   *
   * @param waveId the wave id of the wavelet to open.
   * @param waveletId the wavelet id of the wavelet to open.
   * @param participant the id of the participant that wants to open the
   *        wavelet.
   * @throws InvalidRequestException if the wavelet can not be opened.
   */
  OpBasedWavelet openWavelet(WaveId waveId, WaveletId waveletId, ParticipantId participant)
      throws InvalidRequestException;

  /**
   * Opens the wavelet specified in the given operation. Note: Usually if the
   * wavelet for specified wavelet id doesn't exist - the method returns
   * null. However, for user data wavelets the method will create a new empty one
   * and return it.
   *
   * @param operation the operation specifying which wavelet to open.
   * @param participant the id of the participant that wants to open the
   *        wavelet.
   * @throws InvalidRequestException if the wavelet can not be opened or the
   *         operation does not define the wave and wavelet id.
   */
  OpBasedWavelet openWavelet(OperationRequest operation, ParticipantId participant)
      throws InvalidRequestException;

  /**
   * Gets the conversation for of wavelet for the given wave id and wavelet id.
   * Tries to retrieve and open the wavelet if that has not already been done.
   *
   * @param waveId the wave id of the wavelet.
   * @param waveletId the wavelet id of.
   * @param participant the id of the participant that wants to operation on the
   *        conversation.
   * @throws InvalidRequestException if the wavelet can not be opened.
   */
  ObservableConversationView openConversation(
      WaveId waveId, WaveletId waveletId, ParticipantId participant) throws InvalidRequestException;

  /**
   * Gets the conversation for of wavelet specified in the operation. Tries to
   * retrieve and open the wavelet if that has not already been done.
   *
   * @param operation the operation specifying which wavelet to get the
   *        conversation for.
   * @param participant the id of the participant that wants to operation on the
   *        conversation.
   * @throws InvalidRequestException if the wavelet can not be opened or the
   *         operation does not define the wave and wavelet id.
   */
  ObservableConversationView openConversation(OperationRequest operation, ParticipantId participant)
      throws InvalidRequestException;

  /**
   * Stores a reference from a temporary blip id to a real blip id. If the given
   * id is not a temporary id it will be ignored.
   *
   * @param blipId the temporary blip id.
   * @param newBlip the blip that this id should reference.
   */
  void putBlip(String blipId, ConversationBlip newBlip);

  /**
   * Retrieve a blip with the given, possible temporary id, from the
   * conversation.
   *
   * @param conversation the conversation the blip belongs to.
   * @param blipId the id of the blip, may be be a temporary id.
   * @throws InvalidRequestException if the blip could not be retrieved or has
   *         been deleted.
   */
  ConversationBlip getBlip(Conversation conversation, String blipId) throws InvalidRequestException;

  /**
   * @return the converter to convert to API objects
   */
  EventDataConverter getConverter();

  /**
   * Returns {@link ConversationUtil} which is used to generate conversations
   * and ids.
   */
  ConversationUtil getConversationUtil();

  /**
   * Gets the list of wavelet Ids that are visible to the user.
   *
   * @param operation the operation specifying wave.
   * @param participant the user.
   * @return set of wavelet Ids, visible to the user.
   */
  ImmutableSet<WaveletId> getVisibleWaveletIds(OperationRequest operation, ParticipantId participant)
      throws InvalidRequestException;

  /**
   * Takes snapshot of a wavelet, checking access for the given participant.
   *
   * @param waveletName the wavelet name of the wavelet to get.
   * @param participant the user.
   * @return snapshot on success, null on failure
   */
  CommittedWaveletSnapshot getWaveletSnapshot(WaveletName waveletName, ParticipantId participant)
      throws InvalidRequestException;

  /**
   * Takes deltas history of a wavelet, checking access for the given participant.
   *
   * @param waveletName the wavelet name of the wavelet to get.
   * @param participant the user.
   * @param fromVersion start version (inclusive), minimum 0.
   * @param toVersion start version (exclusive).
   * @param receiver the transformed deltas receiver.
   */
  void getDeltas(WaveletName waveletName, ParticipantId participant,
      HashedVersion fromVersion, HashedVersion toVersion, Receiver<TransformedWaveletDelta> receiver)
      throws InvalidRequestException;
}
