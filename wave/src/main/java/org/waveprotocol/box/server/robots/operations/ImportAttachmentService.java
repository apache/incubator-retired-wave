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

import com.google.common.collect.Maps;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationRequest;
import com.google.inject.Inject;
import com.google.wave.api.ApiIdSerializer;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.impl.RawAttachmentData;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.attachment.AttachmentService;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.util.logging.Log;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.id.*;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.Map;

/**
 * {@link OperationService} for the "importAttachment" operation.
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class ImportAttachmentService implements OperationService {
  private static final Log LOG = Log.get(ImportAttachmentService.class);
  private final AttachmentService attachmentService;

  @Inject
  public ImportAttachmentService(AttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  @Override
  public void execute(OperationRequest operation, OperationContext context, ParticipantId participant)
      throws InvalidRequestException {
    WaveId waveId;
    WaveletId waveletId;
    AttachmentId attachmentId;
    RawAttachmentData attachmentData;
    try {
      waveId = ApiIdSerializer.instance().deserialiseWaveId(
          OperationUtil.<String>getRequiredParameter(operation, ParamsProperty.WAVE_ID));
      waveletId = ApiIdSerializer.instance().deserialiseWaveletId(
          OperationUtil.<String>getRequiredParameter(operation, ParamsProperty.WAVELET_ID));
      attachmentId =  AttachmentId.deserialise(OperationUtil.<String>getRequiredParameter(operation,
          ParamsProperty.ATTACHMENT_ID));
      attachmentData = OperationUtil.<RawAttachmentData>getRequiredParameter(operation,
          ParamsProperty.ATTACHMENT_DATA);
    } catch (InvalidIdException ex) {
      throw new InvalidRequestException("Invalid id", operation, ex);
    }
    try {
      attachmentService.storeAttachment(attachmentId, new ByteArrayInputStream(attachmentData.getData()),
        WaveletName.of(waveId, waveletId), attachmentData.getFileName(), ParticipantId.of(attachmentData.getCreator()));
    } catch (InvalidParticipantAddress ex) {
      throw new InvalidRequestException("Invalid participant " + attachmentData.getCreator(), operation, ex);
    } catch (IOException ex) {
      LOG.severe("Store attachment", ex);
      context.constructErrorResponse(operation, ex.toString());
      return;
    }
    Map<ParamsProperty, Object> response = Maps.<ParamsProperty, Object>newHashMap();
    context.constructResponse(operation, response);
  }
}
