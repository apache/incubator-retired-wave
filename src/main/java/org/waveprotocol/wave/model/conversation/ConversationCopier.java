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

package org.waveprotocol.wave.model.conversation;

import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.Wavelet;

import java.util.Set;

/**
 * Utility methods for copying conversation wavelets. This is an instantiable class to
 * aid unit testing.
 *
 */
public class ConversationCopier {

  /** Utility class, not instantiable. */
  private ConversationCopier() {
  }

  /**
   * Copies the contents of a conversational wavelet to another wavelet,
   * first clearing any documents in the destination document.
   *
   * @param sourceWavelet the source wavelet
   * @param destWavelet the destination wavelet
   */
  public static void clearAndCopyWaveletContents(Wavelet sourceWavelet, Wavelet destWavelet) {
    clearWaveletContents(destWavelet);
    copyWaveletContents(sourceWavelet, destWavelet);
  }

  /**
   * Copies a document from one wavelet to another, appending the contents if the
   * destination document already exists.
   *
   * @param sourceWavelet the source wavelet
   * @param destWavelet the destination wavelet
   * @param docId the id of the document to copy
   */
  public static void copyDocument(Wavelet sourceWavelet, Wavelet destWavelet, String docId) {
    Document document = sourceWavelet.getDocument(docId);
    DocInitialization docInit = document.toInitialization();
    // TODO(user): add a createDocument method to Wavelet so that we can push this ugliness
    // down the stack.
    ObservableDocument destDoc = destWavelet.getDocument(docId);
    destDoc.hackConsume(Nindo.fromDocOp(docInit, false /* don't remove skip */));
  }

  /**
   * Copy the contents of a conversational wavelet to another wavelet, appending
   * the contents to any destination documents which already exist.
   *
   * @param sourceWavelet the source wavelet
   * @param destWavelet the destination wavelet
   */
  public static void copyWaveletContents(Wavelet sourceWavelet, Wavelet destWavelet) {
    Set<String> docIds = sourceWavelet.getDocumentIds();
    Preconditions.checkArgument(docIds.contains(IdUtil.MANIFEST_DOCUMENT_ID),
        "Wavelet is not conversational.");

    for (String docId : docIds) {
      if (!IdUtil.isManifestDocument(docId)) {
        copyDocument(sourceWavelet, destWavelet, docId);
      }
    }
    copyDocument(sourceWavelet, destWavelet, IdUtil.MANIFEST_DOCUMENT_ID);
  }

  /**
   * Erases the content of the provided document.
   *
   * @param wavelet the wavelet containing the document to erase
   * @param documentId the id of the document to erase
   */
  public static void clearDocument(Wavelet wavelet, String documentId) {
    Document document = wavelet.getDocument(documentId);
    if (document.size() != 0) {
      document.emptyElement(document.getDocumentElement());
    }
  }

  /**
   * Clears the content of each document / blip on the provided wavelet.
   *
   * @param wavelet the wavelet to clear
   */
  public static void clearWaveletContents(Wavelet wavelet) {
    Set<String> docIds = wavelet.getDocumentIds();
    for (String docId : docIds) {
      if (!IdUtil.isManifestDocument(docId)) {
        clearDocument(wavelet, docId);
      }
    }
    clearDocument(wavelet, IdUtil.MANIFEST_DOCUMENT_ID);
  }
}
