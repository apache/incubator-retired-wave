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

import org.waveprotocol.wave.client.paging.Traverser.BlockSide;
import org.waveprotocol.wave.client.paging.Traverser.MoveablePoint;
import org.waveprotocol.wave.client.paging.Traverser.Point;

/**
 * An instantiable version of {@link MoveablePoint}.
 *
 */
final class SimpleMoveablePoint extends MoveablePoint {
  SimpleMoveablePoint(BlockSide side, Block block) {
    super(side, block);
  }

  static SimpleMoveablePoint startOf(Block block) {
    return new SimpleMoveablePoint(BlockSide.START, block);
  }

  static SimpleMoveablePoint endOf(Block block) {
    return new SimpleMoveablePoint(BlockSide.END, block);
  }

  static SimpleMoveablePoint at(Point ref) {
    return new SimpleMoveablePoint(ref.side, ref.block);
  }
}
