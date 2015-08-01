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

import org.waveprotocol.wave.model.util.CharBase64;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Random;

/**
 * A {@link TokenGenerator} that uses a {@link Random} to generate tokens.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class TokenGeneratorImpl implements TokenGenerator {

  private final Random random;

  public TokenGeneratorImpl(Random random) {
    this.random = random;
  }

  public String generateToken(int length) {
    Preconditions.checkArgument(length > 0, "Requested length must be larger then 0");

    // Every 4 characters is represented by 3 bytes, therefore rounding up to
    // a multiple of 3.
    int numBytes = (int) Math.ceil(length / 4.0) * 3;
    byte[] bytes = new byte[numBytes];
    random.nextBytes(bytes);

    String token = CharBase64.encodeWebSafe(bytes, false);
    return token.substring(0, length);
  }
}
