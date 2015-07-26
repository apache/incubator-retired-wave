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

import com.google.common.collect.Maps;
import com.google.protobuf.Message;

import java.util.Map;

/**
 * Channel superclass to abstract expected-message mechanism.
 * 
 * Note: {@link #expectMessage} has no effect.
 */
public abstract class MessageExpectingChannel {
  private final Map<String, Message> expectedMessages = Maps.newHashMap();
  /**
   * Register an expected incoming message type.
   * 
   * @param messagePrototype the prototype of the expected type
   */
  public void expectMessage(Message messagePrototype) {
    expectedMessages.put(messagePrototype.getDescriptorForType().getFullName(), messagePrototype);
  }

  public Message getMessagePrototype(String messageType) {
    return expectedMessages.get(messageType);
  }

  /**
   * Send the given message across the connection along with the sequence number.
   * 
   * @param sequenceNo
   * @param message
   */
  public abstract void sendMessage(int sequenceNo, Message message);

  /**
   * Helper method around {{@link #sendMessage(long, Message)} which
   * automatically registers the response type as an expected input to this
   * SequencedProtoChannel.
   * 
   * @param sequenceNo
   * @param message
   * @param expectedResponsePrototype
   */
  public void sendMessage(int sequenceNo, Message message, Message expectedResponsePrototype) {
    expectMessage(expectedResponsePrototype);
    sendMessage(sequenceNo, message);
  }
  
  public void startAsyncRead() {
    // nothing necessarily to do.
  }
}
