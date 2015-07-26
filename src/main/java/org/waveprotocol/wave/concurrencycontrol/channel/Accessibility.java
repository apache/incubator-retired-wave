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

package org.waveprotocol.wave.concurrencycontrol.channel;

/**
 * Accessibility states for a wavelet.
 */
public enum Accessibility {
  /**
   * The wavelet is inaccessible beyond a provided snapshot.
   */
  INACCESSIBLE() {
    @Override
    boolean isReadable() {
      return false;
    }

    @Override
    boolean isWritable() {
      return false;
    }
  },

  /**
   * The wavelet is readable but not writable.
   */
  READ_ONLY() {
    @Override
    boolean isReadable() {
      return true;
    }

    @Override
    boolean isWritable() {
      return false;
    }
  },

  /**
   * The wavelet is readable and writable.
   */
  READ_WRITE() {
    @Override
    boolean isReadable() {
      return true;
    }

    @Override
    boolean isWritable() {
      return true;
    }
  };

  /**
   * Whether updates should be expected from a channel.
   */
  abstract boolean isReadable();

  /**
   * Whether changes may be submitted to a channel.
   */
  abstract boolean isWritable();
}