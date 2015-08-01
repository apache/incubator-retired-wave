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

import org.waveprotocol.wave.model.document.DocumentFactory;

import java.util.Map;

/**
 * A document provider implements the following mechanisms for obtaining a
 * document:
 * <ul>
 *   <li>{@link #create(String, Map) constructing};</li>
 *   <li>{@link #parse(String) parsing}; and</li>
 * </ul>
 *
 * @param <D> document type produced
 */
public interface DocumentProvider<D> extends DocumentFactory<D> {

  /**
   * Creates a document by parsing XML text.
   *
   * @param text  XML text to parse
   * @return new document constructed from {@code text}.
   */
  D parse(String text);

}
