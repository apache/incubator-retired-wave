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

package org.waveprotocol.box.server.robots.agent;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import junit.framework.TestCase;

import org.apache.commons.cli.CommandLine;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.robots.agent.AbstractBaseRobotAgent.ServerFrontendAddressHolder;
import org.waveprotocol.box.server.robots.register.RobotRegistrar;
import org.waveprotocol.wave.model.id.TokenGenerator;

/**
 * Unit tests for the {@link AbstractRobotAgent}.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class AbstractRobotAgentTest extends TestCase {

  @SuppressWarnings("serial")
  private class FakeRobotAgent extends AbstractCliRobotAgent {

    public FakeRobotAgent(String waveDomain, TokenGenerator tokenGenerator,
        ServerFrontendAddressHolder frontendAddressHolder, AccountStore accountStore,
        RobotRegistrar registrar, Boolean sslEnabled) {
      super(waveDomain, frontendAddressHolder, accountStore, registrar, sslEnabled);
    }

    @Override
    protected CommandLine preprocessCommand(String blipContent) throws IllegalArgumentException {
      return super.preprocessCommand(blipContent);
    }

    @Override
    protected String getRobotName() {
      return commandName;
    }

    @Override
    protected String maybeExecuteCommand(CommandLine commandLine, String modifiedBy) {
      return "";
    }

    @Override
    public String getShortDescription() {
      return "";
    }

    @Override
    public int getMinNumOfArguments() {
      return 2;
    }

    @Override
    public int getMaxNumOfArguments() {
      return 3;
    }

    @Override
    public String getFullDescription() {
      return null;
    }

    @Override
    public String getExample() {
      return null;
    }

    @Override
    public String getCommandName() {
      return commandName;
    }

    @Override
    public String getCmdLineSyntax() {
      return "";
    }

    @Override
    public String getRobotUri() {
      return "";
    }

    @Override
    public String getRobotId() {
      return "";
    }
  };

  private static final String commandName = "agent";

  private FakeRobotAgent agent;

  @Override
  protected void setUp() throws Exception {
    ServerFrontendAddressHolder frontendAddressHolder = mock(ServerFrontendAddressHolder.class);
    when(frontendAddressHolder.getAddresses()).thenReturn(Lists.newArrayList("localhost:9898"));
    TokenGenerator tokenGenerator = mock(TokenGenerator.class);
    when(tokenGenerator.generateToken(anyInt())).thenReturn("abcde");
    AccountStore accountStore = mock(AccountStore.class);
    RobotRegistrar registar = mock(RobotRegistrar.class);
    agent =
        new FakeRobotAgent("example.com", tokenGenerator, frontendAddressHolder, accountStore,
            registar, false);
  }

  public void testPreprocessCommandValidInput() throws Exception {
    String content = String.format("%s arg1 arg2\n", commandName);
    CommandLine commandLine = agent.preprocessCommand(content);
    assertEquals(3, commandLine.getArgs().length);
  }

  public void testPreprocessCommandNoCommandInput() throws Exception {
    String content = String.format("%s arg1 arg2\n", "not_a_command");
    CommandLine commandLine = agent.preprocessCommand(content);
    assertNull(commandLine);
  }

  public void testPreprocessCommandFailsOnTooManyArgs() throws Exception {
    String content = String.format("%s arg1 arg2 arg3 arg4\n", commandName);
    try {
      agent.preprocessCommand(content);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }

  public void testPreprocessCommandFailsOnTooFewArgs() throws Exception {
    String content = String.format("%s arg1 \n", commandName);
    try {
      agent.preprocessCommand(content);
      fail();
    } catch (IllegalArgumentException e) {
      // Expected.
    }
  }
}
