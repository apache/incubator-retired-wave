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
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.SuperSink;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.operation.OpComparators;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.TransformException;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.testing.DeltaTestUtil;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This is a thorough test of client CC by emitting fake events and checking the outputs from CC.
 * The operations used in the tests are secondary, we are more focused on on the CC producing the
 * right version numbers etc.
 *
 * @author zdwang@google.com (David Wang)
 */

public class OT3Test extends TestCase {
  private static final ParticipantId DEFAULT_CREATOR = new ParticipantId("test@example.com");
  private static final DeltaTestUtil CLIENT_UTIL = new DeltaTestUtil(DEFAULT_CREATOR);
  private static final DeltaTestUtil EXTRA_UTIL = new DeltaTestUtil("actasme@example.com");

  /**
   * NOTE(zdwang): Different user id to DEFAULT_CREATOR so that comparison test in CC for
   * reconnection works.
   */
  private static final DeltaTestUtil SERVER_UTIL = new DeltaTestUtil("someoneelse@example.com");

  /**
   * Test Config to pretend editor did something or we got something on the wire from the server.
   * @author zdwang@google.com (David Wang)
   */
  private static final class TestConfig {
    private final PrintLogger logger = new PrintLogger();

    ClientMock clientMock;
    ServerConnectionMock serverConnectionMock = new ServerConnectionMock();

    public void init(int version) {
      init(version, null);
    }

    public void init(int version, String intialBlipXml) {
      HashedVersion signature = genSignature(version);
      serverConnectionMock = new ServerConnectionMock();
      ConcurrencyControl clientCC = new ConcurrencyControl(logger, signature);
      serverConnectionMock.setListener(clientCC);

      clientMock = new ClientMock(
          clientCC,
          intialBlipXml != null ? parse(intialBlipXml) : null,
              DEFAULT_CREATOR, serverConnectionMock);
      clientCC.initialise(serverConnectionMock, clientMock);

      try {
        clientCC.onOpen(signature, signature);
      } catch (ChannelException e) {
        fail("onOpen failed: " + e);
      }
    }

    /**
     * Pretend client did some insert at the given op versions. We don't need a version
     * number as it's inferred in client CC.
     *
     * Flush at the end.
     * @throws OperationException
     * @throws TransformException
     */
    public TestConfig clientDoInsert(int insertionPoint, String content)
        throws OperationException, TransformException {
      return clientDoInsert(insertionPoint, content, true);
    }

    /**
     * Pretend client did some insert at the given op versions. We don't need a version
     * number as it's inferred in client CC.
     *
     * @throws OperationException
     * @throws TransformException
     */
    public TestConfig clientDoInsert(int insertionPoint, String content, boolean flush)
        throws OperationException, TransformException {
      clientMock.doInsert(insertionPoint, content);
      if (flush) {
        clientMock.flush();
      }
      return this;
    }

    /**
     * Pretend client did some ops at the given op versions.
     * @throws OperationException
     * @throws TransformException
     */
    private TestConfig clientDoOps(WaveletOperation ... ops)
        throws OperationException, TransformException {
      for (WaveletOperation op : ops) {
        clientMock.addClientOperation(op);
      }
      clientMock.flush();
      return this;
    }

    /**
     * Pretend client did some ops.
     *
     * Assumption: Noops are not merged in OperationMergingDelta. If they are, then tests
     * will break as they count the number of ops sent from the client to the server.
     *
     * @throws TransformException
     */
    public TestConfig clientDoOps(int numOps) throws OperationException, TransformException {
      for (int i = 0; i < numOps; i++) {
        clientMock.addClientOperation(new NoOp(new WaveletOperationContext(
            clientMock.getParticipantId(), 0L, 1L)));
      }
      clientMock.flush();
      return this;
    }

    private WaveletOperation noOpDocOp(String blipId) {
      WaveletOperationContext context = new WaveletOperationContext(
          clientMock.getParticipantId(), 0L, 1L);
      BlipContentOperation blipOp = new BlipContentOperation(context, (new DocOpBuilder()).build());

      return new WaveletBlipOperation(blipId, blipOp);
    }
    /**
     * Pretend client did some doc ops.
     *
     * @throws TransformException
     */
    public TestConfig clientDoDocOps(String... blipIds) throws OperationException,
        TransformException {
      for (String b : blipIds) {
        clientMock.addClientOperation(noOpDocOp(b));
      }
      clientMock.flush();
      return this;
    }

