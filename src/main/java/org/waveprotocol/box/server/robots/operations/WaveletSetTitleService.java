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

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.wave.model.conversation.ObservableConversation;
import org.waveprotocol.wave.model.conversation.TitleHelper;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Implements the "wavelet.setTitle" operation.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class WaveletSetTitleService implements OperationService {

  @Override
  public void execute(
      OperationRequest operation, OperationContext context, ParticipantId participant)
      throws InvalidRequestException {
    
    String title =
        OperationUtil.getRequiredParameter(operation, ParamsProperty.WAVELET_TITLE);
    ObservableConversation conversation =
        context.openConversation(operation, participant).getRoot();
    String blipId = conversation.getRootThread().getFirstBlip().getId();
    Document doc = context.getBlip(conversation, blipId).getContent();
    TitleHelper.setExplicitTitle(doc, title);
  }

  public static WaveletSetTitleService create() {
    return new WaveletSetTitleService();
  }
}
