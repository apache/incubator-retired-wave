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

package org.waveprotocol.wave.crypto;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * A cache for verified certificate chains. It speeds up verification of
 * signatures to cache certificate chain verification results for some amount
 * of time (a few minutes).
 */
public interface VerifiedCertChainCache {

  /**
   * Adds a new verified certificate chain to the cache. This should cause
   * {@link #contains(List)}, when passed the same argument, to return true
   * for a few minutes.
   *
   * @param key the certificate chain.
   */
  public abstract void add(List<? extends X509Certificate> key);

  /**
   * Returns true if the object is present in the cache, false otherwise.
   */
  public abstract boolean contains(List<? extends X509Certificate> key);
}
