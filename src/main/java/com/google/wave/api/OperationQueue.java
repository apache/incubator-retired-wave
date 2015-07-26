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

package com.google.wave.api;

import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.impl.RawAttachmentData;

import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.media.model.AttachmentId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * A utility class that abstracts the queuing of operations, represented by
 * {@link OperationRequest}, using easily callable functions. The
 * {@link OperationQueue} queues the resulting operations in order.
 *
 * Typically there shouldn't be a need to call this directly unless operations
 * are needed on entities outside of the scope of the robot. For example, to
 * modify a blip that does not exist in the current context, you might specify
 * the wave, wavelet, and blip id to generate an operation.
 *
 * Any calls to this will not be reflected in the robot in any way. For example,
 * calling wavelet_append_blip will not result in a new blip being added to the
 * robot current context, only an operation to be sent to the robot proxy.
 */
public class OperationQueue implements Serializable {

  /** A random number generator for the temporary ids. */
  private static final Random ID_GENERATOR = new Random();

  /** The format of temporary blip ids. */
  private static final String TEMP_BLIP_ID_FORMAT = "TBD_%s_%s";

  /** The format of temporary wave ids. */
  private static final String TEMP_WAVE_ID_FORMAT = "%s!TBD_%s";

  /** The format of wavelet ids. */
  private static final String TEMP_WAVELET_ID_FORMAT = "%s!conv+root";

  /** The format of new operation ids. */
  private static final String OP_ID_FORMAT = "op%d";

  /** Some class global counters. */
  private static long nextOpId = 1;

  /** The id that can be set for {@code proxyingFor} parameter. */
  private final String proxyForId;

  /** The operation queue. */
  private List<OperationRequest> pendingOperations;

  /**
   * Constructor that creates a new instance of {@link OperationQueue} with
   * an empty queue and no proxying information set.
   */
  public OperationQueue() {
    this(new ArrayList<OperationRequest>(), null);
  }

  /**
   * Constructor that creates a new instance of {@link OperationQueue} with
   * a specified proxying information.
   *
   * @param proxyForId the proxying information.
   */
  public OperationQueue(String proxyForId) {
    this(new ArrayList<OperationRequest>(), proxyForId);
  }

  /**
   * Constructor that creates a new instance of {@link OperationQueue} with
   * a specified queue and proxying information.
   *
   * @param operations the underlying operation queue that should be used for
   *     this {@link OperationQueue}.
   * @param proxyForId the proxying information.
   */
  public OperationQueue(List<OperationRequest> operations, String proxyForId) {
    this.pendingOperations = operations;
    this.proxyForId = proxyForId;
  }

  /**
   * Returns a list of the pending operations that have been queued up.
   *
   * @return the pending operations.
   */
  public List<OperationRequest> getPendingOperations() {
    return pendingOperations;
  }

  /**
   * Returns the id for {@code proxyingFor} parameter.
   *
   * @return the proxying id.
   */
  public String getProxyForId() {
    return proxyForId;
  }

  /**
   * Creates a view of this {@link OperationQueue} with the proxying for set to
   * the given id.
   *
   * This method returns a new instance of an operation queue that shares the
   * operation list, but has a different {@link #proxyForId} set so when the
   * robot uses this new queue, subsequent operations will be sent out with the
   * {@code proxying_for} field set.
   *
   * @param proxyForId the proxying information.
   * @return a view of this {@link OperationQueue} with the proxying information
   *     set.
   */
  public OperationQueue proxyFor(String proxyForId) {
    return new OperationQueue(pendingOperations, proxyForId);
  }

  /**
   * Clears this operation queue.
   */
  public void clear() {
    pendingOperations.clear();
  }

  /**
   * Appends a blip to a wavelet.
   *
   * @param wavelet the wavelet to append the new blip to.
   * @param initialContent the initial content of the new blip.
   * @return an instance of {@link Blip} that represents the new blip.
   */
  public Blip appendBlipToWavelet(Wavelet wavelet, String initialContent) {
    Blip newBlip = newBlip(wavelet, initialContent, null, generateTempBlipId(wavelet),
        wavelet.getRootThread().getId());
    appendOperation(OperationType.WAVELET_APPEND_BLIP, wavelet,
        Parameter.of(ParamsProperty.BLIP_DATA, newBlip.serialize()));
    return newBlip;
  }

