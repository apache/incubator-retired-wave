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


package org.waveprotocol.wave.client.wave;

import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.model.conversation.Blips;

/**
 * Defines the base handler set for documents in undercurrent.
 *
 */
public final class RegistriesHolder {

  private static final Registries REGISTRIES = Editor.ROOT_REGISTRIES;

  static {
    // Registries that have to be installed for content documents to function,
    // even if there is no rendering.
    Editors.initRootRegistries();

    LineRendering.registerContainer(Blips.BODY_TAGNAME, REGISTRIES.getElementHandlerRegistry());
    Blips.init();
  }

  /** @return the base handlers. */
  public static Registries get() {
    return REGISTRIES;
  }
}
