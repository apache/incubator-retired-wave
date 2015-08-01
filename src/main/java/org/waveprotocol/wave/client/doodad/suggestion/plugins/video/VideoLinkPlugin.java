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

package org.waveprotocol.wave.client.doodad.suggestion.plugins.video;

import org.waveprotocol.wave.client.doodad.link.Link;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.suggestion.Plugin;
import org.waveprotocol.wave.client.doodad.suggestion.misc.GadgetCommand;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.sugg.Menu;
import org.waveprotocol.wave.client.gadget.StateMap;
import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.document.util.RangeTracker;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.Map;

/**
 * Plugin for suggestion that recognizes videos and adds video specific
 * suggestions.
 *
 *
 */
public class VideoLinkPlugin implements Plugin {
  /**
   * Links with this prefix will trigger the youtube embed suggestion.
   * The following sample URLs are accepted:
   * http://www.youtube.com/watch?v=url_friendly_base64-id#something
   * http://www.youtube.com/watch#v=url_friendly_base64-id&something
   * http://www.youtube.com/watch#!v=url_friendly_base64-id!something
   */
  private static final String YOUTUBE_PREFIX = "^http:\\/\\/www\\.youtube\\.com\\/watch(\\?|#!?)v=";
  private static final String YOUTUBE_SUFFIX = "[^\\w\\-].*";

  // @NotInternationalized
  private static final String EMBED_VIDEO = "Embed video";

  private static final String MOVIE_ATTR = "movie";

  private static final String MOVIE_GADGET_URL =
      "http://wave-api.appspot.com/public/gadgets/youtube/youtube.xml";
  private static final String GADGET_MOVIE_KEY = "movie";

  private static VideoLinkPlugin instance = null;

  /**
   * Returns the singleton instance of the plugin.
   */
  public static Plugin get() {
    if (instance == null) {
      instance = new VideoLinkPlugin();
    }
    return instance;
  }

  @Override
  public void maybeFillAttributes(Map<String, Object> before, final StringMap<String> attributes) {
    String url = LinkAnnotationHandler.getLink(before);
    if (url == null) {
      return;
    }
    String movieId = url.replaceFirst(YOUTUBE_PREFIX, "");
    if (movieId.length() == url.length()) {
      // Prefix is not recognized.
      return;
    }
    // Cut off the tail after the video parameter. There could be other parameters, fragment, or
    // other uninteresting information after the ID parameter. The parameter order isn't strictly
    // enforced, but we assume that the prefix ends in v=, so this is safe.
    movieId = movieId.replaceFirst(YOUTUBE_SUFFIX, "");
    if (movieId.isEmpty()) {
      return;
    }
    attributes.put(MOVIE_ATTR, movieId);
  }

  @Override
  public void populateSuggestionMenu(Menu menu, RangeTracker replacementRangeHelper,
      CMutableDocument mutableDocument, ContentElement element) {
    final String movieAttribute = element.getAttribute(MOVIE_ATTR);

    if (movieAttribute != null) {
      final StateMap stateMap = StateMap.create();
      stateMap.put(GADGET_MOVIE_KEY, movieAttribute);

      menu.addItem(EMBED_VIDEO,
          new GadgetCommand<ContentNode, ContentElement, ContentTextNode>(MOVIE_GADGET_URL,
              stateMap, mutableDocument, AnnotationConstants.LINK_AUTO, replacementRangeHelper));
    }
  }
}
