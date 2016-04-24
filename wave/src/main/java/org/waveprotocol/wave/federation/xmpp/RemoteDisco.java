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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.joda.time.DateTimeUtils;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Represents XMPP disco status for a specific remote domain. This class only
 * exposes one public method; {@link #discoverRemoteJID}.
 *
 * @author thorogood@google.com (Sam Thorogood)
 */
public class RemoteDisco {
  private static final Logger LOG = Logger.getLogger(RemoteDisco.class.getCanonicalName());

  static final int MAXIMUM_DISCO_ATTEMPTS = 5;
  static final int MINIMUM_REXMIT_MS = 15000;
  static final int REXMIT_JITTER_MS = 2000;
  static final int DISCO_INFO_TIMEOUT = 20;

  private final long creationTimeMillis;
  private final long failExpirySecs;
  private final long successExpirySecs;

  enum Status {
    INIT, PENDING, COMPLETE
  }

  private final Random random = new SecureRandom();
  private final XmppManager manager;
  private final String remoteDomain;
  private final AtomicReference<Status> status;
  private final Queue<SuccessFailCallback<String, String>> pending;

  // Result JID field that will be available on COMPLETE status.
  private String remoteJid;

  // Error field that will be available on COMPLETE status.
  private FederationError error;


  // These two values are used for tracking success and failure counts.
  // Not yet exposed in the fedone waveserver.
  public static final LoadingCache<String, AtomicLong> statDiscoSuccess =
      CacheBuilder.newBuilder().build(new CacheLoader<String, AtomicLong>() {
            @Override
            public AtomicLong load(String domain) {
              return new AtomicLong();
            }
          });

  public static final LoadingCache<String, AtomicLong> statDiscoFailed =
      CacheBuilder.newBuilder().build(new CacheLoader<String, AtomicLong>() {
            @Override
            public AtomicLong load(String domain) {
              return new AtomicLong();
            }
          });

  /**
   * Construct a new RemoteDisco targeting the given domain. This will not kick
   * off the disco request itself.
   * @param manager           XmppManager object, used to send packets
   * @param remoteDomain      the name of the remote domain (not JID)
   * @param failExpirySecs    how long to keep alive a failed disco result
   * @param successExpirySecs how long to keep alive a successful disco result
   */
  public RemoteDisco(XmppManager manager, String remoteDomain, long failExpirySecs,
                     long successExpirySecs) {
    this.manager = manager;
    status = new AtomicReference<Status>(Status.INIT);
    pending = new ConcurrentLinkedQueue<SuccessFailCallback<String, String>>();
    this.remoteDomain = remoteDomain;
    this.creationTimeMillis = DateTimeUtils.currentTimeMillis();
    this.failExpirySecs = failExpirySecs;
    this.successExpirySecs = successExpirySecs;
  }

  /**
   * Construct a new RemoteDisco - purely for testing - with an already
   * determined result. Either jid or error must be passed.
   *
   * @param remoteDomain the name of the remote domain (not JID)
   * @param jid          the domain's remote JID
   * @param error        the error from disco
   */
  @VisibleForTesting
  RemoteDisco(String remoteDomain, String jid, FederationError error) {
    Preconditions.checkArgument((jid != null)^(error != null));

    manager = null;
    status = new AtomicReference<Status>(Status.COMPLETE);
    pending = null;
    this.remoteDomain = remoteDomain;
    this.remoteJid = jid;
    this.error = error;
    // defaults for testing
    this.creationTimeMillis = DateTimeUtils.currentTimeMillis();
    this.failExpirySecs = 2 * 60;
    this.successExpirySecs = 2 * 60 * 60;
  }

  /**
   * Check whether the request is currently PENDING. Visible only for tests.
   * @return true if pending else false
   */
  @VisibleForTesting
  boolean isRequestPending() {
    return status.get().equals(Status.PENDING);
  }

  /**
   * Attempt to discover the remote JID for this domain. If the JID has already
   * been discovered, then this method will invoke the callback immediately.
   * Otherwise, the callback is guaranteed to be invoked at a later point.
   *
   * @param callback a callback to be invoked when disco is complete
   */
  public void discoverRemoteJID(SuccessFailCallback<String, String> callback) {
    if (status.get().equals(Status.COMPLETE)) {
      complete(callback);
    } else if (status.compareAndSet(Status.INIT, Status.PENDING)) {
      pending.add(callback);
      startDisco();
    } else {
      pending.add(callback);

      // If we've become complete since the start of this method, complete
      // all possible callbacks.
      if (status.get().equals(Status.COMPLETE)) {
        SuccessFailCallback<String, String> item;
        while ((item = pending.poll()) != null) {
          complete(item);
        }
      }
    }
  }

  /**
   * Returns true if this RemoteDisco's time to live is exceeded.
   *
   * We can't use MapMaker's expiration code as it won't let us have different expiry for
   * successful and failed cases.
   *
   * @return whether this object should be deleted and recreated
   */
  public boolean ttlExceeded() {
    if (status.get() == Status.COMPLETE) {
      if (remoteJid == null) {
        // Failed disco case
        if (DateTimeUtils.currentTimeMillis() >
            (creationTimeMillis + (1000 * failExpirySecs))) {
          return true;
        }
      } else {
        // Successful disco case
        if (DateTimeUtils.currentTimeMillis() >
            (creationTimeMillis + (1000 * successExpirySecs))) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Complete any specific callback (in the current thread). Requires the status
   * to be COMPLETE.
   *
   * TODO(thorogood): thread model for completing callbacks
   * @param callback the callback to complete
   */
  private void complete(SuccessFailCallback<String, String> callback) {
    Preconditions.checkState(status.get().equals(Status.COMPLETE));
    if (remoteJid != null) {
      callback.onSuccess(remoteJid);
    } else {
      // TODO(thorogood): better toString, or change failure type to FederationError
      callback.onFailure(error.toString());
    }
  }

  /**
   * Start XMPP discovery. Kicks off a retrying call to dial-up the remote
   * server and discover its available disco items.
   *
   * This should only be called by a method holding the PENDING state.
   */
  private void startDisco() {
    final IQ request = manager.createRequestIQ(remoteDomain);
    request.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_ITEMS);

    final Runnable requester = new Runnable() {
      int attempt = 0;

      final PacketCallback callback = new PacketCallback() {
        @Override
        public void run(Packet result) {
          Preconditions.checkArgument(result instanceof IQ, "Manager must provide response IQ");
          processDiscoItemsResult((IQ) result);
        }

        @Override
        public void error(FederationError error) {
          if (error.getErrorCode().equals(FederationError.Code.REMOTE_SERVER_TIMEOUT)) {
            retry();
          } else {
            LOG.info("Remote server " + remoteDomain + " failed on disco items: "
                + error.getErrorCode());
            processDiscoItemsResult(null);
          }
        }
      };

      void retry() {
        attempt += 1;
        if (attempt > MAXIMUM_DISCO_ATTEMPTS) {
          finish(null, FederationErrors
              .newFederationError(FederationError.Code.REMOTE_SERVER_TIMEOUT));
        } else {
          // TODO(thorogood): fix ms/seconds!
          int timeout = nextDiscoRetransmitTimeout(attempt) / 1000;
          request.setID(XmppUtil.generateUniqueId());
          LOG.info("Sending disco items request for: " + remoteDomain + ", timeout " + timeout
              + " seconds");
          manager.send(request, callback, timeout);
        }
      }

      @Override
      public void run() {
        retry();
      }
    };

    // Kick off requester!
    requester.run();
  }

  /**
   * Calculate the requested timeout for any given request number. Introduces
   * random jitter.
   *
   * @param attempt the attempt count
   * @return request timeout in ms
   */
  private int nextDiscoRetransmitTimeout(int attempt) {
    Preconditions.checkArgument(attempt > 0);
    return MINIMUM_REXMIT_MS * (1 << (attempt - 1)) + random.nextInt(REXMIT_JITTER_MS);
  }

  /**
   * Process a returned set of disco items. Invoke a query for each item in
   * parallel, searching for any item which supports Wave.
   *
   * @param result IQ stanza provided from disco items, if null try default items
   */
  private void processDiscoItemsResult(@Nullable IQ result) {
    Set<String> candidates = Sets.newHashSet();

    // Traverse the source list, finding possible JID candidates.
    if (result != null) {
      List<Element> items = XmppUtil.toSafeElementList(result.getChildElement().elements("item"));
      for (Element item : items) {
        Attribute jid = item.attribute("jid");
        if (jid != null) {
          candidates.add(jid.getValue());
        }
      }
    }

    // Returned nothing for the items list. Try the domain itself.
    if (candidates.isEmpty()) {
      candidates.add(remoteDomain);
    }

    // Always query 'wave.', as an automatic fallback.
    candidates.add("wave." + remoteDomain);

    // Iterate over all candidates, requesting information in parallel.
    AtomicInteger sharedLatch = new AtomicInteger(candidates.size());
    for (String candidate : candidates) {
      requestDiscoInfo(candidate, sharedLatch);
    }
  }

  /**
   * Request disco info from a specific target JID. Accepts a target JID as well
   * as a shared latch: on a result, the latch should be decremented and if it
   * reaches zero, finish() must be invoked with an error.
   *
   * @param target the target JID
   * @param sharedLatch a shared latch
   */
  private void requestDiscoInfo(String target, final AtomicInteger sharedLatch) {
    final IQ request = manager.createRequestIQ(target);
    request.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_INFO);

    PacketCallback callback = new PacketCallback() {
      @Override
      public void error(FederationError error) {
        int currentCount = sharedLatch.decrementAndGet();
        Preconditions.checkState(currentCount >= 0,
            "Info latch should not count down past zero for domain: %s", remoteDomain);
        if (currentCount == 0) {
          finish(null, error);
        }
      }

      @Override
      public void run(Packet packet) {
        Preconditions.checkArgument(packet instanceof IQ);
        IQ result = (IQ) packet;

        List<Element> features =
            XmppUtil.toSafeElementList(result.getChildElement().elements("feature"));
        for (Element feature : features) {
          Attribute var = feature.attribute("var");
          if (var != null && var.getValue().equals(XmppNamespace.NAMESPACE_WAVE_SERVER)) {
            String targetJID = packet.getFrom().toString();
            finish(targetJID, null);

            // Decrement the latch *after* finishing, so we don't allow an error
            // callback to be kicked off.
            Preconditions.checkState(sharedLatch.decrementAndGet() >= 0,
                "Info latch should not count down past zero for domain: %s", remoteDomain);
            return;
          }
        }

        // This result didn't contain a useful result JID, so cause an error.
        error(FederationErrors.newFederationError(FederationError.Code.ITEM_NOT_FOUND));
      }
    };

    LOG.info("Sending disco info request for: " + target);
    manager.send(request, callback, DISCO_INFO_TIMEOUT);
  }

  /**
   * Finish this disco attempt with either a success or error result. This
   * method should only be called on a thread that owns the PENDING state and
   * will (if successful) result in a transition to COMPLETE. If the disco
   * attempt is already complete, return false and do nothing (safe operation).
   *
   * @param jid success JID, or null
   * @param error error proto, or null
   * @return true if successful, false if already finished
   */
  @VisibleForTesting
  boolean finish(String jid, FederationError error) {
    Preconditions.checkArgument((jid != null)^(error != null));
    if (!status.compareAndSet(Status.PENDING, Status.COMPLETE)) {
      return false;
    }

    // Set either the result JID or error state.

    try {
      if (jid != null) {
        this.remoteJid = jid;
        LOG.info("Discovered remote JID: " + jid + " for " + remoteDomain);
          statDiscoSuccess.get(remoteDomain).incrementAndGet();
      } else if (error != null) {
        this.error = error;
        LOG.info("Could not discover remote JID: " + error + " for " + remoteDomain);
        statDiscoFailed.get(remoteDomain).incrementAndGet();
      } else {
        throw new IllegalArgumentException("At least one of jid/error must be set");
      }
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }

    // Complete all available callbacks.
    SuccessFailCallback<String, String> item;
    while ((item = pending.poll()) != null) {
      complete(item);
    }
    return true;
  }
}
