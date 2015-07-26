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
package org.waveprotocol.box.server.rpc;

import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;

import junit.framework.TestCase;

import org.waveprotocol.box.common.comms.WaveClientRpc;

/**
 * @author arb@google.com
 */
public class WebSocketChannelTest extends TestCase {
  private TestWebSocketChannel channel;
  private TestCallback callback;
  private static final int SEQUENCE_NUMBER = 5;

  class TestWebSocketChannel extends WebSocketChannel {
    String message;

    public TestWebSocketChannel(ProtoCallback callback) {
      super(callback);
      this.message = null;
    }

    @Override
    protected void sendMessageString(final String data) {
      this.message = data;
    }
  }

  class TestCallback implements ProtoCallback {
      Message savedMessage = null;
      long sequenceNumber;

      @Override
      public void message(int sequenceNo, final Message message) {
        this.sequenceNumber = sequenceNo;
        this.savedMessage = message;
      }
    }

  @Override
  public void setUp() {
    callback = new TestCallback();
    channel = new TestWebSocketChannel(callback);
  }

  public void testRoundTrippingJson() throws Exception {
    WaveClientRpc.ProtocolOpenRequest.Builder sourceBuilder = buildProtocolOpenRequest();
    checkRoundtripping(sourceBuilder);
  }

  public void testRoundTrippingJsonRepeatedField() throws Exception {
    WaveClientRpc.ProtocolOpenRequest.Builder sourceBuilder = buildProtocolOpenRequest();
    sourceBuilder.addWaveletIdPrefix("aaa");
    sourceBuilder.addWaveletIdPrefix("bbb");
    sourceBuilder.addWaveletIdPrefix("ccc");
    checkRoundtripping(sourceBuilder);
  }

  private void checkRoundtripping(final WaveClientRpc.ProtocolOpenRequest.Builder sourceBuilder) {
    WaveClientRpc.ProtocolOpenRequest sourceRequest = sourceBuilder.build();
    channel.sendMessage(SEQUENCE_NUMBER, sourceRequest);
    String sentRequest = channel.message;
    assertNotNull(sentRequest);
    System.out.println(sentRequest);
    channel.handleMessageString(sentRequest);
    assertNotNull(callback.savedMessage);
    assertEquals(SEQUENCE_NUMBER, callback.sequenceNumber);
    assertEquals(sourceRequest, callback.savedMessage);
  }

  private WaveClientRpc.ProtocolOpenRequest.Builder buildProtocolOpenRequest() {
    WaveClientRpc.ProtocolOpenRequest.Builder sourceBuilder =
        WaveClientRpc.ProtocolOpenRequest.newBuilder();
    sourceBuilder.setParticipantId("test@example.com");
    sourceBuilder.setWaveId("example.com!w+test");
    return sourceBuilder;
  }
}
