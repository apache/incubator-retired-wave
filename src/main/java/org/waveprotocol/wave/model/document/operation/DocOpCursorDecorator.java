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

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Adapter utility which wraps a DocOpCursor and delegates all calls to it.
 * Classes may extend this and implement whichever methods they want to add logic to.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class DocOpCursorDecorator implements DocOpCursor {

  /** The cursor we are delegating to */
  protected final DocOpCursor target;

  /**
   * Constructs the adapter, taking the document that we will delegate to.
   * @param target Delegation target
   */
  protected DocOpCursorDecorator(DocOpCursor target) {
    Preconditions.checkNotNull(target, "Null target");
    this.target = target;
  }

  @Override
  public void annotationBoundary(AnnotationBoundaryMap map) {
    target.annotationBoundary(map);
  }

  @Override
  public void characters(String chars) {
    target.characters(chars);
  }

  @Override
  public void deleteCharacters(String chars) {
    target.deleteCharacters(chars);
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
  public void elementEnd() {
    target.elementEnd();
  }

  @Override
  public void elementStart(String type, Attributes attrs) {
    target.elementStart(type, attrs);
  }

  @Override
  public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
    target.replaceAttributes(oldAttrs, newAttrs);
  }

  @Override
  public void retain(int itemCount) {
    target.retain(itemCount);
  }

  @Override
  public void updateAttributes(AttributesUpdate attrUpdate) {
    target.updateAttributes(attrUpdate);
  }

}
