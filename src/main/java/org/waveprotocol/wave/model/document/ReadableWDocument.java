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
package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.operation.DocInitialization;

/**
 * Document containing everything you need for a readable persistent view
 *
 * TODO(danilatos) Rename:
 *   ReadableDocument -> ReadableDomDocument
 *   RawDocument -> DomDocument
 *   ReadableWDocument -> ReadableDocument
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface ReadableWDocument<N, E extends N, T extends N> extends
    ReadableDocument<N, E, T>,
    ReadableAnnotationSet<String>,
    LocationMapper<N> {

  /**
   * Returns the string representation of the document. Useful for equality
   * comparisons and some forms of serialization.
   *
   * Equivalent to {@code DocOpUtil.toXmlString(toInitialization())}
   *
   * NOTE(danilatos): Should we deprecate this method? Or does is it useful to
   * allow more efficient implementations than the definition above?
   *
   * @return Minimal normalized xml string.
   *
   *         WARNING(danilatos): Do not use this for debugging or logging
   *         purposes. Instead, use {@link #toDebugString()}
   */
  String toXmlString();

  /**
   * Returns a debug representation of the document. The format is not stable.
   * It is not useful for equality comparisons. It is useful for debugging and
   * logging. It is safe to call even if the document is potentially corrupted.
   *
   * If a document is based on an inner ReadableWDocument, it should delegate
   * to the inner instance for this method.
   *
   * Otherwise, a typical implementation might look something like
   * <code>
   * try {
   *   return DocOpUtil.toXmlString(DocOpScrub.maybeScrub(toInitialization()));
   * } catch (RuntimeException e) {
   *   return "#broken document#";
   * }
   * </code>
   *
   * @return A (scrubbed if necessary) representation of the document. Should
   *         not contain decorations e.g. reporting the implementation type.
   *         That way, adapter or wrapper classes can delegate without a large
   *         amount of cruft building up. {@link #toString()} should contain the
   *         extra decorations.
   */
  String toDebugString();

  /**
   * Returns a useful, scrubbed if necessary, representation of the document.
   * Should not be usable for equality comparisons. A typical implementation
   * might look like
   *
   * <code>
   * return "ImplName@" + Integer.toHexString(System.identityHashCode(this))
   *     + "[" + toDebugString() + "]";
   * </code>
   */
  @Override String toString();

  /**
   * @return this document represented as an initialization, suitable for
   *         {@link org.waveprotocol.wave.model.document.MutableDocument#hackConsume(org.waveprotocol.wave.model.document.operation.Nindo)
   *         copying} this document's content into another document.
   */
  DocInitialization toInitialization();
}
