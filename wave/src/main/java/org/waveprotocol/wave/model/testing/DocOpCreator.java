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

package org.waveprotocol.wave.model.testing;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.EvaluatingDocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.AnnotationBoundaryMapImpl;
import org.waveprotocol.wave.model.document.operation.impl.AttributesUpdateImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuffer;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * A convenience class for creating document operations.
 *
 */
public class DocOpCreator {

  /**
   * A builder for BufferedDocOps which is used by the static convenience
   * methods of DocOpCreator for creating operations. This builder allows
   * calling "retain" with an argument of 0 and "characters" and
   * "deleteCharacters" with an empty string argument, in order to make the
   * building process easier in some circumstances.
   */
  private static class SimplifyingDocOpBuilder {

    private final EvaluatingDocOpCursor<DocOp> buffer = new DocOpBuffer();

    public final DocOp build() {
      return buffer.finish();
    }

    public final SimplifyingDocOpBuilder retain(int itemCount) {
      Preconditions.checkArgument(itemCount >= 0, "Negative item count");
      if (itemCount > 0) {
        buffer.retain(itemCount);
      }
      return this;
    }

    public final SimplifyingDocOpBuilder characters(String characters) {
      if (characters.length() > 0) {
        buffer.characters(characters);
      }
      return this;
    }

    public final SimplifyingDocOpBuilder elementStart(String type, Attributes attrs) {
      buffer.elementStart(type, attrs);
      return this;
    }

    public final SimplifyingDocOpBuilder elementEnd() {
      buffer.elementEnd();
      return this;
    }

    public final SimplifyingDocOpBuilder deleteCharacters(String characters) {
      if (characters.length() > 0) {
        buffer.deleteCharacters(characters);
      }
      return this;
    }

    public final SimplifyingDocOpBuilder deleteElementStart(String type, Attributes attrs) {
      buffer.deleteElementStart(type, attrs);
      return this;
    }

    public final SimplifyingDocOpBuilder deleteElementEnd() {
      buffer.deleteElementEnd();
      return this;
    }

    public final SimplifyingDocOpBuilder replaceAttributes(Attributes oldAttrs,
        Attributes newAttrs) {
      buffer.replaceAttributes(oldAttrs, newAttrs);
      return this;
    }

    public final SimplifyingDocOpBuilder updateAttributes(AttributesUpdate update) {
      buffer.updateAttributes(update);
      return this;
    }

    public final SimplifyingDocOpBuilder setAnnotation(int itemCount, String key, String oldValue,
        String newValue) {
      Preconditions.checkArgument(itemCount >= 0, "Negative item count");
      if (itemCount > 0) {
        buffer.annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .updateValues(key, oldValue, newValue)
            .build());
        buffer.retain(itemCount);
        buffer.annotationBoundary(AnnotationBoundaryMapImpl.builder()
            .initializationEnd(key)
            .build());
      }
      return this;
    }

  }

  /**
   * Creates a document operation that inserts the given characters at the given
   * location.
   *
   * @param size The initial size of the document.
   * @param location The location at which to insert characters.
   * @param characters The characters to insert.
   * @return The document operation.
   */
  public static DocOp insertCharacters(int size, int location, String characters) {
    return new SimplifyingDocOpBuilder()
        .retain(location)
        .characters(characters)
        .retain(size - location)
        .build();
  }

  /**
   * Creates a document operation that inserts an element at the given location.
   *
   * @param size The initial size of the document.
   * @param location The location at which to insert the element.
   * @param type The type of the element.
   * @param attributes The attributes of the element.
   * @return The document operation.
   */
  public static DocOp insertElement(int size, int location, String type,
      Attributes attributes) {
    return new SimplifyingDocOpBuilder()
        .retain(location)
        .elementStart(type, attributes)
        .elementEnd()
        .retain(size - location)
        .build();
  }

  /**
   * Creates a document operation that deletes the characters denoted by the
   * given range.
   *
   * @param size The initial size of the document.
   * @param location The location the characters to delete.
   * @param characters The characters to delete.
   * @return The document operation.
   */
  public static DocOp deleteCharacters(int size, int location, String characters) {
    return new SimplifyingDocOpBuilder()
        .retain(location)
        .deleteCharacters(characters)
        .retain(size - location - characters.length())
        .build();
  }

  /**
   * Creates a document operation that deletes an empty element at a given
   * location.
   *
   * @param size The initial size of the document.
   * @param location The location of the element to delete.
   * @param type The type of the element.
   * @param attributes The attributes of the element.
   * @return The document operation.
   */
  public static DocOp deleteElement(int size, int location, String type,
      Attributes attributes) {
    return new SimplifyingDocOpBuilder()
        .retain(location)
        .deleteElementStart(type, attributes)
        .deleteElementEnd()
        .retain(size - location - 2)
        .build();
  }

  /**
   * Creates a document operation that replace all the attributes of an element.
   *
   * @param size The initial size of the document.
   * @param location The location of the element whose attributes are to be set.
   * @param oldAttr The old attributes of the element.
   * @param newAttr The new attributes that the element should have.
   * @return The document operation.
   */
  public static DocOp replaceAttributes(int size, int location, Attributes oldAttr,
      Attributes newAttr) {
    return new SimplifyingDocOpBuilder()
        .retain(location)
        .replaceAttributes(oldAttr, newAttr)
        .retain(size - location - 1)
        .build();
  }

  /**
   * Creates a document operation that sets an attribute of an element.
   *
   * @param size The initial size of the document.
   * @param location The location of the element whose attribute is to be set.
   * @param name The name of the attribute to set.
   * @param oldValue The old value of the attribute.
   * @param newValue The value to which to set the attribute.
   * @return The document operation.
   */
  public static DocOp setAttribute(int size, int location, String name, String oldValue,
      String newValue) {
    return new SimplifyingDocOpBuilder()
        .retain(location)
        .updateAttributes(new AttributesUpdateImpl(name, oldValue, newValue))
        .retain(size - location - 1)
        .build();
  }

  /**
   * Creates a document operation that sets an annotation over a range.
   *
   * @param size The initial size of the document.
   * @param start The location of the start of the range on which the annotation
   *        is to be set.
   * @param end The location of the end of the range on which the annotation is
   *        to be set.
   * @param key The annotation key.
   * @param oldValue The old annotation value.
   * @param newValue The new annotation value.
   * @return The document operation.
   */
  public static DocOp setAnnotation(int size, int start, int end, String key,
      String oldValue, String newValue) {
    return new SimplifyingDocOpBuilder()
        .retain(start)
        .setAnnotation(end - start, key, oldValue, newValue)
        .retain(size - end)
        .build();
  }

  /**
   * Creates a document operation that acts as the identity on a document.
   *
   * @param size The size of the document.
   * @return The document operation.
   */
  public static DocOp identity(int size) {
    return new SimplifyingDocOpBuilder()
        .retain(size)
        .build();
  }

}
