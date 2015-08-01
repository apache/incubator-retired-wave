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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.data.converter.EventDataConverterManager;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.robot.CapabilityFetchException;
import com.google.wave.api.robot.RobotName;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.RobotCapabilities;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * Represents a Robot in the passive API. Is responsible for providing a filter
 * for events it is interested in. Is also able of sending events to robots and
 * executing the operations it receives. It submits the delta back to the
 * {@link RobotsGateway}.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class Robot implements Runnable {

  private static final Log LOG = Log.get(Robot.class);
  /** Appended to the robot url which forms the endpoint for sending rpc calls */
  public static final String RPC_URL = "/_wave/robot/jsonrpc";
  /**
   * Appended to the robot url which forms the endpoint for getting the
   * capabilities
   */
  public static final String CAPABILITIES_URL = "/_wave/capabilities.xml";

  private final RobotName robotName;
  // This is not final because it needs to be updated when the capabilities
  // change.
  // TODO(ljvderijk): Keep up to date with other updates to account?
  private RobotAccountData account;
  private final RobotsGateway gateway;
  private final RobotConnector connector;
  private final EventDataConverterManager converterManager;
  /**
   * This map acts as a queue of wavelets to process events for. The methods
   * that add, updated and remove wavelets synchronize on this object to ensure
   * consistency in face of concurrent changes.
   */
  private final ListMultimap<WaveletName, WaveletAndDeltas> waveletAndDeltasMap =
      LinkedListMultimap.<WaveletName, WaveletAndDeltas> create();
  private final EventGenerator eventGenerator;
  private final RobotOperationApplicator operationApplicator;

  /**
   * Constructs a new Robot which is characterized by its {@link RobotName}.
   *
   * @param robotName the name of the {@link Robot}.
   * @param account the {@link RobotAccountData} belonging to this
   *        {@link Robot}.
   * @param gateway the gateway this robot belongs to.
   * @param connector the {@link RobotConnector} to make connections to
   *        {@link Robot}s.
   * @param converterManager used to convert to Robot API objects.
   * @param waveletProvider used to access wavelets and submit deltas.
   * @param eventGenerator used to generate events
   * @param operationApplicator used to apply the robot operations returned by a
   *        robot.
   */
  Robot(RobotName robotName, RobotAccountData account, RobotsGateway gateway,
      RobotConnector connector, EventDataConverterManager converterManager,
      WaveletProvider waveletProvider, EventGenerator eventGenerator,
      RobotOperationApplicator operationApplicator) {
    Preconditions.checkArgument(account.isVerified(), "Account must be verified");
    this.robotName = robotName;
    this.gateway = gateway;
    this.connector = connector;
    this.converterManager = converterManager;
    this.eventGenerator = eventGenerator;
    this.operationApplicator = operationApplicator;

    setAccount(account);
  }

  /**
   * Sets the account for this Robot. The address of the account must match the
   * address in the {@link RobotName}.
   *
   * @param account the account to set.
   */
  void setAccount(RobotAccountData account) {
    Preconditions.checkArgument(robotName.toEmailAddress().equals(account.getId().getAddress()),
        String.format("The given RobotAccountData doesn't match the RobotName. %s != %s",
            account.getId(), robotName.toEmailAddress()));
    this.account = account;
  }

  /**
   * Returns the name of this robot.
   */
  RobotName getRobotName() {
    return robotName;
  }

  /**
   * The {@link RobotAccountData} of this robot.
   */
  RobotAccountData getAccount() {
    return account;
  }

  /**
   * Processes a wavelet update for this {@link Robot}.
   *
   * <p>
   * The robot keeps an internal queue of wavelets that are to be processed when
   * run() is called. A new entry in the queue is made on two occasions. First
   * if the robot has nothing enqueued for the given wavelet, secondly if the
   * robot does have an update enqueued for the given wavelet but the deltas
   * that are given are not contiguous with the current data.
   *
   * <p>
   * This method synchronizes on the queue because we might be appending deltas
   * while dequeueWavelet() is being called.
   *
   * @param wavelet the wavelet this update is taking place on.
   * @param deltas the deltas that have been applied to the given wavelet.
   * @throws OperationException if an update for a new wavelet could not be
   *         processed.
   */
  void waveletUpdate(ReadableWaveletData wavelet, DeltaSequence deltas)
      throws OperationException {
    WaveletName waveletName = WaveletDataUtil.waveletNameOf(wavelet);

    synchronized (waveletAndDeltasMap) {
      // This returns a view which we can append new delta collections to.
      List<WaveletAndDeltas> wavelets = waveletAndDeltasMap.get(waveletName);

      if (wavelets.isEmpty()) {
        WaveletAndDeltas waveletAndDeltas = WaveletAndDeltas.create(wavelet, deltas);
        wavelets.add(waveletAndDeltas);
      } else {
        WaveletAndDeltas waveletAndDeltas = wavelets.get(wavelets.size() - 1);
        if (waveletAndDeltas.areContiguousToCurrentVersion(deltas)) {
          waveletAndDeltas.appendDeltas(wavelet, deltas);
        } else {
          // We are missing deltas, create a new collection.
          waveletAndDeltas = WaveletAndDeltas.create(wavelet, deltas);
          wavelets.add(waveletAndDeltas);
        }
      }
    }
  }

  /**
   * Dequeues a wavelet for this {@link Robot}.
   *
   * <p>
   * This method synchronizes on the queue because deltas might be added in
   * waveletUpdate().
   *
   * @return the next {@link WaveletAndDeltas} in the queue, null if there is
   *         none.
   */
  @VisibleForTesting
  WaveletAndDeltas dequeueWavelet() {
    synchronized (waveletAndDeltasMap) {
      Iterator<Entry<WaveletName, WaveletAndDeltas>> iterator =
          waveletAndDeltasMap.entries().iterator();
      if (!iterator.hasNext()) {
        return null;
      }
      WaveletAndDeltas wavelet = iterator.next().getValue();
      iterator.remove();
      return wavelet;
    }
  }

  /**
   * Runs this {@link Robot} by checking its queue for a new wavelet and then
   * processing this wavelet. In the end the {@link Robot} will check whether it
   * needs to requeue itself in the RobotGateway or that no further actions need
   * to be taken.
   */
  @Override
  public void run() {
    try {
      LOG.fine(robotName + " called for processing");

      WaveletAndDeltas wavelet = dequeueWavelet();
      if (wavelet == null) {
        gateway.doneRunning(this);
        return;
      }
      process(wavelet);
    } catch (RuntimeException e) {
      LOG.severe("Unexpected error occurred when robot " + robotName + " was called", e);
    }

    // Requeue since we either had an exception or we processed a wavelet.
    gateway.doneRunning(this);
    gateway.ensureScheduled(this);
  }

  /**
   * Processes a single {@link WaveletAndDeltas} by generating events that a
   * {@link Robot} is subscribed to. These events are then sent off to the robot
   * using the {@link RobotConnector} passed during construction. The operations
   * returned by the robot are then processed by the
   * {@link RobotOperationApplicator}.
   *
   * @param wavelet the {@link WaveletAndDeltas} to process.
   */
  private void process(WaveletAndDeltas wavelet) {
    if (account.getCapabilities() == null) {
      try {
        LOG.info(robotName + ": Initializing capabilities");
        gateway.updateRobotAccount(this);
      } catch (CapabilityFetchException e) {
        ReadableWaveletData snapshot = wavelet.getSnapshotAfterDeltas();
        LOG.info(
            "Couldn't initialize the capabilities of robot(" + robotName
                + "), dropping its wavelet(" + WaveletDataUtil.waveletNameOf(snapshot)
                + ") at version " + wavelet.getVersionAfterDeltas(), e);
        return;
      } catch (PersistenceException e) {
        ReadableWaveletData snapshot = wavelet.getSnapshotAfterDeltas();
        LOG.info(
            "Couldn't initialize the capabilities of robot(" + robotName
                + "), dropping its wavelet(" + WaveletDataUtil.waveletNameOf(snapshot)
                + ") at version " + wavelet.getVersionAfterDeltas(), e);
        return;
      }
    }

    RobotCapabilities capabilities = account.getCapabilities();
    EventMessageBundle messages =
        eventGenerator.generateEvents(wavelet, capabilities.getCapabilitiesMap(),
            converterManager.getEventDataConverter(capabilities.getProtocolVersion()));

    if (messages.getEvents().isEmpty()) {
      // No events were generated, we are done
      LOG.info(robotName + ": no events were generated");
      return;
    }

    LOG.info(robotName + ": sending events");
    List<OperationRequest> response =
        connector.sendMessageBundle(messages, this, capabilities.getProtocolVersion());
    LOG.info(robotName + ": received operations");

    operationApplicator.applyOperations(
        response, wavelet.getSnapshotAfterDeltas(), wavelet.getVersionAfterDeltas(), account);
  }
}
