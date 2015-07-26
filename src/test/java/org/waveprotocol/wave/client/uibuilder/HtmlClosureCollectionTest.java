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

package org.waveprotocol.wave.client.uibuilder;


import junit.framework.TestCase;

import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;


public class HtmlClosureCollectionTest extends TestCase {
  static class SimpleUiBuilder implements UiBuilder {
    private final String content;

    public SimpleUiBuilder(String content) {
      this.content = content;
    }

    @Override
    public void outputHtml(SafeHtmlBuilder out) {
      out.appendPlainText(content);
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testInsertAtEnd() throws Exception {
    HtmlClosureCollection collection = new HtmlClosureCollection();
    StringBuffer expected = new StringBuffer();
    for (int i = 0; i < 100; i++) {
      String content = Integer.toString(i);
      collection.add(new SimpleUiBuilder(content));
      expected.append(content);
    }
    SafeHtmlBuilder output = new SafeHtmlBuilder();
    collection.outputHtml(output);
    assertEquals(expected.toString(), output.toSafeHtml().asString());
  }
}