  /**
   * Adds a participant to a wavelet.
   *
   * @param wavelet the wavelet that the new participant should be added to.
   * @param participantId the id of the new participant.
   */
  public void addParticipantToWavelet(Wavelet wavelet, String participantId) {
    appendOperation(OperationType.WAVELET_ADD_PARTICIPANT_NEWSYNTAX, wavelet,
        Parameter.of(ParamsProperty.PARTICIPANT_ID, participantId));
  }

  /**
   * Removes a participant from a wavelet.
   *
   * @param wavelet the wavelet that the participant should be removed from.
   * @param participantId the id of the participant to be removed.
   */
  public void removeParticipantFromWavelet(Wavelet wavelet, String participantId) {
    appendOperation(OperationType.WAVELET_REMOVE_PARTICIPANT_NEWSYNTAX, wavelet,
        Parameter.of(ParamsProperty.PARTICIPANT_ID, participantId));
  }

  /**
   * Creates a new wavelet.
   *
   * @param domain the domain to create the wavelet in.
   * @param participants the initial participants on this new wavelet.
   * @return an instance of {@link Wavelet} that represents the new wavelet.
   */
  public Wavelet createWavelet(String domain, Set<String> participants) {
    return createWavelet(domain, participants, "");
  }

  /**
   * Creates a new wavelet with an optional message.
   *
   * @param domain the domain to create the wavelet in.
   * @param participants the initial participants on this new wavelet.
   * @param message an optional payload that is returned with the corresponding
   *     event.
   * @return an instance of {@link Wavelet} that represents the new wavelet.
   */
  public Wavelet createWavelet(String domain, Set<String> participants, String message) {
    Wavelet newWavelet = newWavelet(domain, participants, this);
    OperationRequest operation = appendOperation(OperationType.ROBOT_CREATE_WAVELET,
        newWavelet, Parameter.of(ParamsProperty.WAVELET_DATA, newWavelet.serialize()));

    // Don't add the message if it's null or empty.
    if (message != null && !message.isEmpty()) {
      operation.addParameter(Parameter.of(ParamsProperty.MESSAGE, message));
    }
    return newWavelet;
  }

  /**
   * Appends search operation for specified query.
   *
   * @param query the query to execute.
   * @param index the index from which to return results.
   * @param numresults the number of results to return.
   */
  public void search(String query, Integer index, Integer numresults) {
    Parameter queryParam = Parameter.of(ParamsProperty.QUERY, query);
    Parameter indexParam = Parameter.of(ParamsProperty.INDEX, index);
    Parameter numresultsParam = Parameter.of(ParamsProperty.NUM_RESULTS, numresults);
    appendOperation(OperationType.ROBOT_SEARCH, queryParam, indexParam, numresultsParam);
  }

  /**
   * Sets a key-value pair on the data document of a wavelet.
   *
   * @param wavelet to set the data document on.
   * @param name the name of this data.
   * @param value the value of this data.
   */
  public void setDatadocOfWavelet(Wavelet wavelet, String name, String value) {
    appendOperation(OperationType.WAVELET_SET_DATADOC, wavelet,
        Parameter.of(ParamsProperty.DATADOC_NAME, name),
        Parameter.of(ParamsProperty.DATADOC_VALUE, value));
  }

  /**
   * Requests a snapshot of the specified wave.
   *
   * @param waveId the id of the wave that should be fetched.
   * @param waveletId the wavelet id that should be fetched.
   */
  public void fetchWavelet(WaveId waveId, WaveletId waveletId) {
    appendOperation(OperationType.ROBOT_FETCH_WAVE, waveId, waveletId, null);
  }

  /**
   * Retrieves list of wave wavelets ids.
   *
   * @param waveId the id of the wave.
   */
  public void retrieveWaveletIds(WaveId waveId) {
    appendOperation(OperationType.ROBOT_FETCH_WAVE, waveId, null, null,
        Parameter.of(ParamsProperty.RETURN_WAVELET_IDS, true));
  }

  /**
   * Exports snapshot of wavelet.
   *
   * @param waveId the id of the wave that should be exported.
   * @param waveletId the id of the wavelet that should be exported.
   */
  public void exportSnapshot(WaveId waveId, WaveletId waveletId) {
    appendOperation(OperationType.ROBOT_EXPORT_SNAPSHOT, waveId, waveletId, null);
  }

