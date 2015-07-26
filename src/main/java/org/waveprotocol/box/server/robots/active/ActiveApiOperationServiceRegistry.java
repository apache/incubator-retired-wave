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

package org.waveprotocol.box.server.robots.active;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.wave.api.OperationType;

import org.waveprotocol.box.server.robots.AbstractOperationServiceRegistry;
import org.waveprotocol.box.server.robots.operations.*;

/**
 * A registry of {@link OperationService}s for the active robot API.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public final class ActiveApiOperationServiceRegistry extends AbstractOperationServiceRegistry {

  // Suppressing warnings about operations that are deprecated but still used by
  // the default client libraries
  @SuppressWarnings("deprecation")
  @Inject
  public ActiveApiOperationServiceRegistry(Injector injector) {
    super();
    NotifyOperationService notifyOpService = injector.getInstance(NotifyOperationService.class);
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
    register(OperationType.ROBOT_CREATE_WAVELET, CreateWaveletService.create());
    register(OperationType.ROBOT_FETCH_WAVE, FetchWaveService.create());
    register(OperationType.DOCUMENT_MODIFY, DocumentModifyService.create());
    register(OperationType.ROBOT_SEARCH, injector.getInstance(SearchService.class));
    register(OperationType.WAVELET_SET_TITLE, WaveletSetTitleService.create());
    register(OperationType.ROBOT_FOLDER_ACTION, FolderActionService.create());
    register(OperationType.ROBOT_FETCH_PROFILES, injector.getInstance(FetchProfilesService.class));
    register(OperationType.ROBOT_EXPORT_SNAPSHOT, ExportSnapshotService.create());
    register(OperationType.ROBOT_EXPORT_DELTAS, ExportDeltasService.create());
    register(OperationType.ROBOT_EXPORT_ATTACHMENT, injector.getInstance(ExportAttachmentService.class));
    register(OperationType.ROBOT_IMPORT_DELTAS, injector.getInstance(ImportDeltasService.class));
    register(OperationType.ROBOT_IMPORT_ATTACHMENT, injector.getInstance(ImportAttachmentService.class));
  }
}
