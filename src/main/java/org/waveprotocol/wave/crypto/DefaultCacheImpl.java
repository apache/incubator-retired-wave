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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache supporting both LRU and time-based expiration.
 *
 * LRU: once maximum size is reached, the least recently accessed element is
 * discarded.
 *
 * Time-based: entries are discarded once they reach a maximum age.
 *
 * This cache is in-memory only, uses 1024 entries and expires them after 10
 * minutes. In large deployments, you might want to replace this implementation
 * with a distributed cache that survives restarts of the servers (although
 * 10 minutes is still a good time for the expirations). Simply inject a
 * different {@link VerifiedCertChainCache} using Guice.
 */
public class DefaultCacheImpl implements VerifiedCertChainCache {

  private static final int VALIDATION_CACHE_SIZE = 1024;
  private static final long VALIDATION_CACHE_AGE_SECONDS = 10 * 60;

  private final LruLinkedHashMap map;
  private final TimeSource timeSource;

  public DefaultCacheImpl(TimeSource timeSource) {
    this.map = new LruLinkedHashMap(VALIDATION_CACHE_SIZE);
    this.timeSource = timeSource;
  }

  public void add(List<? extends X509Certificate> key) {
    synchronized(map) {
      long maxAge = timeSource.currentTimeMillis()
          + VALIDATION_CACHE_AGE_SECONDS * 1000L;
      map.put(key, new EntryWithAge(maxAge));
    }
  }

  public boolean contains(List<? extends X509Certificate> key) {
    synchronized(map) {
      EntryWithAge entry = map.get(key);
      if ((entry != null)
          && (timeSource.currentTimeMillis() < entry.expireMillis)) {
        return true;
      }
      return false;
    }
 }

 private static class EntryWithAge {
   private final long expireMillis;

   public EntryWithAge(long expireMillis) {
     this.expireMillis = expireMillis;
   }
 }

 private static class LruLinkedHashMap
     extends LinkedHashMap<Object, EntryWithAge> {

   private final int capacity;

   public LruLinkedHashMap(int capacity) {
     super(capacity, 0.75f, true);
     this.capacity = capacity;
   }

   @Override
   protected boolean removeEldestEntry(Map.Entry<Object, EntryWithAge> eldest) {
     return this.size() > capacity;
   }
 }
}
