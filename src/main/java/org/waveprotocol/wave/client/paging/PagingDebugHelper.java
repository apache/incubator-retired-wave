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

import org.waveprotocol.wave.client.paging.Traverser.MoveablePoint;

import java.util.Arrays;

/**
 * Helper class for validity checking of paging, and general debug stuff.
 *
 */
public final class PagingDebugHelper {

  /** Enables/disables aggressive (costly) consistency checking. */
  public final static boolean AGGRESSIVE_DEBUG = false;

  /** Current stack depth of activation. */
  private static int depth;
  /** Maximum stack depth of current activation. */
  private static int maxDepth;
  /** Distribution of maximum depths so far (maps depth to frequency). */
  private static int [] depths = new int [20];

  private PagingDebugHelper() {
  }

  /**
   * Checks the monotonicity of locations in a block tree.
   */
  public static void maybeCheckBlocks(Block root) {
    if (AGGRESSIVE_DEBUG) {
      assert areBlocksValid(root);
    }
  }

  private static boolean areBlocksValid(Block root) {
    MoveablePoint point = SimpleMoveablePoint.startOf(root);
    double lastLocation = point.absoluteLocation();
    while (point.hasNext()) {
      point.next();
      double location = point.absoluteLocation();
      assert location + 0.01 >= lastLocation : "point backtracks";
      lastLocation = location;
    }
    return true;
  }

  /**
   * Keeps track of the stack depth of recursive activations.
   */
  public static void enterActivate() {
    if (!AGGRESSIVE_DEBUG) {
      return;
    }
    if (++depth > maxDepth) {
      maxDepth = depth;

    }
  }

  /**
   * Keeps track of the stack depth of recursive activations.
   */
  public static void leaveActivate() {
    if (!AGGRESSIVE_DEBUG) {
      return;
    }

    if (--depth == 0) {
      depths[maxDepth]++;
      maxDepth = 0;
    }
  }

  /**
   * Reports the distribution of stack depths for recursive activation.
   */
  public static void report() {
    int total = 0;
    for (int i = 0; i < depths.length; i++) {
      total += depths[i];
    }
    for (int i = 0; i < depths.length; i++) {
      int count = depths[i];
      int pct = Math.round(count * 100.0f / total);
      System.out.println("Depth " + i + ": " + pct + "%");
    }
    Arrays.fill(depths, 0);
  }
}
