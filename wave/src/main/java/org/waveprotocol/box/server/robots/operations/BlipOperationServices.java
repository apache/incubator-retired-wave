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

package org.waveprotocol.box.server.robots.operations;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.wave.api.ApiIdSerializer;
import com.google.wave.api.BlipData;
import com.google.wave.api.Element;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.data.ApiView;
import com.google.wave.api.event.WaveletBlipCreatedEvent;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

/**
 * {@link OperationService} for methods that create or deletes a blip.
 *
 * <p>
 * These methods are:
 * <li>{@link OperationType#BLIP_CONTINUE_THREAD}</li>
 * <li>{@link OperationType#BLIP_CREATE_CHILD}</li>
 * <li>{@link OperationType#WAVELET_APPEND_BLIP}</li>
 * <li>{@link OperationType#DOCUMENT_APPEND_INLINE_BLIP}</li>
 * <li>{@link OperationType#DOCUMENT_APPEND_MARKUP}</li>
 * <li>{@link OperationType#DOCUMENT_INSERT_INLINE_BLIP}</li>
 * <li>{@link OperationType#DOCUMENT_INSERT_INLINE_BLIP_AFTER_ELEMENT}</li>
 * <li>{@link OperationType#BLIP_DELETE}</li>.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class BlipOperationServices implements OperationService {

  private BlipOperationServices() {
  }

  @Override
  public void execute(
      OperationRequest operation, OperationContext context, ParticipantId participant)
      throws InvalidRequestException {
    OpBasedWavelet wavelet = context.openWavelet(operation, participant);
    ObservableConversationView conversationView = context.openConversation(operation, participant);

    String waveletId = OperationUtil.getRequiredParameter(operation, ParamsProperty.WAVELET_ID);
    String conversationId;

    try {
      // TODO(anorth): Remove this round-trip when the API instead talks about
      // opaque conversation ids, and doesn't use legacy id serialization.
      conversationId = WaveletBasedConversation.idFor(
          ApiIdSerializer.instance().deserialiseWaveletId(waveletId));
    } catch (InvalidIdException e) {
      throw new InvalidRequestException("Invalid conversation id", operation, e);
    }
    ObservableConversation conversation = conversationView.getConversation(conversationId);

    OperationType type = OperationUtil.getOperationType(operation);
    switch (type) {
      case BLIP_CONTINUE_THREAD:
        continueThread(operation, context, participant, conversation);
        break;
      case BLIP_CREATE_CHILD:
        createChild(operation, context, participant, conversation);
        break;
      case WAVELET_APPEND_BLIP:
        appendBlip(operation, context, participant, conversation);
        break;
      case DOCUMENT_APPEND_INLINE_BLIP:
        appendInlineBlip(operation, context, participant, wavelet, conversation);
        break;
      case DOCUMENT_APPEND_MARKUP:
        appendMarkup(operation, context, participant, wavelet, conversation);
        break;
      case DOCUMENT_INSERT_INLINE_BLIP:
        insertInlineBlip(operation, context, participant, wavelet, conversation);
        break;
      case DOCUMENT_INSERT_INLINE_BLIP_AFTER_ELEMENT:
        insertInlineBlipAfterElement(operation, context, participant, wavelet, conversation);
        break;
      case BLIP_DELETE:
        delete(operation, context, participant, conversation);
        break;
      default:
        throw new UnsupportedOperationException(
            "This OperationService does not implement operation of type " + type.method());
    }
  }

  /**
   * Implementation of the {@link OperationType#BLIP_CONTINUE_THREAD} method. It
   * appends a new blip to the end of the thread of the blip specified in the
   * operation.
   *
   * @param operation the operation to execute.
   * @param context the context of the operation.
   * @param participant the participant performing this operation.
   * @param conversation the conversation to operate on.
   * @throws InvalidRequestException if the operation fails to perform
   */
  private void continueThread(OperationRequest operation, OperationContext context,
      ParticipantId participant, ObservableConversation conversation)
      throws InvalidRequestException {
    Preconditions.checkArgument(
        OperationUtil.getOperationType(operation) == OperationType.BLIP_CONTINUE_THREAD,
        "Unsupported operation " + operation);

    BlipData blipData = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_DATA);
    String parentBlipId = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_ID);
    ConversationBlip parentBlip = context.getBlip(conversation, parentBlipId);

    ConversationBlip newBlip = parentBlip.getThread().appendBlip();
    context.putBlip(blipData.getBlipId(), newBlip);

    putContentForNewBlip(newBlip, blipData.getContent());
    processBlipCreatedEvent(operation, context, participant, conversation, newBlip);
  }

  /**
   * Implementation of the {@link OperationType#BLIP_CREATE_CHILD} method. It
   * appends a new reply thread to the blip specified in the operation.
   *
   * @param operation the operation to execute.
   * @param context the context of the operation.
   * @param participant the participant performing this operation.
   * @param conversation the conversation to operate on.
   * @throws InvalidRequestException if the operation fails to perform
   */
  private void createChild(OperationRequest operation, OperationContext context,
      ParticipantId participant, ObservableConversation conversation)
      throws InvalidRequestException {
    Preconditions.checkArgument(
        OperationUtil.getOperationType(operation) == OperationType.BLIP_CREATE_CHILD,
        "Unsupported operation " + operation);

    BlipData blipData = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_DATA);
    String parentBlipId = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_ID);
    ConversationBlip parentBlip = context.getBlip(conversation, parentBlipId);

    ConversationBlip newBlip = parentBlip.addReplyThread().appendBlip();
    context.putBlip(blipData.getBlipId(), newBlip);

    putContentForNewBlip(newBlip, blipData.getContent());
    processBlipCreatedEvent(operation, context, participant, conversation, newBlip);
  }

  /**
   * Implementation for the {@link OperationType#WAVELET_APPEND_BLIP} method. It
   * appends a blip at the end of the root thread.
   *
   * @param operation the operation to execute.
   * @param context the context of the operation.
   * @param participant the participant performing this operation.
   * @param conversation the conversation to operate on.
   * @throws InvalidRequestException if the operation fails to perform
   */
  private void appendBlip(OperationRequest operation, OperationContext context,
      ParticipantId participant, ObservableConversation conversation)
      throws InvalidRequestException {
    Preconditions.checkArgument(
        OperationUtil.getOperationType(operation) == OperationType.WAVELET_APPEND_BLIP,
        "Unsupported operation " + operation);

    BlipData blipData = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_DATA);

    ObservableConversationBlip newBlip = conversation.getRootThread().appendBlip();
    context.putBlip(blipData.getBlipId(), newBlip);

    putContentForNewBlip(newBlip, blipData.getContent());
    processBlipCreatedEvent(operation, context, participant, conversation, newBlip);
  }

  /**
   * Implementation for the {@link OperationType#DOCUMENT_APPEND_INLINE_BLIP}
   * method. It appends an inline blip on a new line in the blip specified in
   * the operation.
   *
   * @param operation the operation to execute.
   * @param context the context of the operation.
   * @param participant the participant performing this operation.
   * @param wavelet the wavelet to operate on.
   * @param conversation the conversation to operate on.
   * @throws InvalidRequestException if the operation fails to perform
   */
  private void appendInlineBlip(OperationRequest operation, OperationContext context,
      ParticipantId participant, ObservableWavelet wavelet, ObservableConversation conversation)
      throws InvalidRequestException {
    Preconditions.checkArgument(
        OperationUtil.getOperationType(operation) == OperationType.DOCUMENT_APPEND_INLINE_BLIP,
        "Unsupported operation " + operation);

    BlipData blipData = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_DATA);
    String parentBlipId = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_ID);
    ConversationBlip parentBlip = context.getBlip(conversation, parentBlipId);

    // Append a new, empty line to the doc for the inline anchor.
    Document doc = parentBlip.getContent();
    Doc.E line = LineContainers.appendLine(doc, XmlStringBuilder.createEmpty());

    // Insert new inline thread with the blip at the empty sentence.
    int location = doc.getLocation(Point.after(doc, line));
    ConversationBlip newBlip = parentBlip.addReplyThread(location).appendBlip();
    context.putBlip(blipData.getBlipId(), newBlip);

    putContentForNewBlip(newBlip, blipData.getContent());
    processBlipCreatedEvent(operation, context, participant, conversation, newBlip);
  }

  /**
   * Implementation for the {@link OperationType#DOCUMENT_APPEND_MARKUP}
   * method. It appends markup within the blip specified in
   * the operation.
   *
   * @param operation the operation to execute.
   * @param context the context of the operation.
   * @param participant the participant performing this operation.
   * @param wavelet the wavelet to operate on.
   * @param conversation the conversation to operate on.
   * @throws InvalidRequestException if the operation fails to perform
   */
  private void appendMarkup(OperationRequest operation, OperationContext context,
      ParticipantId participant, ObservableWavelet wavelet, ObservableConversation conversation)
      throws InvalidRequestException {
    Preconditions.checkArgument(
        OperationUtil.getOperationType(operation) == OperationType.DOCUMENT_APPEND_MARKUP,
        "Unsupported operation " + operation);

    String content = OperationUtil.getRequiredParameter(operation, ParamsProperty.CONTENT);
    String blipId = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_ID);
    ConversationBlip convBlip = context.getBlip(conversation, blipId);

    // Create builder from xml content.
    XmlStringBuilder markupBuilder = XmlStringBuilder.createFromXmlString(content);

    // Append the new markup to the blip doc.
    Document doc = convBlip.getContent();
    LineContainers.appendLine(doc, markupBuilder);

    // Report success.
    context.constructResponse(operation, Maps.<ParamsProperty, Object> newHashMap());
  }

  /**
   * Implementation for the {@link OperationType#DOCUMENT_INSERT_INLINE_BLIP}
   * method. It inserts an inline blip at the location specified in the
   * operation.
   *
   * @param operation the operation to execute.
   * @param context the context of the operation.
   * @param participant the participant performing this operation.
   * @param wavelet the wavelet to operate on.
   * @param conversation the conversation to operate on.
   * @throws InvalidRequestException if the operation fails to perform
   */
  private void insertInlineBlip(OperationRequest operation, OperationContext context,
      ParticipantId participant, ObservableWavelet wavelet, ObservableConversation conversation)
      throws InvalidRequestException {
    Preconditions.checkArgument(
        OperationUtil.getOperationType(operation) == OperationType.DOCUMENT_INSERT_INLINE_BLIP,
        "Unsupported operation " + operation);

    BlipData blipData = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_DATA);
    String parentBlipId = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_ID);
    ConversationBlip parentBlip = context.getBlip(conversation, parentBlipId);

    Integer index = OperationUtil.getRequiredParameter(operation, ParamsProperty.INDEX);
    if (index <= 0) {
      throw new InvalidRequestException(
          "Can't inline a blip on position <= 0, got " + index, operation);
    }

    ApiView view = new ApiView(parentBlip.getContent(), wavelet);
    int xmlLocation = view.transformToXmlOffset(index);

    // Insert new inline thread with the blip at the location as specified.
    ConversationBlip newBlip = parentBlip.addReplyThread(xmlLocation).appendBlip();
    context.putBlip(blipData.getBlipId(), newBlip);

    putContentForNewBlip(newBlip, blipData.getContent());
    processBlipCreatedEvent(operation, context, participant, conversation, newBlip);
  }

  /**
   * Implementation for the
   * {@link OperationType#DOCUMENT_INSERT_INLINE_BLIP_AFTER_ELEMENT} method. It
   * inserts an inline blip after the element specified in the operation.
   *
   * @param operation the operation to execute.
   * @param context the context of the operation.
   * @param participant the participant performing this operation.
   * @param wavelet the wavelet to operate on.
   * @param conversation the conversation to operate on.
   * @throws InvalidRequestException if the operation fails to perform
   */
  private void insertInlineBlipAfterElement(OperationRequest operation, OperationContext context,
      ParticipantId participant, OpBasedWavelet wavelet, ObservableConversation conversation)
      throws InvalidRequestException {
    Preconditions.checkArgument(OperationUtil.getOperationType(operation)
        == OperationType.DOCUMENT_INSERT_INLINE_BLIP_AFTER_ELEMENT,
        "Unsupported operation " + operation);

    BlipData blipData = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_DATA);
    String parentBlipId = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_ID);
    ConversationBlip parentBlip = context.getBlip(conversation, parentBlipId);

    Element element = OperationUtil.getRequiredParameter(operation, ParamsProperty.ELEMENT);

    // view.locateElement will tell where the element actually is.
    ApiView view = new ApiView(parentBlip.getContent(), wavelet);
    int elementApiLocation = view.locateElement(element);

    if (elementApiLocation == -1) {
      throw new InvalidRequestException("Requested element not found", operation);
    }

    // Insert just after the requested element
    int xmlLocation = view.transformToXmlOffset(elementApiLocation + 1);

    // Insert new inline thread with the blip at the location of the element.
    ConversationBlip newBlip = parentBlip.addReplyThread(xmlLocation).appendBlip();
    context.putBlip(blipData.getBlipId(), newBlip);

    putContentForNewBlip(newBlip, blipData.getContent());
    processBlipCreatedEvent(operation, context, participant, conversation, newBlip);
  }

  /**
   * Implementation for the {@link OperationType#BLIP_DELETE} method. It deletes
   * the blip specified in the operation.
   *
   * @param operation the operation to execute.
   * @param context the context of the operation.
   * @param participant the participant performing this operation.
   * @param conversation the conversation to operate on.
   * @throws InvalidRequestException if the operation fails to perform
   */
  private void delete(OperationRequest operation, OperationContext context,
      ParticipantId participant, ObservableConversation conversation)
      throws InvalidRequestException {
    Preconditions.checkArgument(
        OperationUtil.getOperationType(operation) == OperationType.BLIP_DELETE,
        "Unsupported operation " + operation);

    String blipId = OperationUtil.getRequiredParameter(operation, ParamsProperty.BLIP_ID);
    context.getBlip(conversation, blipId).delete();
    // report success.
    context.constructResponse(operation, Maps.<ParamsProperty, Object> newHashMap());
  }

  /**
   * Inserts content into the new blip.
   *
   * @param newBlip the newly created blip.
   * @param content the content to add.
   */
  private void putContentForNewBlip(ConversationBlip newBlip, String content) {
    if (content.length() > 0 && content.charAt(0) == '\n') {
      // While the client libraries force a newline to be sent as the first
      // character we'll remove it here since the new blip we created already
      // contains a newline.
      content = content.substring(1);
    }
    XmlStringBuilder builder = XmlStringBuilder.createText(content);
    LineContainers.appendToLastLine(newBlip.getContent(), builder);
  }

  /**
   * Processes a {@link WaveletBlipCreatedEvent} and puts it into the context.
   *
   * @param operation the operation that has been performed
   * @param context the context of the operation.
   * @param participant the participant performing the operation.
   * @param conversation the conversation to which the new blip was added
   * @param newBlip the newly created blip.
   * @throws InvalidRequestException if the event could not be processed.
   */
  private void processBlipCreatedEvent(OperationRequest operation, OperationContext context,
      ParticipantId participant, ObservableConversation conversation, ConversationBlip newBlip)
      throws InvalidRequestException {
    WaveletBlipCreatedEvent event =
        new WaveletBlipCreatedEvent(null, null, participant.getAddress(),
            System.currentTimeMillis(), ConversationUtil.getRootBlipId(conversation),
            newBlip.getId());
    context.processEvent(operation, event);
  }

  public static BlipOperationServices create() {
    return new BlipOperationServices();
  }
}
