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

package org.waveprotocol.box.server.robots.util;

import com.google.common.base.Strings;
import com.google.wave.api.ApiIdSerializer;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.OperationType;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.robot.RobotName;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.OperationResults;
import org.waveprotocol.box.server.robots.OperationServiceRegistry;
import org.waveprotocol.box.server.robots.RobotWaveletData;
import org.waveprotocol.box.server.robots.operations.OperationService;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.supplement.PrimitiveSupplement;
import org.waveprotocol.wave.model.supplement.SupplementedWave;
import org.waveprotocol.wave.model.supplement.SupplementedWaveImpl;
import org.waveprotocol.wave.model.supplement.SupplementedWaveImpl.DefaultFollow;
import org.waveprotocol.wave.model.supplement.WaveletBasedSupplement;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.util.logging.Log;

import java.util.List;
import java.util.Map.Entry;

/**
 * {@link OperationRequest} utility methods.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class OperationUtil {

  private static final Log LOG = Log.get(OperationUtil.class);

  private OperationUtil() {
  }

  /**
   * Attempts to get a parameter, throws an exception if not found.
   *
   * @param <T> type of class to cast to.
   * @param operation operation to extract property from.
   * @param property the key of the parameter.
   * @return specialized object after being serialized.
   *
   * @throws InvalidRequestException if the property is not found.
   */
  @SuppressWarnings("unchecked")
  public static <T> T getRequiredParameter(OperationRequest operation, ParamsProperty property)
      throws InvalidRequestException {
    Object parameter = operation.getParameter(property);
    Class<T> clazz = (Class<T>) property.clazz();
    if (parameter == null || !clazz.isInstance(parameter)) {
      throw new InvalidRequestException("property " + property + " not found", operation);
    }
    return clazz.cast(parameter);
  }

  /**
   * Attempts to get a parameter, returns the {@code null} if not found.
   *
   * @param <T> type of class to cast to.
   * @param operation operation to extract property from.
   * @param key the key of the parameter.
   * @return specialized object after being serialized, or {@code null} if the
   *         parameter could not be found.
   */
  public static <T> T getOptionalParameter(OperationRequest operation, ParamsProperty key) {
    return OperationUtil.<T> getOptionalParameter(operation, key, null);
  }

  /**
   * Attempts to get a parameter, returns the default if not found.
   *
   * @param <T> type of class to cast to.
   * @param operation operation to extract property from.
   * @param property the key of the parameter.
   * @param defaultValue default value to return if parameter could not be
   *        found.
   * @return specialized object after being serialized, or the default value if
   *         the parameter could not be found.
   */
  @SuppressWarnings("unchecked")
  public static <T> T getOptionalParameter(
      OperationRequest operation, ParamsProperty property, T defaultValue) {
    Object parameter = operation.getParameter(property);
    Class<T> clazz = (Class<T>) property.clazz();
    if (parameter != null && clazz.isInstance(parameter)) {
      return clazz.cast(parameter);
    }
    return defaultValue;
  }

  /**
   * Determines the protocol version of a given operation bundle by inspecting
   * the first operation in the bundle. If it is a {@code robot.notify}
   * operation, and contains {@code protocolVersion} parameter, then this method
   * will return the value of that parameter. Otherwise, this method will return
   * the default version.
   *
   * @param operations the {@link OperationRequest}s to inspect.
   * @return the wire protocol version of the given operation bundle.
   */
  public static ProtocolVersion getProtocolVersion(List<OperationRequest> operations) {
    if (operations.size() == 0) {
      return ProtocolVersion.DEFAULT;
    }

    OperationRequest firstOperation = operations.get(0);
    if (firstOperation.getMethod().equals(OperationType.ROBOT_NOTIFY.method())) {
      String versionString = (String) firstOperation.getParameter(ParamsProperty.PROTOCOL_VERSION);
      if (versionString != null) {
        return ProtocolVersion.fromVersionString(versionString);
      }
    }
    return ProtocolVersion.DEFAULT;
  }

  /**
   * @return the type of operation present in the request
   */
  public static OperationType getOperationType(OperationRequest operation) {
    String methodName = operation.getMethod();

    // TODO(ljvderijk): This might be removed after the deserialization is fixed
    if (methodName.startsWith("wave.")) {
      methodName = methodName.replaceFirst("^wave[.]", "");
    }
    return OperationType.fromMethodName(methodName);
  }

  /**
   * Executes an {@link OperationRequest}. If the operation throws an
   * {@link InvalidRequestException} this exception will be used to construct an
   * error response in the {@link OperationContext}.
   *
   * @param operation the operation to be executed. If the operation contains
   *        {@link ParamsProperty.PROXYING_FOR} - then it will be taken in
   *        account.
   * @param operationRegistry the registry containing the operations that can be
   *        performed.
   * @param context the context in which the operation is to be executed.
   * @param author the author of the operation.
   */
  public static void executeOperation(OperationRequest operation,
      OperationServiceRegistry operationRegistry, OperationContext context, ParticipantId author) {
    try {
      OperationService service =
          operationRegistry.getServiceFor(OperationUtil.getOperationType(operation));
      ParticipantId proxyParticipant = OperationUtil.computeParticipant(operation, author);
      service.execute(operation, context, proxyParticipant);
    } catch (InvalidRequestException e) {
      LOG.warning("Operation " + operation + " failed to execute", e);
      context.constructErrorResponse(operation, e.getMessage());
    }
  }

  /**
   * Submits all deltas to the wavelet provider that are generated by the open
   * wavelets in the {@link OperationResults}.
   *
   * @param results the results of performing robot operations.
   * @param waveletProvider wavelet provider used to send the deltas to.
   * @param requestListener callback for deltas that are submitted to the
   *        wavelet provider.
   */
  public static void submitDeltas(OperationResults results, WaveletProvider waveletProvider,
      SubmitRequestListener requestListener) {
    for (Entry<WaveletName, RobotWaveletData> entry : results.getOpenWavelets().entrySet()) {
      WaveletName waveletName = entry.getKey();
      RobotWaveletData w = entry.getValue();
      for (WaveletDelta delta : w.getDeltas()) {
        ProtocolWaveletDelta protocolDelta = CoreWaveletOperationSerializer.serialize(delta);
        waveletProvider.submitRequest(waveletName, protocolDelta, requestListener);
      }
    }
  }


  /**
   * Appends proxyFor to the participant address.
   *
   * @param proxyFor the proxyFor.
   * @param participant the participant to apply the proxyFor.
   * @return new participant instance in the format
   *         somebody+proxyFor@example.com. If proxyFor is null then just
   *         returns unmodified participant.
   * @throws InvalidParticipantAddress if participant address and/or proxy are
   *         invalid.
   */
  public static ParticipantId toProxyParticipant(ParticipantId participant, String proxyFor)
      throws InvalidParticipantAddress {
    if (!Strings.isNullOrEmpty(proxyFor)) {
      RobotName robotName = RobotName.fromAddress(participant.getAddress());
      robotName.setProxyFor(proxyFor);
      String robotAddress = robotName.toParticipantAddress();
      if (!RobotName.isWellFormedAddress(robotAddress)) {
        throw new InvalidParticipantAddress(robotAddress,
            "is not a valid robot name, the proxy is likely to be wrong");
      }
      return ParticipantId.of(robotName.toParticipantAddress());
    } else {
      return participant;
    }
  }

  /**
   * Computes participant ID using optional {@link ParamsProperty.PROXYING_FOR}
   * parameter.
   *
   * @param operation the operation to be executed.
   * @param participant the base participant id.
   * @return new participant instance in the format
   *         somebody+proxyFor@example.com. If proxyFor is null then just
   *         returns unmodified participant.
   * @throws InvalidRequestException if participant address and/or proxyFor are
   *         invalid.
   */
  public static ParticipantId computeParticipant(OperationRequest operation,
      ParticipantId participant) throws InvalidRequestException {
    String proxyAddress =
        OperationUtil.getOptionalParameter(operation, ParamsProperty.PROXYING_FOR);
    try {
      return toProxyParticipant(participant, proxyAddress);
    } catch (InvalidParticipantAddress e) {
      throw new InvalidRequestException(
          participant.getAddress()
          + (proxyAddress != null ? "+" + proxyAddress : ""
              + " is not a valid participant address"), operation);
    }
  }

  /**
   * Builds the supplement model for a wave.
   *
   * @param operation the operation.
   * @param context the operation context.
   * @param participant the viewer.
   * @return the wave supplement.
   * @throws InvalidRequestException if the wave id provided in the operation is
   *         invalid.
   */
  public static SupplementedWave buildSupplement(OperationRequest operation,
      OperationContext context, ParticipantId participant) throws InvalidRequestException {
    OpBasedWavelet wavelet = context.openWavelet(operation, participant);
    ConversationView conversationView = context.getConversationUtil().buildConversation(wavelet);

    // TODO (Yuri Z.) Find a way to obtain an instance of IdGenerator and use it
    // to create udwId.
    WaveletId udwId = IdUtil.buildUserDataWaveletId(participant);
    String waveIdStr = OperationUtil.getRequiredParameter(operation, ParamsProperty.WAVE_ID);
    WaveId waveId = null;
    try {
      waveId = ApiIdSerializer.instance().deserialiseWaveId(waveIdStr);
    } catch (InvalidIdException e) {
      throw new InvalidRequestException("Invalid WAVE_ID parameter: " + waveIdStr, operation, e);
    }
    OpBasedWavelet udw = context.openWavelet(waveId, udwId, participant);

    PrimitiveSupplement udwState = WaveletBasedSupplement.create(udw);

    SupplementedWave supplement =
        SupplementedWaveImpl.create(udwState, conversationView, participant, DefaultFollow.ALWAYS);
    return supplement;
  }

}
