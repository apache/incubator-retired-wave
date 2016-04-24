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

import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.jivesoftware.whack.ExternalComponentManager;
import org.xmpp.component.Component;
import org.xmpp.component.ComponentException;
import org.xmpp.component.ComponentManager;
import org.xmpp.packet.JID;
import org.xmpp.packet.Packet;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * Talks to a XMPP server using the Jabber Component Protocol (XEP-0114).
 *
 * Implements {@link OutgoingPacketTransport} allowing users to send packets,
 * and accepts an {@link IncomingPacketHandler} which can process incoming
 * packets.
 *
 * @author thorogood@google.com (Sam Thorogood)
 */
public class ComponentPacketTransport implements Component, OutgoingPacketTransport {
  private static final Logger LOG =
      Logger.getLogger(ComponentPacketTransport.class.getCanonicalName());

  private final IncomingPacketHandler handler;
  private final String componentName;
  private final String serverDomain;
  private final String serverSecret;
  private final String serverAddress;
  private final int serverPort;

  // Contains packets queued but not sent (while offline).
  private final Queue<Packet> queuedPackets;

  // Object used to lock around online/offline state changes.
  private final Object connectionLock = new Object();

  private ExternalComponentManager componentManager = null;
  private boolean connected = false;

  @Inject
  public ComponentPacketTransport(IncomingPacketHandler handler, Config config) {
    this.handler = handler;
    this.componentName = config.getString("federation.xmpp_component_name");
    this.serverDomain = config.getString("federation.xmpp_server_hostname");
    this.serverSecret = config.getString("federation.xmpp_server_secret");
    this.serverAddress = config.getString("federation.xmpp_server_ip");
    this.serverPort = config.getInt("federation.xmpp_server_component_port");

    queuedPackets = new LinkedList<>();
  }

  /**
   * Bind the component to the XMPP server.
   *
   * @throws ComponentException if the component couldn't talk to the server
   */
  public void run() throws ComponentException {
    componentManager = new ExternalComponentManager(serverAddress, serverPort);
    componentManager.setDefaultSecretKey(serverSecret);
    componentManager.setServerName(serverDomain);

    // Register this component with the manager.
    componentManager.addComponent(componentName, this);
  }

  @Override
  public void sendPacket(Packet packet) {
    synchronized (connectionLock) {
      if (connected) {
        componentManager.sendPacket(this, packet);
      } else {
        queuedPackets.add(packet);
      }
    }
  }

  @Override
  public String getDescription() {
    return "Wave in a Box Server";
  }

  @Override
  public String getName() {
    return componentName;
  }

  @Override
  public void initialize(JID jid, ComponentManager componentManager) {
    // TODO(thorogood): According to XEP-0114, the only valid JID here is the
    // same JID we attempt to connect to the XMPP server with.
    LOG.info("Initializing with JID: " + jid);
  }

  /**
   * {@inheritDoc}
   *
   * Pass the incoming on-the-wire packet onto the incoming handler.
   */
  @Override
  public void processPacket(Packet packet) {
    handler.receivePacket(packet);
  }

  @Override
  public void shutdown() {
    synchronized (connectionLock) {
      LOG.info("Disconnected from XMPP server.");
      componentManager = null;
      connected = false;
    }
  }

  @Override
  public void start() {
    synchronized (connectionLock) {
      connected = true;
      LOG.info("Connected to XMPP server with JID: " + componentName + "." + serverDomain);

      // Send all queued outgoing packets.
      while (!queuedPackets.isEmpty()) {
        componentManager.sendPacket(this, queuedPackets.poll());
      }
    }
  }
}
