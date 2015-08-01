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

package org.waveprotocol.wave.model.document.parser;

import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Item type compatible with XmlPullParser interface
 *
 */
class Item {
  final ItemType type;
  final String name;
  final StringMap<String> attrs;
  final String data;

  /**
   * @param tagName
   * @param attrList
   * @return element start item
   */
  public static Item elementStart(String tagName, StringMap<String> attrList) {
    return new Item(ItemType.START_ELEMENT, tagName, attrList, null);
  }

  /**
   * @param closingName
   * @return element end item
   */
  public static Item elementEnd(String closingName) {
    return new Item(ItemType.END_ELEMENT, closingName, null, null);
  }

  /**
   * @param name
   * @param value
   * @return processing instruction item
   */
  public static Item processingInstruction(String name, String value) {
    return new Item(ItemType.PROCESSING_INSTRUCTION, name, null, value);
  }

  /**
   * @param text
   * @return text item
   */
  public static Item text(String text) {
    return new Item(ItemType.TEXT, null, null, text);
  }

  /**
   * @return an end element that corresponds to this start element.
   */
  public Item startElementToEndElement() {
    Preconditions.checkState(type == ItemType.START_ELEMENT,
        "Can only convert start elements to end elements");
    return new Item(ItemType.END_ELEMENT, name, null, null);
  }

  /**
   * @param type
   * @param name
   * @param attrs
   * @param data
   */
  public Item(ItemType type, String name, StringMap<String> attrs, String data) {
    this.type = type;
    this.name = name;
    this.attrs = attrs == null || attrs.isEmpty() ? null : CollectionUtils.copyStringMap(attrs);
    this.data = data;
  }

  String getProcessingInstructionName() {
    checkAtProcessingInstruction();
    return name;
  }

  String getProcessingInstructionValue()  {
    checkAtProcessingInstruction();
    return data;
  }

  String getTagName() {
    checkAtElement();
    return name;
  }

  String getText() {
    checkAtText();
    return data;
  }

  Item copy() {
    Item copy = new Item(type, name, attrs, data);
    return copy;
  }

  StringMap<String> getAttributes() {
    checkAtElementStart();
    if (attrs == null) {
      return CollectionUtils.emptyMap();
    }
    return attrs;
  }

  private void checkAtProcessingInstruction() {
    Preconditions.checkState(type == ItemType.PROCESSING_INSTRUCTION,
        "Cursor not at processing instruction");
  }

  private void checkAtElement() {
    Preconditions.checkState(type == ItemType.START_ELEMENT || type == ItemType.END_ELEMENT,
        "Cursor not at element");
  }

  private void checkAtElementStart() {
    Preconditions.checkState(type == ItemType.START_ELEMENT, "Cursor not at element start");
  }

  private void checkAtText() {
    Preconditions.checkState(type == ItemType.TEXT, "Cursor not at text");
  }
}
