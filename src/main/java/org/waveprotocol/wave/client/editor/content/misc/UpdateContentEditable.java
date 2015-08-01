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

package org.waveprotocol.wave.client.editor.content.misc;

import com.google.gwt.dom.client.Element;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.misc.DisplayEditModeHandler.EditModeListener;

/**
 * Convenience handler that keeps the editability of elements in sync with the
 * global editability setting.
 */
public final class UpdateContentEditable implements EditModeListener {
  private UpdateContentEditable() {
  }

  private static final EditModeListener INSTANCE = new UpdateContentEditable();

  /**
   * Return instance of this class.
   */
  public static EditModeListener get() {
    return INSTANCE;
  }

  @Override
  public void onEditModeChange(ContentElement element, boolean isEditing) {
    Element implNodelet = element.getContainerNodelet();
    if (implNodelet != null) {
      DomHelper.setContentEditable(implNodelet, isEditing, true);
    }
  }
}
