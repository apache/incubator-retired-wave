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

package org.waveprotocol.box.server.robots.passive;

import com.google.inject.Inject;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.RobotSerializer;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.robot.CapabilityFetchException;
import com.google.wave.api.robot.RobotCapabilitiesParser;
import com.google.wave.api.robot.RobotConnection;
import com.google.wave.api.robot.RobotConnectionException;

import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.account.RobotAccountDataImpl;
import org.waveprotocol.box.server.robots.RobotCapabilities;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Collections;
import java.util.List;

/**
 * This class sends {@link EventMessageBundle} to a robot and receives their
 * response. It will gracefully handle failure by acting like the robot sent no
 * operations.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotConnector {

  private static final Log LOG = Log.get(RobotConnector.class);

  private final RobotConnection connection;

  private final RobotSerializer serializer;

  @Inject
  public RobotConnector(RobotConnection connection, RobotSerializer serializer) {
    this.connection = connection;
    this.serializer = serializer;
  }

  /**
   * Synchronously sends an {@link EventMessageBundle} off to a robot and hands
   * back the response in the form of a list of {@link OperationRequest}s.
   *
   * @param bundle the bundle to send to the robot.
   * @param robot the {@link RobotAccountData} of the robot.
   * @param version the version that we should speak to the robot.
   * @returns list of {@link OperationRequest}s that the robot wants to have
   *          executed.
   */
  public List<OperationRequest> sendMessageBundle(
      EventMessageBundle bundle, Robot robot, ProtocolVersion version) {
    String serializedBundle = serializer.serialize(bundle, version);

    String robotUrl = robot.getAccount().getUrl() + Robot.RPC_URL;
    LOG.info("Sending: " + serializedBundle + " to " + robotUrl);

    try {
      String response = connection.postJson(robotUrl, serializedBundle);
      LOG.info("Received: " + response + " from " + robotUrl);
      return serializer.deserializeOperations(response);
    } catch (RobotConnectionException e) {
      LOG.info("Failed to receive a response from " + robotUrl, e);
    } catch (InvalidRequestException e) {
      LOG.info("Failed to deserialize passive API response", e);
    }

    // Return an empty list and let the caller ignore the failure
    return Collections.emptyList();
  }

  /**
   * Returns a new {@link RobotAccountData} updated with the new capabilities
   * using the given {@link RobotAccountData}.
   *
   * @param account The {@link RobotAccountData} to update the capabilities for.
   * @param activeApiUrl the url of the Active Robot API.
   * @throws CapabilityFetchException if the capabilities couldn't be retrieved
   *         or parsed.
   */
  public RobotAccountData fetchCapabilities(RobotAccountData account, String activeApiUrl)
      throws CapabilityFetchException {
    RobotCapabilitiesParser parser = new RobotCapabilitiesParser(
        account.getUrl() + Robot.CAPABILITIES_URL, connection, activeApiUrl);
    RobotCapabilities capabilities = new RobotCapabilities(
        parser.getCapabilities(), parser.getCapabilitiesHash(), parser.getProtocolVersion());

    return new RobotAccountDataImpl(account.getId(), account.getUrl(), account.getConsumerSecret(),
        capabilities, account.isVerified());
  }
}
