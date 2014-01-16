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

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tiny MockDisco class that wraps XmppDisco.
 *
 * Use {@link #testInjectInDomainToJidMap} to configure custom immediate responses, otherwise
 * responses will be placed on a pending queue.
 *
 * @author thorogood@google.com (Sam Thorogood)
 */
public class MockDisco extends XmppDisco {

  private static final int FAIL_EXPIRY_SECS = 5 * 60;
  private static final int SUCCESS_EXPIRY_SECS = 2 * 60 * 60;

  MockDisco(String serverName) {
    super(serverName, FAIL_EXPIRY_SECS, SUCCESS_EXPIRY_SECS);
  }

  public static class PendingMockDisco {
    public final String remoteDomain;
    public final Queue<SuccessFailCallback<String, String>> callbacks = Lists.newLinkedList();

    private PendingMockDisco(String remoteDomain) {
      this.remoteDomain = remoteDomain;
    }

    private void addCallback(SuccessFailCallback<String, String> callback) {
      callbacks.add(callback);
    }
  }

  public LoadingCache<String, PendingMockDisco> pending = CacheBuilder.newBuilder()
      .build(new CacheLoader<String, PendingMockDisco>() {
        @Override
        public PendingMockDisco load(String domain) {
          return new PendingMockDisco(domain);
        }
      });

  @Override
  public void discoverRemoteJid(String remoteDomain, SuccessFailCallback<String, String> callback) {
    if (isDiscoRequestAvailable(remoteDomain)) {
      // Note: tiny race condition in case this is purged between above and
      // below, but since this is only used in tests, we can probably ignore it.
      super.discoverRemoteJid(remoteDomain, callback);
    } else {
      try {
        pending.get(remoteDomain).addCallback(callback);
      } catch (ExecutionException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

}
