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

package org.waveprotocol.wave.model.testing;

import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipationHelper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Mock {@link ParticipationHelper}.
 *
 */
public class MockParticipationHelper implements ParticipationHelper {

  /**
   * Frame used with {@link MockParticipationHelper} to record expectations
   * and desired results.
   */
  public static class Frame {
    private final Set<ParticipantId> candidates;
    private final ParticipantId editor;
    private final ParticipantId result;

    /**
     * Creates a frame that will either return a given participant or throw an
     * {@link IllegalStateException} if no participant is given.
     *
     * @param result participant to return from this frame, or null if an
     *        {@link IllegalStateException} should be thrown.
     * @param editor required for this frame to apply.
     * @param candidates required for this frame to apply.
     */
    public Frame(ParticipantId result, ParticipantId editor,
        ParticipantId... candidates) {
      this.result = result;
      this.editor = editor;
      this.candidates = new HashSet<ParticipantId>(Arrays.asList(candidates));
    }

    /** Returns the result or throws the exception dictated by this frame. */
    public ParticipantId apply() {
      if (result == null) {
        throw new IllegalStateException("Authoriser set to throw exception on this frame.");
      } else {
        return result;
      }
    }

    /** Checks whether the given arguments match those expected by this frame. */
    public boolean matches(ParticipantId editor, Set<ParticipantId> candidates) {
      return editor.equals(this.editor) && candidates.equals(this.candidates);
    }
  }

  private final LinkedList<Frame> frames = new LinkedList<Frame>();

  /**
   * {@inheritDoc}
   *
   * Makes a decision by comparing against the next frame in the stub. If
   * successful, that frame will then be discarded.
   *
   * @return the return participant of the frame if the arguments match those of
   *         the frame and the frame includes a return participant.
   * @throws IllegalStateException if the arguments match those of the frame and
   *         the frame is designed to throw such an exception.
   * @throws AssertionError if the arguments do not match those of the frame.
   * @throws NoSuchElementException if there are no frames left.
   */
  @Override
  public ParticipantId getAuthoriser(ParticipantId editor, Set<ParticipantId> candidates) {
    if (frames.isEmpty()) {
      throw new NoSuchElementException("No frames left to compare with getAuthoriser("
          + editor + ", " + candidates + ")");
    } else {
      Frame frame = frames.removeFirst();
      if (frame.matches(editor, candidates)) {
        return frame.apply();
      } else {
        throw new AssertionError();
      }
    }
  }

  /** Adds a given frame to the end of the list of those expected by this stub. */
  public void program(Frame frame) {
    frames.addLast(frame);
  }
}
