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

package org.waveprotocol.wave.model.operation.wave;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.testing.BasicFactories;
import org.waveprotocol.wave.model.testing.WaveletDataFactory;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.impl.WaveletDataImpl;

import java.util.Collections;
import java.util.Set;

/**
 * Abstract base class supporting operation tests.
 *
 * @author anorth@google.com (Alex North)
 */

abstract class OperationTestBase extends TestCase {

  protected final ParticipantId creator = new ParticipantId("lars@example.com");
  protected final ParticipantId fred = new ParticipantId("fred@example.com");
  protected final ParticipantId jane = new ParticipantId("jane@example.com");
  protected final Set<ParticipantId> noParticipants = Collections.emptySet();

  protected WaveletOperationContext context;
  protected WaveletDataImpl waveletData;

  protected static final long CREATION_TIMESTAMP = 100L;
  protected static final long LAST_MODIFIED_TIMESTAMP = CREATION_TIMESTAMP + 10L;
  protected static final long CONTEXT_TIMESTAMP = LAST_MODIFIED_TIMESTAMP + 5L;
  protected static final long CONTEXT_VERSION = 4L;
  protected static final HashedVersion CONTEXT_HASHED_VERSION =
      HashedVersion.of(CONTEXT_VERSION, new byte[] { 4, 4, 4, 4 });

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    context = new WaveletOperationContext(fred, CONTEXT_TIMESTAMP, 1L);
    WaveletDataImpl.Factory holderFactory =
        WaveletDataImpl.Factory.create(BasicFactories.fakeDocumentFactory());
    waveletData = WaveletDataFactory.of(holderFactory).create();
  }
}
