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

package org.waveprotocol.wave.model.wave.opbased;

import org.waveprotocol.wave.model.document.util.EmptyDocument;
import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.WaveletDataFactory;
import org.waveprotocol.wave.model.wave.BlipDataTestBase;
import org.waveprotocol.wave.model.wave.data.BlipData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

/**
 * Runs BlipData interface tests with the POJO implementation.
 *
 * @author anorth@google.com (Alex North)
 */

public class BlipDataWithBlipDataImplTest extends BlipDataTestBase {
  @Override
  protected BlipData createBlipData() {
    WaveletData waveletData =
        WaveletDataFactory.of(BasicFactories.waveletDataImplFactory()).create();
    return waveletData.createDocument("root", waveletData.getCreator(), // \u2620
        waveletData.getParticipants(), EmptyDocument.EMPTY_DOCUMENT, 42L, 42L);
  }
}
