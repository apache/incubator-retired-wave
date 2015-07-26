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

package org.waveprotocol.wave.client.editor.content;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.DocOpComponentType;
import org.waveprotocol.wave.model.document.operation.DocOp;

/**
 * A DocOp that only supports apply(), not random access of mutation components.
 * This violates the DocOp contract.  This is a hack that shouldn't be used
 * anywhere outside of DiffHighlightingFilter.
 *
 * @author ohler@google.com (Christian Ohler)
 */
abstract class DiffOpWrapperBase implements DocOp {

  private final String exceptionMessage;

  protected DiffOpWrapperBase(String exceptionMessage) {
    this.exceptionMessage = exceptionMessage;
  }

  @Override
  public int size() {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public DocOpComponentType getType(int i) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public void applyComponent(int i, DocOpCursor c) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public AnnotationBoundaryMap getAnnotationBoundary(int i) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public String getCharactersString(int i) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public String getElementStartTag(int i) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public Attributes getElementStartAttributes(int i) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public int getRetainItemCount(int i) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public String getDeleteCharactersString(int i) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public String getDeleteElementStartTag(int i) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public Attributes getDeleteElementStartAttributes(int i) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public Attributes getReplaceAttributesOldAttributes(int i) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public Attributes getReplaceAttributesNewAttributes(int i) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

  @Override
  public AttributesUpdate getUpdateAttributesUpdate(int i) {
    throw new UnsupportedOperationException(exceptionMessage);
  }

}
