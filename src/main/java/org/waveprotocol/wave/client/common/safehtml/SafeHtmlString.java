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

package org.waveprotocol.wave.client.common.safehtml;

//NOTE: In the near future, the files in this package will be open sourced as
//part of a different project. Do not rely on them staying here.

/**
 * A string wrapped as an object of type {@link SafeHtml}.
 *
 * <p>This class is package-private and intended for internal use by the
 * {@link org.waveprotocol.wave.client.common.safehtml} package.
 */
class SafeHtmlString implements SafeHtml {
  private String html;

  /**
   * Constructs a {@link SafeHtmlString} from a string.  Callers are responsible for ensuring that
   * the string passed as the argument to this constructor satisfies the constraints of the contract
   * imposed by the {@link SafeHtml} interface.
   *
   * @param html the string to be wrapped as a {@link SafeHtml}
   */
  SafeHtmlString(String html) {
    this.html = html;
  }

  /**
   * No-arg constructor for compatibility with GWT serialization.
   */
  SafeHtmlString() {
  }

  /** {@inheritDoc} */
  @Override
  public String asString() {
    return html;
  }
}

