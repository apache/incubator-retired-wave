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

package org.waveprotocol.box.webclient.search;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;

/**
 * Loads all the Css resources needed by the search panel. The necessity of this
 * loader is due to the unfortunate asynchrony with GWT's style injection.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class SearchPanelResourceLoader {

  private final static SearchWidget.Resources search = GWT.create(SearchWidget.Resources.class);
  private final static SearchPanelWidget.Resources panel = GWT.create(SearchPanelWidget.Resources.class);
  private final static DigestDomImpl.Resources digest = GWT.create(DigestDomImpl.Resources.class);

  static {
    // Inject all CSS synchronously. CSS must be injected synchronously, so that
    // any layout queries, that may happen to occur in the same event cycle,
    // operate on the correct state (GWT's default injection mode is
    // asynchronous). CSS is injected together in one bundle to minimize layout
    // invalidation, and to leave open the possibility of merging stylesheets
    // together for efficiency.
    boolean isSynchronous = true;
    StyleInjector.inject(search.css().getText(), isSynchronous);
    StyleInjector.inject(panel.css().getText(), isSynchronous);
    StyleInjector.inject(digest.css().getText(), isSynchronous);
  }

  private SearchPanelResourceLoader() {
  }

  public static SearchWidget.Resources getSearch() {
    return search;
  }

  public static SearchPanelWidget.Resources getPanel() {
    return panel;
  }

  public static DigestDomImpl.Resources getDigest() {
    return digest;
  }

  /**
   * Loads all the CSS required by the search panel.
   */
  public static void loadCss() {
    // Static initializer does the work.
  }
}
