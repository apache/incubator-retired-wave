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

/**
 * A mutable view of a wave document.
 *
 * A document comprises a DOM-style tree of nodes, which are elements with
 * attributes or text, and key-value annotation pairs over ranges of those
 * nodes. Locations within the document are given by integer offsets or {@code
 * Point}s, referencing a space between two nodes.
 *
 * Unlike traditional DOMs, the node types are not mutable objects; all
 * mutations are applied through this interface.
 *
 * The node types are represented by the vacuous {@code Doc.E} for elements,
 * {@code Doc.T} for text nodes, and {@code Doc.N} for both. These are type
 * parameters to {@link MutableDocument}, which is the Generic version of this
 * interface.
 *
 * @see MutableDocument
 * @see ReadableAnnotationSet
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface Document extends MutableDocument<Doc.N, Doc.E, Doc.T> {

}
