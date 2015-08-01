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

package org.waveprotocol.wave.client.editor.extract;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Point.El;

/**
 * Listener that receives notifications on repairer reverts.
 * @author patcoleman@google.com (Pat Coleman)
 */
public interface RepairListener {
  /**
   * Callback for notifications of when a document range is about to be reverted.
   * @param start Start of revert range
   * @param end End of revert range
   */
  void onRangeRevert(Point.El<ContentNode> start, Point.El<ContentNode> end);

  /**
   * Callback for notifications of when an entire document is being reverted.
   * @param doc The document being reverted.
   */
  void onFullDocumentRevert(ReadableDocument<ContentNode, ContentElement, ContentTextNode> doc);

  /**
   * Simple implementation where neither of the callbacks do anything.
   */
  public static RepairListener NONE = new RepairListener() {
    @Override
    public void onFullDocumentRevert(
        ReadableDocument<ContentNode, ContentElement, ContentTextNode> doc) {
    }
    @Override
    public void onRangeRevert(El<ContentNode> start, El<ContentNode> end) {
    }
  };
}
