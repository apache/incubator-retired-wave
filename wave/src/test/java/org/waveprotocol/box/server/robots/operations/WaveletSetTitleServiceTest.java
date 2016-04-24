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

import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationRequest.Parameter;
import com.google.wave.api.OperationType;

import org.waveprotocol.box.server.robots.RobotsTestBase;
import org.waveprotocol.box.server.robots.testing.OperationServiceHelper;
import org.waveprotocol.wave.model.conversation.ObservableConversationBlip;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.id.LegacyIdSerialiser;

/**
 * Unit tests for {@link WaveletSetTitleService}.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class WaveletSetTitleServiceTest extends RobotsTestBase {
  private static final String INITIAL_CONTENT = "Hello world!";
  private WaveletSetTitleService service;
  private OperationServiceHelper helper;

  @Override
  protected void setUp() throws Exception {
    service = WaveletSetTitleService.create();
    helper = new OperationServiceHelper(WAVELET_NAME, ALEX);

    ObservableConversationBlip rootBlip = getRootBlip();
    LineContainers.appendToLastLine(
        rootBlip.getContent(), XmlStringBuilder.createText(INITIAL_CONTENT));
  }

  public void testSetTitle() throws Exception {
    String title = "Some title";
    OperationRequest operation =
        new OperationRequest(OperationType.WAVELET_SET_TITLE.method(),
            WAVELET_ID.getId(),
            Parameter.of(
                ParamsProperty.WAVELET_TITLE, title),
            Parameter.of(
                ParamsProperty.WAVE_ID, LegacyIdSerialiser.INSTANCE.serialiseWaveId(WAVE_ID)),
            Parameter.of(
                ParamsProperty.WAVELET_ID,
                LegacyIdSerialiser.INSTANCE.serialiseWaveletId(WAVELET_ID)));

    service.execute(operation, helper.getContext(), ALEX);

    String annotation = getRootBlip().getContent().getAnnotation(0, TitleHelper.TITLE_KEY);
    assertEquals("Expected conv/title annotation on range (0,1)", title, annotation);
  }

  private ObservableConversationBlip getRootBlip() throws InvalidRequestException {
    ObservableConversationBlip rootBlip =
        helper.getContext().openConversation(WAVE_ID, WAVELET_ID, ALEX).getRoot().getRootThread()
            .getFirstBlip();
    return rootBlip;
  }
}
