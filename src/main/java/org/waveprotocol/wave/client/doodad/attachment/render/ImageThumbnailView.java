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

package org.waveprotocol.wave.client.doodad.attachment.render;

/**
 * Write-only interface for updating the display of an image thumbnail
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface ImageThumbnailView {

  /**
   * Listener to Image Thumbnail UI events
   */
  public interface ImageThumbnailViewListener {

    /**
     * The user requested a change to "full size mode"
     *
     * @param isOn if true, show full size, if false, thumbnail mode.
     */
    void onRequestSetFullSizeMode(boolean isOn);

    /**
     * The user clicked on the image.
     */
    public void onClickImage();
  }

  /**
   * Displays a special image to indicate that an image's upload has failed
   * and will never complete.
   *
   * @param tooltip to be shown when the mouse hovers over the error.
   */
  public void displayDeadImage(String tooltip);

  /**
   * Sets the URL that the attachment file is stored at
   */
  public void setAttachmentUrl(String url);

  /**
   * Sets the size of the attachment image.
   *
   * @param width image width (0 if unknown).
   * @param height image height (0 if unknown).
   */
  public void setAttachmentSize(int width, int height);

  /**
   * Sets the URL for the thumbnail preview image
   * @param url the non-null url
   */
  public void setThumbnailUrl(String url);

  /**
   * Sets the size of the thumbnail image.
   *
   * @param width image width (0 if unknown).
   * @param height image height (0 if unknown).
   */
  public void setThumbnailSize(int width, int height);

  /**
   * Displays progress information
   */
  public void showUploadProgress();

  /**
   * Stops displaying progress information
   */
  public void hideUploadProgress();

  /**
   * Sets progress
   * @param progress number between 0 and 1
   */
  public void setUploadProgress(double progress);

  /**
   * Choose displaymode
   *
   * @param isOn if true, full size mode, if false, thumbnail mode
   */
  void setFullSizeMode(boolean isOn);

  /**
   * Set listener for thumbnail UI events
   *
   * @param listener
   */
  void setListener(ImageThumbnailViewListener listener);
}
