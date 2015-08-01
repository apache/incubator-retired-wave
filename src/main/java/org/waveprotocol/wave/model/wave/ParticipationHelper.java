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

package org.waveprotocol.wave.model.wave;

import java.util.Set;

/**
 * Methods to help figure out how to participate in a wavelet.
 *
 */
public interface ParticipationHelper {
  /**
   * Throws UnsupportedOperationException in methods that shouldn't be used
   * in read only wavelets.
   */
  static final ParticipationHelper READONLY = new ParticipationHelper() {
      @Override
      public ParticipantId getAuthoriser(ParticipantId editor, Set<ParticipantId> candidates) {
        throw new UnsupportedOperationException("Read only");
      }
    };

  /**
   * Ignores access control.
   */
  static final ParticipationHelper IGNORANT = new ParticipationHelper() {
      @Override
      public ParticipantId getAuthoriser(ParticipantId editor, Set<ParticipantId> candidates) {
        return null;
      }
    };

  /**
   * Grants access to all control.
   */
  static final ParticipationHelper AUTO = new ParticipationHelper() {
      @Override
      public ParticipantId getAuthoriser(ParticipantId editor, Set<ParticipantId> candidates) {
        return editor;
      }
    };

  /**
   * The default participation control.
   */
  static final ParticipationHelper DEFAULT = AUTO;

  /**
   * Selects a participant who will be willing to authorise that the given
   * editor should be able to make changes.
   *
   * @param editor who needs someone else's authorisation before they will be
   *        allowed to make changes.
   * @param candidates who are sufficiently privileged to give the editor
   *        access to make changes. May contain the editor.
   * @return the candidate that should be used to authorise the editor's
   *         changes. Null can be returned in special circumstances to
   *         indicate that the model user wishes to override the normal
   *         behaviour and not attempt to meet the usual authorisation
   *         requirements.
   * @throws IllegalStateException if none of the candidates are able to
   *         approve the editor's changes.
   */
  ParticipantId getAuthoriser(ParticipantId editor, Set<ParticipantId> candidates);
}
