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


package org.waveprotocol.wave.client.paging;

/**
 * A unit of paging.  A block's state is potentially dynamic; its
 * {@link #getStart() start} and {@link #getEnd() end} may change over time.
 *
 */
public interface Block extends Region, TreeNode {

  /**
   * Pages this block in.  Does not page in any child blocks.
   */
  void pageIn();

  /**
   * Pages this block out. Does not page out child blocks. This method is only
   * to be called if all child blocks are paged out. Block implementations may
   * choose to throw an exception if any child blocks are still paged in.
   */
  void pageOut();

  /**
   * @return the origin, relative to which the positions of this block's
   *         children are expressed.
   */
  double getChildrenOrigin();

  //
  // Covariant overrides.
  //

  @Override
  Block getFirstChild();

  @Override
  Block getLastChild();

  @Override
  Block getNextSibling();

  @Override
  Block getPreviousSibling();

  @Override
  Block getParent();

}
