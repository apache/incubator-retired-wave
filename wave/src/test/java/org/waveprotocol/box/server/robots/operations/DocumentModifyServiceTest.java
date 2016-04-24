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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.wave.api.Element;
import com.google.wave.api.ElementType;
import com.google.wave.api.Gadget;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.Range;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.data.ApiView;
import com.google.wave.api.data.ApiView.ElementInfo;
import com.google.wave.api.impl.DocumentModifyAction;
import com.google.wave.api.impl.DocumentModifyAction.BundledAnnotation;
import com.google.wave.api.impl.DocumentModifyAction.ModifyHow;
import com.google.wave.api.impl.DocumentModifyQuery;

import org.waveprotocol.box.server.robots.RobotsTestBase;
import org.waveprotocol.box.server.robots.testing.OperationServiceHelper;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for {@link DocumentModifyService}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class DocumentModifyServiceTest extends RobotsTestBase {

  private static final String NO_ANNOTATION_KEY = null;
  private static final List<String> NO_VALUES = Collections.<String> emptyList();
  private static final List<BundledAnnotation> NO_BUNDLED_ANNOTATIONS = Collections.emptyList();
  private static final List<Element> NO_ELEMENTS = Collections.emptyList();
  private static final String ANNOTATION_VALUE = "annotationValue";
  private static final String ANNOTATION_KEY = "annotationKey";
  private static final String INITIAL_CONTENT = "Hello world!";
  private static final int CONTENT_START_TEXT = 1;
  private static final int CONTENT_START_XML = 3;

  private DocumentModifyService service;
  private OperationServiceHelper helper;
  private String rootBlipId;

  @Override
  protected void setUp() throws Exception {
    service = DocumentModifyService.create();
    helper = new OperationServiceHelper(WAVELET_NAME, ALEX);

    ObservableConversationBlip rootBlip = getRootBlip();
    rootBlipId = rootBlip.getId();
    LineContainers.appendToLastLine(
        rootBlip.getContent(), XmlStringBuilder.createText(INITIAL_CONTENT));
  }

  public void testFailOnMultipleWhereParams() throws Exception {
    OperationRequest operation =
        operationRequest(OperationType.DOCUMENT_MODIFY, rootBlipId,
            Parameter.of(ParamsProperty.MODIFY_ACTION, new DocumentModifyAction()),
            Parameter.of(ParamsProperty.RANGE, new Range(0, 1)),
            Parameter.of(ParamsProperty.INDEX, 0));

    try {
      service.execute(operation, helper.getContext(), ALEX);
      fail("Expected InvalidRequestException");
    } catch (InvalidRequestException e) {
      // expected
    }
  }

  public void testAnnotate() throws Exception {
    OperationRequest operation =
        operationRequest(OperationType.DOCUMENT_MODIFY, rootBlipId,
            Parameter.of(ParamsProperty.MODIFY_ACTION, new DocumentModifyAction(ModifyHow.ANNOTATE,
                Collections.singletonList(ANNOTATION_VALUE), ANNOTATION_KEY, NO_ELEMENTS,
                NO_BUNDLED_ANNOTATIONS, false)),
            Parameter.of(ParamsProperty.INDEX, CONTENT_START_TEXT));

    service.execute(operation, helper.getContext(), ALEX);

    String annotation = getRootBlip().getContent().getAnnotation(CONTENT_START_XML, ANNOTATION_KEY);
    assertEquals("Expected the text to be annotated", ANNOTATION_VALUE, annotation);
    assertNull("Expected this text not to be annotated",
        getRootBlip().getContent().getAnnotation(CONTENT_START_XML + 1, ANNOTATION_KEY));
  }

  public void testClearAnnotatation() throws Exception {
    Document doc = getRootBlip().getContent();
    doc.setAnnotation(CONTENT_START_XML, CONTENT_START_XML + 1, ANNOTATION_KEY, ANNOTATION_VALUE);

    String annotation = getRootBlip().getContent().getAnnotation(CONTENT_START_XML, ANNOTATION_KEY);
    assertEquals("Expected the text to be annotated", ANNOTATION_VALUE, annotation);

    OperationRequest operation =
      operationRequest(OperationType.DOCUMENT_MODIFY, rootBlipId,
            Parameter.of(ParamsProperty.MODIFY_ACTION,
                new DocumentModifyAction(ModifyHow.CLEAR_ANNOTATION, NO_VALUES, ANNOTATION_KEY,
                    NO_ELEMENTS, NO_BUNDLED_ANNOTATIONS, false)),
            Parameter.of(ParamsProperty.INDEX, CONTENT_START_TEXT));

    service.execute(operation, helper.getContext(), ALEX);

    assertNull("Expected this text not to be annotated",
        getRootBlip().getContent().getAnnotation(CONTENT_START_XML, ANNOTATION_KEY));
  }
  
  public void testInsertGadget() throws Exception {
    String gadgetUrl = "http://wave-api.appspot.com/public/gadgets/areyouin/gadget.xml";
    List<Element> elementsIn = Lists.newArrayListWithCapacity(1);
    Map<String,String> properties = Maps.newHashMap(); 
    properties.put("url", gadgetUrl);
    properties.put("author", ALEX.getAddress());
    elementsIn.add(new Gadget(properties));

    OperationRequest operation =
      operationRequest(OperationType.DOCUMENT_MODIFY, rootBlipId,
            Parameter.of(ParamsProperty.MODIFY_ACTION,
                new DocumentModifyAction(ModifyHow.INSERT, NO_VALUES, NO_ANNOTATION_KEY,
                    elementsIn, NO_BUNDLED_ANNOTATIONS, false)),
            Parameter.of(ParamsProperty.INDEX, CONTENT_START_TEXT));

    service.execute(operation, helper.getContext(), ALEX);
    
    Gadget gadget = null;
    List<ElementInfo> elementsOut = getApiView().getElements();
    int size = 0;
    for (ElementInfo elementOut : elementsOut) {
      if (!elementOut.element.isGadget()) {
        continue;
      } else {
        size++;
        gadget = (Gadget)elementOut.element;
      }
    }
    assertEquals(1, size);
    assertEquals(gadgetUrl, gadget.getUrl());
    assertEquals(ALEX.getAddress(), gadget.getAuthor());
  }
  
  public void testUpdateGadget() throws Exception {
    String propertyName = "propertyName";
    String propertyValue = "propertyValue";

    String propertyNameToDelete = "propertyNameToDelete";
    String propertyValueToDelete = "propertyValueToDelete";

    String gadgetUrl = "http://wave-api.appspot.com/public/gadgets/areyouin/gadget.xml";
    String gadgetXml =
        "<gadget author=\"" + ALEX.getAddress() + "\" prefs=\"\" state=\"\" title=\"\" " 
        + "url=\"" + gadgetUrl + "\">" 
        + "<state name=\"author\" value=\"" + ALEX.getAddress()+ "\"/>"
        + "<state name=\"url\" " + "value=\"" + gadgetUrl + "\"/>" 
        + "<state name=\"" + propertyName + "\" value=\"" + propertyValue + "\"/>"
        + "<state name=\"" + propertyNameToDelete + "\" value=\"" + propertyValueToDelete + "\"/>"
        + "</gadget>";

    ObservableConversationBlip rootBlip = getRootBlip();
    rootBlipId = rootBlip.getId();
    LineContainers.appendToLastLine(rootBlip.getContent(),
        XmlStringBuilder.createFromXmlString(gadgetXml));

    List<Element> updatedElementsIn = Lists.newArrayListWithCapacity(1);
    Map<String, String> newProperties = Maps.newHashMap();

    String updatedPropertyValue = "updatedPropertyValue";
    newProperties.put(propertyName, updatedPropertyValue);
    String newPropertyName = "newPropertyName";
    String newPropertyValue = "newPropertyValue";
    newProperties.put(newPropertyName, newPropertyValue);
    newProperties.put(propertyNameToDelete, null);
    updatedElementsIn.add(new Gadget(newProperties));

    OperationRequest updateOperation =
        operationRequest(OperationType.DOCUMENT_MODIFY, rootBlipId,
            Parameter.of(ParamsProperty.MODIFY_ACTION, 
                new DocumentModifyAction(ModifyHow.UPDATE_ELEMENT,
                NO_VALUES, NO_ANNOTATION_KEY, updatedElementsIn, NO_BUNDLED_ANNOTATIONS, false)),
            Parameter.of(ParamsProperty.MODIFY_QUERY, new DocumentModifyQuery(ElementType.GADGET,
                ImmutableMap.of("url", gadgetUrl), 1)));

    service.execute(updateOperation, helper.getContext(), ALEX);

    Gadget gadget = null;
    List<ElementInfo> elementsOut = getApiView().getElements();
    for (ElementInfo elementOut : elementsOut) {
      if (elementOut.element.isGadget()) {
        gadget = (Gadget) elementOut.element;
      }
    }
    assertEquals(gadgetUrl, gadget.getUrl());
    assertEquals(ALEX.getAddress(), gadget.getAuthor());
    assertEquals(updatedPropertyValue, gadget.getProperty(propertyName));
    assertNotNull(gadget.getProperty(newPropertyName));
    assertEquals(newPropertyValue, gadget.getProperty(newPropertyName));
    assertNull(gadget.getProperty(propertyNameToDelete));
  }

  public void testDelete() throws Exception {
    OperationRequest operation =
      operationRequest(OperationType.DOCUMENT_MODIFY, rootBlipId,
            Parameter.of(ParamsProperty.MODIFY_ACTION,
                new DocumentModifyAction(ModifyHow.DELETE, NO_VALUES, NO_ANNOTATION_KEY,
                    NO_ELEMENTS, NO_BUNDLED_ANNOTATIONS, false)),
            Parameter.of(ParamsProperty.INDEX, CONTENT_START_TEXT));

    service.execute(operation, helper.getContext(), ALEX);

    // Cut off the /n
    String after = getApiView().apiContents().substring(1);
    assertEquals("First character should be deleted", INITIAL_CONTENT.substring(1), after);
  }

  public void testInsert() throws Exception {
    String toInsert = "insertedText";

    // Insert a new piece of annotated text before the current text.
    OperationRequest operation =
        operationRequest(OperationType.DOCUMENT_MODIFY, rootBlipId,
            Parameter.of(ParamsProperty.MODIFY_ACTION,
                new DocumentModifyAction(ModifyHow.INSERT, Collections.singletonList(toInsert),
                    NO_ANNOTATION_KEY, NO_ELEMENTS,
                    BundledAnnotation.listOf(ANNOTATION_KEY, ANNOTATION_VALUE), false)),
            Parameter.of(ParamsProperty.INDEX, CONTENT_START_TEXT));

    service.execute(operation, helper.getContext(), ALEX);

    // Cut off the /n
    String result = getApiView().apiContents().substring(1);
    assertEquals(
        "The result should start with the inserted text", toInsert + INITIAL_CONTENT, result);

    String annotation = getRootBlip().getContent().getAnnotation(CONTENT_START_XML, ANNOTATION_KEY);
    assertEquals("Expected the text to be annotated", ANNOTATION_VALUE, annotation);
  }

  public void testInsertAfter() throws Exception {
    String toInsert = "insertedText";
    OperationRequest operation =
        operationRequest(OperationType.DOCUMENT_MODIFY, rootBlipId,
            Parameter.of(ParamsProperty.MODIFY_ACTION,
                new DocumentModifyAction(ModifyHow.INSERT_AFTER,
                    Collections.singletonList(toInsert), NO_ANNOTATION_KEY, NO_ELEMENTS,
                    NO_BUNDLED_ANNOTATIONS, false)),
            Parameter.of(ParamsProperty.INDEX, CONTENT_START_TEXT));

    service.execute(operation, helper.getContext(), ALEX);

    // Cut off the /n
    String after = getApiView().apiContents().substring(1);
    assertEquals("Content should be insterted after the first character",
        INITIAL_CONTENT.charAt(0) + toInsert + INITIAL_CONTENT.substring(1), after);
  }

  public void testReplace() throws Exception {
    String replacement = "replacedText";
    OperationRequest operation =
        operationRequest(OperationType.DOCUMENT_MODIFY, rootBlipId,
            Parameter.of(ParamsProperty.MODIFY_ACTION,
                new DocumentModifyAction(ModifyHow.REPLACE, Collections.singletonList(replacement),
                    NO_ANNOTATION_KEY, NO_ELEMENTS, NO_BUNDLED_ANNOTATIONS, false)),
            Parameter.of(ParamsProperty.RANGE,
                new Range(CONTENT_START_TEXT, CONTENT_START_TEXT + INITIAL_CONTENT.length())));

    service.execute(operation, helper.getContext(), ALEX);

    // Cut off the /n
    String after = getApiView().apiContents().substring(1);
    assertEquals("The entire text should be replaced", replacement, after);
  }

  private ApiView getApiView() throws InvalidRequestException {
    ApiView view = new ApiView(
        getRootBlip().getContent(), helper.getContext().openWavelet(WAVE_ID, WAVELET_ID, ALEX));
    return view;
  }

  private ObservableConversationBlip getRootBlip() throws InvalidRequestException {
    ObservableConversationBlip rootBlip =
        helper.getContext().openConversation(WAVE_ID, WAVELET_ID, ALEX).getRoot().getRootThread()
            .getFirstBlip();
    return rootBlip;
  }
}
