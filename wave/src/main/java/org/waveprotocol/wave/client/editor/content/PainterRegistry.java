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

import org.waveprotocol.wave.client.editor.content.AnnotationPainter.BoundaryFunction;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.Set;

/**
 * Registry of rendering functions for annotations.
 *
 */
public interface PainterRegistry {
  /**
   * Set of keys that affect rendering.
   */
  ReadableStringSet getKeys();

  /**
   * Set of paint functions.
   */
  Set<AnnotationPainter.PaintFunction> getPaintFunctions();

  /**
   * Set of boundary functions.
   */
  Set<AnnotationPainter.BoundaryFunction> getBoundaryFunctions();

  /**
   * Tag name to use for paint nodes.
   */
  String getPaintTagName();

  /**
   * Tag name to use for boundary nodes.
   */
  String getBoundaryTagName();

  /**
   * Register rendering functions for AnnotationPaint.
   * @param dependentKeys
   * @param function
   */
  void registerPaintFunction(ReadableStringSet dependentKeys, PaintFunction function);

  /**
   * Register rendering functions for AnnotationBoundary
   * @param dependentKeys
   * @param function
   */
  void registerBoundaryFunction(ReadableStringSet dependentKeys, BoundaryFunction function);

  /**
   * Unregisters rendering functions for AnnotationPaint.
   * @param dependentKeys
   * @param function
   */
  void unregisterPaintFunction(ReadableStringSet dependentKeys, PaintFunction function);

  /**
   * Unregisters rendering functions for AnnotationBoundary
   * @param dependentKeys
   * @param function
   */
  void unregisterBoundaryFunction(ReadableStringSet dependentKeys, BoundaryFunction function);

  /***/
  AnnotationPainter getPainter();

  /**
   * Creates an extension of this paint registry.
   */
  PainterRegistry createExtension();
}
