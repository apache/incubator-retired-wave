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

package org.waveprotocol.wave.client.wavepanel.view;


/**
 * The 'meta' part of a blip, which contains all the view state and structure
 * relating to its document.
 *
 */
public interface BlipMetaView extends View, IntrinsicBlipMetaView {

  //
  // Structure.
  //

  /**
   * @return the inline anchor before {@code ref}, or the last inline anchor if
   *         {@code ref} is null.
   */
  AnchorView getInlineAnchorBefore(AnchorView ref);

  /**
   * @return the inline anchor after {@code ref}, or the first inline anchor if
   *         {@code ref} is null.
   */
  AnchorView getInlineAnchorAfter(AnchorView ref);

  /**
   * Moves an existing anchor before another existing anchor.
   *
   * @param ref reference location, after which to insert
   * @param x anchor to move
   */
  // Note: inline anchors are expected to be generated outside this view by an
  // external mechanism (i.e., a document doodad). As such, this method simply
  // updates whatever metadata this view chooses to associate with anchors.
  void insertInlineAnchorBefore(AnchorView ref, AnchorView x);

  /** @return the blip view that contains this view. */
  BlipView getParent();

  //
  // NOTE(user): These focus frame methods are going to be removed once the
  // focus frame is implemented using (Javascript-calculated) positioning.
  // It is not black and white whether these focus-frame methods are structural
  // or statural. Implementation-wise, they look structural, so are categorized
  // as such.
  //

  /**
   * Places the focus frame on the this blip view.
   *
   * @param frame
   */
  void placeFocusFrame(FocusFrameView frame);

  /**
   * Removes the chrome that places focus on a blip.
   *
   * @param frame focus chrome
   */
  void removeFocusChrome(FocusFrameView frame);
}
