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

package org.waveprotocol.wave.model.conversation;

import org.waveprotocol.wave.model.util.StringMap;

import java.util.Iterator;

public class WrapperIterator<M extends HasId, W> implements Iterator<W> {

  // The invariant for this iterator is that 'next' always contains the next
  // manifest to return, if there is one, and the manifest iterator is ready to
  // return the manifest immediately after the one corresponding to 'next',
  // possibly one that has no backing overlay object.
  private final Iterator<? extends M> manifestIterator;
  private final StringMap<W> wrapperMap;
  private W next;

  private WrapperIterator(Iterator<? extends M> manifestIterator, StringMap<W> wrapperMap) {
    this.manifestIterator = manifestIterator;
    this.wrapperMap = wrapperMap;
    advance();
  }

  /**
   * Create a new wrapper iterator for the given series of manifests, using the
   * associated wrapper map to fetch wrappers.
   */
  public static <M extends HasId, W> WrapperIterator<M, W> create(
      Iterator<? extends M> manifestIterator, StringMap<W> wrapperMap) {
    return new WrapperIterator<M, W>(manifestIterator, wrapperMap);
  }

  private void advance() {
    while (manifestIterator.hasNext()) {
      M nextManifest = manifestIterator.next();
      W nextWrapper = wrapperMap.get(nextManifest.getId());
      if (nextWrapper != null) {
        next = nextWrapper;
        return;
      }
    }
    next = null;
  }

  @Override
  public boolean hasNext() {
    return next != null;
  }

  @Override
  public W next() {
    W result = next;
    advance();
    return result;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}