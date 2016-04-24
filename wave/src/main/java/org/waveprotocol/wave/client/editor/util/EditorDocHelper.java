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

package org.waveprotocol.wave.client.editor.util;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Document helper utilities that have some GWT dependency.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class EditorDocHelper {

  /**
   * @param node
   * @param tagName
   * @return true if node is an element with tag name equal to the given one
   */
  public static boolean isNamedElement(ContentNode node, String tagName) {
    assert node != null;
    return !node.isTextNode() && ((ContentElement) node).getTagName().equals(tagName);
  }

  /**
   * Preconditions-style check to ensure a node is an element of a given tag
   * name
   *
   * @param node
   * @param tagName
   * @param context debugging context (e.g. a method name). avoid runtime string
   *        computation.
   */
  public static void checkNamedElement(ContentNode node, String tagName, String context) {
    if (!isNamedElement(node, tagName)) {
      Preconditions.illegalArgument(
          context + ": expected element '" + tagName + "', got " +
          (node.isTextNode() ? "a text node" : "element '" + node.asElement().getTagName() + "'"));
    }
  }
}
