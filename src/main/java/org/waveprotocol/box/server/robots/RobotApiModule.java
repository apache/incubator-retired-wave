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

package org.waveprotocol.box.server.robots;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.wave.api.RobotSerializer;
import com.google.wave.api.data.converter.EventDataConverterModule;
import com.google.wave.api.robot.HttpRobotConnection;
import com.google.wave.api.robot.RobotConnection;

import com.typesafe.config.Config;
import net.oauth.OAuthServiceProvider;
import net.oauth.OAuthValidator;
import net.oauth.SimpleOAuthValidator;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.waveprotocol.box.server.robots.active.ActiveApiOperationServiceRegistry;
import org.waveprotocol.box.server.robots.dataapi.DataApiOAuthServlet;
import org.waveprotocol.box.server.robots.dataapi.DataApiOperationServiceRegistry;
import org.waveprotocol.box.server.robots.passive.RobotConnector;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Robot API Module.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotApiModule extends AbstractModule {

  private static final int NUMBER_OF_THREADS = 10;

  private static final String AUTHORIZE_TOKEN_PATH = "/OAuthAuthorizeToken";
  private static final String REQUEST_TOKEN_PATH = "/OAuthGetRequestToken";
  private static final String ACCESS_TOKEN_PATH = "/OAuthGetAccessToken";
  private static final String ALL_TOKENS_PATH = "/OAuthGetAllTokens";

  @Override
  protected void configure() {
    install(new EventDataConverterModule());
    install(new RobotSerializerModule());

    bind(String.class).annotatedWith(Names.named("authorize_token_path")).toInstance(
        AUTHORIZE_TOKEN_PATH);
    bind(String.class).annotatedWith(Names.named("request_token_path")).toInstance(
        REQUEST_TOKEN_PATH);
    bind(String.class).annotatedWith(Names.named("access_token_path")).toInstance(
        ACCESS_TOKEN_PATH);
    bind(String.class).annotatedWith(Names.named("all_tokens_path")).toInstance(
        ALL_TOKENS_PATH);
  }

  @Provides
  @Inject
  @Singleton
  protected RobotConnector provideRobotConnector(
      RobotConnection connection, RobotSerializer serializer) {
    return new RobotConnector(connection, serializer);
  }

  @Provides
  @Singleton
  protected RobotConnection provideRobotConnection() {
    HttpClient httpClient = new HttpClient(new MultiThreadedHttpConnectionManager());

    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("RobotConnection").build();
    return new HttpRobotConnection(
        httpClient, Executors.newFixedThreadPool(NUMBER_OF_THREADS, threadFactory));
  }

  @Provides
  @Singleton
  @Named("GatewayExecutor")
  protected Executor provideGatewayExecutor() {
    ThreadFactory threadFactory =
        new ThreadFactoryBuilder().setNameFormat("PassiveRobotRunner").build();
    return Executors.newFixedThreadPool(NUMBER_OF_THREADS, threadFactory);
  }

  @Provides
  @Singleton
  @Inject
  @Named("ActiveApiRegistry")
  protected OperationServiceRegistry provideActiveApiRegistry(Injector injector) {
    return new ActiveApiOperationServiceRegistry(injector);
  }

  @Provides
  @Singleton
  @Inject
  @Named("DataApiRegistry")
  protected OperationServiceRegistry provideDataApiRegistry(Injector injector) {
    return new DataApiOperationServiceRegistry(injector);
  }

  @Provides
  @Singleton
  protected OAuthValidator provideOAuthValidator() {
    // TODO(ljvderijk): This isn't an industrial strength validator, it grows
    // over time. It should be replaced or cleaned out on a regular interval.
    return new SimpleOAuthValidator();
  }

  @Provides
  @Singleton
  protected OAuthServiceProvider provideOAuthServiceProvider(Config config) {
      String publicAddress = config.getString("core.http_frontend_public_address");
    // Three urls, first is to get an unauthorized request token, second is to
    // authorize the request token, third is to exchange the authorized request
    // token with an access token.
    String requestTokenUrl = getOAuthUrl(publicAddress, REQUEST_TOKEN_PATH);
    String authorizeTokenUrl = getOAuthUrl(publicAddress, AUTHORIZE_TOKEN_PATH);
    String accessTokenUrl = getOAuthUrl(publicAddress, ACCESS_TOKEN_PATH);

    return new OAuthServiceProvider(requestTokenUrl, authorizeTokenUrl, accessTokenUrl);
  }

  /**
   * Returns the full url used to do 3-legged OAuth in the data api.
   *
   * @param publicAddress the address of the http frontend
   * @param postFix the end part of the url
   */
  private String getOAuthUrl(String publicAddress, String postFix) {
    return String.format(
        "http://%s%s%s", publicAddress, DataApiOAuthServlet.DATA_API_OAUTH_PATH, postFix);
  }
}
