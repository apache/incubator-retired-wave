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

package org.waveprotocol.wave.model.richtext;

import org.waveprotocol.wave.model.richtext.RichTextTokenizerImpl.Token;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Mock implementation of the rich text tokenizer for testing.
 *
 */
class MockRichTextTokenizer implements RichTextTokenizer {

  private final List<Token> expectedTokens;

  private int currentTokenIndex = -1;

  /**
   * Constructor.
   */
  MockRichTextTokenizer() {
    expectedTokens = CollectionUtils.newArrayList();
  }

  private MockRichTextTokenizer(MockRichTextTokenizer o) {
    this.expectedTokens = CollectionUtils.newArrayList(o.expectedTokens);
    this.currentTokenIndex = o.currentTokenIndex;
  }

  /**
   * Append a token with data that will be present in the stream.
   *
   * @param token The token to expect.
   */
  void expectToken(Token token) {
    expectedTokens.add(token);
  }

  @Override
  public boolean hasNext() {
    return !expectedTokens.isEmpty() && currentTokenIndex < expectedTokens.size() - 1;
  }

  @Override
  public Type next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    return expectedTokens.get(++currentTokenIndex).getType();
  }

  @Override
  public Type getCurrentType() {
    return expectedTokens.get(currentTokenIndex).getType();
  }

  @Override
  public String getData() {
    return expectedTokens.get(currentTokenIndex).getData();
  }

  @Override
  public RichTextTokenizer copy() {
    return new MockRichTextTokenizer(this);
  }
}
