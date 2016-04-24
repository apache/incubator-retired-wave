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

package org.waveprotocol.wave.model.document.operation;

import org.waveprotocol.wave.model.operation.Operation;

/**
 * A document operation.
 *
 * Implementations MUST be immutable.
 */
// TODO: this isn't really an operation on DocOpCursors.
public interface DocOp extends Operation<DocOpCursor> {
  /**
   * Essentially a visitor pattern.
   */
  void apply(DocOpCursor c);

  /** @return the number of components to this operation. */
  int size();
  DocOpComponentType getType(int i);

  // A method get(int) that returns a DocOpComponent would be more intuitive,
  // but with accessors like the ones below, we can avoid reifying the component
  // objects.

  void applyComponent(int i, DocOpCursor c);

  AnnotationBoundaryMap getAnnotationBoundary(int i);
  String getCharactersString(int i);
  String getElementStartTag(int i);
  Attributes getElementStartAttributes(int i);

  int getRetainItemCount(int i);
  String getDeleteCharactersString(int i);
  String getDeleteElementStartTag(int i);
  Attributes getDeleteElementStartAttributes(int i);
  Attributes getReplaceAttributesOldAttributes(int i);
  Attributes getReplaceAttributesNewAttributes(int i);
  AttributesUpdate getUpdateAttributesUpdate(int i);

  // TODO: better name
  public interface IsDocOp {
    DocInitialization asOperation();
  }
}
