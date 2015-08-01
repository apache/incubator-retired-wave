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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;

/**
 * Bundles a protocol buffer with its serialised representation.
 * <p>
 * This class is used to represent objects whose serialised representation
 * must be kept intact, because signatures or hashes depend
 * on the exact representation as bytes.
 * <p>
 * This would not be an issue if
 * protocol buffer serialisation was unique, but that's not the case.
 * We can only count on the equality
 *
 *   {@code message.equals(T.parseFrom(message.toByteString()))},
 *
 * but not
 *
 *   {@code byteString.equals(T.parseFrom(byteString).toByteString())}.
 *
 * @param <T> the protocol buffer {@link Message} type.
 */
public final class ByteStringMessage<T extends Message> {

  /**
   * Parses a {@link ByteStringMessage} from its {@link ByteString}
   * representation.
   *
   * @param prototype used to create a {@link Message.Builder} to parse the
   *        message
   * @param byteString representation of the message
   * @throws InvalidProtocolBufferException if {@code byteString} is not a
   *         valid protocol buffer
   */
  public static <K extends Message> ByteStringMessage<K> parseFrom(K prototype,
      ByteString byteString) throws InvalidProtocolBufferException {
    return new ByteStringMessage<K>(parse(prototype, byteString), byteString);
  }

  @SuppressWarnings("unchecked")
  private static <K extends Message> K parse(K prototype, ByteString byteString)
      throws InvalidProtocolBufferException {
    return (K) prototype.newBuilderForType().mergeFrom(byteString).build();
  }

  /**
   * Parses a {@link ProtocolWaveletDelta}. Convenience method, equivalent to
   * {@code from(ProtocolWaveletDelta.getDefaultInstanceForType(), byteString)}.
   */
  public static ByteStringMessage<ProtocolWaveletDelta> parseProtocolWaveletDelta(
      ByteString byteString) throws InvalidProtocolBufferException {
    return new ByteStringMessage<ProtocolWaveletDelta>(
        ProtocolWaveletDelta.parseFrom(byteString), byteString);
  }

  /**
   * Parses a {@link ProtocolAppliedWaveletDelta}. Convenience method, equivalent to
   * {@code from(ProtocolAppliedWaveletDelta.getDefaultInstanceForType(), byteString)}.
   */
  public static ByteStringMessage<ProtocolAppliedWaveletDelta> parseProtocolAppliedWaveletDelta(
      ByteString byteString) throws InvalidProtocolBufferException {
    return new ByteStringMessage<ProtocolAppliedWaveletDelta>(
        ProtocolAppliedWaveletDelta.parseFrom(byteString), byteString);
  }

  /**
   * Serialises a {@link Message} into a {@link ByteStringMessage}.
   * This should only be used once, when the message is originally created.
   *
   * @param message to form the serialised version of
   */
  public static <K extends Message> ByteStringMessage<K> serializeMessage(K message) {
    return new ByteStringMessage<K>(message, message.toByteString());
  }

  private final ByteString byteString;
  private final T message;

  /**
   * Constructs a {@link ByteStringMessage} from corresponding {@link Message}
   * and serialised {@link ByteString} representations of protocol buffer.
   *
   * @param message a protocol message equal to {@code T.parseFrom(byteString)}
   * @param byteString representation of {@code message}
   */
  private ByteStringMessage(T message, ByteString byteString) {
    this.message = message;
    this.byteString = byteString;
  }

  /**
   * @return the immutable underlying {@code Message}
   */
  public T getMessage() {
    return message;
  }

  /**
   * @return the serialised representation of this message
   */
  public ByteString getByteString() {
    return this.byteString;
  }

  /**
   * @return the serialised byte array representation of this message
   */
  public byte[] getByteArray() {
    return this.byteString.toByteArray();
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ByteStringMessage)) {
      return false;
    } else {
      ByteStringMessage<Message> bsm = (ByteStringMessage<Message>) o;
      return byteString.equals(bsm.byteString);
    }
  }

  @Override
  public int hashCode() {
    return byteString.hashCode();
  }

  @Override
  public String toString() {
    return "ByteStringMessage: " + message;
  }
}
