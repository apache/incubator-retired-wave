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

package org.waveprotocol.wave.model.document.operation.util;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;

/**
 * Decomposes an operation into units of size one
 *
 * Useful utility for writing other debugging utilities
 */
public class ExplodedDocOp {

  private final DocOp source;

  /**
   * Operation to explode
   *
   * @param source
   */
  public ExplodedDocOp(DocOp source) {
    this.source = source;
  }

  private void apply(final DocOpCursor target) {
    source.apply(new DocOpCursor() {
      @Override
      public void deleteCharacters(String chars) {
        for (int i = 0; i < chars.length(); i++) {
          target.deleteCharacters(chars.substring(i, i + 1));
        }
      }

      @Override
      public void deleteElementEnd() {
        target.deleteElementEnd();
      }

      @Override
      public void deleteElementStart(String type, Attributes attrs) {
        target.deleteElementStart(type, attrs);
      }

      @Override
      public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
        target.replaceAttributes(oldAttrs, newAttrs);
      }

      @Override
      public void retain(int itemCount) {
        for (int i = 0; i < itemCount; i++) {
          target.retain(1);
        }
      }

      @Override
      public void updateAttributes(AttributesUpdate attrUpdate) {
        target.updateAttributes(attrUpdate);
      }

      @Override
      public void annotationBoundary(AnnotationBoundaryMap map) {
        target.annotationBoundary(map);
      }

      @Override
      public void characters(String chars) {
        for (int i = 0; i < chars.length(); i++) {
          target.characters(chars.substring(i, i + 1));
        }
      }

      @Override
      public void elementEnd() {
        target.elementEnd();
      }

      @Override
      public void elementStart(String type, Attributes attrs) {
        target.elementStart(type, attrs);
      }
    });
  }

  public <T> T explodeWith(EvaluatingDocOpCursor<T> cursor) {
    apply(cursor);
    return cursor.finish();
  }

  public static DocOp explode(DocOp op) {
    return new ExplodedDocOp(op).explodeWith(new DocOpBuffer());
  }

  public static DocInitialization explode(DocInitialization op) {
    return DocOpUtil.asInitialization(explode((DocOp) op));
  }
}
