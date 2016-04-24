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

import org.xmpp.packet.Packet;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Dummy implementation of {@link OutgoingPacketTransport} that stores packets
 * being sent over-the-wire. May optionally accept a {@link Router} instance
 * where packets are automatically forwarded.
 *
 * @author thorogood@google.com (Sam Thorogood)
 */
public class MockOutgoingPacketTransport implements OutgoingPacketTransport {

  public interface Router {
    public void route(Packet packet);
  }

  // wrapped router object, if null then packets are not routed
  public Router router;

  // pending outgoing packets
  public final Queue<Packet> packets = new LinkedList<Packet>();

  // last packet sent
  public Packet lastPacketSent = null;

  // total number of packets sent here
  public long packetsSent = 0;

  public MockOutgoingPacketTransport() {
    router = null;
  }

  public MockOutgoingPacketTransport(Router router) {
    this.router = router;
  }

  @Override
  public void sendPacket(Packet packet) {
    if (!packets.offer(packet)) {
      throw new IllegalStateException("Can't offer packet to queue: " + packets);
    }
    lastPacketSent = packet;
    packetsSent++;

    if (router != null) {
      router.route(packet);
    }
  }

}
