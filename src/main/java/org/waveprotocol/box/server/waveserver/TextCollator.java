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

package org.waveprotocol.box.server.waveserver;

import com.google.inject.ImplementedBy;

import org.waveprotocol.box.common.Snippets;
import org.waveprotocol.box.server.waveserver.TextCollator.SnippetTextCollatorImpl;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

/**
 * Adapter interface that provdes simpler access to {@link Snippet} methods.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
@ImplementedBy(SnippetTextCollatorImpl.class)
public interface TextCollator {
  String collateTextForWavelet(ReadableWaveletData waveletData);

  static class SnippetTextCollatorImpl implements TextCollator {

    @Override
    public String collateTextForWavelet(ReadableWaveletData waveletData) {
      return Snippets.collateTextForWavelet(waveletData);
    }
  }
}
