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

package org.waveprotocol.wave.client.doodad.experimental.htmltemplate;

import org.waveprotocol.wave.client.editor.NodeMutationHandlerImpl;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;

/**
 * Mutation handler for <namevaluepair> nodes. Notifies Caja code of changes.
 *
 * @author ihab@google.com (Ihab Awad)
 */
class NameValuePairNodeMutationHandler
    extends NodeMutationHandlerImpl<ContentNode, ContentElement> {

  @Override
  public void onAddedToParent(ContentElement nameValuePair, ContentElement oldParent) {
    getContext(nameValuePair).onNameValuePairAdded(nameValuePair);
  }

  @Override
  public void onAttributeModified(ContentElement nameValuePair, String name,
      String oldValue, String newValue) {
    getContext(nameValuePair).onNameValuePairAttributeModified(nameValuePair, name, oldValue, newValue);
  }

  @Override
  public void onRemovedFromParent(ContentElement nameValuePair, ContentElement newParent) {
    getContext(nameValuePair).onNameValuePairRemoved(nameValuePair);
  }

  private PluginContext getContext(ContentElement nameValuePair) {
    return nameValuePair.getParentElement()
        .getProperty(HtmlTemplate.TEMPLATE_PLUGIN_CONTEXT);
  }
}