  /**
   * Exports deltas of wavelet.
   *
   * @param waveId the id of the wave that should be exported.
   * @param waveletId the id of the wavelet that should be exported.
   * @param fromVersion start version.
   * @param toVersion to version.
   */
  public void exportRawDeltas(WaveId waveId, WaveletId waveletId,
      byte[] fromVersion, byte[] toVersion) {
    appendOperation(OperationType.ROBOT_EXPORT_DELTAS, waveId, waveletId, null,
        Parameter.of(ParamsProperty.FROM_VERSION, fromVersion),
        Parameter.of(ParamsProperty.TO_VERSION, toVersion));
  }

  /**
   * Export attachment.
   *
   * @param attachmentId the id of attachment.
   */
  public void exportAttachment(AttachmentId attachmentId) {
    appendOperation(OperationType.ROBOT_EXPORT_ATTACHMENT,
        Parameter.of(ParamsProperty.ATTACHMENT_ID, attachmentId.serialise()));
  }

  /**
   * Imports deltas of wavelet.
   *
   * @param waveId the id of the wave that should be imported.
   * @param waveletId the id of the wavelet that should be imported.
   * @param history the history in deltas.
   */
  public void importRawDeltas(WaveId waveId, WaveletId waveletId, List<byte[]> history) {
    appendOperation(OperationType.ROBOT_IMPORT_DELTAS,
        waveId, waveletId, null, Parameter.of(ParamsProperty.RAW_DELTAS, history));
  }

  /**
   * Imports attachment.
   *
   * @param waveId the id of the wave that should be imported.
   * @param waveletId the id of the wavelet that should be imported.
   * @param attachmentId the id of attachment.
   * @param attachmentData the attachment data.
   */
  public void importAttachment(WaveId waveId, WaveletId waveletId,
      AttachmentId attachmentId, RawAttachmentData attachmentData) {
    appendOperation(OperationType.ROBOT_IMPORT_ATTACHMENT,
        waveId, waveletId, null,
        Parameter.of(ParamsProperty.ATTACHMENT_ID, attachmentId.serialise()),
        Parameter.of(ParamsProperty.ATTACHMENT_DATA, attachmentData));
  }

  /**
   * Sets the title of a wavelet.
   *
   * @param wavelet the wavelet whose title will be changed.
   * @param title the new title to be set.
   */
  public void setTitleOfWavelet(Wavelet wavelet, String title) {
    appendOperation(OperationType.WAVELET_SET_TITLE, wavelet,
        Parameter.of(ParamsProperty.WAVELET_TITLE, title));
  }

  /**
   * Modifies a tag in a wavelet.
   *
   * @param wavelet the wavelet to modify the tag from.
   * @param tag the name of the tag to be modified
   * @param modifyHow how to modify the tag. The default behavior is to add the
   *     tag. Specify {@code remove} to remove, or specify {@code null} or
   *     {@code add} to add.
   */
  public void modifyTagOfWavelet(Wavelet wavelet, String tag, String modifyHow) {
    appendOperation(OperationType.WAVELET_MODIFY_TAG, wavelet,
        Parameter.of(ParamsProperty.NAME, tag),
        Parameter.of(ParamsProperty.MODIFY_HOW, modifyHow));
  }

  /**
   * Creates a child blip of another blip.
   *
   * @param blip the parent blip.
   * @return an instance of {@link Blip} that represents the new child blip.
   */
  public Blip createChildOfBlip(Blip blip) {
    // Create a new thread.
    String tempBlipId = generateTempBlipId(blip.getWavelet());
    Wavelet wavelet = blip.getWavelet();
    BlipThread thread = new BlipThread(tempBlipId, -1, new ArrayList<String>(),
        wavelet.getBlips());

    // Add the new thread to the blip and wavelet.
    blip.addThread(thread);
    wavelet.addThread(thread);

    // Create a new blip in the new thread.
    Blip newBlip = newBlip(blip.getWavelet(), "", blip.getBlipId(), tempBlipId, thread.getId());
    appendOperation(OperationType.BLIP_CREATE_CHILD, blip,
        Parameter.of(ParamsProperty.BLIP_DATA, newBlip.serialize()));
    return newBlip;
  }

