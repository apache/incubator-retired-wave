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
package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.raw.TextNodeOrganiser;

/**
 * An initial, tentative stab at what a document context would look like, based
 * on rough use cases. This needs to be compacted/refined muchly.
 *
 * TODO(danilatos): Carry through with planned changes to this interface.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <N> Document Node
 * @param <E> Document Element
 * @param <T> Document Text Node
 */
public interface DocumentContext<N, E extends N, T extends N> {

  /** Document, persistent view, operation generating mutators, annotation set */
  MutableDocument<N, E, T> document();

  /** Full view of the document, with mutator accessors that do not affect the persistent view */
  LocalDocument<N, E, T> annotatableContent();

  /** Maps ints to points and vice versa in the persistent view */
  LocationMapper<N> locationMapper();

  /** Makes non-operation generating changes to text nodes */
  TextNodeOrganiser<T> textNodeOrganiser();

  /** A ReadableDocumentView interface for the persistent view */
  ReadableDocumentView<N, E, T> persistentView();

  /**
   * View of the hard elements plus text nodes
   * @see PersistentContent#hardView()
   */
  ReadableDocumentView<N, E, T> hardView();

  /** Non-persistent annotations for local book keeping. Can have values of any type. */
  MutableAnnotationSet.Local localAnnotations();

  /** For getting transient properties from an element */
  ElementManager<E> elementManager();
}
