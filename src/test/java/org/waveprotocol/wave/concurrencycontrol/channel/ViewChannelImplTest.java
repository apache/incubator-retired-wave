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

package org.waveprotocol.wave.concurrencycontrol.channel;


import junit.framework.TestCase;

import org.waveprotocol.wave.common.logging.AbstractLogger;
import org.waveprotocol.wave.common.logging.PrintLogger;
import org.waveprotocol.wave.concurrencycontrol.channel.ViewChannel.Listener;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.concurrencycontrol.testing.FakeWaveViewServiceUpdate;
import org.waveprotocol.wave.concurrencycontrol.testing.MockWaveViewService;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdFilters;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Unit test for ViewChannelImpl.
 *
 * @author zdwang@google.com (David Wang)
 */

public class ViewChannelImplTest extends TestCase {

  /**
   * This is mock class to test that calls back from ViewChannel are as expected.
   */
  private static class MockViewChannelListener implements Listener {
    public enum MethodCall {
      ON_CONNECTED,
      ON_CLOSED,
      ON_EXCEPTION,
      ON_OPEN_FINISHED,
      ON_UPDATE,
      ON_SNAPSHOT
    }

    public class MethodCallContext {
      final MethodCall method;
      final WaveletId waveletId;
      final ObservableWaveletData snapshot;
      final List<TransformedWaveletDelta> deltas;
      final HashedVersion lastCommittedVersion;
      final HashedVersion currentSignedVersion;

      public MethodCallContext(MethodCall method) {
        this.method = method;
        this.waveletId = null;
        this.snapshot = null;
        this.deltas = null;
        this.lastCommittedVersion = null;
        this.currentSignedVersion = null;
      }

      public MethodCallContext(MethodCall method, WaveletId waveletId,
          ObservableWaveletData snapshot,
          HashedVersion lastCommittedVersion,
          HashedVersion currentSignedVersion) {
        this.method = method;
        this.waveletId = waveletId;
        this.snapshot = snapshot;
        this.deltas = null;
        this.lastCommittedVersion = lastCommittedVersion;
        this.currentSignedVersion = currentSignedVersion;
      }

      public MethodCallContext(MethodCall method, WaveletId waveletId,
          List<TransformedWaveletDelta> deltas,
          HashedVersion lastCommittedVersion,
          HashedVersion currentSignedVersion) {
        this.method = method;
        this.waveletId = waveletId;
        this.snapshot = null;
        this.deltas = deltas;
        this.lastCommittedVersion = lastCommittedVersion;
        this.currentSignedVersion = currentSignedVersion;
      }

      public MethodCall method() {
        return method;
      }
    }

    private final ArrayList<MethodCallContext> methodCalls = new ArrayList<MethodCallContext>();

    @Override
    public void onConnected() {
      methodCalls.add(new MethodCallContext(MethodCall.ON_CONNECTED));
    }

    @Override
    public void onClosed() {
      methodCalls.add(new MethodCallContext(MethodCall.ON_CLOSED));
    }

    @Override
    public void onException(ChannelException e) {
      methodCalls.add(new MethodCallContext(MethodCall.ON_EXCEPTION));
    }

    @Override
    public void onOpenFinished() {
      methodCalls.add(new MethodCallContext(MethodCall.ON_OPEN_FINISHED));
    }

    @Override
    public void onSnapshot(WaveletId waveletId, ObservableWaveletData wavelet,
        HashedVersion lastCommittedVersion, HashedVersion currentSignedVersion) {
      methodCalls.add(new MethodCallContext(MethodCall.ON_SNAPSHOT,
          waveletId, wavelet, lastCommittedVersion, currentSignedVersion));
    }

    @Override
    public void onUpdate(WaveletId waveletId, List<TransformedWaveletDelta> deltas,
        HashedVersion lastCommittedVersion, HashedVersion currentSignedVersion) {
      methodCalls.add(new MethodCallContext(MethodCall.ON_UPDATE,
          waveletId, deltas, lastCommittedVersion, currentSignedVersion));
    }

    public void expectedCall(MethodCall method) {
      assertEquals(method, methodCalls.get(0).method);
      methodCalls.remove(0);
    }

