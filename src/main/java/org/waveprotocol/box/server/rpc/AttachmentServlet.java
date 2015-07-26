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
import com.typesafe.config.Config;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.waveprotocol.box.attachment.AttachmentMetadata;
import org.waveprotocol.box.server.attachment.AttachmentService;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AttachmentStore.AttachmentData;
import org.waveprotocol.box.server.persistence.AttachmentUtil;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

/**
 * Serves attachments from a provided store.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 *
 */

@SuppressWarnings("serial")
@Singleton
public class AttachmentServlet extends HttpServlet {
  public static String ATTACHMENT_URL = "/attachment";
  public static String THUMBNAIL_URL = "/thumbnail";

  public static String THUMBNAIL_PATTERN_FORMAT_NAME = "png";
  public static String THUMBNAIL_PATTERN_DEFAULT = "default";

  private static final Log LOG = Log.get(AttachmentServlet.class);

  private final AttachmentService service;
  private final WaveletProvider waveletProvider;
  private final SessionManager sessionManager;
  private final String thumbnailPattternsDirectory;

  @Inject
  private AttachmentServlet(AttachmentService service, WaveletProvider waveletProvider,
      SessionManager sessionManager, Config config) {
    this.service = service;
    this.waveletProvider = waveletProvider;
    this.sessionManager = sessionManager;
    this.thumbnailPattternsDirectory = config.getString("core.thumbnail_patterns_directory");
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    AttachmentId attachmentId = getAttachmentIdFromRequest(request);

    if (attachmentId == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    String fileName = getFileNameFromRequest(request);
    String waveRefStr = getWaveRefFromRequest(request);

    AttachmentMetadata metadata = service.getMetadata(attachmentId);
    WaveletName waveletName;

    if (metadata == null) {
      // Old attachments does not have metainfo.
      if (waveRefStr != null) {
        waveletName = AttachmentUtil.waveRef2WaveletName(waveRefStr);
      } else {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
    } else {
      waveletName = AttachmentUtil.waveRef2WaveletName(metadata.getWaveRef());
    }

    ParticipantId user = sessionManager.getLoggedInUser(request.getSession(false));
    boolean isAuthorized = false;
    try {
      isAuthorized = waveletProvider.checkAccessPermission(waveletName, user);
    } catch (WaveServerException e) {
      LOG.warning("Problem while authorizing user: " + user + " for wavelet: " + waveletName, e);
    }
    if (!isAuthorized) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    if (metadata == null) {
      metadata = service.buildAndStoreMetadataWithThumbnail(attachmentId, waveletName, fileName, null);
    }

    String contentType;
    AttachmentData data;
    if (request.getRequestURI().startsWith(ATTACHMENT_URL)) {
      contentType = metadata.getMimeType();
      data = service.getAttachment(attachmentId);
      if (data == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return;
      }
    } else if (request.getRequestURI().startsWith(THUMBNAIL_URL)) {
      if (metadata.hasImageMetadata()) {
        contentType = AttachmentService.THUMBNAIL_MIME_TYPE;
        data = service.getThumbnail(attachmentId);
        if (data == null) {
          response.sendError(HttpServletResponse.SC_NOT_FOUND);
          return;
        }
      } else {
        contentType = THUMBNAIL_PATTERN_FORMAT_NAME;
        data = getThumbnailByContentType(metadata.getMimeType());
      }
    } else {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
    if (data == null) {
      response.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    response.setContentType(contentType);
    response.setContentLength((int)data.getSize());
    response.setHeader("Content-Disposition", "attachment; filename=\"" + metadata.getFileName() + "\"");
    response.setStatus(HttpServletResponse.SC_OK);
    response.setDateHeader("Last-Modified", Calendar.getInstance().getTimeInMillis());
    AttachmentUtil.writeTo(data.getInputStream(), response.getOutputStream());

    LOG.info("Fetched attachment with id '" + attachmentId + "'");
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
      IOException {
    // Process only multipart requests.
    if (ServletFileUpload.isMultipartContent(request)) {
      // Create a factory for disk-based file items.
      FileItemFactory factory = new DiskFileItemFactory();

      // Create a new file upload handler.
      ServletFileUpload upload = new ServletFileUpload(factory);

      // Parse the request.
      try {
        @SuppressWarnings("unchecked")
        List<FileItem> items = upload.parseRequest(request);
        AttachmentId id = null;
        String waveRefStr = null;
        FileItem fileItem = null;
        for (FileItem item : items) {
          // Process only file upload - discard other form item types.
          if (item.isFormField()) {
            if (item.getFieldName().equals("attachmentId")) {
              id = AttachmentId.deserialise(item.getString());
            }
            if (item.getFieldName().equals("waveRef")) {
              waveRefStr = item.getString();
            }
          } else {
            fileItem = item;
          }
        }

        if (id == null) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No attachment Id in the request.");
          return;
        }
        if (waveRefStr == null) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No wave reference in request.");
          return;
        }

        WaveletName waveletName = AttachmentUtil.waveRef2WaveletName(waveRefStr);
        ParticipantId user = sessionManager.getLoggedInUser(request.getSession(false));
        boolean isAuthorized = waveletProvider.checkAccessPermission(waveletName, user);
        if (!isAuthorized) {
          response.sendError(HttpServletResponse.SC_FORBIDDEN);
          return;
        }

        // Get only the file name not whole path.
        if (fileItem != null && fileItem.getName()  != null) {
          String fileName = FilenameUtils.getName(fileItem.getName());
          service.storeAttachment(id, fileItem.getInputStream(), waveletName, fileName, user);
          response.setStatus(HttpServletResponse.SC_CREATED);
          String msg =
              String.format("The file with name: %s and id: %s was created successfully.",
                  fileName, id);
          LOG.fine(msg);
          response.getWriter().print("OK");
          response.flushBuffer();
        }
      } catch (Exception e) {
        LOG.severe("Upload error", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "An error occurred while upload the file : " + e.getMessage());
      }
    } else {
      LOG.severe("Request contents type is not supported by the servlet.");
      response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
          "Request contents type is not supported by the servlet.");
    }
  }

  private static AttachmentId getAttachmentIdFromRequest(HttpServletRequest request) {
    if (request.getPathInfo().length() == 0) {
      return null;
    }
    String id = getAttachmentIdStringFromRequest(request);
    try {
      return AttachmentId.deserialise(id);
    } catch (InvalidIdException ex) {
      LOG.log(Level.SEVERE, "Deserialize attachment Id " + id, ex);
      return null;
    }
  }

  private static String getAttachmentIdStringFromRequest(HttpServletRequest request) {
    // Discard the leading '/' in the pathinfo.
    return request.getPathInfo().substring(1);
  }

  private AttachmentData getThumbnailByContentType(String contentType) throws IOException {
    File file = new File(thumbnailPattternsDirectory, contentType.replaceAll("/", "_"));
    if (!file.exists()) {
      file = new File(thumbnailPattternsDirectory, THUMBNAIL_PATTERN_DEFAULT);
    }
    final File thumbFile = file;
    return new AttachmentData() {

      @Override
      public InputStream getInputStream() throws IOException {
        return new FileInputStream(thumbFile);
      }

      @Override
      public long getSize() {
        return thumbFile.length();
      }
    };
  }

  private static String getFileNameFromRequest(HttpServletRequest request) {
    String fileName = request.getParameter("fileName");
    return fileName != null ? fileName : "";
  }

  private static String getWaveRefFromRequest(HttpServletRequest request) {
    String waveRefStrEncoded = request.getParameter("waveRef");
    String waveRefStr = null;
    if (waveRefStrEncoded != null) {
      try {
        waveRefStr = URLDecoder.decode(waveRefStrEncoded, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        LOG.warning("Problem decoding: " + waveRefStrEncoded, e);
      }
    }
    return waveRefStr;
  }
}
