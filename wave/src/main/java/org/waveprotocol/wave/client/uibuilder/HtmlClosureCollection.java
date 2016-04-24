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

import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.model.util.CollectionUtils;

import java.util.Collection;

/**
 * An HtmlClosure composed of a list of HtmlClosures.
 *
 */
public final class HtmlClosureCollection implements HtmlClosure {

  /** Collection of closures, lazily created. */
  private Collection<HtmlClosure> children;

  /**
   * Creates a closures collection.
   */
  public HtmlClosureCollection() {
  }

  /**
   * Adds a closure to this collection.
   *
   * @param child closure to add
   */
  public void add(HtmlClosure child) {
    if (children == null) {
      children = CollectionUtils.createQueue();
    }
    children.add(child);
  }

  @Override
  public void outputHtml(SafeHtmlBuilder out) {
    if (children != null) {
      for (HtmlClosure builder : children) {
        builder.outputHtml(out);
      }
    }
  }
}
