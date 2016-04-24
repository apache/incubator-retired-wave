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

package org.waveprotocol.wave.concurrencycontrol.client;


import junit.framework.TestCase;

import org.waveprotocol.wave.common.logging.PrintLogger;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.server.ConcurrencyControlCore;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.SuperSink;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This tests the CC works with a client and server component. The actual
 * operations used in this test is secondary.
 *
 * TODO(zdwang): Add more meaningful operations in the future more for a sanity
 * check.
 *
 * @author zdwang@google.com (David Wang)
 */

public class ClientAndServerTest extends TestCase {
  private static final DeltaTestUtil NOBODY_UTIL = new DeltaTestUtil("nobody@example.com");

  /**
   * Test Config to pretend editor did something or we got something on the wire from the server.
   * @author zdwang@google.com (David Wang)
   */
  private static final class TestConfig {
    private final PrintLogger logger = new PrintLogger();

    /** always start at version 0 */
    private final SimpleDeltaHistory history = new SimpleDeltaHistory(genSignature(0));
    private final ServerMock serverMock = new ServerMock(
        new ConcurrencyControlCore(history), history);
    private final List<ClientMock> clientMocks = new ArrayList<ClientMock>();

    /**
     * Constructor for test config.
     * @param injectV0Delta by default we inject a NoOp at V0 due to a security constraint on
     *        OT, where you can't transform against V0. By injecting an op at v0, all clients can
     *        happily submit deltas concurrently.
     */
    public TestConfig(String intialBlipXml, int numClients, boolean injectV0Delta)
        throws TransformException, OperationException {
      for (int i = 0; i < numClients; i++) {
        ServerConnectionMock serverConnectionMock = new ServerConnectionMock();
        serverConnectionMock.setServerMock(serverMock);
        serverMock.addClientConnection(serverConnectionMock);

        ConcurrencyControl clientCC = new ConcurrencyControl(logger, genSignature(0));
        serverConnectionMock.setListener(clientCC);

        ClientMock clientMock =
            new ClientMock(clientCC, parse(intialBlipXml), new ParticipantId(i + "@example.com"),
                serverConnectionMock);
        clientCC.initialise(serverConnectionMock, clientMock);

        clientMocks.add(clientMock);
        // Always start at version 0.
        try {
          HashedVersion signature = genSignature(0);
          clientCC.onOpen(signature, signature);
        } catch (ChannelException e) {
          fail("onOpen failed: " + e);
        }
      }

      if (injectV0Delta) {
        // Inject a single NoOp from a null connection, this ensures that all
        // clients submit AFTER version 0.
        WaveletDelta initialDelta = new WaveletDelta(NOBODY_UTIL.getAuthor(),
            genSignature(0), Arrays.asList(NOBODY_UTIL.noOp()));
        serverMock.receive(null, initialDelta);
        serverProcessDeltas();
      }
    }

    /**
     * Pretend client did some insert at the given op versions. We don't need a version
     * number as it's inferred in client CC.
     *
     * @throws OperationException
     * @throws TransformException
     */
    public TestConfig clientDoInsert(int clientNumber, int insertionPoint, String content)
        throws OperationException, TransformException {
      ClientMock clientMock = clientMocks.get(clientNumber);
      clientMock.doInsert(insertionPoint, content);
      clientMock.flush();
      return this;
    }

    /**
     * Predictable signature at given version.
     */
    private HashedVersion genSignature(int version) {
      return HashedVersion.of(version, new byte[] { (byte) version });
    }

    /**
     * Process deltas from the clients on the server.
     * @throws OperationException
     * @throws TransformException
     */
    public void serverProcessDeltas() throws TransformException, OperationException {
      serverMock.start();
    }

    /**
     * Make all the client read operations from CC.
     */
    private void clientsReceiveServerOperations() {
      for (ClientMock c : clientMocks) {
        c.receiveServerOperations();
      }
    }

    private static SuperSink parse(String xml) {
      return DocProviders.POJO.parse(xml);
    }

