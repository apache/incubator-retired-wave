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
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.impl.RawAttachmentData;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.box.server.attachment.AttachmentService;
import org.waveprotocol.box.attachment.AttachmentMetadata;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.util.logging.Log;
import org.waveprotocol.box.server.robots.util.OperationUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * {@link OperationService} for the "exportAttachment" operation.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
public class ExportAttachmentService implements OperationService {
  private static final Log LOG = Log.get(ExportAttachmentService.class);

  private final AttachmentService attachmentService;

  @Inject
  private ExportAttachmentService(AttachmentService attachmentSerive) {
    this.attachmentService = attachmentSerive;
  }

  @Override
  public void execute(OperationRequest operation, OperationContext context, ParticipantId participant)
      throws InvalidRequestException {
    AttachmentId attachmentId;
    try {
      attachmentId =  AttachmentId.deserialise(OperationUtil.<String>getRequiredParameter(operation,
          ParamsProperty.ATTACHMENT_ID));
    } catch (InvalidIdException ex) {
      throw new InvalidRequestException("Invalid id", operation, ex);
    }
    AttachmentMetadata meta;
    byte[] data;
    try {
      meta = attachmentService.getMetadata(attachmentId);
      data = readInputStreamToBytes(attachmentService.getAttachment(attachmentId).getInputStream());
    } catch (IOException ex) {
      LOG.info("Get attachment", ex);
      context.constructErrorResponse(operation, ex.toString());
      return;
    }
    RawAttachmentData attachment = new RawAttachmentData(meta.getFileName(), meta.getCreator(),
        data);
    Map<ParamsProperty, Object> parameters =
      ImmutableMap.<ParamsProperty, Object> of(ParamsProperty.ATTACHMENT_DATA, attachment);
    context.constructResponse(operation, parameters);
  }

  private static byte[] readInputStreamToBytes(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buffer = new byte[256];
    int length;
    while ((length = in.read(buffer)) != -1) {
      out.write(buffer, 0, length);
    }
    return out.toByteArray();
  }

}
