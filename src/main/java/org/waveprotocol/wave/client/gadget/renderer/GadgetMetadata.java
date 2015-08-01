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

package org.waveprotocol.wave.client.gadget.renderer;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import org.waveprotocol.wave.client.gadget.StateMap;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;

/**
 * Gadget metadata container and parser class.
 *
 *  TODO(user): Replace gwt JSON classes with either overlay types or other
 * lightweight alternative.
 *
 *  TODO(davidbyttow): Reduce this class to just metadata and pull some of the
 * data into a single GadgetData class used by Gadget widgets.
 *
 *  TODO(davidbyttow): Unit tests.
 *
 *  TODO(davidbyttow): Factor out the parsing code, possibly use GWT javascript
 * objects instead.
 *
 */
public class GadgetMetadata {
  /** Gadget View info. */

  /** Metadata fields. */
  private String iframeUrl;
  private String url;
  private String moduleId;
  private String title;
  private String titleUrl;
  private String directoryTitle;
  private String thumbnail;
  private String screenshot;
  private String author;
  private String authorEmail;
  private Long height;
  private Long width;
  private Boolean scrolling;

  /** Gadget View info. */
  private final StringMap<View> views = CollectionUtils.createStringMap();

  /**
   * Encapsulates gadget view information.
   */
  private static class View {
    private String type;
    private Long preferredHeight;
    private Long preferredWidth;
  }

  /** User Prefs object. */
  private final GadgetUserPrefs userPrefs = GadgetUserPrefs.create();

  /**
   * Constructs the object from metadata JSON.
   *
   * @param metadataJson JSON object to extract the data from.
   */
  public GadgetMetadata(JSONObject metadataJson) {
    parse(metadataJson);
  }

  /**
   * Helper function to extract a string value from given JSON object.
   *
   * @param json JSON object to extract the value from.
   * @param key key of the value to extract.
   * @return the string object extracted from JSON (can be null if the value
   *         does not exist or is invalid.
   */
  private static String getJsonStringValue(JSONObject json, String key) {
    JSONValue value = json.get(key);
    JSONString string = (value == null) ? null : value.isString();
    if (string != null) {
      return string.stringValue();
    } else {
      return null;
    }
  }

  /**
   * Helper function to extract a long value from given JSON object.
   *
   * @param json JSON object to extract the value from.
   * @param key key of the value to extract.
   * @return the Long object extracted from JSON (can be null if the value does
   *         not exist or is invalid.
   */
  private static Long getJsonLongValue(JSONObject json, String key) {
    JSONValue value = json.get(key);
    JSONNumber number = (value == null) ? null : value.isNumber();
    if (number != null) {
      return Math.round(number.doubleValue());
    } else {
      return null;
    }
  }

  /**
   * Helper function to extract a boolean value from given JSON object.
   *
   * @param json JSON object to extract the value from.
   * @param key key of the value to extract.
   * @return the Boolean object extracted from JSON (can be null if the value
   *         does not exist or is invalid.
   */
  private static Boolean getJsonBooleanValue(JSONObject json, String key) {
    JSONValue value = json.get(key);
    JSONBoolean bool = (value == null) ? null : value.isBoolean();
    if (bool != null) {
      return bool.booleanValue();
    } else {
      return null;
    }
  }

  /**
   * Helper function to extract a JSONObject from a JSONValue if it exists.
   *
   * @param json JSON object to extract the value from.
   * @param key key of the value to extract.
   * @return the JSONObject if it exists.
   */
  private static JSONObject getJsonObjectValue(JSONObject json, String key) {
    JSONValue value = json.get(key);
    return (value != null) ? value.isObject() : null;
  }

