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

package org.waveprotocol.wave.client.wavepanel.view.fake;

import org.waveprotocol.wave.client.wavepanel.view.View;

/**
 * Fake, pojo implementation of a document view.
 *
 */
public final class FakeDocumentView implements View {
  private final String content;

  public FakeDocumentView(String content) {
    this.content = content;
  }

  @Override
  public Type getType() {
    // A document is not a first-class view in the wave panel's view grammar.
    // However, in order to plug in to a WaveRenderer that uses Views as
    // rendering type of all renderings, we have to fake it a bit.
    return null;
  }

  @Override
  public void remove() {
    throw new IllegalStateException();
  }

  @Override
  public String toString() {
    return "'" + content + "'";
  }
}
