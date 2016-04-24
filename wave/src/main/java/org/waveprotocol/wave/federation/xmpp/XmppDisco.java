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
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.dom4j.Element;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.xmpp.packet.IQ;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of XMPP Discovery. Provides public methods to respond to incoming disco requests
 * (via {@link XmppManager}), as well as outgoing disco via {{@link #discoverRemoteJid}.
 *
 * @author arb@google.com (Anthony Baxter)
 * @author thorogood@google.com (Sam Thorogood)
 */
public class XmppDisco {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(XmppDisco.class.getCanonicalName());

  // This tracks the number of disco attempts started.
  public static final LoadingCache<String, AtomicLong> statDiscoStarted =
      CacheBuilder.newBuilder().build(new CacheLoader<String, AtomicLong>() {
            @Override
            public AtomicLong load(@SuppressWarnings("NullableProblems") String domain) {
              return new AtomicLong();
            }
          });

  private final LoadingCache<String, RemoteDisco> discoRequests;
  private final String serverDescription;

  private XmppManager manager = null;
  // Accessed by XmppFederationHostForDomain.
  final long failExpirySecs;
  final long successExpirySecs;
  final long discoExpirationHours;
  final String discoInfoCategory;
  final String discoInfoType;

  /**
   * Constructor. Note that {@link #setManager} must be called before this class is ready to use.
   */
  @Inject
  public XmppDisco(Config config) {
    this.serverDescription = config.getString("federation.xmpp_server_description");
    this.discoInfoCategory = config.getString("federation.disco_info_category");
    this.discoInfoType = config.getString("federation.disco_info_type");
    this.failExpirySecs = config.getDuration("federation.xmpp_disco_failed_expiry", TimeUnit.SECONDS);
    this.successExpirySecs = config.getDuration("federation.xmpp_disco_successful_expiry", TimeUnit.SECONDS);
    this.discoExpirationHours = config.getDuration("federation.disco_expiration", TimeUnit.HOURS);

    //noinspection NullableProblems
    discoRequests =
        CacheBuilder.newBuilder().expireAfterWrite(
                discoExpirationHours, TimeUnit.HOURS).build(
        new CacheLoader<String, RemoteDisco>() {

          @Override
          public RemoteDisco load(String domain) throws Exception {
            statDiscoStarted.get(domain).incrementAndGet();
            return new RemoteDisco(manager, domain, failExpirySecs, successExpirySecs);
          }
        });
  }

  /**
   * Set the manager instance for this class. Must be invoked before any other
   * methods are used.
   * @param manager an XmppManager instance
   */
  public void setManager(XmppManager manager) {
    this.manager = manager;
  }

  /**
   * Handles a disco info get from a foreign source. A remote server is trying to ask us what we
   * support. Send back a message identifying as a wave component.
   *
   * @param iq the IQ packet.
   * @param responseCallback callback used to send response
   */
  void processDiscoInfoGet(IQ iq, PacketCallback responseCallback) {
    IQ response = IQ.createResultIQ(iq);
    Element query = response.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_INFO);

    query.addElement("identity")
        .addAttribute("category", discoInfoCategory)
        .addAttribute("type", discoInfoType)
      .addAttribute("name", serverDescription);

    query.addElement("feature")
        .addAttribute("var", XmppNamespace.NAMESPACE_WAVE_SERVER);

    responseCallback.run(response);
  }


  /**
   * Handles a disco items get from a foreign XMPP agent. No useful responses, since we're not a
   * domain on it's own: just the wave component.
   *
   * @param iq the IQ packet.
   * @param responseCallback callback used to send response
   */
  void processDiscoItemsGet(IQ iq, PacketCallback responseCallback) {
    IQ response = IQ.createResultIQ(iq);
    response.setChildElement("query", XmppNamespace.NAMESPACE_DISCO_ITEMS);
    responseCallback.run(response);
  }

  /**
   * Attempt to discover the remote JID for this domain. Hands control to {@link RemoteDisco}.
   *
   * @param remoteDomain the domain to discover
   * @param callback     a callback to trigger when disco completes
   */
  public void discoverRemoteJid(String remoteDomain, SuccessFailCallback<String, String> callback) {
    Preconditions.checkNotNull("Must call setManager first", manager);
    RemoteDisco disco = discoRequests.getIfPresent(remoteDomain);
    if (disco != null) {
      // This is a race condition, but we don't care if we lose it, because the ttl timestamp
      // won't be exceeded in that case.
      if (disco.ttlExceeded()) {
        if (LOG.isLoggable(Level.FINE)) {
          LOG.info("discoverRemoteJid for " + remoteDomain + ": result ttl exceeded.");
        }
        // TODO(arb): should we expose the disco cache somehow for debugging?
        discoRequests.invalidate(remoteDomain);
      }
    }
    try {
      discoRequests.get(remoteDomain).discoverRemoteJID(callback);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Inject a predetermined result into the disco results map. If the passed jid is null, generate
   * an error/not-found case.
   *
   * @param domain remote domain
   * @param jid remote JID
   * @throws IllegalStateException if there is already a result for this domain
   */
  @VisibleForTesting
  void testInjectInDomainToJidMap(String domain, String jid) {
    FederationError error = null;
    if (jid == null) {
      error = FederationErrors.badRequest("Fake injected error");
    }
    RemoteDisco disco = discoRequests.getIfPresent(domain);
    Preconditions.checkState(disco == null);
    discoRequests.put(domain, new RemoteDisco(domain, jid, error));
  }

  /**
   * Determine whether a request for the given domain is pending.
   *
   * @param domain remote domain
   * @return true/false
   */
  @VisibleForTesting
  boolean isDiscoRequestPending(String domain) throws ExecutionException {
    RemoteDisco disco = discoRequests.getIfPresent(domain);
    return disco != null && disco.isRequestPending();
  }

  /**
   * Determine whether the disco request for the given domain has been touched or is at all
   * available.
   *
   * @param domain remote domain
   * @return true/false
   */
  @VisibleForTesting
  boolean isDiscoRequestAvailable(String domain) {
    return discoRequests.getIfPresent(domain) != null;
  }
}
