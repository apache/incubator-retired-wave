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

package com.google.wave.api;

import com.google.wave.api.JsonRpcConstant.ParamsProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that represents an operation request.
 *
 * @author mprasetya@google.com (Marcel Prasetya)
 */
public class OperationRequest {

  /**
   * A helper inner class that represents an operation parameter. Useful for
   * constructing the operation.
   *
   * @author mprasetya@google.com (Marcel Prasetya)
   */
  public static class Parameter {

    private final ParamsProperty key;
    private final Object value;

    /**
     * Factory method.
     *
     * @param key the key of the parameter
     * @param value the value of the parameter
     * @return a parameter with the specified key and value
     */
    public static Parameter of(ParamsProperty key, Object value) {
      return new Parameter(key, value);
    }

    /**
     * Constructor.
     *
     * @param key the key of the parameter.
     * @param value the value of the parameter.
     */
    private Parameter(ParamsProperty key, Object value) {
      this.key = key;
      this.value = value;
    }

    /**
     * Returns the key of the parameter.
     *
     * @return the key of the parameter.
     */
    public ParamsProperty getKey() {
      return key;
    }

    /**
     * Returns the value of the parameter.
     *
     * @return the value of the parameter.
     */
    public Object getValue() {
      return value;
    }
  }

  private final String method;
  private final String id;
  private final Map<ParamsProperty, Object> parameters;

  /**
   * Constructor.
   *
   * @param method the method or operation to be executed.
   *     See {@link OperationType}.
   * @param id the id of the request.
   * @param waveId the wave id to apply this operation to.
   * @param waveletId the wavelet id to apply this operation to.
   * @param blipId the blip id to apply this operation to.
   * @param params additional parameters for this operation. See
   *     {@link ParamsProperty}.
   */
  public OperationRequest(String method, String id, String waveId, String waveletId, String blipId,
      Parameter... params) {
    this.method = method;
    this.id = id;
    this.parameters = new HashMap<ParamsProperty, Object>(params.length + 3);
    setWaveId(waveId);
    setWaveletId(waveletId);
    setBlipId(blipId);
    for (Parameter parameter : params) {
      this.parameters.put(parameter.getKey(), parameter.getValue());
    }
  }

  /**
   * Constructor without {@code blipId}.
   *
   * @param method the method or operation to be executed.
   *     See {@link OperationType}.
   * @param id the id of the request.
   * @param waveId the wave id to apply this operation to.
   * @param waveletId the wavelet id to apply this operation to.
   * @param parameters additional parameters for this operation. See
   *     {@link ParamsProperty}.
   */
  public OperationRequest(String method, String id, String waveId, String waveletId,
      Parameter... parameters) {
    this(method, id, waveId, waveletId, null, parameters);
  }

  /**
   * Constructor without {@code waveId}, {@code waveletId}, and {@code blipId}.
   *
   * @param method the method or operation to be executed.
   *     See {@link OperationType}.
   * @param id the id of the request.
   * @param parameters additional parameters for this operation. See
   *     {@link ParamsProperty}.
   */
  public OperationRequest(String method, String id, Parameter... parameters) {
    this(method, id, null, null, null, parameters);
  }

  /**
   * Constructor that extracts {@code waveId}, {@code waveletId}, and
   * {@code blipId} from the given {@code BlipData}.
   *
   * @param method the method or operation to be executed.
   *     See {@link OperationType}.
   * @param id the id of the request.
   * @param blipData the {@code BlipData} to extract {@code waveId},
   *     {@code waveletId}, and {@code blipId} from.
   * @param parameters additional parameters for this operation. See
   *     {@link ParamsProperty}.
   */
  public OperationRequest(String method, String id, BlipData blipData, Parameter...parameters) {
    this(method, id, blipData.getWaveId(), blipData.getWaveletId(), blipData.getBlipId(),
        parameters);
  }

  /**
   * Returns the method name that should be invoked.
   *
   * @return the method name.
   */
  public String getMethod() {
    return method;
  }

  /**
   * Returns the id of this request.
   *
   * @return the id of this request.
   */
  public String getId() {
    return id;
  }

  /**
   * Returns the parameters of this request.
   *
   * @return the parameters of this request.
   */
  public Map<ParamsProperty, Object> getParams() {
    return parameters;
  }

  /**
   * @param waveId to set
   */
  private void setWaveId(String waveId) {
    if (waveId != null) {
      parameters.put(ParamsProperty.WAVE_ID, waveId);
    }
  }

  /**
   * Returns the wave id where this request should be invoked on. This might not
   * be applicable to all requests.
   *
   * @return the wave id.
   */
  public String getWaveId() {
    return (String) getParameter(ParamsProperty.WAVE_ID);
  }

  /**
   * @param waveletId to set
   */
  private void setWaveletId(String waveletId) {
    if (waveletId != null) {
      parameters.put(ParamsProperty.WAVELET_ID, waveletId);
    }
  }

  /**
   * Returns the wavelet id where this request should be invoked on. This might
   * not be applicable to all requests.
   *
   * @return the wavelet id.
   */
  public String getWaveletId() {
    return (String) getParameter(ParamsProperty.WAVELET_ID);
  }

  /**
   * @param blipId to set
   */
  private void setBlipId(String blipId) {
    if (blipId != null) {
      parameters.put(ParamsProperty.BLIP_ID, blipId);
    }
  }

  /**
   * Returns the blip id where this request should be invoked on. This might not
   * be applicable to all requests.
   *
   * @return the blip id.
   */
  public String getBlipId() {
    return (String) getParameter(ParamsProperty.BLIP_ID);
  }

  /**
   * Returns a parameter of this request.
   *
   * @param property the key of the parameter.
   * @return a parameter of this request, that is keyed by the given input, or
   *     {@code null} if the parameter doesn't exist.
   */
  public Object getParameter(ParamsProperty property) {
    return parameters.get(property);
  }

  /**
   * Adds a parameter to this request.
   *
   * @param parameter to be added.
   */
  public void addParameter(Parameter parameter) {
    parameters.put(parameter.getKey(), parameter.getValue());
  }

  @Override
  public String toString() {
    return String.format("{'method':'%s','id':'%s','waveId':'%s','waveletId':'%s','blipId':'%s'," +
        "'parameters':%s}", method, id, getWaveId(), getWaveletId(), getBlipId(), parameters);
  }
}
