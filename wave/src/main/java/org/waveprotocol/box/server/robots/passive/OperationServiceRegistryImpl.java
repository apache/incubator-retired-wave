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
import com.google.wave.api.OperationType;

import org.waveprotocol.box.server.robots.AbstractOperationServiceRegistry;
import org.waveprotocol.box.server.robots.operations.BlipOperationServices;
import org.waveprotocol.box.server.robots.operations.CreateWaveletService;
import org.waveprotocol.box.server.robots.operations.DocumentModifyService;
import org.waveprotocol.box.server.robots.operations.FolderActionService;
import org.waveprotocol.box.server.robots.operations.NotifyOperationService;
import org.waveprotocol.box.server.robots.operations.OperationService;
import org.waveprotocol.box.server.robots.operations.ParticipantServices;
import org.waveprotocol.box.server.robots.operations.WaveletSetTitleService;

/**
 * Class for registering and accessing {@link OperationService} to execute
 * operations for use in the Passive Robot API.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public final class OperationServiceRegistryImpl extends AbstractOperationServiceRegistry {

  // Suppressing warnings about operations that are deprecated but still used by
  // the default client libraries.
  @SuppressWarnings("deprecation")
  @Inject
  OperationServiceRegistryImpl(NotifyOperationService notifyOpService) {
    super();

    // Register all the OperationProviders
    register(OperationType.ROBOT_NOTIFY, notifyOpService);
    register(OperationType.ROBOT_NOTIFY_CAPABILITIES_HASH, notifyOpService);
    register(OperationType.WAVELET_ADD_PARTICIPANT_NEWSYNTAX, ParticipantServices.create());
    register(OperationType.WAVELET_APPEND_BLIP, BlipOperationServices.create());
    register(OperationType.WAVELET_REMOVE_PARTICIPANT_NEWSYNTAX, ParticipantServices.create());
    register(OperationType.BLIP_CONTINUE_THREAD, BlipOperationServices.create());
    register(OperationType.BLIP_CREATE_CHILD, BlipOperationServices.create());
    register(OperationType.BLIP_DELETE, BlipOperationServices.create());
    register(OperationType.DOCUMENT_APPEND_INLINE_BLIP, BlipOperationServices.create());
    register(OperationType.DOCUMENT_APPEND_MARKUP, BlipOperationServices.create());
    register(OperationType.DOCUMENT_INSERT_INLINE_BLIP, BlipOperationServices.create());
    register(OperationType.DOCUMENT_INSERT_INLINE_BLIP_AFTER_ELEMENT,
        BlipOperationServices.create());
    register(OperationType.WAVELET_CREATE, CreateWaveletService.create());
    register(OperationType.DOCUMENT_MODIFY, DocumentModifyService.create());
    register(OperationType.WAVELET_SET_TITLE, WaveletSetTitleService.create());
    register(OperationType.ROBOT_FOLDER_ACTION, FolderActionService.create());
  }
}
