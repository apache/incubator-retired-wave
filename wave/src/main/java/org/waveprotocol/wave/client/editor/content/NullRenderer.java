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

package org.waveprotocol.wave.client.editor.content;

import com.google.gwt.dom.client.Element;

/**
 * A renderer that produces no html implementation by design. Its purpose is to
 * be used with nodes that provide meta-information for a document (For example,
 * the click tracking on a button).
 *
 */
public class NullRenderer implements Renderer {

  /** Singleton instance */
  public static NullRenderer INSTANCE = new NullRenderer();

  @Override
  public Element createDomImpl(Renderable element) {
    return null;
  }

  // Singleton
  private NullRenderer() {
  }
}
