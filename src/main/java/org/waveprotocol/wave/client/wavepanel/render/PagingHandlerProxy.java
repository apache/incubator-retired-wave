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

package org.waveprotocol.wave.client.wavepanel.render;

import org.waveprotocol.wave.client.wavepanel.view.dom.full.BlipQueueRenderer.PagingHandler;
import org.waveprotocol.wave.model.conversation.ConversationBlip;

/**
 * Fans out how to page content in/out to a set of handlers.
 *
 */
public final class PagingHandlerProxy implements PagingHandler {

  private final PagingHandler[] handlers;

  PagingHandlerProxy(PagingHandler... handlers) {
    this.handlers = handlers;
  }

  /**
   * Creates a paging proxy.
   */
  public static PagingHandlerProxy create(PagingHandler... handlers) {
    return new PagingHandlerProxy(handlers);
  }

  @Override
  public void pageIn(ConversationBlip blip) {
    for (PagingHandler handler : handlers) {
      handler.pageIn(blip);
    }
  }

  @Override
  public void pageOut(ConversationBlip blip) {
    for (PagingHandler handler : handlers) {
      handler.pageOut(blip);
    }
  }
}