    /**
     * Pretend client did some insert at the given op versions.
     * @throws OperationException
     * @throws TransformException
     */
    public TestConfig serverDoInsert(int startVersion, int insertionPoint, String content,
        int remaining) throws OperationException, TransformException {
      TransformedWaveletDelta d = TransformedWaveletDelta.cloneOperations(SERVER_UTIL.getAuthor(),
          genSignature(startVersion + 1), 0L, Arrays.asList(
              SERVER_UTIL.insert(insertionPoint, content, remaining, null)));
      serverConnectionMock.triggerServerDeltas(Collections.singletonList(d));
      return this;
    }

    /**
     * Pretend server did some ops at the given op version using different deltas.
     * @throws OperationException
     * @throws TransformException
     */
    public TestConfig serverDoOps(int version) throws TransformException, OperationException {
      return serverDoOps(version, 1);
    }

    /**
     * Pretend server did some ops at the given op version using different deltas.
     *
     * We generate a predictable signature here for testing later. Each signature is the
     * version on the server after the ops.
     */
    public TestConfig serverDoOps(int startVersion, int numOps)
        throws TransformException, OperationException {
      ArrayList<TransformedWaveletDelta> deltas = CollectionUtils.newArrayList();
      for (int i = 0; i < numOps; i++) {
        TransformedWaveletDelta d = TransformedWaveletDelta.cloneOperations(
            SERVER_UTIL.getAuthor(), genSignature(startVersion + i + 1), 0L,
            Arrays.asList(SERVER_UTIL.noOp()));
        deltas.add(d);
      }
      serverConnectionMock.triggerServerDeltas(deltas);
      return this;
    }

    /**
     * Pretend the server echos back client's operation. Timestamp default to 0L.
     */
    public TestConfig serverDoEchoBack(int startVersion)
        throws TransformException, OperationException {
      return serverDoEchoBack(startVersion, 0L);
    }

    /**
     * Pretend the server echos back client's operation. Timestamp default to 0L.
     */
    public TestConfig serverDoEchoBackDocOp(int startVersion, String blipId)
        throws TransformException, OperationException {
      TransformedWaveletDelta d = TransformedWaveletDelta.cloneOperations(
          clientMock.getParticipantId(), genSignature(startVersion + 1), 0L,
          Arrays.asList(noOpDocOp(blipId)));
      serverConnectionMock.triggerServerDeltas(Collections.singletonList(d));
      return this;
    }

    /**
     * Pretend the server echos back client's operation.
     */
    public TestConfig serverDoEchoBack(int startVersion, long timestamp)
        throws TransformException, OperationException {
      TransformedWaveletDelta d = TransformedWaveletDelta.cloneOperations(
          clientMock.getParticipantId(), genSignature(startVersion + 1), timestamp,
          Arrays.asList(SERVER_UTIL.noOp()));
      serverConnectionMock.triggerServerDeltas(Collections.singletonList(d));
      return this;
    }

    /**
     * Pretend the server acked one op the given version
     * @throws OperationException
     * @throws TransformException
     */
    public TestConfig serverAck(int version) throws TransformException, OperationException {
      return serverAck(version, 1);
    }

    /**
     * Pretend the server acked the given version.
     *
     * We generate a predictable signature here for testing later. Which is the version on the
     * server after the op.
     *
     * @param version The new version after the operations from the client is applied.
     * @param numApplied Number of operations applied on the server.
     * @throws OperationException
     * @throws TransformException
     */
    public TestConfig serverAck(int version, int numApplied)
        throws TransformException, OperationException {
      serverConnectionMock.triggerServerSuccess(numApplied, genSignature(version));
      return this;
    }

    /**
     * Pretend the server send a commit notification for the given version.
     *
     * @param version The new version after the operations from the client is applied.
     * @throws OperationException
     * @throws TransformException
     */
    public TestConfig serverCommit(int version)
        throws TransformException, OperationException {
      serverConnectionMock.triggerServerCommit(version);
      return this;
    }

    /**
     * Pretend we are reconnecting to the server.
     * @throws OperationException
     * @throws TransformException
     */
    public TestConfig reconnectToServer() throws Exception {
      serverConnectionMock.setOpen(true);
      serverConnectionMock.reconnect(clientMock.getReconnectionVersions());
      return this;
    }

    /**
     * Pretend we are disconnected to the server.
     */
    public TestConfig disconnectFromServer() {
      serverConnectionMock.setOpen(false);
      return this;
    }

    /**
     * Predictable signature at given version.
     */
    private HashedVersion genSignature(int version) {
      return HashedVersion.of(version, new byte[] {(byte) version});
    }


