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

package org.waveprotocol.wave.model.document.operation.impl;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;

public class InitializationCursorAdapter implements DocOpCursor {

  public static DocOpCursor adapt(DocInitializationCursor initializationCursor) {
    if (initializationCursor instanceof DocOpCursor) {
      return (DocOpCursor) initializationCursor;
    } else {
      return new InitializationCursorAdapter(initializationCursor);
    }
  }

  private final DocInitializationCursor inner;

  private InitializationCursorAdapter(DocInitializationCursor inner) {
    this.inner = inner;
  }

  @Override
  public void annotationBoundary(AnnotationBoundaryMap map) {
    inner.annotationBoundary(map);
  }

  @Override
  public void characters(String chars) {
    inner.characters(chars);
  }

  @Override
  public void elementEnd() {
    inner.elementEnd();
  }

  @Override
  public void elementStart(String type, Attributes attrs) {
    inner.elementStart(type, attrs);
  }

  @Override
  public void deleteCharacters(String chars) {
    throw new UnsupportedOperationException("deleteCharacters");
  }

  @Override
  public void deleteElementEnd() {
    throw new UnsupportedOperationException("deleteElementEnd");
  }

  @Override
  public void deleteElementStart(String type, Attributes attrs) {
    throw new UnsupportedOperationException("deleteElementStart");
  }

  @Override
  public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
    throw new UnsupportedOperationException("replaceAttributes");
  }

  @Override
  public void retain(int itemCount) {
    throw new UnsupportedOperationException("retain");
  }

  @Override
  public void updateAttributes(AttributesUpdate attrUpdate) {
    throw new UnsupportedOperationException("updateAttributes");
  }


}