    /**
     * Check client has a document that looks like the following.
     */
    public TestConfig checkClientDoc(int clientNumber, String xml) {
      clientsReceiveServerOperations();
      DocInitialization expected = parse(xml).asOperation();
      DocInitialization actual = clientMocks.get(clientNumber).getDoc().asOperation();
      assertTrue("[Expected: " + expected + "] [Actual: " + actual + "]",
          OpComparators.SYNTACTIC_IDENTITY.equal(expected, actual));
      return this;
    }

    /**
     * Prevents sending the delta to the server. But the connection
     * still seems connected.
     */
    public void preventSending(int clientNumber) {
      ClientMock client = clientMocks.get(clientNumber);
      ServerConnectionMock connection = client.getConnection();
      serverMock.removeClientConnection(connection);
      connection.setServerMock(null);
    }

    /**
     * Disconnect a single client from server.
     */
    public void killClient(int clientNumber) {
      ClientMock client = clientMocks.get(clientNumber);
      ServerConnectionMock connection = client.getConnection();
      serverMock.removeClientConnection(connection);
      connection.setOpen(false);
      connection.getReceivedDeltas().clear();
      connection.getSentDeltas().clear();
      connection.setServerMock(null);
    }

    /**
     * Reconnect a single client from server
     * @throws OperationException
     * @throws TransformException
     */
    public void reconnectClient(int clientNumber) throws ChannelException,
        TransformException, OperationException {
      ClientMock client = clientMocks.get(clientNumber);
      ServerConnectionMock connection = client.getConnection();
      serverMock.addClientConnection(connection);
      connection.setServerMock(serverMock);
      connection.setOpen(true);
      connection.reconnect(client.getReconnectionVersions());
    }

    /**
     * Turn all the deltas sent by a client into ghosts.
     * @param ghostSend should the client's out going ops be ghosted.
     */
    public void ghostClientDeltas(int clientNumber, boolean ghostSend) {
      ClientMock client = clientMocks.get(clientNumber);
      ServerConnectionMock connection = client.getConnection();
      connection.setGhostSend(ghostSend);
    }

    /**
     * Send all the ghosted deltas from a client.
     */
    public void sendGhostDeltas(int clientNumber) {
      ClientMock client = clientMocks.get(clientNumber);
      ServerConnectionMock connection = client.getConnection();
      connection.sendGhosts();
    }

    /**
     * Reboots the server.
     *
     * @param version the version the server wakes up at
     */
    public void rebootServer(int version) {
      serverMock.reboot(version);
    }
  }

  /**
   * 2 Clients, one do some insert, followed by another client doing some stuff. No transformation
   * on server
   * @throws OperationException  needed.
   * @throws TransformException
   */
  public void testSimple2Client() throws OperationException, TransformException {
    TestConfig t = new TestConfig("<blip><p>abc</p></blip>", 2, true);
    t.clientDoInsert(0, 2, "X").serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>Xabc</p></blip>");

    t.clientDoInsert(1, 3, "Y").serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>XYabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>XYabc</p></blip>");

    t.clientDoInsert(0, 4, "Z").serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>XYZabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>XYZabc</p></blip>");
  }

  /**
   * 3 Clients, concurrently editing.
   *
   * @throws OperationException  needed.
   * @throws TransformException
   */
  public void testConcurrent3Client() throws OperationException, TransformException {
    TestConfig t = new TestConfig("<blip><p>abc</p></blip>", 3, true);
    t.clientDoInsert(0, 2, "0X").clientDoInsert(1, 2, "1X").clientDoInsert(2, 2, "2X");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2X1X0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2X1X0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2X1X0Xabc</p></blip>");

    // lots of concurrent editing
    t.clientDoInsert(0, 5, "A").clientDoInsert(0, 6, "B");
    t.clientDoInsert(1, 3, "E").clientDoInsert(1, 4, "F");
    t.clientDoInsert(2, 6, "G").clientDoInsert(2, 7, "H");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2EFX1ABXGH0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2EFX1ABXGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2EFX1ABXGH0Xabc</p></blip>");
  }