    /**
     * Server open the connection at the given version and tells of the last version.
     */
    public TestConfig serverDoOpen(int startVersion, int endVersion)
        throws ChannelException {
      serverConnectionMock.triggerOnOpen(genSignature(startVersion), genSignature(endVersion));
      return this;
    }

    private void clientReceiveOpFromCC(boolean expectOps) {
      if (expectOps) {
        assertTrue(clientMock.getNumOpsReceived() > 0);
      }
      clientMock.clearEvents();
      clientMock.receiveServerOperations();
    }

    /**
     * Check client got nothing from the server.
     */
    public TestConfig checkClientGotOps() {
      clientReceiveOpFromCC(false);
      ArrayList<WaveletOperation> clientGot = clientMock.getServerOperations();
      assertEquals(0, clientGot.size());
      return this;
    }

    /**
     * Check client got operations from the server with the version before the first op.
     */
    public TestConfig checkClientGotOps(int startVersion) {
      return checkClientGotOps(startVersion, 1);
    }

    /**
     * Check client got operations from the server with the version before the first op and
     * the given number of ops.
     */
    public TestConfig checkClientGotOps(int startVersion, int numOps) {
      clientReceiveOpFromCC(numOps != 0);
      ArrayList<WaveletOperation> clientGot = clientMock.getServerOperations();
      assertEquals(numOps, clientGot.size());
      for (int i = 0; i < numOps; i++) {
        assertEquals(1, clientGot.get(i).getContext().getVersionIncrement());
      }
      // Clear the client recieved ops in clientMock
      clientGot.clear();
      return this;
    }

    /**
     * Check client didn't send anything.
     */
    public TestConfig checkClientSentOps() {
      List<WaveletDelta> serverGot = serverConnectionMock.getSentDeltas();
      assertEquals(0, serverGot.size());
      return this;
    }

    /**
     * Check server got operations from the client with the version before the first op as
     * the argument.
     */
    public TestConfig checkClientSentOps(int startVersion) {
      return checkClientSentOps(startVersion, 1);
    }

    /**
     * Check server got operations from the client with the given target version
     * and the given number of ops.
     */
    public TestConfig checkClientSentOps(int startVersion, int numOps) {
      List<WaveletDelta> serverGot = serverConnectionMock.getSentDeltas();
      // Should have only sent 1 delta
      assertEquals(1, serverGot.size());

      // Delta has the version number
      assertEquals(startVersion, serverGot.get(0).getTargetVersion().getVersion());

      assertEquals(numOps, serverGot.get(0).size());

      // Clear the sent ops inside serverConnectionMock
      serverGot.clear();
      return this;
    }

    /**
     * Check client sent signatures from the given version.
     * @param versions Each version number is after the operation as applied.
     */
    public TestConfig checkClientSentOpen(int ... versions) {
      List<HashedVersion> signatures = serverConnectionMock.getReconnectSignatures();
      assertEquals(versions.length, signatures.size());

      for (int i = 0; i < versions.length; i++) {
        assertEquals(genSignature(versions[i]), signatures.get(i));
      }
      signatures.clear();
      return this;
    }

    private static SuperSink parse(String xml) {
      return DocProviders.POJO.parse(xml);
    }

    /**
     * Check client has a document that looks like the following.
     */
    public TestConfig checkClientDoc(String xml) {
      DocInitialization expected = parse(xml).asOperation();
      DocInitialization actual = clientMock.getDoc().asOperation();
      assertTrue(OpComparators.SYNTACTIC_IDENTITY.equal(expected, actual));
      return this;
    }
  }

