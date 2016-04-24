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

package org.waveprotocol.wave.client.doodad.suggestion;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;

import org.waveprotocol.wave.client.common.safehtml.EscapeUtils;
import org.waveprotocol.wave.client.common.safehtml.SafeHtml;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;
import org.waveprotocol.wave.client.doodad.link.Link;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.ContentTextNode;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationHelper;
import org.waveprotocol.wave.client.editor.sugg.Menu;
import org.waveprotocol.wave.client.editor.sugg.SuggestionResources;
import org.waveprotocol.wave.client.editor.sugg.SuggestionsManager.HasSuggestions;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.document.util.RangeTracker;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Suggestion doodad.
 *
 * This class should be used to provide uniform set of suggestion for different
 * functions (spelling/linky lightbulb) inside the editor.
 *
 * TODO(user): Convert spelly to use this, that way this class will be forced
 * to become more general.
 *
 *
 */
public class Suggestion implements HasSuggestions {
  public static final Command NOOP_COMMAND = new Command() {
    @Override
    public void execute() {
    }
  };

  private static final Property<RangeTracker> LINK_RANGE_TRACKER_PROP =
    Property.immutable("link_rangetracker");

  private static final SuggestionResources resources = GWT.create(SuggestionResources.class);
  private static final List<Plugin> plugins = new ArrayList<Plugin>();

  private static final SafeHtml START_SUGGEST_LINK =
      EscapeUtils.fromSafeConstant("<a class='" + resources.css().sugg() + "'><span>");
  private static final SafeHtml END_SUGGEST_LINK = EscapeUtils.fromSafeConstant("</span></a>");

  /**
   * Debug string used as part of the range tracking key.
   */
  private static final String CURRENT_LINK_DEBUG_ID = "current_link";

  static {
    StyleInjector.inject(resources.css().getText());
  }

  /**
   * Registers the handlers and factory with the handler registry.
   */
  public static void register(ElementHandlerRegistry handlerRegistry) {
    SuggestionRenderer renderer = new SuggestionRenderer();
    renderer.register(handlerRegistry);
  }

  /** The element to which this Suggestion applies. */
  private final ContentElement contentElement;

  /**
   * Used to track the replacement range.
   */
  private final RangeTracker replacementRangeHelper;

  /**
   * Constructor.
   *
   * @param contentElement The element that has this suggestion.
   * @param documentContext
   */
  // TODO(mtsui/danilatos): Remove the dependency on content element here. This
  // might need the following to happen
  // 1) Change suggestion manager to avoid requiring the getSuggestionElement()
  // 2) The context required by populateSuggestionMenu should be passed in at that point
  public Suggestion(ContentElement contentElement,
      DocumentContext<ContentNode, ContentElement, ContentTextNode> documentContext) {
    this.contentElement = contentElement;
    this.replacementRangeHelper = getOrCreateRangeTracker(documentContext);
   }

  private RangeTracker getOrCreateRangeTracker(
      DocumentContext<ContentNode, ContentElement, ContentTextNode> context) {
    ContentElement docElement = context.document().getDocumentElement();
    RangeTracker ret =
        docElement.getProperty(LINK_RANGE_TRACKER_PROP);

    if (ret == null) {
      ret = new RangeTracker(context.localAnnotations(), CURRENT_LINK_DEBUG_ID);
      docElement.setProperty(LINK_RANGE_TRACKER_PROP, ret);
    }
    return ret;
  }

  @Override
  public ContentElement getSuggestionElement() {
    return contentElement;
  }

  @Override
  public void handleHideSuggestionMenu() {
    replacementRangeHelper.clearRange();
  }

  @Override
  public void handleShowSuggestionMenu() {
  }

  @Override
  public boolean isAccessibleFromKeyboard() {
    // TODO(user): should do this properly, but until then returning true is
    // fine for linky.
    return true;
  }

  /**
   * Find an annotation range for links.
   *
   * @param doc
   * @param element
   * @return returns the range or null if none is found.
   */
  private Range findLinkAnnotationRange(CMutableDocument doc, ContentElement element) {
    // By default, we will look for link keys.
    // TODO(user): Refactor this to not assume that we are interested
    // in just links.
    // Find range that covers contentElement and the annotation.
    for (String key : Link.LINK_KEYS) {
      Range range = AnnotationHelper.getRangePrecedingElement(doc, contentElement, key);
      if (range != null) {
        return range;
      }
    }
    return null;
  }

  @Override
  public void populateSuggestionMenu(final Menu menu) {
    // Some suggestions will replace the annotated range (links), if one is found,
    // track it to be passed along to the suggestion menu.
    Range replaceRange = findLinkAnnotationRange(contentElement.getMutableDoc(), contentElement);
    if (replaceRange != null) {
      replacementRangeHelper.trackRange(replaceRange);
    }
    Menu menuWrapper = new Menu() {
      @Override
      public MenuItem addItem(SafeHtml title, Command callback) {
        SafeHtmlBuilder builder = new SafeHtmlBuilder();
        builder.append(START_SUGGEST_LINK);
        builder.append(title);
        builder.append(END_SUGGEST_LINK);

        return menu.addItem(builder.toSafeHtml(), callback);
      }

      @Override
      public MenuItem addItem(String title, Command callback) {
        return addItem(EscapeUtils.fromString(title), callback);
      }
    };
    for (Plugin plugin : plugins) {
      plugin.populateSuggestionMenu(menuWrapper, replacementRangeHelper,
          contentElement.getMutableDoc(), contentElement);
    }
  }

  public static Attributes maybeCreateSuggestions(Map<String, Object> before) {
    StringMap<String> attributes = CollectionUtils.createStringMap();
    for (Plugin plugin : plugins) {
      plugin.maybeFillAttributes(before, attributes);
    }
    return new AttributesImpl(CollectionUtils.newJavaMap(attributes));
  }

  /**
   * Register a plugin that listens to annotations and populates a suggestion
   * menu.
   *
   * @param plugin
   */
  public static void registerPlugin(Plugin plugin) {
    plugins.add(plugin);
  }
}
