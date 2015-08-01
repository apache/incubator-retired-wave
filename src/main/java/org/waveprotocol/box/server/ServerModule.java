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

package org.waveprotocol.box.server;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import com.typesafe.config.Config;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.authentication.SessionManagerImpl;
import org.waveprotocol.box.server.robots.register.RobotRegistrar;
import org.waveprotocol.box.server.robots.register.RobotRegistrarImpl;
import org.waveprotocol.box.server.rpc.ProtoSerializer;
import org.waveprotocol.box.server.rpc.ServerRpcProvider;
import org.waveprotocol.box.server.rpc.WebSocketChannel;
import org.waveprotocol.box.server.waveserver.WaveServerImpl;
import org.waveprotocol.box.server.waveserver.WaveServerModule;
import org.waveprotocol.wave.federation.FederationHostBridge;
import org.waveprotocol.wave.federation.FederationRemoteBridge;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.IdGeneratorImpl.Seed;
import org.waveprotocol.wave.model.id.TokenGenerator;
import org.waveprotocol.wave.model.id.TokenGeneratorImpl;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.login.Configuration;

/**
 * Guice Module for the prototype Server.
 *
 *
 */
public class ServerModule extends AbstractModule {
  private final WaveServerModule waveServerModule;

  @Inject
  public ServerModule(WaveServerModule waveServerModule) {
    this.waveServerModule = waveServerModule;
  }

  @Override
  protected void configure() {
    bind(WaveServerImpl.class).in(Singleton.class);
    // Receive updates from the outside world, and push them into our local Wave
    // Server.
    bind(WaveletFederationListener.Factory.class).annotatedWith(FederationRemoteBridge.class).to(
        WaveServerImpl.class);

    // Provide history and respond to submits about our own local waves.
    bind(WaveletFederationProvider.class).annotatedWith(FederationHostBridge.class).to(
        WaveServerImpl.class);

    install(waveServerModule);
    TypeLiteral<List<String>> certs = new TypeLiteral<List<String>>() {};
    bind(certs).annotatedWith(Names.named("certs")).toInstance(Arrays.<String> asList());

    bind(ProtoSerializer.class).in(Singleton.class);

    bind(Configuration.class).toInstance(Configuration.getConfiguration());
    bind(SessionManager.class).to(SessionManagerImpl.class).in(Singleton.class);

    bind(ServerRpcProvider.class).in(Singleton.class);

    bind(RobotRegistrar.class).to(RobotRegistrarImpl.class);

    requestStaticInjection(WebSocketChannel.class);
  }

  @Provides
  @Singleton
  public IdGenerator provideIdGenerator(@Named(CoreSettingsNames.WAVE_SERVER_DOMAIN) String domain,
      Seed seed) {
    return new IdGeneratorImpl(domain, seed);
  }

  @Provides
  @Singleton
  public SecureRandom provideSecureRandom() {
    return new SecureRandom();
  }

  @Provides
  @Singleton
  public TokenGenerator provideTokenGenerator(SecureRandom random) {
    return new TokenGeneratorImpl(random);
  }

  @Provides
  @Singleton
  public Seed provideSeed(final SecureRandom random) {
    return new Seed() {
      @Override
      public String get() {
        return Long.toString(Math.abs(random.nextLong()), 36);
      }
    };
  }

  @Provides
  @Singleton
  public org.eclipse.jetty.server.SessionManager provideSessionManager(Config config) {
    HashSessionManager sessionManager = new HashSessionManager();
    sessionManager.getSessionCookieConfig().setMaxAge(config.getInt("network.session_cookie_max_age"));
    return sessionManager;
  }
}
