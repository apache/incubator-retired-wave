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

package org.waveprotocol.wave.federation.xmpp;

import com.google.protobuf.AbstractMessageLite;
import com.google.protobuf.ByteString;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.Charset;

/**
 * Utility class for encoding and decoding ByteStrings, byte arrays and encoding
 * generic protocol buffers.
 *
 * @author arb@google.com (Anthony Baxter)
 * @author thorogood@google.com (Sam Thorogood)
 */
public final class Base64Util {

  // Character set for all encoding and decoding. Base64 can be correctly
  // represented using UTF-8.
  private static final Charset CHAR_SET = Charset.forName("UTF-8");

  /**
   * Utility class only, cannot be instantiated.
   */
  private Base64Util() {
  }

  public static String encode(ByteString bs) {
    return new String(Base64.encodeBase64(bs.toByteArray()), CHAR_SET);
  }

  public static String encode(byte[] ba) {
    return new String(Base64.encodeBase64(ba), CHAR_SET);
  }

  public static String encode(AbstractMessageLite message) {
    return new String(Base64.encodeBase64(message.toByteArray()), CHAR_SET);
  }

  public static byte[] decodeFromArray(String str) {
    return Base64.decodeBase64(str.getBytes(CHAR_SET));
  }

  public static ByteString decode(String str) {
    return ByteString.copyFrom(Base64.decodeBase64(str.getBytes(CHAR_SET)));
  }
}
