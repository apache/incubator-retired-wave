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

package org.waveprotocol.wave.client.gadget.renderer;

import org.waveprotocol.wave.model.id.WaveletName;

/**
 * Gadget data store interface. Defines methods to fetch Gadget data for
 * rendering. The implementing classes may access the Google Gadget Servers
 * and cache the data.
 * See: http://code.google.com/apis/gadgets/docs/spec.html for more details.
 *
 * TODO(user): This is really a GadgetRenderer, as it prepares a gadget
 * for rendering and retrieves everything required to do so. So this should
 * really be renamed and return a GadgetInstance.
 *
 */
public interface GadgetDataStore {
  /**
   * Data callback interface to asynchronously receive the data or error
   * information from the store.
   */
  public interface DataCallback {
    /**
     * Called by the store when the requested gadget data is ready.
     * TODO(user): Change to GadgetData class when implemented.
     *
     * @param metadata The gadget metadata object passed from the store.
     * @param securityToken The security token for this gadget, may be null.
     */
    void onDataReady(GadgetMetadata metadata, String securityToken);

    /**
     * Called by the store if the requested metadata cannot be retrieved.
     *
     * @param message error message.
     * @param exception the exception that caused the error (can be null).
     */
    void onError(String message, Throwable exception);
  }

  /**
   * Requests gadget data from the store.
   *
   * @param gadgetSpecUrl the gadget specification URL that identified the
   *        gadget.
   * @param waveletName The Wavelet name for this gadget.
   * @param instanceId The gadget instance id.
   * @param receiveDataCommand callback that receives the result.
   */
  void getGadgetData(final String gadgetSpecUrl, WaveletName waveletName,
      int instanceId, final DataCallback receiveDataCommand);
}