  /**
   * Test the various divergence to check we transform the operations correctly. It's not
   * an exhaustive test of transformation, but to test CC call transformation properly.
   *
   * @throws OperationException
   * @throws TransformException
   */
  public void testTransformOperations() throws TransformException, OperationException {
    TestConfig t = new TestConfig();

    // Empty Case
    t.init(0);
    t.checkClientGotOps().checkClientSentOps();

    // Simple insert from client
    t.init(0, "<blip><p>abc</p></blip>");
    t.clientDoInsert(2, "X");
    t.checkClientGotOps().checkClientSentOps(0).checkClientDoc("<blip><p>Xabc</p></blip>");

    // Simple insert from server
    t.init(0, "<blip><p>abc</p></blip>");
    t.serverDoInsert(0, 2, "X", 5);
    t.checkClientGotOps(0).checkClientSentOps().checkClientDoc("<blip><p>Xabc</p></blip>");

    // Simple conflict
    //     c1 /\ s1
    t.init(0, "<blip><p>abc</p></blip>");
    t.clientDoInsert(2, "X").serverDoInsert(0, 3, "Y", 4);
    t.checkClientGotOps(0).checkClientSentOps(0).checkClientDoc("<blip><p>XaYbc</p></blip>");

    // Conflict
    //     c1 /\ s1
    //    c2 /
    t.init(0, "<blip><p>abc</p></blip>");
    t.clientDoInsert(2, "X").clientDoInsert(3, "Y");
    t.serverDoInsert(0, 3, "A", 4);
    t.checkClientGotOps(0).checkClientSentOps(0).checkClientDoc("<blip><p>XYaAbc</p></blip>");

    // Conflict
    // c1, c2 /\ s1
    //    c3 /
    t.init(0, "<blip><p>abc</p></blip>");
    t.clientDoInsert(2, "X", false).clientDoInsert(4, "Y").clientDoInsert(6, "Z");
    t.serverDoInsert(0, 3, "A", 5);
    t.checkClientGotOps(0).checkClientSentOps(0, 1).checkClientDoc("<blip><p>XaYAbZc</p></blip>");

    // Conflict
    //     c1 /\ s1
    //          \ s2
    t.init(0, "<blip><p>abc</p></blip>");
    t.clientDoInsert(2, "X").serverDoInsert(0, 3, "Y", 4).serverDoInsert(1, 4, "Z", 4);
    t.checkClientGotOps(0, 2).checkClientSentOps(0).checkClientDoc("<blip><p>XaYZbc</p></blip>");

    // Conflict
    //    c1 /\ s1
    //   c2 /  \ s2
    t.init(0, "<blip><p>abc</p></blip>");
    t.clientDoInsert(2, "X").clientDoInsert(3, "Y");
    t.serverDoInsert(0, 3, "Z", 4).serverDoInsert(1, 4, "A", 4);
    t.checkClientGotOps(0, 2).checkClientSentOps(0).checkClientDoc("<blip><p>XYaZAbc</p></blip>");


    // Empty Server Delta
    t.init(0);
    t.serverConnectionMock.triggerServerDeltas(Collections.singletonList(
        new TransformedWaveletDelta(null, HashedVersion.unsigned(0), 0L,
            Arrays.<WaveletOperation> asList())));
    t.checkClientGotOps().checkClientSentOps();
    t.clientDoOps(1);
    t.checkClientGotOps().checkClientSentOps(0);
  }

  /**
   * Test the CC caches the operations from the server correct when the client is not
   * ready receive them yet.
   *
   * @throws OperationException
   * @throws TransformException
   */
  public void testServerOperationCache() throws TransformException, OperationException {
    TestConfig t = new TestConfig();

    //                     c1 /\ s1
    //  c2 (not given to cc) /
    t.init(0, "<blip><p>abc</p></blip>");
    t.clientDoInsert(2, "X").clientDoInsert(3, "Y", false).checkClientSentOps(0);
    t.checkClientDoc("<blip><p>XYabc</p></blip>");
    t.serverDoInsert(0, 3, "A", 4).serverDoInsert(1, 4, "B", 4);
    t.clientMock.flush();
    t.checkClientGotOps(0, 2).checkClientSentOps().checkClientDoc("<blip><p>XYaABbc</p></blip>");


    // Check that if we don't flush the operations cached in the client, bad things happen
    //                     c1 /\ s1
    //  c2 (not given to cc) /
    t.init(0, "<blip><p>abc</p></blip>");
    t.clientDoInsert(2, "X").clientDoInsert(3, "Y", false).checkClientSentOps(0);
    t.checkClientDoc("<blip><p>XYabc</p></blip>");
    t.serverDoInsert(0, 3, "A", 4);
    try {
      t.checkClientGotOps(0, 2).checkClientSentOps().checkClientDoc("<blip><p>XYABabc</p></blip>");
      fail("Expected a runtime exception");
    } catch (RuntimeException expected) {
      // Expect an exception because the client ops didn't get transformed
      // so the expected length of the document is incorrect when applying
      // server ops.
      assertTrue(expected.getCause() instanceof OperationException);
    }
  }

