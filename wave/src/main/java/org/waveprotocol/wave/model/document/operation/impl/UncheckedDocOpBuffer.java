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
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
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
 * An implementation of {@link EvaluatingDocOpCursor} that buffers the operation
 * and returns it as a {@link DocOp}.
 *
 * See also {@link DocOpBuilder}, which is similar but implements a standard
 * Java builder pattern instead of EvaluatingDocOpCursor.
 *
 * The operation returned is not guaranteed to be well-formed, as there is no
 * check upon completion by default.
 *
 * Use sparingly. Its use should only be for ill-formedness tests, and possibly
 * for efficiency if profiling reveals it to be necessary. If in doubt, use
 * DocOpBuffer which does a well-formedness check.
 */
public class UncheckedDocOpBuffer implements EvaluatingDocOpCursor<DocOp> {

  private static final DocOpComponent[] EMPTY_ARRAY = new DocOpComponent[0];

  private final ArrayList<DocOpComponent> accu = new ArrayList<DocOpComponent>();

  /**
   * {@inheritDoc}
   *
   * Behaviour is undefined if this buffer is used after calling this method.
   */
  @Override
  public DocOp finish() {
    return finishUnchecked();
  }

  /**
   * Finish and do a well formedness check as well
   *
   * Behaviour is undefined if this buffer is used after calling this method.
   */
  public final DocOp finishChecked() {
    return BufferedDocOpImpl.create(accu.toArray(EMPTY_ARRAY));
  }

  /**
   * @see #finish()
   */
  // This is dangerous; we currently use it for ill-formedness-detection
  // tests, and may use it for efficiency in other places in the future.
  public final DocOp finishUnchecked() {
    return BufferedDocOpImpl.createUnchecked(accu.toArray(EMPTY_ARRAY));
  }

  @Override
  public final void annotationBoundary(AnnotationBoundaryMap map) {
    accu.add(new AnnotationBoundary(map));
  }
  @Override
  public final void characters(String s) {
    accu.add(new Characters(s));
  }
  @Override
  public final void elementEnd() {
    accu.add(ElementEnd.INSTANCE);
  }
  @Override
  public final void elementStart(String type, Attributes attrs) {
    accu.add(new ElementStart(type, attrs));
  }
  @Override
  public final void deleteCharacters(String s) {
    accu.add(new DeleteCharacters(s));
  }
  @Override
  public final void retain(int itemCount) {
    accu.add(new Retain(itemCount));
  }
  @Override
  public final void deleteElementEnd() {
    accu.add(DeleteElementEnd.INSTANCE);
  }
  @Override
  public final void deleteElementStart(String type, Attributes attrs) {
    accu.add(new DeleteElementStart(type, attrs));
  }
  @Override
  public final void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
    accu.add(new ReplaceAttributes(oldAttrs, newAttrs));
  }
  @Override
  public final void updateAttributes(AttributesUpdate update) {
    accu.add(new UpdateAttributes(update));
  }
}

