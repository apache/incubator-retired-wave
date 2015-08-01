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

package org.waveprotocol.box.server.robots.operations;

import static org.mockito.Mockito.mock;

import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.data.converter.EventDataConverter;

import junit.framework.TestCase;

import org.waveprotocol.box.server.robots.OperationContextImpl;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Unit test for the {@link DoNothingService}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class DoNothingServiceTest extends TestCase {

  private static final ParticipantId BOB = ParticipantId.ofUnsafe("bob@example.com");

  private OperationService operationService;

  @Override
  protected void setUp() throws Exception {
    operationService = DoNothingService.create();
  }

  public void testReturnsEmptyResponse() throws Exception {
    // Type of operation doesn't matter in this case
    OperationRequest request = new OperationRequest("wavelet.fetch", "op1");

    WaveletProvider waveletProvider = mock(WaveletProvider.class);
    EventDataConverter converter = mock(EventDataConverter.class);
    ConversationUtil conversationUtil = mock(ConversationUtil.class);

    OperationContextImpl context =
        new OperationContextImpl(waveletProvider, converter, conversationUtil);

    operationService.execute(request, context, BOB);

    JsonRpcResponse response = context.getResponse(request.getId());
    assertFalse("Expected non error response", response.isError());
    assertTrue("Empty Response must be set", response.getData().isEmpty());
  }
}
