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

package org.waveprotocol.wave.model.document.raw.impl;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.indexed.NodeType;

/**
 * Mimics a DOM Text node.
 *
 */
public final class Text extends Node implements Doc.T {

  private String data;

  /**
   * Constructs a Text node containing the given data.
   *
   * @param data The data the new Text node should contain.
   */
  public Text(String data) {
    this.data = data;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public short getNodeType() {
    return NodeType.TEXT_NODE;
  }

  /**
   * @return The text content of this node.
   */
  public String getData() {
    return data;
  }

  /**
   * @return The length of the text content of this node.
   */
  public int getLength() {
    return data.length();
  }

  /**
   * Appends the given string to the contents of this text node.
   *
   * @param arg The text to append.
   */
  public void appendData(String arg) {
    data += arg;
  }

  /**
   * Inserts the given string into the contents of this text node at the given
   * offset.
   *
   * @param offset The offset at which to insert the text.
   * @param arg The text to insert.
   */
  public void insertData(int offset, String arg) {
    data = data.substring(0, offset) + arg + data.substring(offset);
  }

  /**
   * Deletes the characters in the specified range.
   *
   * @param offset The offset of the start point of the range.
   * @param count The size of the range.
   */
  public void deleteData(int offset, int count) {
    data = data.substring(0, offset) + data.substring(offset + count);
  }

  /**
   * Splits this text node at the given offset.
   * If the offset is zero, no split occurs, and the current node is returned.
   * If the offset is equal to or greater than the length of the text node, no split
   * occurs, and null is returned.
   *
   * @param offset The offset at which to split this text node.
   * @return The text node containing all text that came after the given offset
   *         in the old text node.
   */
  public Text splitText(int offset) {
    if (offset == 0) {
      return this;
    } else if (offset >= getLength()) {
      return null;
    }

    String splitOffText = data.substring(offset);
    data = data.substring(0, offset);
    Text newNode = new Text(splitOffText);
    parent.insertBefore(newNode, nextSibling);
    return newNode;
  }

  /** {@inheritDoc} */
  @Override
  public int calculateSize() {
    return getLength();
  }

  @Override
  public Element asElement() {
    return null;
  }

  @Override
  public Text asText() {
    return this;
  }

  @Override
  public String toString() {
    return getData();
  }
}