  /**
   * Tests bundling of operations into deltas and queuing of operations while
   * others are in flight.
   */
  public void testClientOperationQueuing() throws TransformException, OperationException {
    TestConfig t = new TestConfig();

    // Tests queuing of a delta until one which is in flight has been acked, at
    // which point the waiting delta should be sent.
    t.init(1);
    t.clientDoOps(1).checkClientSentOps(1).checkClientGotOps();
    t.clientDoOps(1).checkClientSentOps().checkClientGotOps();
    t.serverAck(2).checkClientSentOps(2).checkClientGotOps(1);
    t.serverAck(3).checkClientSentOps().checkClientGotOps(2);

    // Tests bundling of operations by the one author into a single delta.
    t.init(1);
    t.clientDoOps(new WaveletOperation[] {CLIENT_UTIL.noOp(), CLIENT_UTIL.noOp()});
    t.checkClientSentOps(1, 2).checkClientGotOps();
    t.serverAck(3, 2).checkClientSentOps().checkClientGotOps(1, 2);

    // Tests that two operations from differing creators are sent as separate
    // deltas. In the process, checks again that only one delta is in flight at
    // a time and that the acking of the first causes the second to be sent.
    t.init(1);
    t.clientDoOps(new WaveletOperation[] {CLIENT_UTIL.noOp(), EXTRA_UTIL.noOp()});
    t.checkClientSentOps(1, 1).checkClientGotOps();
    t.serverAck(2, 1).checkClientSentOps(2, 1).checkClientGotOps(1, 1);
    t.serverAck(3, 1).checkClientSentOps().checkClientGotOps(2, 1);
  }

  /**
   * Test various ways the operations are interleaved. We don't test they are transformed
   * as they are tested elsewhere.
   * @throws OperationException
   * @throws TransformException
   */
  public void testConcurrencySimulation() throws OperationException, TransformException {
    TestConfig t = new TestConfig();

    // Simple insert from client
    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0);
    t.serverAck(1).checkClientSentOps();

    // Simple insert from server
    t.init(0);
    t.serverDoOps(0).checkClientGotOps(0).checkClientSentOps();

    // No Conflict. Left is client action. Right is server action.
    //             c1 / ack c1
    //            c2 /\ s1
    //  c3 (cached) / / ack c2, causes c3' to be sent
    //          s1' \/ ack c3
    //               \ s2
    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
    t.serverAck(1).checkClientSentOps().checkClientGotOps(0); // Expect a version update op here.
    t.serverDoOps(1).checkClientSentOps().checkClientGotOps(1);
    t.clientDoOps(1).clientDoOps(1).checkClientSentOps(2).checkClientGotOps();
    t.serverAck(3).checkClientSentOps(3).checkClientGotOps(2);
    t.serverAck(4).checkClientSentOps().checkClientGotOps(3);
    t.serverDoOps(4).checkClientSentOps().checkClientGotOps(4);

