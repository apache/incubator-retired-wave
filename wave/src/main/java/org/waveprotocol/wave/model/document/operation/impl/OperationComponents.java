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
import org.waveprotocol.wave.model.document.operation.DocInitializationComponentType;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.DocOpComponentType;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;

/**
 * Immutable reference mutation components.
 *
 *
 */
public class OperationComponents {

  private OperationComponents() {}

  public static abstract class DocOpComponent {
    abstract DocOpComponentType getType();
    abstract void apply(DocOpCursor c);
  }

  public static abstract class DocInitializationComponent extends DocOpComponent {
    @Override
    abstract DocInitializationComponentType getType();
    abstract void apply(DocInitializationCursor c);
    @Override
    final void apply(DocOpCursor c) {
      apply((DocInitializationCursor) c);
    }
  }

  public static class AnnotationBoundary extends DocInitializationComponent {
    final AnnotationBoundaryMap boundary;
    AnnotationBoundary(AnnotationBoundaryMap boundary) {
      this.boundary = boundary;
    }
    @Override
    DocInitializationComponentType getType() {
      return DocInitializationComponentType.ANNOTATION_BOUNDARY;
    }
    @Override
    void apply(DocInitializationCursor c) {
      c.annotationBoundary(boundary);
    }
  }

  public static class Characters extends DocInitializationComponent {
    final String string;
    Characters(String string) {
      this.string = string;
    }
    @Override
    DocInitializationComponentType getType() {
      return DocInitializationComponentType.CHARACTERS;
    }
    @Override
    void apply(DocInitializationCursor c) {
      c.characters(string);
    }
  }

  public static class ElementStart extends DocInitializationComponent {
    final String type;
    final Attributes attrs;
    ElementStart(String type, Attributes attrs) {
      this.type = type;
      this.attrs = attrs;
    }
    @Override
    DocInitializationComponentType getType() {
      return DocInitializationComponentType.ELEMENT_START;
    }
    @Override
    void apply(DocInitializationCursor c) {
      c.elementStart(type, attrs);
    }
  }

  public static class ElementEnd extends DocInitializationComponent {
    static final ElementEnd INSTANCE = new ElementEnd();
    ElementEnd() {}
    @Override
    DocInitializationComponentType getType() {
      return DocInitializationComponentType.ELEMENT_END;
    }
    @Override
    void apply(DocInitializationCursor c) {
      c.elementEnd();
    }
  }

  public static class Retain extends DocOpComponent {
    final int itemCount;
    Retain(int itemCount) {
      this.itemCount = itemCount;
    }
    @Override
    DocOpComponentType getType() {
      return DocOpComponentType.RETAIN;
    }
    @Override
    void apply(DocOpCursor c) {
      c.retain(itemCount);
    }
  }

  public static class DeleteCharacters extends DocOpComponent {
    final String string;
    DeleteCharacters(String string) {
      this.string = string;
    }
    @Override
    DocOpComponentType getType() {
      return DocOpComponentType.DELETE_CHARACTERS;
    }
    @Override
    void apply(DocOpCursor c) {
      c.deleteCharacters(string);
    }
  }

  public static class DeleteElementStart extends DocOpComponent {
    final String type;
    final Attributes attrs;
    DeleteElementStart(String type, Attributes attrs) {
      this.type = type;
      this.attrs = attrs;
    }
    @Override
    DocOpComponentType getType() {
      return DocOpComponentType.DELETE_ELEMENT_START;
    }
    @Override
    void apply(DocOpCursor c) {
      c.deleteElementStart(type, attrs);
    }
  }

  public static class DeleteElementEnd extends DocOpComponent {
    static final DeleteElementEnd INSTANCE = new DeleteElementEnd();
    DeleteElementEnd() {}
    @Override
    DocOpComponentType getType() {
      return DocOpComponentType.DELETE_ELEMENT_END;
    }
    @Override
    void apply(DocOpCursor c) {
      c.deleteElementEnd();
    }
  }

  public static class ReplaceAttributes extends DocOpComponent {
    final Attributes oldAttrs;
    final Attributes newAttrs;
    ReplaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
      this.oldAttrs = oldAttrs;
      this.newAttrs = newAttrs;
    }
    @Override
    DocOpComponentType getType() {
      return DocOpComponentType.REPLACE_ATTRIBUTES;
    }
    @Override
    void apply(DocOpCursor c) {
      c.replaceAttributes(oldAttrs, newAttrs);
    }
  }

  public static class UpdateAttributes extends DocOpComponent {
    final AttributesUpdate update;
    UpdateAttributes(AttributesUpdate update) {
      this.update = update;
    }
    @Override
    DocOpComponentType getType() {
      return DocOpComponentType.UPDATE_ATTRIBUTES;
    }
    @Override
    void apply(DocOpCursor c) {
      c.updateAttributes(update);
    }
  }
}
