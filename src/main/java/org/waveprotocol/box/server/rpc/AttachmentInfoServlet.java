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

package org.waveprotocol.box.server.rpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.logging.Level;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.rpc.ProtoSerializer.SerializationException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.waveprotocol.box.attachment.AttachmentMetadata;
import org.waveprotocol.box.attachment.AttachmentProto.AttachmentsResponse;
import org.waveprotocol.box.attachment.proto.AttachmentMetadataProtoImpl;
import org.waveprotocol.box.server.attachment.AttachmentService;
import org.waveprotocol.box.server.persistence.AttachmentUtil;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.wave.media.model.AttachmentId;

/*
 * Serves attachments info from a provided store.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */

@SuppressWarnings("serial")
@Singleton
public class AttachmentInfoServlet extends HttpServlet {
  public static final String ATTACHMENTS_INFO_URL = "/attachmentsInfo";

  private static final Log LOG = Log.get(AttachmentInfoServlet.class);

  private final AttachmentService service;
  private final WaveletProvider waveletProvider;
  private final SessionManager sessionManager;
  private final ProtoSerializer serializer;

  @Inject
  private AttachmentInfoServlet(AttachmentService service, WaveletProvider waveletProvider,
      SessionManager sessionManager, ProtoSerializer serializer) {
    this.service = service;
    this.waveletProvider = waveletProvider;
    this.sessionManager = sessionManager;
    this.serializer = serializer;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    List<AttachmentId> attachmentIds = getIdsFromRequest(request);

    if (attachmentIds == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    ParticipantId user = sessionManager.getLoggedInUser(request.getSession(false));

    AttachmentsResponse.Builder attachmentsResponse = AttachmentsResponse.newBuilder();
    for (AttachmentId id : attachmentIds) {
      AttachmentMetadata metadata = service.getMetadata(id);
      if (metadata != null) {
        boolean isAuthorized = false;
        WaveletName waveletName = AttachmentUtil.waveRef2WaveletName(metadata.getWaveRef());
        try {
          isAuthorized = waveletProvider.checkAccessPermission(waveletName, user);
        } catch (WaveServerException e) {
          LOG.warning("Problem while authorizing user: " + user + " for wavelet: " + waveletName, e);
        }
        if (isAuthorized) {
          attachmentsResponse.addAttachment(new AttachmentMetadataProtoImpl(metadata).getPB());
        }
      }
    }

    String info;
    try {
      info = serializer.toJson(attachmentsResponse.build()).toString();
    } catch (SerializationException ex) {
      LOG.log(Level.SEVERE, "Attachments info serialize", ex);
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json; charset=utf8");
    response.setHeader("Cache-Control", "no-store");
    response.getWriter().append(info);

    LOG.info("Fetched info for " + attachmentIds.size() + " attachments");
  }

  /**
   * Get the attachment Ids from the URL in the request.
   *
   * @param request
   * @return the list of Ids.
   */
  private static List<AttachmentId> getIdsFromRequest(HttpServletRequest request) {
    String par = request.getParameter("attachmentIds");
    if (par != null) {
      List<AttachmentId> ids = new ArrayList<AttachmentId>();
      for (String id : par.split(",", -1)) {
        try {
          ids.add(AttachmentId.deserialise(id));
        } catch (InvalidIdException ex) {
          LOG.log(Level.SEVERE, "Deserialize attachment Id " + id, ex);
        }
      }
      return ids;
    }
    return null;
  }

}
