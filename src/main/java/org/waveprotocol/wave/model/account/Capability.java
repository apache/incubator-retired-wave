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

package org.waveprotocol.wave.model.account;

/**
 * Describes the access an address has to a wavelet, or to another address.
 *
 * This is used in two ways:<ul>
 * <li>As an edge between an address and a wave, in which case the address can
 * perform the capability directly to the wave.
 * <li>As an edge between two addresses, in which case the source address can
 * perform the capability as the destination address for waves.
 */
public enum Capability {
  /**
   * The address can add new participants to the wave. In the case of this
   * capability existing between two addresses, the source address may request
   * that the destination address add them to a wave the destination is already
   * a participant on.
   */
  JOIN,

  /**
   * In the case of access to a wavelet, the wave should be written to the
   * address' index.
   *
   * In the case of access to an address, the source address is permitted to
   * present in its index any data which is available in the destination
   * address' index.
   */
  INDEX,

  /**
   * This allows the source address to read wavelets directly, or read all
   * wavelets that the target address may read.
   *
   * The address can read the wavelet, or the address may read wavelets that may
   * be read by the target address.
   */
  READ,

  /**
   * The source address can add the destination address to a wavelet,
   * regardless of whether addition of that address is usually restricted.
   * When an edge exists between two addresses, this should be set if and only
   * if the source can add the destination address.
   */
  ADD,

  /**
   * The address can issue ops to the wavelet, or (between two addresses)
   * perform ops as the target address.
   */
  WRITE,
}