  /**
   * Appends a new blip to the end of the thread of the given blip.
   *
   * @param blip the blip whose thread will be appended.
   * @return an instance of {@link Blip} that represents the new blip.
   */
  public Blip continueThreadOfBlip(Blip blip) {
    Blip newBlip = newBlip(blip.getWavelet(), "", blip.getParentBlipId(),
        generateTempBlipId(blip.getWavelet()), blip.getThread().getId());
    appendOperation(OperationType.BLIP_CONTINUE_THREAD, blip,
        Parameter.of(ParamsProperty.BLIP_DATA, newBlip.serialize()));
    return newBlip;
  }

  /**
   * Deletes the specified blip.
   *
   * @param wavelet the wavelet that owns the blip.
   * @param blipId the id of the blip that will be deleted.
   */
  public void deleteBlip(Wavelet wavelet, String blipId) {
    appendOperation(OperationType.BLIP_DELETE, wavelet.getWaveId(), wavelet.getWaveletId(),
        blipId);
  }

  /**
   * Appends content with markup to a blip.
   *
   * @param blip the blip where this markup content should be added to.
   * @param content the markup content that should be added to the blip.
   */
  public void appendMarkupToDocument(Blip blip, String content) {
    appendOperation(OperationType.DOCUMENT_APPEND_MARKUP, blip,
        Parameter.of(ParamsProperty.CONTENT, content));
  }

  /**
   * Submits this operation queue when the given {@code other} operation queue
   * is submitted.
   *
   * @param other the other operation queue to merge this operation queue with.
   */
  public void submitWith(OperationQueue other) {
    other.pendingOperations.addAll(this.pendingOperations);
    this.pendingOperations = other.pendingOperations;
  }

  /**
   * Creates and queues a document modify operation.
   *
   * @param blip the blip to modify.
   * @return an instance of {@code OperationRequest} that represents this
   *     operation. The caller of this method should append the required and/or
   *     optional parameters, such as:
   *     <ul>
   *       <li>{@code modifyAction}</li>
   *       <li>{@code modifyQuery}</li>
   *       <li>{@code index}</li>
   *       <li>{@code range}</li>
   *     </ul>
   */
  public OperationRequest modifyDocument(Blip blip) {
    return appendOperation(OperationType.DOCUMENT_MODIFY, blip);
  }

  /**
   * Inserts a new inline blip at a specified location.
   *
   * @param blip the blip to anchor this inline blip from.
   * @param position the position in the given blip to insert this new inline
   *     blip.
   * @return an instance of {@link Blip} that represents the inline blip.
   */
  public Blip insertInlineBlipToDocument(Blip blip, int position) {
    // Create a new thread.
    String tempBlipId = generateTempBlipId(blip.getWavelet());
    Wavelet wavelet = blip.getWavelet();
    BlipThread thread = new BlipThread(tempBlipId, position, new ArrayList<String>(),
        wavelet.getBlips());

    // Add the new thread to the blip and wavelet.
    blip.addThread(thread);
    wavelet.addThread(thread);

    // Create a new blip in the new thread.
    Blip inlineBlip = newBlip(blip.getWavelet(), "", blip.getBlipId(), tempBlipId,
        thread.getId());
    appendOperation(OperationType.DOCUMENT_INSERT_INLINE_BLIP, blip,
        Parameter.of(ParamsProperty.INDEX, position),
        Parameter.of(ParamsProperty.BLIP_DATA, inlineBlip.serialize()));
    return inlineBlip;
  }

  /**
   * Creates and appends a new operation to the operation queue.
   *
   * @param opType the type of the operation.
   * @param parameters the parameters that should be added as a property of
   *     the operation.
   * @return an instance of {@link OperationRequest} that represents the queued
   *     operation.
   */
  OperationRequest appendOperation(OperationType opType, Parameter... parameters) {
    return appendOperation(opType, null, null, null, parameters);
  }

  /**
   * Creates and appends a new operation to the operation queue.
   *
   * @param opType the type of the operation.
   * @param wavelet the wavelet to apply the operation to.
   * @param parameters the parameters that should be added as a property of
   *     the operation.
   * @return an instance of {@link OperationRequest} that represents the queued
   *     operation.
   */
  OperationRequest appendOperation(OperationType opType, Wavelet wavelet,
      Parameter... parameters) {
    return appendOperation(opType, wavelet.getWaveId(), wavelet.getWaveletId(), null, parameters);
  }

