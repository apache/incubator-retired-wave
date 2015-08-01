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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

/**
 * Represents a single timer node.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
class ExecutionNode {

  private final ExecutionNode parent;
  private final String name;
  private final String module;
  private final boolean isRequest;
  private final List<ExecutionNode> children;
  private final Map<String, ExecutionNode> childMap;
  private final Measurement measurement;

  ExecutionNode(ExecutionNode parent, String name, String module, boolean isRequest, int threshold) {
    this.parent = parent;
    this.name = name;
    this.module = module;
    this.isRequest = isRequest;
    this.children = Lists.newArrayList();
    this.childMap = Maps.newHashMap();
    this.measurement = new Measurement();
    measurement.setThreshold(threshold);
  }

  /**
   * Creates
   * @param name
   * @param module
   * @param isRequest
   * @param threshold
   * @return
   */
  synchronized ExecutionNode newChild(String name, String module, boolean isRequest, int threshold) {
    ExecutionNode node = childMap.get(name);
    if (node == null) {
      node = new ExecutionNode(this, name, module, isRequest, threshold);
      children.add(node);
      childMap.put(name, node);
    }
    return node;
  }

  synchronized void sample(int delta) {
    measurement.sample(delta);
  }

  String getName() {
    return name;
  }

  String getModule() {
    return module;
  }

  boolean isRequest() {
    return isRequest;
  }

  Measurement getMeasurement() {
    return measurement;
  }

  ExecutionNode getParent() {
    return parent;
  }

  synchronized Iterable<ExecutionNode> getChildren() {
    return ImmutableList.copyOf(children);
  }
}
