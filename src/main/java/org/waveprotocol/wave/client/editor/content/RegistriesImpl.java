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

package org.waveprotocol.wave.client.editor.content;

import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;

/**
 * Bundle of registries for handling/rendering of elements and annotations.
 *
 * NOTE(user): The implementation of each registry's createExtension is just a
 * normal copy. Other improvements can be copy on write for efficiency, and also
 * some mechanism so that writes to the parent propagates to the children.
 *
 * NOTE(user): Consider having one registry hierarchy with each element in it
 * containing (doodads, handlers, annotation, paint ...) rather than N
 * hierarchies superimposed.
 *
 */
public final class RegistriesImpl implements Registries {
  private final ElementHandlerRegistry elementHandlerRegistry;
  private final AnnotationRegistry annotationHandlerRegistry;
  private final PainterRegistry paintRegistry;

  public RegistriesImpl(ElementHandlerRegistry elementHandlerRegistry,
      AnnotationRegistry annotationHandlerRegistry, PainterRegistry paintRegistry) {
    this.elementHandlerRegistry = elementHandlerRegistry;
    this.annotationHandlerRegistry = annotationHandlerRegistry;
    this.paintRegistry = paintRegistry;
  }

  @Override
  public ElementHandlerRegistry getElementHandlerRegistry() {
    return elementHandlerRegistry;
  }

  @Override
  public AnnotationRegistry getAnnotationHandlerRegistry() {
    return annotationHandlerRegistry;
  }

  @Override
  public PainterRegistry getPaintRegistry() {
    return paintRegistry;
  }

  @Override
  public Registries createExtension() {
    return new RegistriesImpl(elementHandlerRegistry.createExtension(),
        annotationHandlerRegistry.createExtension(), paintRegistry.createExtension());
  }
}
