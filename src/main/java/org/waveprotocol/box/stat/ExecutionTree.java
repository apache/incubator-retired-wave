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
package org.waveprotocol.box.stat;

import com.google.gwt.core.shared.GWT;

/**
 * Class that helps build a hierarchical tree of timers.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class ExecutionTree implements RequestScope.Value {
  private static final int DEFAULT_THRESHOLD = 500;

  private final ExecutionNode root;
  private ExecutionNode currentNode;

  public ExecutionTree() {
    root = new ExecutionNode(null, null, "", false, 0);
    currentNode = root;
  }

  private ExecutionTree(ExecutionNode root, ExecutionNode currentNode) {
    this.root = root;
    this.currentNode = currentNode;
  }

  /**
   * Push a new timer node.
   */
  Timer start(String name) {
    return start(name, false, DEFAULT_THRESHOLD);
  }

  /**
   * Push a new timer node.
   */
  Timer start(String name, int threshold) {
    return start(name, false, threshold);
  }

  /**
   * Push a new timer node.
   */
  Timer startRequest(String name) {
    return start(name, true, DEFAULT_THRESHOLD);
  }

  /**
   * Push a new timer node.
   */
  Timer startRequest(String name, int threshold) {
    return start(name, true, threshold);
  }

  /**
   * Push a new timer node.
   */
  Timer start(String name, boolean isRequest, int threshold) {
    ExecutionNode node = currentNode.newChild(name, getModuleName(), isRequest, threshold);
    currentNode = node;
    Timer timer = new Timer(node);
    timer.start();
    return timer;
  }

  /**
   * Pop the timer node.
   */
  void pop(ExecutionNode node) {
    while (currentNode != node && currentNode.getParent() != null) {
      currentNode = currentNode.getParent();
    }
    if (node == currentNode && currentNode.getParent() != null) {
      currentNode = currentNode.getParent();
    }
  }

  /**
   * Record statistics.
   */
  void record(String name, int interval) {
    record(name, false, interval, DEFAULT_THRESHOLD);
  }

  /**
   * Record statistics.
   */
  void record(String name, int interval, int threshold) {
    record(name, false, interval, threshold);
  }

  /**
   * Record statistics.
   */
  void recordRequest(String name, int interval) {
    record(name, true, interval, DEFAULT_THRESHOLD);
  }

  /**
   * Record statistics.
   */
  void recordRequest(String name, int interval, int threshold) {
    record(name, true, interval, threshold);
  }

  @Override
  public RequestScope.Value clone() {
    return new ExecutionTree(root, currentNode);
  }

  /**
   * Record statistics.
   */
  void record(String name, boolean isRequest, int interval, int threshold) {
    ExecutionNode node = currentNode.newChild(name, getModuleName(), isRequest, threshold);
    record(node, interval);
  }

  /**
   * Record statistics.
   */
  void record(ExecutionNode node, int interval) {
    node.sample(interval);

    Timing.getStatsRecorder().record(node.getName(), node.getModule(), interval, node.getMeasurement().getThreshold());
    if (node.isRequest()) {
      Timing.getStatsRecorder().recordRequest(node);
    }
  }

  ExecutionNode getRoot() {
    return root;
  }

  ExecutionNode getCurrent() {
    return currentNode;
  }

  private String getModuleName() {
    if (GWT.isClient()) {
      return com.google.gwt.core.client.GWT.getModuleName();
    }
    return "";
  }
}
