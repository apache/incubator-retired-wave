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

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import org.waveprotocol.wave.federation.FederationHostBridge;
import org.waveprotocol.wave.federation.FederationRemoteBridge;
import org.waveprotocol.wave.federation.FederationTransport;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.federation.WaveletFederationProvider;

/**
 * Module for setting up an XMPP federation subsystem
 *
 * @author tad.glines@gmail.com (Tad Glines)
 */
public class XmppFederationModule extends AbstractModule {

  @Override
  protected void configure() {
    // Request history and submit deltas to the outside world *from* our local
    // Wave Server.
    bind(WaveletFederationProvider.class).annotatedWith(FederationRemoteBridge.class).to(
        XmppFederationRemote.class).in(Singleton.class);

    // Serve updates to the outside world about local waves.
    bind(WaveletFederationListener.Factory.class).annotatedWith(FederationHostBridge.class).to(
        XmppFederationHost.class).in(Singleton.class);

    bind(XmppDisco.class).in(Singleton.class);
    bind(XmppFederationRemote.class).in(Singleton.class);
    bind(XmppFederationHost.class).in(Singleton.class);

    bind(XmppManager.class).in(Singleton.class);
    bind(IncomingPacketHandler.class).to(XmppManager.class);
    bind(ComponentPacketTransport.class).in(Singleton.class);
    bind(OutgoingPacketTransport.class).to(ComponentPacketTransport.class);

    bind(FederationTransport.class).to(XmppFederationTransport.class).in(Singleton.class);
  }

}
