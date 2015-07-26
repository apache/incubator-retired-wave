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

package com.google.wave.api.event;

import com.google.wave.api.Context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An interface for robot event handler. This interface defines various methods
 * that would respond to all possible robot events.
 */
public interface EventHandler {

  /**
   * An annotation that would define the robot's interest in handling a
   * particular event. Robot should annotate the overriden event handler method
   * with this annotation to specify the event contexts and filter.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  static @interface Capability {

    /**
     * @return the list of contexts that should be send with this particular
     *     event.
     */
    Context[] contexts() default {Context.ROOT, Context.PARENT, Context.CHILDREN};

    /**
     * @return the filter for this particular event.
     */
    String filter() default "";
  }

  /**
   * Handler for {@link AnnotatedTextChangedEvent}.
   *
   * @param event the annotated text changed event.
   */
  void onAnnotatedTextChanged(AnnotatedTextChangedEvent event);

  /**
   * Handler for {@link BlipContributorsChangedEvent}.
   *
   * @param event the blip contributors changed event.
   */
  void onBlipContributorsChanged(BlipContributorsChangedEvent event);

  /**
   * Handler for {@link BlipSubmittedEvent}.
   *
   * @param event the blip submitted event.
   */
  void onBlipSubmitted(BlipSubmittedEvent event);

  /**
   * Handler for {@link DocumentChangedEvent}.
   *
   * @param event the document changed event.
   */
  void onDocumentChanged(DocumentChangedEvent event);

  /**
   * Handler for {@link FormButtonClickedEvent}.
   *
   * @param event the form button clicked event.
   */
  void onFormButtonClicked(FormButtonClickedEvent event);

  /**
   * Handler for {@link GadgetStateChangedEvent}.
   *
   * @param event the gadget state changed event.
   */
  void onGadgetStateChanged(GadgetStateChangedEvent event);

  /**
   * Handler for {@link WaveletBlipCreatedEvent}.
   *
   * @param event the blip created event.
   */
  void onWaveletBlipCreated(WaveletBlipCreatedEvent event);

  /**
   * Handler for {@link WaveletBlipRemovedEvent}.
   *
   * @param event the blip removed event.
   */
  void onWaveletBlipRemoved(WaveletBlipRemovedEvent event);

  /**
   * Handler for {@link WaveletCreatedEvent}.
   *
   * @param event the wavelet created event.
   */
  void onWaveletCreated(WaveletCreatedEvent event);

  /**
   * Handler for {@link WaveletFetchedEvent}.
   *
   * @param event the wavelet fetched event.
   */
  void onWaveletFetched(WaveletFetchedEvent event);

  /**
   * Handler for {@link WaveletParticipantsChangedEvent}.
   *
   * @param event the participants changed event.
   */
  void onWaveletParticipantsChanged(WaveletParticipantsChangedEvent event);

  /**
   * Handler for {@link WaveletSelfAddedEvent}.
   *
   * @param event the self added event.
   */
  void onWaveletSelfAdded(WaveletSelfAddedEvent event);

  /**
   * Handler for {@link WaveletSelfRemovedEvent}.
   *
   * @param event the self removed event.
   */
  void onWaveletSelfRemoved(WaveletSelfRemovedEvent event);

  /**
   * Handler for {@link WaveletTagsChangedEvent}.
   *
   * @param event the tags changed event.
   */
  void onWaveletTagsChanged(WaveletTagsChangedEvent event);

  /**
   * Handler for {@link WaveletTitleChangedEvent}.
   *
   * @param event the title changed event.
   */
  void onWaveletTitleChanged(WaveletTitleChangedEvent event);

  /**
   * Handler for {@link OperationErrorEvent}.
   *
   * @param event the operation error event.
   */
  void onOperationError(OperationErrorEvent event);
}