    // Conflict. Left is client action. Right is server action.
    //             c1 / ack c1
    //                \ s1
    //             c2 /\ s2
    //   c3 (cached) / / ack c2, causes c3', c4' to be sent
    //  c4 (cached) /
    //        s2''' \
    //           c5 /
    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
    t.serverAck(1).checkClientSentOps().checkClientGotOps(0);
    t.serverDoOps(1).checkClientSentOps().checkClientGotOps(1);
    t.clientDoOps(1).clientDoOps(1).clientDoOps(1).serverDoOps(2)
        .checkClientSentOps(2).checkClientGotOps(2);
    t.serverAck(4).checkClientSentOps(4, 2).checkClientGotOps(3);
    t.clientDoOps(1).checkClientSentOps().checkClientGotOps();
  }

  /**
   * Test errors in the protocol.
   */
  public void testErrorConditions() throws TransformException, OperationException {
    TestConfig t = new TestConfig();

    // Missing ack
    //             c1 /\ s1
    //            s1' \
    //                 \ s2
    t.init(0);
    t.clientDoOps(1).serverDoOps(0).checkClientSentOps(0).checkClientGotOps(0);
    try {
      t.serverDoOps(2);
      fail("Suppose to fail with unexpected version");
    } catch (TransformException ex) {
    }

    // Wrong ack
    //             c1 /
    //
    //                | ack c2
    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
    try {
      t.serverAck(2);
      fail("Suppose to fail with unexpected version");
    } catch (TransformException ex) {
    }
  }

  /**
   * test reconnecting to the server.
   * @throws OperationException
   * @throws TransformException
   */
  public void testRecovery() throws Exception {
    TestConfig t = new TestConfig();
    // Simple case, but also test breaking connection.
    //              c1 / ack c1 <-- recover from here
    //         c2, c3 / ack c2, c3
    // c4 (in flight)/ <-- connection broken
    //  c5 (cached) /
    // c6 (cached) /
    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
    t.serverAck(1).checkClientSentOps().checkClientGotOps(0);
    t.clientDoOps(2).checkClientSentOps(1, 2).checkClientGotOps();
    t.serverAck(3, 2).checkClientSentOps().checkClientGotOps(1, 2);
    t.clientDoOps(1).checkClientSentOps(3).checkClientGotOps();
    t.clientDoOps(1).checkClientSentOps().checkClientGotOps();
    // Break connection
    t.disconnectFromServer();
    t.clientDoOps(1).checkClientSentOps().checkClientGotOps();
    // Reconnect
    t.reconnectToServer().checkClientSentOpen(0, 1, 3);
    // Resend delta containing c2, c3
    t.serverDoOpen(1, 3).checkClientSentOps(1, 2);


    // Simple case
    //               c1 / ack c1
    //              c2 / ack c2 <-- recover from here
    // c4 (in flight) / <-- connection broken
    //  c5 (cached)  /
    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
    t.serverAck(1).checkClientSentOps().checkClientGotOps(0);
    t.clientDoOps(1).checkClientSentOps(1).checkClientGotOps();
    t.serverAck(2).checkClientSentOps().checkClientGotOps(1);
    t.clientDoOps(1).checkClientSentOps(2).checkClientGotOps();
    t.clientDoOps(1).checkClientSentOps().checkClientGotOps();
    // Reconnect
    t.reconnectToServer().checkClientSentOpen(0, 1, 2);
    // Resend delta containing c4
    t.serverDoOpen(2, 2).checkClientSentOps(2, 1);


    // Simple case
    //              c1 / ack c1 <-- recover from here
    //             c2 / ack c2
    // c3 (ack lost) / ack c3 <-- connection broken
    //  c4 (cached) /
    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
    t.serverAck(1).checkClientSentOps().checkClientGotOps(0);
    t.clientDoOps(1).checkClientSentOps(1).checkClientGotOps();
    t.serverAck(2).checkClientSentOps().checkClientGotOps(1);
    t.clientDoOps(1).checkClientSentOps(2).checkClientGotOps();
    t.clientDoOps(1).checkClientSentOps().checkClientGotOps();
    // Reconnect
    t.reconnectToServer().checkClientSentOpen(0, 1, 2);
    t.serverDoOpen(2, 3).checkClientSentOps();
    // Resend delta containing c2
    t.serverDoEchoBack(2).checkClientSentOps(3);

    // Server ops, no transformation needed at recovery
    //              c1 / ack c1
    // U               \ s1
    //              c2 / ack c2 <-- recover from here (test we chop the inferred server path)
    // c3 (in flight) / <-- connection broken
    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
    t.serverAck(1).checkClientSentOps().checkClientGotOps(0);
    t.serverDoOps(1).checkClientSentOps().checkClientGotOps(1);
    t.clientDoOps(1).checkClientSentOps(2).checkClientGotOps();
    t.serverAck(3).checkClientSentOps().checkClientGotOps(2);
    t.clientDoOps(1).checkClientSentOps(3).checkClientGotOps();
    // Reconnect
    t.reconnectToServer().checkClientSentOpen(2, 3);
    // Resend delta containing c3
    t.serverDoOpen(2, 2).checkClientSentOps(2, 1);

    // Server ops, transformation needed at recovery and no operation comparison need.
    //              c1 / ack c1 <-- recover from here  <-- point of resend
    //    c2 (cached) /\ s2 <-- connection broken
    //                  \ s3
    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
    t.serverAck(1).checkClientSentOps().checkClientGotOps(0);
    // Break connection
    t.disconnectFromServer();
    t.clientDoOps(1).checkClientSentOps().checkClientGotOps();
    // Reconnect
    t.reconnectToServer().checkClientSentOpen(0, 1);
    t.serverDoOpen(1, 4).checkClientSentOps(1);
    // Now send back ops on server, need to test comparison here.
    t.serverDoOps(1).checkClientSentOps().checkClientGotOps(1);
    t.serverDoOps(2).checkClientSentOps().checkClientGotOps(2);


    // Server ops, transformation needed at recovery and operation comparison need.
    //              c1 / ack c1 <-- recover from here
    //  c2 (ack lost) /\ s2 <-- connection broken
    //  c3 (cached)  / / ack c2 <-- point of resend
    //                 \ s3
    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
    t.serverAck(1).checkClientSentOps().checkClientGotOps(0);
    t.clientDoOps(1).clientDoOps(1).checkClientSentOps(1).checkClientGotOps();
    // Reconnect
    t.reconnectToServer().checkClientSentOpen(0, 1);
    t.serverDoOpen(1, 4).checkClientSentOps();
    // Now send back ops on server, need to test comparison here.
    t.serverDoOps(1).checkClientSentOps().checkClientGotOps(1);
    t.serverDoEchoBack(2).checkClientSentOps(3).checkClientGotOps(2);
    t.serverDoOps(3).checkClientSentOps().checkClientGotOps(3);


    // Server ops, transformation needed at recovery and operation comparison need.
    //              c1 / ack c1 <-- recover from here
    // c2 (in flight) /\ s2 <-- connection broken
    //  c3 (cached)  /  \ s3 <-- point of resend
    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
    t.serverAck(1).checkClientSentOps().checkClientGotOps(0);
    t.clientDoOps(1).clientDoOps(1).checkClientSentOps(1).checkClientGotOps();
    // Reconnect
    t.reconnectToServer().checkClientSentOpen(0, 1);
    t.serverDoOpen(1, 3).checkClientSentOps();
    // Now send back ops on server, need to test comparison here.
    t.serverDoOps(1).checkClientSentOps().checkClientGotOps(1);
    t.serverDoOps(2).checkClientSentOps(3, 1).checkClientGotOps(2);

    // Not recoverable, no signatures recognised.
    //                   <-- server restarted before inferred server path
    //              c1 / ack c1
    //  c2 (ack lost) / <-- connection broken
    t.init(1);  // start at version 1
    t.clientDoOps(1).checkClientSentOps(1).checkClientGotOps();
    t.serverAck(2).checkClientSentOps().checkClientGotOps(1);
    t.clientDoOps(1).checkClientSentOps(2).checkClientGotOps();
    // Reconnect
    t.reconnectToServer().checkClientSentOpen(1, 2);
    // Server doesn't recognize any signature, so it sends the latest signature it knows
    try {
      t.serverDoOpen(0, 0);
      fail("ConnectionFailedException expected");
    } catch (ChannelException expected) {
    }
  }

  /**
   * Mainly to test that we are doing comparison for doc op.
   */
  public void testEchoBackDocumentEquality() throws Exception {
    TestConfig t = new TestConfig();
    // Simple case
    //              c1 / ack c1
    //             c2 / ack c2
    // c3 (ack lost) / ack c3 <-- connection broken, <-- recover from here
    //  c4 (cached) /
    t.init(0);
    // Use different blip ids so that we don't merge ops
    t.clientDoDocOps("blip1").checkClientSentOps(0).checkClientGotOps();
    t.serverAck(1).checkClientSentOps().checkClientGotOps(0);
    t.clientDoDocOps("blip2").checkClientSentOps(1).checkClientGotOps();
    t.serverAck(2).checkClientSentOps().checkClientGotOps(1);
    t.clientDoDocOps("blip3").checkClientSentOps(2).checkClientGotOps();
    t.clientDoDocOps("blip4").checkClientSentOps().checkClientGotOps();
    // Reconnect
    t.reconnectToServer().checkClientSentOpen(0, 1, 2);
    t.serverDoOpen(2, 3).checkClientSentOps();
    // Client interprets the echo-back as an ack, which we test by
    // seeing that it goes on to send c4
    t.serverDoEchoBackDocOp(2, "blip3").checkClientSentOps(3);
  }

  /**
   * Test being disconnected several times.
   */
  public void testRecoveryMultipleTimes() throws Exception {
      TestConfig t = new TestConfig();
      // Simple case, but also test breaking connection.
      //              c1 / ack c1 <-- (2) recover from here, (4) recover from here again
      //         c2, c3 / ack c2, c3
      // c4 (in flight)/ <-- (1) connection broken, (3) rebroken again
      //  c5 (cached) /
      // c6 (cached) /
      t.init(0);
      t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
      t.serverAck(1).checkClientSentOps().checkClientGotOps(0);
      t.clientDoOps(2).checkClientSentOps(1, 2).checkClientGotOps();
      t.serverAck(3, 2).checkClientSentOps().checkClientGotOps(1, 2);
      t.clientDoOps(1).checkClientSentOps(3).checkClientGotOps();
      t.clientDoOps(1).checkClientSentOps().checkClientGotOps();

      // Break connection
      t.disconnectFromServer();
      t.clientDoOps(1).checkClientSentOps().checkClientGotOps();

      // Reconnect
      t.reconnectToServer().checkClientSentOpen(0, 1, 3);
      // Resend delta containing c1
      t.serverDoOpen(1, 1).checkClientSentOps(1, 2);

      // Break connection again
      t.disconnectFromServer();

      // Reconnect again
      t.reconnectToServer().checkClientSentOpen(0, 1);
      // Resend delta containing c1
      t.serverDoOpen(1, 1).checkClientSentOps(1, 2);
  }

  public void testRecoveryWithCommit() throws Exception {
    TestConfig t = new TestConfig();

    // Reconnect after server commit
    //              c1 / ack c1 <-- server commit, reconnect here.
    //             c2 / ack c2

    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
    t.serverAck(1).checkClientSentOps().checkClientGotOps(0);
    t.clientDoOps(1).checkClientSentOps(1).checkClientGotOps();
    t.serverAck(2).checkClientSentOps().checkClientGotOps(1);
    t.serverCommit(1);

    // Reconnect
    t.reconnectToServer().checkClientSentOpen(1, 2);
  }

  /**
   * Test that the client ignores the timestamp in the echo back op from the server.
   */
  public void testEchobackWithDifferentTimeStamp() throws Exception {
    TestConfig t = new TestConfig();

    // Simple case
    //              c1 / ack c1 <-- recover from here
    //             c2 / ack c2
    // c3 (ack lost) / ack c3 <-- connection broken
    //  c4 (cached) /
    t.init(0);
    t.clientDoOps(1).checkClientSentOps(0).checkClientGotOps();
    t.serverAck(1).checkClientSentOps().checkClientGotOps(0);
    t.clientDoOps(1).checkClientSentOps(1).checkClientGotOps();
    t.serverAck(2).checkClientSentOps().checkClientGotOps(1);
    t.clientDoOps(1).checkClientSentOps(2).checkClientGotOps();
    t.clientDoOps(1).checkClientSentOps().checkClientGotOps();

    // Reconnect
    t.reconnectToServer().checkClientSentOpen(0, 1, 2);
    t.serverDoOpen(2, 3).checkClientSentOps();
    // Using a different timestamp. The client should not care about the timestamp.
    t.serverDoEchoBack(2, 12345L).checkClientSentOps(3);
  }

  /**
   * Test that a double submit from the same user is nullified.
   */
  public void testDoubleSubmit() throws Exception {
    TestConfig t = new TestConfig();
    // Simple double submit. After recovery, server sends client's own op back then ack
    //              c1 /\ c1 <-- connection broken, <-- recover before here <--- got c1 then ack c1
    //    c2 (cached) /
    t.init(0);
    t.clientDoDocOps("blip1").checkClientSentOps(0).checkClientGotOps();
    t.clientDoDocOps("blip2").checkClientSentOps().checkClientGotOps();
    // Reconnect before c1
    t.reconnectToServer().checkClientSentOpen(0);
    // Client resending c1
    t.serverDoOpen(0, 0).checkClientSentOps(0);

    // Should not send c2 on echo back, just nullify c1
    t.serverDoEchoBackDocOp(0, "blip1").checkClientSentOps().checkClientGotOps(0);
    // Send c2, once we get an ack on c1
    t.serverAck(1, 0).checkClientSentOps(1).checkClientGotOps();


    // Slightly more complicated double submit.
    //            c1 / ack <-- (2) recover from here
    //           c2 /\ c2 <-- (1) connection broken <--- (3) got c2 then ack c2 <-- (4) broken again
    // c3 (cached) /
    t.init(0);
    t.clientDoDocOps("blip1").checkClientSentOps(0).checkClientGotOps();
    t.clientDoDocOps("blip2").checkClientSentOps().checkClientGotOps();
    t.serverAck(1).checkClientSentOps(1).checkClientGotOps(0);
    t.clientDoDocOps("blip3").checkClientSentOps().checkClientGotOps();
    // Reconnect after c1
    t.reconnectToServer().checkClientSentOpen(0, 1);
    // Client resending c2
    t.serverDoOpen(1, 1).checkClientSentOps(1);

    // Should not send c2 on echo back, just nullify c2
    t.serverDoEchoBackDocOp(1, "blip2").checkClientSentOps().checkClientGotOps(1);
    // Send c3, once we get an ack on c2
    t.serverAck(2, 0).checkClientSentOps(2).checkClientGotOps();

    // Connection broken again at c2. There should be just one signature sent out in open.
    t.reconnectToServer().checkClientSentOpen(2);
  }
}
