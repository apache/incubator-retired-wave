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
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.AnnotationBoundary;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.Characters;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.DeleteCharacters;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.DeleteElementEnd;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.DeleteElementStart;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.DocOpComponent;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.ElementEnd;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.ElementStart;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.ReplaceAttributes;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.Retain;
import org.waveprotocol.wave.model.document.operation.impl.OperationComponents.UpdateAttributes;

import java.util.ArrayList;

/**
 * A builder for {@link DocOp}s.
 *
 * Use {@link DocOpBuffer} instead if you need an implementation of the interface
 * {@link org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor}.
 */
public class DocOpBuilder {
  private static final DocOpComponent[] EMPTY_ARRAY = new DocOpComponent[0];

  private final ArrayList<DocOpComponent> accu = new ArrayList<DocOpComponent>();

  /**
   * Constructs an operation from this builder's state.
   *
   * Behaviour is undefined if this builder is used after calling this method.
   */
  public final DocOp build() {
    return BufferedDocOpImpl.create(accu.toArray(EMPTY_ARRAY));
  }

  /** @see #build() */
  // This is dangerous; we currently use it for ill-formedness-detection
  // tests, and may use it for efficiency in other places in the future.
  public final DocOp buildUnchecked() {
    // TODO: This should not need a call to asInitialization().
    return BufferedDocOpImpl.createUnchecked(accu.toArray(EMPTY_ARRAY));
  }

  public final DocOpBuilder annotationBoundary(AnnotationBoundaryMap map) {
    accu.add(new AnnotationBoundary(map));
    return this;
  }
  public final DocOpBuilder characters(String s) {
    accu.add(new Characters(s));
    return this;
  }
  public final DocOpBuilder elementEnd() {
    accu.add(ElementEnd.INSTANCE);
    return this;
  }
  public final DocOpBuilder elementStart(String type, Attributes attrs) {
    accu.add(new ElementStart(type, attrs));
    return this;
  }
  public final DocOpBuilder deleteCharacters(String s) {
    accu.add(new DeleteCharacters(s));
    return this;
  }
  public final DocOpBuilder retain(int itemCount) {
    accu.add(new Retain(itemCount));
    return this;
  }
  public final DocOpBuilder deleteElementEnd() {
    accu.add(DeleteElementEnd.INSTANCE);
    return this;
  }
  public final DocOpBuilder deleteElementStart(String type, Attributes attrs) {
    accu.add(new DeleteElementStart(type, attrs));
    return this;
  }
  public final DocOpBuilder replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
    accu.add(new ReplaceAttributes(oldAttrs, newAttrs));
    return this;
  }
  public final DocOpBuilder updateAttributes(AttributesUpdate update) {
    accu.add(new UpdateAttributes(update));
    return this;
  }
}
