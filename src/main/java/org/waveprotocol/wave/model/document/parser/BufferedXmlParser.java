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

import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

/**
 * XmlPullParser implementation that parses and validates input eagerly.
 *
 */
public class BufferedXmlParser implements SafeXmlPullParser {
  private final StreamingXmlParser parserImpl;

  /** Parsed tokens. */
  private final Queue<Item> tokens = new LinkedList<Item>();

  /** The current token. */
  private Item current;

  public BufferedXmlParser(String input) throws XmlParseException {
    parserImpl = new StreamingXmlParser(input);
    parse();
  }

  @Override
  public StringMap<String> getAttributes() throws IllegalStateException {
    return current.getAttributes();
  }

  @Override
  public ItemType getCurrentType() throws IllegalStateException {
    return current != null ? current.type : null;
  }

  @Override
  public String getTagName() throws IllegalStateException {
    return current.getTagName();
  }

  @Override
  public String getText() throws IllegalStateException {
    return current.getText();
  }

  @Override
  public String getProcessingInstructionName() throws IllegalStateException {
    return current.getProcessingInstructionName();
  }

  @Override
  public String getProcessingInstructionValue() throws IllegalStateException {
    return current.getProcessingInstructionValue();
  }

  @Override
  public ItemType next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    current = tokens.poll();
    return getCurrentType();
  }

  @Override
  public boolean hasNext() {
    return !tokens.isEmpty();
  }

  void parse() throws XmlParseException {
    try {
      while (parserImpl.hasNext()) {
        parserImpl.next();
        tokens.add(parserImpl.getCurrentItem());
      }
    } catch (RuntimeXmlParseException e) {
      throw new XmlParseException(e);
    }
  }

  @Override
  public void remove() {
    Preconditions.illegalState("Not implemented");
  }
}
