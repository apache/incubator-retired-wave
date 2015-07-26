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

package org.waveprotocol.wave.client.doodad.attachment;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.waveprotocol.box.attachment.jso.AttachmentsResponseJsoImpl;
import org.waveprotocol.box.attachment.jso.AttachmentMetadataJsoImpl;
import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.client.scheduler.Scheduler.Task;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.communication.gwt.JsonMessage;
import org.waveprotocol.wave.communication.json.JsonException;
import org.waveprotocol.wave.media.model.Attachment;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * Gets attachments meta info from server.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class AttachmentManagerImpl implements SimpleAttachmentManager {
  interface GetAttachmentsInfoCallback {

    public void onSuccess(List<Attachment> attachments);

    public void onFailure(List<Attachment> attachments, String message);
  }

  private static final String ATTACHMENTS_INFO_URL_BASE = "/attachmentsInfo";

  private static final LoggerBundle LOG = new DomLogger(AttachmentManagerImpl.class.getName());

  private static SimpleAttachmentManager instance;

  private final TimerService scheduler;
  private final Map<String, AttachmentImpl> attachmentsInfo = new HashMap<String, AttachmentImpl>();
  private final List<String> pendingQueue = new ArrayList<String>();
  private final List<Listener> listeners = new ArrayList<Listener>();

  public static SimpleAttachmentManager getInstance() {
    if (instance != null) {
      return instance;
    }
    instance = new AttachmentManagerImpl();
    return instance;
  }

  private Task getAttachmentsInfoTask = new Task() {
    @Override
    public void execute() {
      getAttachmentsInfo(new GetAttachmentsInfoCallback() {

        @Override
        public void onSuccess(List<Attachment> attachments) {
          for (Attachment attachment : attachments) {
            notifyImageUpdated(attachment);
          }
        }

        @Override
        public void onFailure(List<Attachment> attachments, String message) {
          LOG.error().log("Getting of attachments info failed: " + message);
          for (Attachment attachment : attachments) {
            notifyImageUpdated(attachment);
          }
        }
      });
    }
  };

  private AttachmentManagerImpl() {
    this.scheduler = SchedulerInstance.getMediumPriorityTimer();
  }

  @Override
  public Attachment getAttachment(String attachmentId) {
    AttachmentImpl attachment = attachmentsInfo.get(attachmentId);
    if (attachment == null) {
      attachment = new AttachmentImpl();
      attachmentsInfo.put(attachmentId, attachment);
      if (!pendingQueue.contains(attachmentId)) {
        pendingQueue.add(attachmentId);
        scheduler.schedule(getAttachmentsInfoTask);
      }
    }
    return attachment;
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private void notifyImageUpdated(Attachment attachment) {
    for (Listener listener : listeners) {
      listener.onContentUpdated(attachment);
      listener.onThumbnailUpdated(attachment);
    }
  }

  private void getAttachmentsInfo(final GetAttachmentsInfoCallback callback) {

    LOG.trace().log("Getting attachments info");

    String request = ATTACHMENTS_INFO_URL_BASE + "?attachmentIds=";

    for (String attacmentId : pendingQueue) {
      if (!request.endsWith("=")) {
        request += ",";
      }
      request += attacmentId;
    }
    final List<String> requestedAttachments = new ArrayList<String>(pendingQueue);
    pendingQueue.clear();

    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, request);

    requestBuilder.setCallback(new RequestCallback() {
      @Override
      public void onResponseReceived(Request request, Response response) {
        LOG.trace().log("Attachments info was received: ", response.getText());
        if (response.getStatusCode() != Response.SC_OK) {
          callback.onFailure(setFailureStatus(requestedAttachments), "Got back status code " + response.getStatusCode());
        } else if (!response.getHeader("Content-Type").startsWith("application/json")) {
          callback.onFailure(setFailureStatus(requestedAttachments), "Search service did not return json");
        } else {
          AttachmentsResponseJsoImpl attachmentsProto;
          try {
            attachmentsProto = JsonMessage.parse(response.getText());
          } catch (JsonException e) {
            callback.onFailure(setFailureStatus(requestedAttachments), e.getMessage());
            return;
          }
          List<Attachment> attachments = initializeAttachments(attachmentsProto);
          callback.onSuccess(attachments);
        }
      }

      @Override
      public void onError(Request request, Throwable e) {
        LOG.error().log("Getting attachments info error: ", e);
        callback.onFailure(setFailureStatus(requestedAttachments), e.getMessage());
      }
    });

    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onFailure(setFailureStatus(requestedAttachments), e.getMessage());
    }
  }

  private List<Attachment> initializeAttachments(AttachmentsResponseJsoImpl protoAttachments) {
    List<Attachment> attachments = CollectionUtils.newArrayList();
    for (AttachmentMetadataJsoImpl protoAttachment : protoAttachments.getAttachment()) {
      AttachmentImpl attachment = attachmentsInfo.get(protoAttachment.getAttachmentId());
      if (attachment != null) {
        attachment.copyMetadata(protoAttachment);
        attachments.add(attachment);
      }
    }
    return attachments;
  }

  private List<Attachment> setFailureStatus(List<String> attachmentIds) {
    List<Attachment>  attachments = new ArrayList<Attachment>();
    for (String attachmentId : attachmentIds) {
      AttachmentImpl attachment = attachmentsInfo.get(attachmentId);
      if (attachment != null) {
        attachment.setStatus(Attachment.Status.FAILED_AND_NOT_RETRYABLE);
        attachments.add(attachment);
      }
    }
    return attachments;
  }
}
