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

package org.waveprotocol.wave.federation.noop;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import org.waveprotocol.wave.federation.FederationHostBridge;
import org.waveprotocol.wave.federation.FederationRemoteBridge;
import org.waveprotocol.wave.federation.FederationTransport;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.federation.WaveletFederationProvider;

/**
 * Module for setting up an no-op federation subsystem
 *
 * @author tad.glines@gmail.com (Tad Glines)
 */
public class NoOpFederationModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(WaveletFederationProvider.class).annotatedWith(FederationRemoteBridge.class).to(
        NoOpFederationRemote.class).in(Singleton.class);

    bind(WaveletFederationListener.Factory.class).annotatedWith(FederationHostBridge.class).to(
        NoOpFederationHost.class).in(Singleton.class);

    bind(FederationTransport.class).to(NoOpFederationTransport.class).in(Singleton.class);
  }

}