  /**
   * 1 Client dies and tries to recover.
   *
   * The disconnected client did not send any delta before disconnection.
   *
   * @throws OperationException  needed.
   * @throws TransformException
   */
  public void testRecoveryNoUnacknowledged() throws Exception {
    TestConfig t = new TestConfig("<blip><p>abc</p></blip>", 3, true);
    t.clientDoInsert(0, 2, "0X").clientDoInsert(1, 2, "1X").clientDoInsert(2, 2, "2X");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2X1X0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2X1X0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2X1X0Xabc</p></blip>");

    t.killClient(0);
    t.clientDoInsert(0, 5, "A").clientDoInsert(0, 6, "B");

    // lots of concurrent editing without client 0 connected
    t.clientDoInsert(1, 3, "E").clientDoInsert(1, 4, "F");
    t.clientDoInsert(2, 6, "G").clientDoInsert(2, 7, "H");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2X1ABX0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2EFX1XGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2EFX1XGH0Xabc</p></blip>");

    // Reconnect client 1
    t.reconnectClient(0);
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2EFX1ABXGH0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2EFX1ABXGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2EFX1ABXGH0Xabc</p></blip>");
  }

  /**
   * 1 Client dies and tries to recover.
   *
   * The disconnected client did set a delta before disconnection.
   * The server got the delta.
   *
   * @throws OperationException  needed.
   * @throws TransformException
   */
  public void testRecoveryUnacknowledgedRecieved() throws Exception {
    TestConfig t = new TestConfig("<blip><p>abc</p></blip>", 3, true);
    t.clientDoInsert(0, 2, "0X").clientDoInsert(1, 2, "1X").clientDoInsert(2, 2, "2X");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2X1X0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2X1X0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2X1X0Xabc</p></blip>");

    t.clientDoInsert(0, 5, "A");
    t.killClient(0);
    t.clientDoInsert(0, 6, "B");

    // lots of concurrent editing without client 0 connected
    t.clientDoInsert(1, 3, "E").clientDoInsert(1, 4, "F");
    t.clientDoInsert(2, 6, "G").clientDoInsert(2, 7, "H");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2X1ABX0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2EFX1AXGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2EFX1AXGH0Xabc</p></blip>");

    // Reconnect client 1
    t.reconnectClient(0);
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2EFX1ABXGH0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2EFX1ABXGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2EFX1ABXGH0Xabc</p></blip>");
  }

  /**
   * 1 Client dies and tries to recover.
   *
   * The disconnected client did set a delta before disconnection.
   * The delta was not received by the server.
   *
   * @throws OperationException  needed.
   * @throws TransformException
   */
  public void testRecoveryUnacknowledgedMissing() throws Exception {
    TestConfig t = new TestConfig("<blip><p>abc</p></blip>", 3, true);
    t.clientDoInsert(0, 2, "0X").clientDoInsert(1, 2, "1X").clientDoInsert(2, 2, "2X");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2X1X0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2X1X0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2X1X0Xabc</p></blip>");

    t.preventSending(0);
    t.clientDoInsert(0, 5, "A");
    t.clientDoInsert(0, 6, "B");
    t.killClient(0);

    // lots of concurrent editing without client 0 connected
    t.clientDoInsert(1, 3, "E").clientDoInsert(1, 4, "F");
    t.clientDoInsert(2, 6, "G").clientDoInsert(2, 7, "H");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2X1ABX0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2EFX1XGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2EFX1XGH0Xabc</p></blip>");

    // Reconnect client 1
    t.reconnectClient(0);
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2EFX1ABXGH0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2EFX1ABXGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2EFX1ABXGH0Xabc</p></blip>");
  }