  /**
   * Creates and appends a new operation to the operation queue.
   *
   * @param opType the type of the operation.
   * @param blip the blip to apply this operation to.
   * @param parameters the parameters that should be added as a property of
   *     the operation.
   * @return an instance of {@link OperationRequest} that represents the queued
   *     operation.
   */
  OperationRequest appendOperation(OperationType opType, Blip blip, Parameter... parameters) {
    return appendOperation(opType, blip.getWaveId(), blip.getWaveletId(), blip.getBlipId(),
        parameters);
  }

  /**
   * Creates and appends a new operation to the operation queue.
   *
   * @param opType the type of the operation.
   * @param waveId the wave id in which the operation should be applied to.
   * @param waveletId the wavelet id of the given wave in which the operation
   *     should be applied to.
   * @param blipId the optional blip id of the given wave in which the operation
   *     should be applied to. Not all operations require blip id.
   * @param parameters the parameters that should be added as a property of
   *     the operation.
   * @return an instance of {@link OperationRequest} that represents the queued
   *     operation.
   */
  OperationRequest appendOperation(OperationType opType, WaveId waveId, WaveletId waveletId,
      String blipId, Parameter... parameters) {
    return addOperation(opType, waveId, waveletId, blipId, pendingOperations.size(), parameters);
  }

  /**
   * Creates and prepends a new operation to the operation queue.
   *
   * @param opType the type of the operation.
   * @param waveId the wave id in which the operation should be applied to.
   * @param waveletId the wavelet id of the given wave in which the operation
   *     should be applied to.
   * @param blipId the optional blip id of the given wave in which the operation
   *     should be applied to. Not all operations require blip id.
   * @param parameters the parameters that should be added as a property of
   *     the operation.
   * @return an instance of {@link OperationRequest} that represents the queued
   *     operation.
   */
  OperationRequest prependOperation(OperationType opType, WaveId waveId, WaveletId waveletId,
      String blipId, Parameter... parameters) {
    return addOperation(opType, waveId, waveletId, blipId, 0, parameters);
  }

  /**
   * Creates and adds a new operation to the operation queue.
   *
   * @param opType the type of the operation.
   * @param waveId the wave id in which the operation should be applied to.
   * @param waveletId the wavelet id of the given wave in which the operation
   *     should be applied to.
   * @param blipId the optional blip id of the given wave in which the operation
   *     should be applied to. Not all operations require blip id.
   * @param index the index where this new operation should be added to in the
   *     queue.
   * @param parameters the parameters that should be added as a property of
   *     the operation.
   * @return an instance of {@link OperationRequest} that represents the queued
   *     operation.
   */
  OperationRequest addOperation(OperationType opType, WaveId waveId, WaveletId waveletId,
      String blipId, int index, Parameter... parameters) {
    String waveIdString = null;
    if (waveId != null) {
      waveIdString = ApiIdSerializer.instance().serialiseWaveId(waveId);
    }

    String waveletIdString = null;
    if (waveletId != null) {
      waveletIdString = ApiIdSerializer.instance().serialiseWaveletId(waveletId);
    }

    OperationRequest operation = new OperationRequest(opType.method(),
        String.format(OP_ID_FORMAT, nextOpId++),
        waveIdString, waveletIdString, blipId, parameters);

    // Set the proxying for parameter, if necessary.
    if (proxyForId != null && !proxyForId.isEmpty()) {
      operation.addParameter(Parameter.of(ParamsProperty.PROXYING_FOR, proxyForId));
    }

    pendingOperations.add(index, operation);
    return operation;
  }

  /**
   * Generates a temporary blip id.
   *
   * @param wavelet the wavelet to seed the temporary id.
   * @return a temporary blip id.
   */
  private static String generateTempBlipId(Wavelet wavelet) {
    return String.format(TEMP_BLIP_ID_FORMAT,
        ApiIdSerializer.instance().serialiseWaveletId(wavelet.getWaveletId()),
        ID_GENERATOR.nextInt());
  }