    public void expectedNothing() {
      assertEquals(0, methodCalls.size());
    }

    /**
     * We don't test for container message as it's not important.
     */
    public void expectedCall(MethodCall method, WaveletId waveletId) {
      assertFalse(methodCalls.isEmpty());
      MethodCallContext context = methodCalls.get(0);
      assertEquals(method, context.method);
      assertEquals(waveletId, context.waveletId);

      methodCalls.remove(0);
    }

    public void clear() {
      methodCalls.clear();
    }
  }

  /**
   * This is mock class to test that calls back from ViewChannel are as expected.
   */
  private static class MockSubmitListener implements SubmitCallback {
    public enum MethodCall {
      ON_SUCCESS,
      ON_FAILURE
    }

    public class MethodCallContext {
      final MethodCall method;
      final int opsApplied;
      final HashedVersion version;
      final String error;

      public MethodCallContext(MethodCall method, int opsApplied,
          HashedVersion version) {
        this(method, null, opsApplied, version);
      }

      public MethodCallContext(MethodCall method, String reason, HashedVersion version) {
        this(method, reason, 0, version);
      }

      public MethodCallContext(MethodCall method, String reason) {
        this(method, reason, 0, HashedVersion.unsigned(0));
      }

      public MethodCallContext(MethodCall method, String reason, int opsApplied,
          HashedVersion version) {
        this.method = method;
        this.error = reason;
        this.version = version;
        this.opsApplied = opsApplied;
      }
    }

    ArrayList<MethodCallContext> methodCalls = new ArrayList<MethodCallContext>();

    @Override
    public void onSuccess(int opsApplied, HashedVersion version,
        ResponseCode responseCode, String errorMessage) {
      methodCalls.add(new MethodCallContext(
          MethodCall.ON_SUCCESS, errorMessage, opsApplied, version));
    }

    @Override
    public void onFailure(String reason) {
      methodCalls.add(new MethodCallContext(MethodCall.ON_FAILURE, reason));

    }

    public void expectedCall(MethodCall method, String error) {
      MethodCallContext context = methodCalls.get(0);
      assertEquals(method, context.method);
      assertEquals(error, context.error);

      methodCalls.remove(0);
    }

    public void expectedCall(MethodCall method, int opsApplied, HashedVersion version) {
      MethodCallContext context = methodCalls.get(0);
      assertEquals(method, context.method);
      assertEquals(opsApplied, context.opsApplied);
      assertEquals(version, context.version);

      methodCalls.remove(0);
    }
  }

  /**
   * Wavelet id to use in the tests.
   */
  private static final WaveletId WAVELET_ID = WaveletId.of("example.com", "waveletId_1");

  /**
   * Channel Id to be used in the tests.
   */
  private static final String CHANNEL_ID = "channelId_1";

  private static final AbstractLogger logger = new PrintLogger();

  //
  // Fields used in most or all tests.
  //

  private ViewChannelImpl channel;
  private MockViewChannelListener viewOpenListener;
  private MockWaveViewService waveViewService;

  @Override
  protected void setUp() {
    WaveId waveId = WaveId.of("example.com", "waveid");
    ViewChannelImpl.setMaxViewChannelsPerWave(Integer.MAX_VALUE);
    waveViewService = new MockWaveViewService();
    viewOpenListener = new MockViewChannelListener();
    channel = new ViewChannelImpl(waveId, waveViewService, logger);
  }

  /**
   * Opens the channel from the client side only.
   */
  private void halfOpen() {
    Map<WaveletId, List<HashedVersion>> knownWavelets = Collections.emptyMap();
    channel.open(viewOpenListener, IdFilters.ALL_IDS, knownWavelets);
  }

  /**
   * Simulates the server responding with the channel id.
   */
  private void respondWithChannelId() {
    waveViewService.lastOpen().callback.onUpdate(
        new FakeWaveViewServiceUpdate().setChannelId(CHANNEL_ID));
  }