  /**
   * 1 Client dies and tries to recover several time.
   *
   * @throws OperationException  needed.
   * @throws TransformException
   */
  public void testFlakyClient() throws Exception {
    TestConfig t = new TestConfig("<blip><p>abc</p></blip>", 3, true);
    t.clientDoInsert(0, 2, "0X").clientDoInsert(1, 2, "1X").clientDoInsert(2, 2, "2X");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2X1X0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2X1X0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2X1X0Xabc</p></blip>");

    t.killClient(0);

    // lots of concurrent editing without client 0 connected
    t.clientDoInsert(1, 3, "E").clientDoInsert(1, 4, "F");
    t.clientDoInsert(2, 6, "G").clientDoInsert(2, 7, "H");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2X1X0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2EFX1XGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2EFX1XGH0Xabc</p></blip>");

    // Reconnect client 1
    t.reconnectClient(0);
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2EFX1XGH0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2EFX1XGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2EFX1XGH0Xabc</p></blip>");

    // more edits
    t.clientDoInsert(1, 3, "+");
    t.clientDoInsert(2, 4, "-");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>2+E-FX1XGH0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2+E-FX1XGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2+E-FX1XGH0Xabc</p></blip>");

    t.killClient(0);
    t.clientDoInsert(0, 2, "?");

    // more edits
    t.clientDoInsert(1, 4, "+");
    t.clientDoInsert(2, 6, "-");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>?2+E-FX1XGH0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2++E--FX1XGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2++E--FX1XGH0Xabc</p></blip>");

    // Reconnect client 1
    t.reconnectClient(0);
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>?2++E--FX1XGH0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>?2++E--FX1XGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>?2++E--FX1XGH0Xabc</p></blip>");

    t.killClient(0);
    t.clientDoInsert(0, 3, "?");
    t.checkClientDoc(0, "<blip><p>??2++E--FX1XGH0Xabc</p></blip>");

    // Reconnect client 1
    t.reconnectClient(0);
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>??2++E--FX1XGH0Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>??2++E--FX1XGH0Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>??2++E--FX1XGH0Xabc</p></blip>");
  }

  /**
   * Server crashes with 1 client and reset to version 0
   *
   * The client connects back to an older version
   *
   * @throws OperationException  needed.
   * @throws TransformException
   */
  public void testRecoveryServerCrash1ClientReset0() throws Exception {
    TestConfig t = new TestConfig("<blip><p>abc</p></blip>", 1, false);
    t.clientDoInsert(0, 2, "0X").clientDoInsert(0, 4, "1X");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>0X1Xabc</p></blip>");

    t.killClient(0);
    t.rebootServer(0);
    t.reconnectClient(0);
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>0X1Xabc</p></blip>");
  }

  /**
   * Server crashes with 1 client without reset to version 0
   *
   * The client connects back to an older version
   *
   * @throws OperationException  needed.
   * @throws TransformException
   */
  public void testRecoveryServerCrash1Client() throws Exception {
    TestConfig t = new TestConfig("<blip><p>abc</p></blip>", 1, true);
    t.clientDoInsert(0, 2, "0X").clientDoInsert(0, 4, "1X").clientDoInsert(0, 6, "2X");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>0X1X2Xabc</p></blip>");

    t.killClient(0);
    t.rebootServer(1);

    t.clientDoInsert(0, 8, "0Y");
    t.checkClientDoc(0, "<blip><p>0X1X2X0Yabc</p></blip>");

    t.reconnectClient(0);
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>0X1X2X0Yabc</p></blip>");
  }


  /**
   * Server crashes with 2 client
   *
   * The client connects back to an older version
   *
   * @throws OperationException  needed.
   * @throws TransformException
   */
  public void testRecoveryServerCrash2Clients() throws Exception {
    TestConfig t = new TestConfig("<blip><p>abc</p></blip>", 2, true);
    t.clientDoInsert(0, 2, "0X").clientDoInsert(0, 4, "1X");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>0X1Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>0X1Xabc</p></blip>");

    t.killClient(0);
    t.killClient(1);
    // Server wakes up at version 1
    t.rebootServer(1);

    // Clients do more stuff whilst off line
    t.clientDoInsert(0, 2, "0Y").clientDoInsert(1, 2, "1Y");

    t.reconnectClient(0);
    try {
      t.reconnectClient(1);
      fail("ConnectionFailedException expected");
    } catch (ChannelException expected) {
    }
    // Client 1 dead, since for v1 release we don't support recovery from
    // server crash where there are multiple client that are concurrently editing
    t.killClient(1);

    t.serverProcessDeltas();

    t.checkClientDoc(0, "<blip><p>0Y0X1Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>1Y0X1Xabc</p></blip>");
  }

