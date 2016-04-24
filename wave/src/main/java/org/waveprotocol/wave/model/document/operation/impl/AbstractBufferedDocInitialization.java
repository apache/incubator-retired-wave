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

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.util.DocOpScrub;

/**
 * An abstract base class for DocInitialization with default
 * implementations for the unsupported methods.
 *
 *
 */
public abstract class AbstractBufferedDocInitialization
    extends AbstractDocInitialization implements DocInitialization {

  @Override
  public void apply(DocOpCursor c) {
    apply((DocInitializationCursor) c);
  }

  @Override
  public String toString() {
    return "BufferedInit@" + Integer.toHexString(System.identityHashCode(this)) + "[" +
        DocOpUtil.toConciseString(DocOpScrub.maybeScrub(this)) + "]";
  }

  @Override
  public final int getRetainItemCount(int i) {
    throw new UnsupportedOperationException("Initializations have no retain components");
  }

  @Override
  public final String getDeleteCharactersString(int i) {
    throw new UnsupportedOperationException(
        "Initializations have no delete characters components");
  }

  @Override
  public String getDeleteElementStartTag(int i) {
    throw new UnsupportedOperationException(
        "Initializations have no delete element start components");
  }

  @Override
  public Attributes getDeleteElementStartAttributes(int i) {
    throw new UnsupportedOperationException(
        "Initializations have no delete element start components");
  }

  @Override
  public Attributes getReplaceAttributesOldAttributes(int i) {
    throw new UnsupportedOperationException(
        "Initializations have no replace attributes components");
  }

  @Override
  public Attributes getReplaceAttributesNewAttributes(int i) {
    throw new UnsupportedOperationException(
        "Initializations have no replace attributes components");
  }

  @Override
  public AttributesUpdate getUpdateAttributesUpdate(int i) {
    throw new UnsupportedOperationException(
        "Initializations have no update attributes components");
  }

}
