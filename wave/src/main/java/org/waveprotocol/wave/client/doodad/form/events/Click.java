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

package org.waveprotocol.wave.client.doodad.form.events;

import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.NullRenderer;
import org.waveprotocol.wave.client.editor.util.EditorDocHelper;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

public final class Click {

  private static final String TAGNAME = "click";

  /**
   * NOTE(user): Unused, think about what to do with these, or delete.
   */
  private static final String TIME = "time";
  private static final String CLICKER = "clicker";
  static final String[] ATTRIBUTE_NAMES
      = new String[] {TIME, CLICKER};

  public static void register(ElementHandlerRegistry handlerRegistry) {
    handlerRegistry.registerRenderer(TAGNAME, NullRenderer.INSTANCE);
  }

  private Click() {
  }

  /**
   * @return A content xml string containing a click
   *
   * TODO(user): record clicker + time, etc. in the click node
   */
  public static XmlStringBuilder constructXml() {
    return XmlStringBuilder.createEmpty().wrap(TAGNAME);
  }

  public static boolean isClick(ContentNode node) {
    return EditorDocHelper.isNamedElement(node, TAGNAME);
  }
}
