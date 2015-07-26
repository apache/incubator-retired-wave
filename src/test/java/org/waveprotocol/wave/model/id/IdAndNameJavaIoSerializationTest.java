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

package org.waveprotocol.wave.model.id;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Tests for {@link WaveId}, {@link WaveletId}, and {@link WaveletName}
 * serialization and deserialization using {@code java.io} serialization
 * framework.
 *
 */
public class IdAndNameJavaIoSerializationTest extends TestCase {

  public void testWaveIdSerializationAndDeserialization() throws Exception {
    WaveId expected = WaveId.of("example.com", "123");
    byte[] serialized = serialize(expected);
    Object deserialized = deserialize(serialized);
    assertTrue(deserialized instanceof WaveId);
    WaveId actual = (WaveId) deserialized;
    assertEquals(expected, actual);
  }

  public void testWaveletIdSerializationAndDeserialization() throws Exception {
    WaveletId expected = WaveletId.of("example.com", "123");
    byte[] serialized = serialize(expected);
    Object deserialized = deserialize(serialized);
    assertTrue(deserialized instanceof WaveletId);
    WaveletId actual = (WaveletId) deserialized;
    assertEquals(expected, actual);
  }

  public void testWaveletNameSerializationAndDeserialization() throws Exception {
    WaveletName expected = WaveletName.of(WaveId.of("example.com", "wave1"),
        WaveletId.of("example.com", "wavelet1"));
    byte[] serialized = serialize(expected);
    Object deserialized = deserialize(serialized);
    assertTrue(deserialized instanceof WaveletName);
    WaveletName actual = (WaveletName) deserialized;
    assertEquals(expected, actual);
  }

  /**
   * Serializes the given object into {@code byte} array.
   *
   * @param object the object to serialize.
   * @return a {@code byte} array that represents the object.
   *
   * @throws IOException if I/O error occurs during serialization.
   */
  private static byte[] serialize(Object object) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ObjectOutputStream objectOutput = new ObjectOutputStream(outputStream);
    objectOutput.writeObject(object);
    return outputStream.toByteArray();
  }

  /**
   * Deserializes the given {@code byte} array into object.
   *
   * @param serialized a {@code byte} array that represents an object.
   * @return an object, deserialized from the {@code byte} array.
   *
   * @throws ClassNotFoundException if the class of the serialized object could
   *     not be found.
   * @throws IOException if I/O error occurs during deserialization.
   */
  private static Object deserialize(byte[] serialized) throws ClassNotFoundException, IOException {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(serialized);
    ObjectInputStream objectInput = new ObjectInputStream(inputStream);
    return objectInput.readObject();
  }
}
