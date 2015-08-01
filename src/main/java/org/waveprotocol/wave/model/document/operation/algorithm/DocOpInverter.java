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

package org.waveprotocol.wave.model.document.operation.algorithm;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;

/**
 * A reverser of document operations.
 *
 * @param <T> the type that the <code>finish()</code> method returns
 */
public final class DocOpInverter<T> implements EvaluatingDocOpCursor<T> {

  private final EvaluatingDocOpCursor<T> target;

  public DocOpInverter(EvaluatingDocOpCursor<T> target) {
    this.target = target;
  }

  @Override
  public T finish() {
    return target.finish();
  }

  @Override
  public void retain(int itemCount) {
    target.retain(itemCount);
  }

  @Override
  public void characters(String chars) {
    target.deleteCharacters(chars);
  }

  @Override
  public void elementStart(String type, Attributes attrs) {
    target.deleteElementStart(type, attrs);
  }

  @Override
  public void elementEnd() {
    target.deleteElementEnd();
  }

  @Override
  public void deleteCharacters(String chars) {
    target.characters(chars);
  }

  @Override
  public void deleteElementStart(String type, Attributes attrs) {
    target.elementStart(type, attrs);
  }

  @Override
  public void deleteElementEnd() {
    target.elementEnd();
  }

  @Override
  public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
    target.replaceAttributes(newAttrs, oldAttrs);
  }

  @Override
  public void updateAttributes(AttributesUpdate attrUpdate) {
    AttributesUpdate update = new AttributesUpdateImpl();
    // TODO: This is a little silly. We should do this a better way.
    for (int i = 0; i < attrUpdate.changeSize(); ++i) {
      update = update.composeWith(new AttributesUpdateImpl(attrUpdate.getChangeKey(i),
          attrUpdate.getNewValue(i), attrUpdate.getOldValue(i)));
    }
    target.updateAttributes(update);
  }

  @Override
  public void annotationBoundary(final AnnotationBoundaryMap map) {
    // Warning: Performing multiple reversals can cause multiple wrappers to be created.
    // TODO: Maybe we should change this so that this issue doesn't occur.
    target.annotationBoundary(new AnnotationBoundaryMap() {

      @Override
      public int changeSize() {
        return map.changeSize();
      }

      @Override
      public String getChangeKey(int changeIndex) {
        return map.getChangeKey(changeIndex);
      }

      @Override
      public String getOldValue(int changeIndex) {
        return map.getNewValue(changeIndex);
      }

      @Override
      public String getNewValue(int changeIndex) {
        return map.getOldValue(changeIndex);
      }

      @Override
      public int endSize() {
        return map.endSize();
      }

      @Override
      public String getEndKey(int endIndex) {
        return map.getEndKey(endIndex);
      }

    });
  }

  public static DocOp invert(DocOp input) {
    DocOpInverter<DocOp> inverter = new DocOpInverter<DocOp>(new DocOpBuffer());
    input.apply(inverter);
    return inverter.finish();
  }

}