  /**
   * Simulates the server responding with the open-finished marker.
   */
  private void respondWithMarker(boolean waveEmpty) {
    waveViewService.lastOpen().callback.onUpdate(
        new FakeWaveViewServiceUpdate().setMarker(waveEmpty));
  }

  /**
   * Simulates the server sending a streaming update.
   *
   * @param waveletId wavelet to update
   */
  private void respondWithEmptyUpdate(WaveletId waveletId) {
    waveViewService.lastOpen().callback.onUpdate(new FakeWaveViewServiceUpdate()
        .setWaveletId(waveletId)
        .setLastCommittedVersion(HashedVersion.unsigned(0))
        .addDelta(new TransformedWaveletDelta(null, HashedVersion.unsigned(0), 0L,
            Arrays.<WaveletOperation> asList())));
  }

  private void respondToSubmit(HashedVersion version, int opsApplied, String error,
      ResponseCode response) {
    waveViewService.lastSubmit().callback.onSuccess(version, opsApplied, error, response);
  }

  private void respondToSubmitWithFailure() {
    waveViewService.lastSubmit().callback.onFailure("WAVE_SERVER_ERROR");
  }

  /**
   * Opens the channel, and simulates the server responding with a channel and
   * a marker.
   */
  private void open() {
    halfOpen();
    respondWithChannelId();
    respondWithMarker(false);
  }

  private void close() {
    channel.close();
  }

  private void terminateOpenRpcWithSuccess() {
    waveViewService.lastOpen().callback.onSuccess(null);
  }

  private void terminateOpenRpcWithError() {
    waveViewService.lastOpen().callback.onSuccess("Server error for testing");
  }

  private void terminateOpenRpcWithFailure(String status) {
    waveViewService.lastOpen().callback.onFailure(status);
  }

  private static WaveletDelta emptyDelta() {
    return new WaveletDelta(null, null, Arrays.<WaveletOperation> asList());
  }