  /**
   * Parses JSON object to extract metadata information.
   *
   * @param gadget JSON object returned in the gadget field from the server.
   */
  public void parse(JSONObject gadget) {
    iframeUrl = getJsonStringValue(gadget, "iframeUrl");
    // Get rid of the protocol as we should rely on the browser to do the right
    // thing with "//" as the beginning of the url.
    // By doing this it also works around an issue in shindig where it shouldn't
    // put "//" in front of "http://" see:
    // https://issues.apache.org/jira/browse/SHINDIG-1460?page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel&focusedCommentId=12928110#action_12928110
    // This work around may no longer be needed on the next version of shindig
    // Added null check on iframeUrl.Issue #166:Client shiny on broken gadget.
    if(iframeUrl != null) {
      iframeUrl = iframeUrl.replaceFirst("^//http[s]?://", "//");
    }
    url = getJsonStringValue(gadget, "url");
    moduleId = getJsonStringValue(gadget, "moduleId");
    title = getJsonStringValue(gadget, "title");
    titleUrl = getJsonStringValue(gadget, "titleUrl");
    directoryTitle = getJsonStringValue(gadget, "directoryTitle");
    thumbnail = getJsonStringValue(gadget, "thumbnail");
    screenshot = getJsonStringValue(gadget, "screenshot");
    author = getJsonStringValue(gadget, "author");
    authorEmail = getJsonStringValue(gadget, "authorEmail");
    height = getJsonLongValue(gadget, "height");
    width = getJsonLongValue(gadget, "width");
    scrolling = getJsonBooleanValue(gadget, "scrolling");
    // Added null check on gadget.get("userPrefs").Issue #166:Client shiny on broken gadget.
    if(gadget.get("userPrefs") != null) {
      userPrefs.parseDefaultValues(gadget.get("userPrefs").isObject());
    }
    JSONObject gadgetViews = getJsonObjectValue(gadget, "views");
    if (gadgetViews != null) {
      View lastView = null;
      for (String viewName : gadgetViews.keySet()) {
        JSONObject viewJson = gadgetViews.get(viewName).isObject();
        View v = new View();
        v.type = viewName;
        v.preferredHeight = getJsonLongValue(viewJson, "preferredHeight");
        v.preferredWidth = getJsonLongValue(viewJson, "preferredWidth");
        views.put(v.type, v);
        lastView = v;
      }
    }
  }

  /** Metadata field accessors. */
  public boolean hasView(String view) {
    return views.containsKey(view);
  }

  public String getIframeUrl(String view) {
    return iframeUrl;
  }

  public boolean hasPreferredWidth(String view) {
    View v = views.get(view);
    return v != null && v.preferredWidth != null;
  }

  public long getPreferredWidth(String view) {
    View v = views.get(view);
    if (v == null) {
      return 0;
    }
    return v.preferredWidth;
  }

  public boolean hasPreferredHeight(String view) {
    View v = views.get(view);
    return v != null && v.preferredHeight != null;
  }

  public long getPreferredHeight(String view) {
    View v = views.get(view);
    if (v == null) {
      return 0;
    }
    return v.preferredHeight;
  }

  public boolean hasIframeUrl() {
    return iframeUrl != null;
  }

  public String getIframeUrl() {
    return iframeUrl;
  }

  public boolean hasUrl() {
    return url != null;
  }

  public String getUrl() {
    return url;
  }

  public boolean hasModuleId() {
    return moduleId != null;
  }

  public String getModuleId() {
    return moduleId;
  }

  public boolean hasTitle() {
    return title != null;
  }

  public String getTitle() {
    return title;
  }

  public boolean hasTitleUrl() {
    return titleUrl != null;
  }

  public String getTitleUrl() {
    return titleUrl;
  }

  public boolean hasHeight() {
    return height != null;
  }

  public long getHeight() {
    return height;
  }

  public boolean hasWidth() {
    return width != null;
  }

  public long getWidth() {
    return width;
  }

  public boolean hasScrolling() {
    return scrolling != null;
  }

  public Boolean getScrolling() {
    return scrolling;
  }

  public boolean hasDirectoryTitle() {
    return directoryTitle != null;
  }

  public String getDirectoryTitle() {
    return directoryTitle;
  }

  public boolean hasThumbnail() {
    return thumbnail != null;
  }

  public String getThumbnail() {
    return thumbnail;
  }

  public boolean hasAuthor() {
    return author != null;
  }

  public String getAuthor() {
    return author;
  }

  public boolean hasAuthorEmail() {
    return authorEmail != null;
  }

  public String getAuthorEmail() {
    return authorEmail;
  }

  public boolean hasScreenshot() {
    return screenshot != null;
  }

  public String getScreenshot() {
    return screenshot;
  }

  public StateMap getUserPrefs() {
    return userPrefs;
  }

  /**
   * @returns Set of views implemented by the gadget.
   */
  public ReadableStringSet getViewSet() {
    return views.keySet();
  }
}
