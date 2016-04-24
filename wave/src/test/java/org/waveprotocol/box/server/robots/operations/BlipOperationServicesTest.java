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

import com.google.wave.api.BlipData;
import com.google.wave.api.Element;
import com.google.wave.api.ElementType;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.data.ApiView;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.OperationContextImpl;
import org.waveprotocol.box.server.robots.RobotsTestBase;
import org.waveprotocol.box.server.robots.testing.OperationServiceHelper;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationThread;
import org.waveprotocol.wave.model.conversation.ConversationBlip.LocatedReplyThread;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.impl.DocInitializationBuilder;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.Iterator;

/**
 * Unit tests for {@link BlipOperationServices}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class BlipOperationServicesTest extends RobotsTestBase {

  private static final String TEMP_BLIP_ID = OperationContext.TEMP_ID_MARKER + "blip1";
  private static final String NEW_BLIP_CONTENT = "Hello World";

  private BlipOperationServices service;
  private OperationServiceHelper helper;
  private BlipData blipData;

  @Override
  protected void setUp() {
    service = BlipOperationServices.create();
    helper = new OperationServiceHelper(WAVELET_NAME, ALEX);
    // BlipData constructor is broken, it doesn't set the blipId passed in the
    // constructor
    blipData = new BlipData(s(WAVE_ID), s(WAVELET_ID), TEMP_BLIP_ID, NEW_BLIP_CONTENT);
    blipData.setBlipId(TEMP_BLIP_ID);
  }

  public void testContinueThread() throws Exception {
    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ALEX).getRoot();

    // Append a random blip to check that we actually append to the end of this
    // thread.
    conversation.getRootThread().appendBlip();

    // Append to continue the thread of the root blip
    String rootBlipId = ConversationUtil.getRootBlipId(conversation);
    OperationRequest operation = operationRequest(OperationType.BLIP_CONTINUE_THREAD,
        rootBlipId, Parameter.of(ParamsProperty.BLIP_DATA, blipData));

    service.execute(operation, context, ALEX);

    JsonRpcResponse response = context.getResponse(OPERATION_ID);
    assertFalse(response.isError());

    ConversationBlip newBlip = checkAndGetNewBlip(context, conversation, response);

    Iterator<? extends ObservableConversationBlip> it =
        conversation.getRootThread().getBlips().iterator();
    it.next(); // skip, root
    it.next(); // skip, first reply
    assertEquals("New blip should be the third blip in the root thread", newBlip, it.next());
  }

  public void testCreateChild() throws Exception {
    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ALEX).getRoot();

    // Append a random blip to check that we actually make a new child blip
    conversation.getRootThread().appendBlip();

    // Create a child to the rootblip
    String rootBlipId = ConversationUtil.getRootBlipId(conversation);
    OperationRequest operation = operationRequest(OperationType.BLIP_CREATE_CHILD, rootBlipId,
        Parameter.of(ParamsProperty.BLIP_DATA, blipData));

    service.execute(operation, context, ALEX);

    JsonRpcResponse response = context.getResponse(OPERATION_ID);
    assertFalse(response.isError());

    ConversationBlip newBlip = checkAndGetNewBlip(context, conversation, response);

    Iterator<? extends ObservableConversationThread> it =
        conversation.getRootThread().getFirstBlip().getReplyThreads().iterator();
    assertEquals("New blip should be the first blip in the first reply thread",
        it.next().getFirstBlip(), newBlip);
  }

  public void testAppendBlip() throws Exception {
    OperationRequest operation = operationRequest(OperationType.WAVELET_APPEND_BLIP,
        Parameter.of(ParamsProperty.BLIP_DATA, blipData));

    OperationContextImpl context = helper.getContext();
    service.execute(operation, context, ALEX);

    JsonRpcResponse response = context.getResponse(OPERATION_ID);
    assertFalse(response.isError());

    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ALEX).getRoot();
    ConversationBlip newBlip = checkAndGetNewBlip(context, conversation, response);

    Iterator<? extends ObservableConversationBlip> it =
        conversation.getRootThread().getBlips().iterator();
    it.next(); // skip, root
    assertEquals("New blip should be the second blip in the root thread", newBlip, it.next());
  }

  public void testAppendBadMarkup() throws Exception {
    // Set markup with different end tag.
    String markup = "<custom>"+NEW_BLIP_CONTENT+"</diffcustom>";

    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ALEX).getRoot();

    // Append the custom markup to the newly created blip.
    ConversationBlip markupBlip = conversation.getRootThread().appendBlip();

    OperationRequest operation = operationRequest(OperationType.DOCUMENT_APPEND_MARKUP,
        markupBlip.getId(), Parameter.of(ParamsProperty.CONTENT, markup));

    try {
      service.execute(operation, context, ALEX);

      fail("Bad Markup should have generated error in service execution.");
    } catch(IllegalArgumentException e) {
      // Good.
    }
  }

  public void testAppendCustomMarkup() throws Exception {
    String markup = "<custom>this is custom markup</custom>";

    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ALEX).getRoot();

    // Append the custom markup to the newly created blip.
    ConversationBlip markupBlip = conversation.getRootThread().appendBlip();

    OperationRequest operation = operationRequest(OperationType.DOCUMENT_APPEND_MARKUP,
        markupBlip.getId(), Parameter.of(ParamsProperty.CONTENT, markup));

    service.execute(operation, context, ALEX);

    JsonRpcResponse response = context.getResponse(OPERATION_ID);
    assertFalse("CustomMarkup generated error in service execution.", response.isError());

    // The xml in new blip should match custom markup.
    String actualContent = markupBlip.getContent().toXmlString();
    assertTrue("Expected the new blip to contain the custom markup as specified in the " +
        "operation. actualcontent: " + actualContent, actualContent.contains(markup));
  }

  /**
   * Validates the behavior when a caller invokes the DOCUMENT_APPEND_MARKUP
   * without actually passing in xml markup (so just the text).  The behavior
   * should be to create a default wave line element, so the scenario is:
   *
   * input: "Hello World"
   * output: <line></line>Hello World
   */
  public void testAppendLineMarkup() throws Exception {
    // Just insert text. should create <line></line>Text.
    String markup = NEW_BLIP_CONTENT;

    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ALEX).getRoot();

    // Append the text markup to the newly created blip.
    DocInitialization blipInitContent = new DocInitializationBuilder().build();
    ConversationBlip markupBlip = conversation.getRootThread().appendBlip(blipInitContent);

    OperationRequest operation = operationRequest(OperationType.DOCUMENT_APPEND_MARKUP,
        markupBlip.getId(), Parameter.of(ParamsProperty.CONTENT, markup));

    service.execute(operation, context, ALEX);

    JsonRpcResponse response = context.getResponse(OPERATION_ID);
    assertFalse("LineMarkup generated error in service execution.", response.isError());

    // The output should now include the default <line/> element.
    String lineContent = markupBlip.getContent().toXmlString();
    assertTrue("Expected the blip to append the default wave <line/> element. " +
        "actual content: " + lineContent, lineContent.contains("<line/>"+markup));
  }

  public void testAppendInlineBlip() throws Exception {
    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ALEX).getRoot();

    // Append the inline blip to the root blip
    String rootBlipId = ConversationUtil.getRootBlipId(conversation);
    OperationRequest operation = operationRequest(OperationType.DOCUMENT_APPEND_INLINE_BLIP,
        rootBlipId, Parameter.of(ParamsProperty.BLIP_DATA, blipData));

    service.execute(operation, context, ALEX);

    JsonRpcResponse response = context.getResponse(OPERATION_ID);
    assertFalse(response.isError());

    ConversationBlip newBlip = checkAndGetNewBlip(context, conversation, response);

    Iterator<? extends LocatedReplyThread<? extends ObservableConversationThread>> it =
        conversation.getRootThread().getFirstBlip().locateReplyThreads().iterator();
    LocatedReplyThread<? extends ObservableConversationThread> inlineReplyThread = it.next();

    // The inline reply thread should be located just after the last line
    // element
    Document doc = conversation.getRootThread().getFirstBlip().getContent();
    Doc.E lastLine = DocHelper.getLastElementWithTagName(doc, LineContainers.LINE_TAGNAME);
    int lastLineLocation = doc.getLocation(Point.after(doc, lastLine));
    assertEquals("The inline reply was not located just after the last line element",
        lastLineLocation, inlineReplyThread.getLocation());
  }

  public void testInsertInlineBlip() throws Exception {
    OperationContextImpl context = helper.getContext();
    OpBasedWavelet wavelet = context.openWavelet(WAVE_ID, WAVELET_ID, ALEX);
    ObservableConversation conversation =
        context.openConversation(WAVE_ID, WAVELET_ID, ALEX).getRoot();

    int insertAtApiLocation = 2;
    ApiView apiView =
        new ApiView(conversation.getRootThread().getFirstBlip().getContent(), wavelet);
    int insertAtXmlLocation = apiView.transformToXmlOffset(insertAtApiLocation);

    // Append the inline blip to the root blip
    String rootBlipId = ConversationUtil.getRootBlipId(conversation);
    OperationRequest operation = operationRequest(OperationType.DOCUMENT_INSERT_INLINE_BLIP,
        rootBlipId, Parameter.of(ParamsProperty.BLIP_DATA, blipData),
        Parameter.of(ParamsProperty.INDEX, insertAtApiLocation));

    service.execute(operation, context, ALEX);

    JsonRpcResponse response = context.getResponse(OPERATION_ID);
    assertFalse(response.isError());

    ConversationBlip newBlip = checkAndGetNewBlip(context, conversation, response);

    Iterator<? extends LocatedReplyThread<? extends ObservableConversationThread>> it =
        conversation.getRootThread().getFirstBlip().locateReplyThreads().iterator();
    LocatedReplyThread<? extends ObservableConversationThread> inlineReplyThread = it.next();
    assertEquals("The inline reply was not located where specified", insertAtXmlLocation,
        inlineReplyThread.getLocation());
  }

  public void testInsertInlineBlipAfterElement() throws Exception {
    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
      context.openConversation(WAVE_ID, WAVELET_ID, ALEX).getRoot();

    // Make an inline blip at a certain location, we will then have the
    // BlipOperationService insert one after that blip.
    ObservableConversationBlip rootBlip = conversation.getRootThread().getFirstBlip();
    Document doc = rootBlip.getContent();
    Doc.E lastLine = DocHelper.getLastElementWithTagName(doc, LineContainers.LINE_TAGNAME);
    int lastLineLocation = doc.getLocation(Point.after(doc, lastLine));
    ObservableConversationBlip firstInlineBlip =
        rootBlip.addReplyThread(lastLineLocation).appendBlip();

    // Append the inline blip to the root blip
    String rootBlipId = ConversationUtil.getRootBlipId(conversation);
    Element inlineBlipElement = new Element(ElementType.INLINE_BLIP);
    inlineBlipElement.setProperty("id", firstInlineBlip.getId());
    OperationRequest operation = operationRequest(
        OperationType.DOCUMENT_INSERT_INLINE_BLIP_AFTER_ELEMENT, rootBlipId,
        Parameter.of(ParamsProperty.BLIP_DATA, blipData),
        Parameter.of(ParamsProperty.ELEMENT, inlineBlipElement));

    service.execute(operation, context, ALEX);

    JsonRpcResponse response = context.getResponse(OPERATION_ID);
    assertFalse(response.isError());

    ConversationBlip newBlip = checkAndGetNewBlip(context, conversation, response);

    // The second InlineReplyThread is created by the BlipOperationService, it
    // should be located just after the first one.
    Iterator<? extends LocatedReplyThread<?>> it =
        conversation.getRootThread().getFirstBlip().locateReplyThreads().iterator();
    // Inline blips have a length of 2.
    assertEquals("The inline reply was not located where specified", it.next().getLocation() + 2,
        it.next().getLocation());
  }

  public void testDeleteBlip() throws Exception {
    OperationContextImpl context = helper.getContext();
    ObservableConversation conversation =
      context.openConversation(WAVE_ID, WAVELET_ID, ALEX).getRoot();

    // Delete the root blip
    String rootBlipId = ConversationUtil.getRootBlipId(conversation);
    OperationRequest operation = operationRequest(OperationType.BLIP_DELETE, rootBlipId);

    service.execute(operation, context, ALEX);

    JsonRpcResponse response = context.getResponse(OPERATION_ID);
    assertFalse(response.isError());
    assertNull("Blip should have been deleted", conversation.getBlip(rootBlipId));
  }

  /**
   * Methods that checks that the new blip was actually created and stored in
   * the context. As well as that it checks its contents
   */
  private ConversationBlip checkAndGetNewBlip(
      OperationContextImpl context, ObservableConversation conversation, JsonRpcResponse response)
      throws InvalidRequestException {
    // Retrieve the blip using the context so that the temp blip storage is
    // checked
    ConversationBlip newBlip = context.getBlip(conversation, TEMP_BLIP_ID);
    assertEquals("The response didn't contain the id of the new blip", newBlip.getId(),
        response.getData().get(ParamsProperty.NEW_BLIP_ID));
    String actualContent = newBlip.getContent().toXmlString();
    assertTrue("Expected the new blip to contain the contens as specified in the operation",
        actualContent.contains(NEW_BLIP_CONTENT));
    return newBlip;
  }
}
