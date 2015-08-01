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

package org.waveprotocol.box.server.robots.agent.welcome;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.wave.api.Wavelet;
import com.typesafe.config.Config;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.agent.AbstractBaseRobotAgent;
import org.waveprotocol.box.server.robots.util.RobotsUtil;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.waveprotocol.box.server.robots.agent.RobotAgentUtil.appendLine;


/**
 * The robot that adds a "Welcome" wave to the inbox of new users.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
@SuppressWarnings("serial")
@Singleton
public class WelcomeRobot extends AbstractBaseRobotAgent {


  private static final Logger LOG = Logger.getLogger(WelcomeRobot.class.getName());
  public static final String ROBOT_URI = AGENT_PREFIX_URI + "/welcome";

  /** The id of the wave that serves as a template for the welcome wave. */
  private WaveId welcomeWaveId = null;

  @Inject
  public WelcomeRobot(Injector injector) {
    super(injector);
    String welcomeWaveIdStr =
      injector.getInstance(Config.class).getString("administration.welcome_wave_id");
    if (!"".equals(welcomeWaveIdStr)) {
      try {
        welcomeWaveId = WaveId.ofChecked(getWaveDomain(), welcomeWaveIdStr);
      } catch (InvalidIdException e) {
        LOG.log(Level.WARNING, "Problem parsing welcome wave id: " + welcomeWaveIdStr);
      }
    }
  }

  /**
   * Greets new users by creating a new wave with welcome message and
   * adding it to the inbox of the new user.
   *
   * @param id the participant id of the new user.
   * @throws IOException if there is a problem submitting the new wave.
   */
  public void greet(ParticipantId id) throws IOException {
    Preconditions.checkNotNull(id);
    RobotAccountData account = null;
    String rpcUrl = getFrontEndAddress() + "/robot/rpc";
    try {
      account =
        getAccountStore()
        .getAccount(ParticipantId.ofUnsafe(getRobotId() + "@" + getWaveDomain())).asRobot();
    } catch (PersistenceException e) {
      LOG.log(Level.WARNING, "Cannot fetch account data for robot id: " + getRobotId(), e);
    }
    if (account != null) {
      setupOAuth(account.getId().getAddress(), account.getConsumerSecret(), rpcUrl);
      Wavelet newWelcomeWavelet = newWave(getWaveDomain(), Sets.newHashSet(id.getAddress()));
      if (welcomeWaveId != null) {
        Wavelet templateWelcomeWavelet =
          fetchWavelet(welcomeWaveId, WaveletId.of(getWaveDomain(), "conv+root"), rpcUrl);
        RobotsUtil.copyBlipContents(templateWelcomeWavelet.getRootBlip(),
            newWelcomeWavelet.getRootBlip());
      } else {
        appendLine(newWelcomeWavelet.getRootBlip(), "Welcome to " + getWaveDomain() + "!");
      }
      submit(newWelcomeWavelet, rpcUrl);
    }
  }

  @Override
  public String getRobotUri() {
    return ROBOT_URI;
  }

  @Override
  public String getRobotId() {
    return "welcome-bot";
  }

  @Override
  protected String getRobotName() {
    return "Welcome-Bot";
  }
}
