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

package org.waveprotocol.wave.media.model;

import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.Set;

/**
 * Utility class for attachment data document related functionality.
 *
 */
public class AttachmentDataDocHelper {

  private AttachmentDataDocHelper() {
    // Utility classes should not have public constructors.
  }

  /**
   * Converts an attachment id into an attachment data document id.
   *
   * @param attachmentId the attachment id to be converted.
   * @return the corresponding attachment data doc for the attachment id.
   */
  public static String dataDocIdFromAttachmentId(String attachmentId) {
    return IdUtil.join(IdConstants.ATTACHMENT_METADATA_PREFIX, attachmentId);
  }

  /**
   * Locates an attachment data doc for a given attachment id within a wavelet.
   * First, it looks for an attachment data doc that matches the fully
   * qualified id. If that cannot be found, it looks for any attachment data
   * document where the id component matches (ignoring the domain component).
   * If no matching data document can be found, a new one is created.
   *
   * TODO(user): Remove this method once the attachment id domain migration
   * is complete.
   *
   * @param wavelet the wavelet within which to search for the data document.
   * @param attachmentIdString the id of the attachment which forms the data
   *     doc id.
   * @return the found or newly created data document.
   * @throws InvalidIdException if an invalid attachment id is
   *     encountered.
   */
  public static ObservableDocument getAttachmentDataDoc(Wavelet wavelet,
      String attachmentIdString) throws InvalidIdException {
    Preconditions.checkNotNull(attachmentIdString, "attachmentIdString must not be null");
    if (attachmentIdString.startsWith(IdConstants.ATTACHMENT_METADATA_PREFIX)) {
      Preconditions.illegalArgument("attachmentIdString must not start with "
          + IdConstants.ATTACHMENT_METADATA_PREFIX);
    }
    Set<String> documentIds = wavelet.getDocumentIds();
    String attachDataDocId = null;

    // First, search for a data doc that matches the id.
    attachDataDocId = dataDocIdFromAttachmentId(attachmentIdString);
    if (documentIds.contains(attachDataDocId)) {
      return wavelet.getDocument(attachDataDocId);
    }

    // Second, strip off the domain component and look for pre-domain ids.
    AttachmentId attachmentId = AttachmentId.deserialise(attachmentIdString);
    attachDataDocId = dataDocIdFromAttachmentId(attachmentId.getId());
    if (documentIds.contains(attachDataDocId)) {
      return wavelet.getDocument(attachDataDocId);
    }

    // Third, check all attachment data docs where the id component matches.
    for (String documentId : documentIds) {
      if (IdUtil.isAttachmentDataDocument(documentId)) {
        String[] docIdComponents = IdUtil.split(documentId);
        if (docIdComponents != null && docIdComponents.length == 2) {
          AttachmentId docAttachmentId = AttachmentId.deserialise(docIdComponents[1]);
          if (docAttachmentId.getId().equals(attachmentId.getId())) {
            return wavelet.getDocument(documentId);
          }
        } else {
          // Using NOP logger as model package contains no dependencies.
          LoggerBundle.NOP_IMPL.error().log("AttachmentDataDocHelper: Can't parse the" +
              "attachment data doc id - " + documentId);
        }
      }
    }

    // Otherwise, create the data document for the given id.
    attachDataDocId = dataDocIdFromAttachmentId(attachmentIdString);
    return wavelet.getDocument(attachDataDocId);
  }
}