  /**
   * Test a ghost submit that ended up on the server from a previous client session before
   * the client's resubmit.
   */
  public void testRecoveryGhostBeforeResubmit() throws Exception {
    TestConfig t = new TestConfig("<blip><p>abc</p></blip>", 1, false);
    // mimic delta not getting to server
    t.ghostClientDeltas(0, true);
    t.clientDoInsert(0, 2, "0X").clientDoInsert(0, 4, "1X");
    t.checkClientDoc(0, "<blip><p>0X1Xabc</p></blip>");
    t.killClient(0);
    t.ghostClientDeltas(0, false);

    // server now gets client 0's delta from a previous session before client's resend
    t.sendGhostDeltas(0);
    t.reconnectClient(0);

    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>0X1Xabc</p></blip>");
  }

  /**
   * Test a ghost submit that ended up on the server from a previous client session after
   * a client's resubmit.
   */
  public void testRecoveryGhostAfterResubmit() throws Exception {
    TestConfig t = new TestConfig("<blip><p>abc</p></blip>", 1, false);
    // mimic delta not getting to server
    t.ghostClientDeltas(0, true);
    t.clientDoInsert(0, 2, "0X").clientDoInsert(0, 4, "1X");
    t.checkClientDoc(0, "<blip><p>0X1Xabc</p></blip>");
    t.killClient(0);
    t.ghostClientDeltas(0, false);

    t.reconnectClient(0);
    // server now gets client 0's delta from a previous session after client's resend
    t.sendGhostDeltas(0);

    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>0X1Xabc</p></blip>");
  }


  /**
   * 1 Client dies and tries to recover, but there is a ghost delta from a previous submit.
   *
   * The disconnected client send a delta before disconnecting.
   *
   * @throws OperationException  needed.
   * @throws TransformException
   */
  public void testRecoveryWithGhost() throws Exception {
    TestConfig t = new TestConfig("<blip><p>abc</p></blip>", 3, true);
    // mimic delta not getting to server for client 0
    t.ghostClientDeltas(0, true);
    t.clientDoInsert(0, 2, "0X").clientDoInsert(1, 2, "1X").clientDoInsert(2, 2, "2X");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>0X2X1Xabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>2X1Xabc</p></blip>");
    t.checkClientDoc(2, "<blip><p>2X1Xabc</p></blip>");

    t.killClient(0);
    t.ghostClientDeltas(0, false);

    t.clientDoInsert(0, 7, "A").clientDoInsert(0, 8, "B");

    // lots of concurrent editing without client 0 connected
    t.clientDoInsert(1, 2, "E").clientDoInsert(1, 3, "F");
    t.clientDoInsert(2, 7, "G").clientDoInsert(2, 8, "H");
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>0X2X1ABXabc</p></blip>");
    t.checkClientDoc(1, "<blip><p>EF2X1XaGHbc</p></blip>");
    t.checkClientDoc(2, "<blip><p>EF2X1XaGHbc</p></blip>");

    // Reconnect client 0
    t.reconnectClient(0);
    // server now gets client 0's delta from a previous session
    t.sendGhostDeltas(0);
    t.serverProcessDeltas();
    t.serverProcessDeltas();
    t.checkClientDoc(0, "<blip><p>0XEF2X1ABXaGHbc</p></blip>");
    t.checkClientDoc(1, "<blip><p>0XEF2X1ABXaGHbc</p></blip>");
    t.checkClientDoc(2, "<blip><p>0XEF2X1ABXaGHbc</p></blip>");
  }

}
