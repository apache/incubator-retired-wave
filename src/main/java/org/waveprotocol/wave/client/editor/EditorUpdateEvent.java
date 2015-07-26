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

package org.waveprotocol.wave.client.editor;


/**
 * Batched update event information
 *
 * WARNING: Event objects get reused! Defensively copy if you will use it
 * outside of the call to onUpdate method.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface EditorUpdateEvent {

  /** Listener for update events */
  public interface EditorUpdateListener {

    /**
     * Fired asynchronously after editor content or selection is modified.
     *
     * WARNING:
     * Editor update event is recycled - do not use outside context of notification
     * method call, its methods will return different values
     */
    void onUpdate(EditorUpdateEvent event);
  }

  /**
   * Indicates whether the XY coordinates of the user's selection may have changed.
   */
  boolean selectionCoordsChanged();

  /**
   * Indicates whether the selection integer location may have changed.
   * Operations transform the selection, and this does not count.
   */
  boolean selectionLocationChanged();

  /**
   * Indicates whether the content changed at all
   * NOTE(danilatos): Currently just persistent content, but should we
   * also include local content? (Hopefully local content is enough of
   * a function of the persistent content that it's not necessary...)
   */
  boolean contentChanged();

  /**
   * Indicates the content changed as a direct (not indirect) result of a local
   * user action.
   *
   * @see Responsibility
   */
  boolean contentChangedDirectlyByUser();

  /**
   * Retrieve the context in which the update event took place.
   */
  EditorContext context();
}
