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

package org.waveprotocol.wave.client.editor.content;

/**
 * Manages transparent nodes.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface TransparentManager<E> {

  /**
   * When we need to split a transparent node, this method is called on the
   * manager associated with the transparent node. It should return the new
   * next sibling to use, and then some of the children will be shifted into the new
   * sibling as needed.
   *
   * @param transparentNode The transparent node that needs to be split
   * @return The new sibling to use for the split, inserted after the original element
   */
  E needToSplit(E transparentNode);
}