  /**
   * Creates a new {@code Blip} object used for this session. A temporary
   * id will be assigned to the newly created {@code Blip} object.
   *
   * @param wavelet the wavelet that owns this blip.
   * @param initialContent the initial content of the new blip.
   * @param parentBlipId the parent of this blip.
   * @return an instance of new {@code Blip} object used for this session.
   */
  private static Blip newBlip(Wavelet wavelet, String initialContent, String parentBlipId,
      String blipId, String threadId) {
    Blip newBlip = new Blip(blipId, initialContent, parentBlipId, threadId, wavelet);
    if (parentBlipId != null) {
      Blip parentBlip = wavelet.getBlips().get(parentBlipId);
      if (parentBlip != null) {
        parentBlip.getChildBlipIds().add(newBlip.getBlipId());
      }
    }
    wavelet.getBlips().put(newBlip.getBlipId(), newBlip);

    BlipThread thread = wavelet.getThread(threadId);
    if (thread != null) {
      thread.appendBlip(newBlip);
    }
    return newBlip;
  }

  /**
   * Creates a new {@code Wavelet} object used for this session. A temporary
   * wave id will be assigned to this newly created {@code Wavelet} object.
   *
   * @param domain the domain that is used for the wave and wavelet ids.
   * @param participants the participants that should be added to the new
   *     wavelet.
   * @param opQueue the operation queue of the new wavelet.
   * @return an instance of new {@code Wavelet} object used for this
   *     session.
   */
  private static Wavelet newWavelet(String domain, Set<String> participants,
      OperationQueue opQueue) {
    // Make sure that participant list is not null;
    if (participants == null) {
      participants = Collections.emptySet();
    }

    WaveId waveId;
    WaveletId waveletId;
    try {
      waveId = ApiIdSerializer.instance().deserialiseWaveId(
          String.format(TEMP_WAVE_ID_FORMAT, domain, ID_GENERATOR.nextInt()));
      waveletId = ApiIdSerializer.instance().deserialiseWaveletId(
          String.format(TEMP_WAVELET_ID_FORMAT, domain));
    } catch (InvalidIdException e) {
      throw new IllegalStateException("Invalid temporary id", e);
    }

    String rootBlipId = String.format(TEMP_BLIP_ID_FORMAT,
        ApiIdSerializer.instance().serialiseWaveletId(waveletId),
        ID_GENERATOR.nextInt());
    Map<String, Blip> blips = new HashMap<String, Blip>();
    Map<String, String> roles = new HashMap<String, String>();
    Map<String, BlipThread> threads = new HashMap<String, BlipThread>();

    List<String> blipIds = new ArrayList<String>();
    blipIds.add(rootBlipId);
    BlipThread rootThread = new BlipThread("", -1, blipIds, blips);

    Wavelet wavelet = new Wavelet(waveId, waveletId, rootBlipId, rootThread, participants,
        roles, blips, threads, opQueue);

    Blip rootBlip = new Blip(rootBlipId, "", null, "", wavelet);
    blips.put(rootBlipId, rootBlip);

    return wavelet;
  }

  /**
   * Modifies the role of a participant in a wavelet.
   *
   * @param wavelet the wavelet that the participant is on
   * @param participant whose role to modify
   * @param role to set for the participant
   */
  public void modifyParticipantRoleOfWavelet(Wavelet wavelet, String participant, String role) {
    appendOperation(OperationType.WAVELET_MODIFY_PARTICIPANT_ROLE, wavelet,
        Parameter.of(ParamsProperty.PARTICIPANT_ID, participant),
        Parameter.of(ParamsProperty.PARTICIPANT_ROLE, role));
  }

  /**
   * Notifies the robot information.
   *
   * @param protocolVersion the wire protocol version of the robot.
   * @param capabilitiesHash the capabilities hash of the robot.
   */
  public void notifyRobotInformation(ProtocolVersion protocolVersion, String capabilitiesHash) {
    prependOperation(OperationType.ROBOT_NOTIFY, null, null, null,
        Parameter.of(ParamsProperty.PROTOCOL_VERSION, protocolVersion.getVersionString()),
        Parameter.of(ParamsProperty.CAPABILITIES_HASH, capabilitiesHash));
  }

  public void fetchProfiles(FetchProfilesRequest request) {
    appendOperation(OperationType.ROBOT_FETCH_PROFILES,
        Parameter.of(ParamsProperty.FETCH_PROFILES_REQUEST, request));
  }
}
