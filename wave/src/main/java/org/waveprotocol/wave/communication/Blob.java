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

package org.waveprotocol.wave.communication;

/**
 * An arbitrary blob of data, comparable with other blobs of data.
 * <p>
 * This type is used to implement the primitive 'bytes' type of a protocol
 * buffer in a manner that is more suitable for web-based DTOs based on JSON.
 * The standard type, {@link com.google.protobuf.ByteString}, is not easily
 * cross-compilable (e.g., by GWT), and does not have an obvious and efficient
 * JSON serialization.
 * <p>
 * This type holds an encoding of arbitrary data as a string. The string
 * encoding is unspecified, so the value of {@link #getData()} should not be
 * interpreted to be meaningful.
 */
public final class Blob {
  private final String data;

  /**
   * @param data
   */
  public Blob(String data) {
    this.data = data;
  }

  public String getData() {
    return data;
  }

  @Override
  public boolean equals(Object o) {
    return this == o || ((o instanceof Blob) && data.equals(((Blob) o).data));
  }

  @Override
  public int hashCode() {
    return 7 + 23 * data.hashCode();
  }
}
