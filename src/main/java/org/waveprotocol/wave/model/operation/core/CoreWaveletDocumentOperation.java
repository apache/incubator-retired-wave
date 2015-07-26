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

package org.waveprotocol.wave.model.operation.core;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

/**
 * Operation class for an operation that will modify a document within a given wavelet.
 */
public final class CoreWaveletDocumentOperation extends CoreWaveletOperation {
  /** Identifier of the document within the target wavelet to modify. */
  private final String documentId;

  /** Document operation which modifies the target document. */
  private final DocOp operation;

  /**
   * Constructor.
   *
   * @param documentId
   * @param operation
   */
  public CoreWaveletDocumentOperation(String documentId, DocOp operation) {
    Preconditions.checkNotNull(documentId, "Null document id");
    Preconditions.checkNotNull(operation, "Null document operation");
    this.documentId = documentId;
    this.operation = operation;
  }

  public String getDocumentId() {
    return documentId;
  }

  public DocOp getOperation() {
    return operation;
  }

  @Override
  protected void doApply(CoreWaveletData target) throws OperationException {
    target.modifyDocument(documentId, operation);
  }

  @Override
  public CoreWaveletOperation getInverse() {
    DocOpInverter<DocOp> inverse = new DocOpInverter<DocOp>(new DocOpBuffer());
    operation.apply(inverse);
    return new CoreWaveletDocumentOperation(documentId, inverse.finish());
  }

  @Override
  public String toString() {
    return "WaveletDocumentOperation(" + documentId + "," + operation + ")";
  }

  @Override
  public int hashCode() {
    // Note that we don't have an implementation of operation.hashCode()
    // which is compatible with OpComparators.SYNTACTIC_IDENTITY.equal().
    // Therefore we ignore operation in the hash code computation here
    // so that it's compatible with equals().
    return documentId.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof CoreWaveletDocumentOperation)) {
      return false;
    }
    CoreWaveletDocumentOperation other = (CoreWaveletDocumentOperation) obj;
    return documentId.equals(other.documentId)
        && OpComparators.SYNTACTIC_IDENTITY.equal(operation, other.operation);
  }
}