  /**
   * Test that when everything is ok, we can connect, submit, create wavelet and disconnect.
   * This is not supposed to be a thorough test.
   */
  public void testSunnyDayScenario() {
    open();

    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_CONNECTED);
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_OPEN_FINISHED);

    // pretend an update with update wavelet. Note we don't add any data in the wavelet
    // because it's not really relevant for the test.
    respondWithEmptyUpdate(WAVELET_ID);
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_UPDATE, WAVELET_ID);

    // Submit a delta and check that we have the right channel id remembered.
    MockSubmitListener submitListener = new MockSubmitListener();
    channel.submitDelta(WAVELET_ID, emptyDelta(), submitListener);
    assertEquals(1, waveViewService.submits.size());
    assertEquals(CHANNEL_ID, waveViewService.lastSubmit().channelId);

    // Return a success message on the submitted delta
    byte[] hash = new byte[] {1, 2, 3, 4};
    respondToSubmit(HashedVersion.of(2, hash), 1, null, ResponseCode.OK);
    submitListener.expectedCall(MockSubmitListener.MethodCall.ON_SUCCESS, 1,
        HashedVersion.of(2, hash));

    // Check disconnect
    channel.close();
    assertEquals(1, waveViewService.closes.size());
    waveViewService.lastClose().callback.onSuccess();
  }

  /**
   * Tests that {@link ViewChannelImpl#open(Listener, IdFilter, Map)}
   * synchronously calls the ViewOpen rpc on its wave service.
   */
  public void testOpenIssuesViewOpenRpc() {
    open();
    assertEquals(1, waveViewService.opens.size());
  }

  /**
   * Tests that a channel fails if it does not receive a channel id in the first
   * message.
   */
  public void testInitialUpdateWithoutAChannelIdFails() {
    halfOpen();

    // Receive an update with no channel id.
    waveViewService.lastOpen().callback.onUpdate(new FakeWaveViewServiceUpdate());
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_EXCEPTION);
  }

  /**
   * Tests that a channel fails if it receives a message before it is opened.
   */
  public void testMessageBeforeOpenThrowsException() {
    try {
      channel.onUpdate(new FakeWaveViewServiceUpdate());
      fail("Should not be able to receive update without open call");
    } catch (IllegalStateException expected) {
      // Expected.
    }
  }

  public void testSuccessBeforeOpenThrowsException() {
    try {
      channel.onSuccess("for testing");
      fail("Should not be able to receive onSuccess without open call");
    } catch (IllegalStateException expected) {
      // Expected.
    }
  }

  public void testFailureBeforeOpenThrowsException() {
    try {
      channel.onFailure("for testing");
      fail("Should not be able to receive onFailure without open call");
    } catch (IllegalStateException expected) {
      // Expected.
    }
  }

  /**
   * Tests a channel fails if it receives success before channel id.
   */
  public void testSuccessBeforeChannelIdFails() {
    halfOpen();
    channel.onSuccess("for testing");
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_EXCEPTION);
  }

  /**
   * Tests a channel closes if it receives failure before channel id.
   */
  public void testFailureBeforeChannelIdClosesChannel() {
    halfOpen();
    channel.onFailure("for testing");
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_CLOSED);
  }

  /**
   * Tests that an update with the end-marker triggers the open-finished
   * callback.
   */
  public void testMarkerTriggersOpenFinished() {
    halfOpen();
    respondWithChannelId();
    viewOpenListener.clear();
    respondWithMarker(false);
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_OPEN_FINISHED);
  }

  /**
   * Tests that an update with the end-marker triggers the open-finished
   * callback.
   */
  public void testChannelIdTriggersConnectCallback() {
    halfOpen();
    respondWithChannelId();
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_CONNECTED);
  }

  /**
   * Tests that updates that arrive before the end-marker are passed on as
   * updates, and that when the end-marker eventually arrives, open-finished
   * is triggered then.
   */
  public void testUpdatesBeforeOpenFinishedStillTriggersOpenFinished() {
    halfOpen();
    respondWithChannelId();

    viewOpenListener.clear();
    respondWithEmptyUpdate(WAVELET_ID);
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_UPDATE, WAVELET_ID);

    respondWithMarker(false);
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_OPEN_FINISHED);
  }

  /**
   * Tests that closing the channel after a full open issues a ViewClose rpc.
   */
  public void testCloseAfterChannelIdCallsCloseRpc() {
    open();
    close();
    assertEquals(1, waveViewService.closes.size());
    waveViewService.lastClose().callback.onSuccess();
  }

  /**
   * Tests that closing the channel before the server has responded with a
   * channel id does not issue a ViewClose rpc.
   */
  public void testCloseWithoutChannelIdDoesNotCallCloseRpc() {
    halfOpen();
    close();
    assertEquals(0, waveViewService.closes.size());
  }

  /**
   * Tests that closing the channel before the server has responded with a
   * channel id causes a ViewClose rpc to be sent as soon as a channel id
   * arrives later.
   */
  public void testCloseWithoutChannelIdCallsCloseRpcIfChannelIdArrives() {
    halfOpen();
    close();
    assertEquals(0, waveViewService.closes.size());
    respondWithChannelId();
    // Have got channel id, so we should now have issued a close.
    assertEquals(1, waveViewService.closes.size());
    waveViewService.lastClose().callback.onSuccess();
  }

  /**
   * Tests that closing the channel prevents updates that arrive later from
   * being passed to the channel listener.
   */
  public void testCloseMasksFutureUpdatesFromOpenListener() {
    open();
    close();
    viewOpenListener.clear();
    respondWithEmptyUpdate(WAVELET_ID);
    viewOpenListener.expectedNothing();
  }

  public void testCloseTriggersCloseCallback() {
    open();
    viewOpenListener.clear();
    close();
    // The underlying service is expected to have the following behaviour.
    terminateOpenRpcWithSuccess();
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_CLOSED);
  }

  public void testOpenRpcTerminationAfterUpdatesAndCloseTriggersCloseCallback() {
    open();
    respondWithEmptyUpdate(WAVELET_ID);
    viewOpenListener.clear();
    close();

    // The service should cause the open rpc to terminate successfully.
    terminateOpenRpcWithSuccess();
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_CLOSED);
  }

  public void testOpenRpcTerminationWithoutCloseTriggersFailureAndClose() {
    open();
    viewOpenListener.clear();
    terminateOpenRpcWithSuccess();
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_CLOSED);
  }

  public void testOpenRpcOnFailureAndThenTerminationWithoutCloseTriggersOneFailureAndClose() {
    open();
    viewOpenListener.clear();
    terminateOpenRpcWithFailure("WAVE_SERVER_ERROR");
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_CLOSED);
    terminateOpenRpcWithError();
    viewOpenListener.expectedNothing();
  }

  public void testMultipleClose() {
    open();
    viewOpenListener.clear();
    close();
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_CLOSED);
    close();
    viewOpenListener.expectedNothing();
  }

  public void testCloseWithoutOpen() {
    close();
    viewOpenListener.expectedNothing();
  }

  public void testOpenAfterOpenThrowsException() {
    open();
    try {
      channel.open(null, null, null);
      fail("Should not be able to open again after open is called.");
    } catch (RuntimeException ex) {
      // Expected error.
    }
  }

  public void testSubmitDeltaWithErrorMessage() {
    open();

    // submit a delta
    MockSubmitListener submitListener = new MockSubmitListener();
    channel.submitDelta(WAVELET_ID, emptyDelta(), submitListener);

    // Success with error the submit delta.
    String errorMessage = "Bad things happened on the server";
    respondToSubmit(HashedVersion.of(2, new byte[] {1, 2, 3, 4}), 1, errorMessage, ResponseCode.OK);
    submitListener.expectedCall(MockSubmitListener.MethodCall.ON_SUCCESS, errorMessage);
  }

  /**
   * Tests that a failure of a ViewSubmit rpc causes the channel to call the
   * failure callback registered on delta submission.
   */
  public void testDeltaSubmissionFailureCallsSubmissionFailureCallback() {
    open();

    // Submit a delta.
    MockSubmitListener listener = new MockSubmitListener();
    channel.submitDelta(WAVELET_ID, emptyDelta(), listener);

    // Fail the submit delta, and expect that the submit listened got the error.
    respondToSubmitWithFailure();
    listener.expectedCall(MockSubmitListener.MethodCall.ON_FAILURE, "WAVE_SERVER_ERROR");
  }

  public void testSubmitDeltaOnClosedChannelThrowsIllegalStateException() {
    open();
    close();

    // submit a delta should fail
    try {
      channel.submitDelta(WAVELET_ID, emptyDelta(), new MockSubmitListener());
      fail("Should not be able to submit on a closed channel");
    } catch (IllegalStateException ex) {
      // expect exception
    }
  }

  public void testOpenAfterCloseThrowsIllegalStateException() {
    open();
    close();

    // Opening the channel again should fail.
    try {
      halfOpen();
      fail("Should not be able to open a closed channel");
    } catch (IllegalStateException ex) {
      // expect exception
    }
  }

  public void testFailedOpenCallsListenerFailure() {
    halfOpen();
    // fail the open
    terminateOpenRpcWithFailure("WAVE_SERVER_ERROR");
    viewOpenListener.expectedCall(MockViewChannelListener.MethodCall.ON_CLOSED);
  }

  public void testCannotCreateTooManyChannels() {
    ViewChannelImpl.setMaxViewChannelsPerWave(4);
    WaveId waveId = WaveId.of("example.com", "toomanywaveid");
    for  (int i = 0; i < 4; i++) {
      channel = new ViewChannelImpl(waveId, waveViewService, logger);
    }
    try {
      channel = new ViewChannelImpl(waveId, waveViewService, logger);
      fail("Should not be allowed to create any more view channels");
    } catch (IllegalStateException ex) {
      // expected
    }
  }

  public void testClosingOneChannelMakesRoomForAnother() {
    WaveId waveId = WaveId.of("example.com", "makeroomwaveid");
    ViewChannelImpl.setMaxViewChannelsPerWave(4);
    for (int i = 0; i < 4; i++) {
      channel = new ViewChannelImpl(waveId, waveViewService, logger);
    }
    channel.close(); // Close the last channel, making room for another.
    channel = new ViewChannelImpl(waveId, waveViewService, logger);
  }
}
