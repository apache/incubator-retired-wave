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

package com.google.wave.api;

import java.util.Map;

/**
 * Gadgets are external code that can be executed within a protected
 * environment within a Wave. Gadgets are indentified by the url that points to
 * their gadget specification. Gadgets can also maintain state that both they
 * and Robots can modify.
 */
public class Gadget extends Element {

  public static final String AUTHOR = "author";
  public static final String CATEGORY = "category";
  public static final String IFRAME = "ifr";
  public static final String PREF = "pref";
  public static final String THUMBNAIL = "thumbnail";
  public static final String TITLE = "title";
  public static final String URL = "url";

  /**
   * Constructs an empty gadget.
   */
  public Gadget() {
    super(ElementType.GADGET);
    setUrl("");
  }

  /**
   * Constructs a gadget with a given set of properties.
   *
   * @param properties the properties of the gadget.
   */
  public Gadget(Map<String, String> properties) {
    super(ElementType.GADGET, properties);
  }

  /**
   * Constructs a gadget for the specified url.
   *
   * @param url the url of the gadget specification.
   */
  public Gadget(String url) {
    super(ElementType.GADGET);
    setUrl(url);
  }

  /**
   * Returns the author of the gadget.
   *
   * @return the author of the gadget.
   */
  public String getAuthor() {
    return getProperty(AUTHOR);
  }

  /**
   * Changes the author of the gadget to the given author.
   *
   * @param author the new gadget author.
   */
  public void setAuthor(String author) {
    setProperty(AUTHOR, author);
  }

  /**
   * Returns the category of the gadget.
   *
   * @return the category of the gadget.
   */
  public String getCategory() {
    return getProperty(CATEGORY);
  }

  /**
   * Changes the cached IFrame source of the gadget.
   *
   * @param iframe the new cached gadget iframe source.
   */
  public void setIframe(String iframe) {
    setProperty(IFRAME, iframe);
  }

  /**
   * Returns the cached iframe source of the gadget.
   *
   * @return the cached iframe source of the gadget.
   */
  public String getIframe() {
    return getProperty(IFRAME);
  }

  /**
   * Changes the category of the gadget to the given category.
   *
   * @param category the new gadget category.
   */
  public void setCategory(String category) {
    setProperty(CATEGORY, category);
  }

  /**
   * Returns the pref of the gadget.
   *
   * @return the pref of the gadget.
   */
  public String getPref() {
    return getProperty(PREF);
  }

  /**
   * Changes the pref of the gadget to the given pref.
   *
   * @param pref the new gadget pref.
   */
  public void setPref(String pref) {
    setProperty(PREF, pref);
  }

  /**
   * Returns the thumbnail of the gadget.
   *
   * @return the thumbnail of the gadget.
   */
  public String getThumbnail() {
    return getProperty(THUMBNAIL);
  }

  /**
   * Changes the thumbnail of the gadget to the given thumbnail.
   *
   * @param thumbnail the new gadget thumbnail.
   */
  public void setThumbnail(String thumbnail) {
    setProperty(THUMBNAIL, thumbnail);
  }

  /**
   * Returns the title of the gadget.
   *
   * @return the title of the gadget.
   */
  public String getTitle() {
    return getProperty(TITLE);
  }

  /**
   * Changes the title of the gadget to the given title.
   *
   * @param title the new gadget title.
   */
  public void setTitle(String title) {
    setProperty(TITLE, title);
  }

  /**
   * Returns the URL for the gadget.
   *
   * @return the URL for the gadget.
   */
  public String getUrl() {
    return getProperty(URL);
  }

  /**
   * Changes the URL for the gadget to the given url. This will cause the new
   * gadget to be initialized and loaded.
   *
   * @param url the new gadget url.
   */
  public void setUrl(String url) {
    setProperty(URL, url);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * gadget with the given author.
   *
   * @param author the author to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByAuthor(String author) {
    return Restriction.of(AUTHOR, author);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * gadget with the given category.
   *
   * @param category the category to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByCategory(String category) {
    return Restriction.of(CATEGORY, category);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * gadget with the given cached iframe source.
   *
   * @param iframe the iframe source to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByIframe(String iframe) {
    return Restriction.of(IFRAME, iframe);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * gadget with the given pref.
   *
   * @param pref the pref to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByPref(String pref) {
    return Restriction.of(PREF, pref);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * gadget with the given thumbnail.
   *
   * @param thumbnail the thumbnail to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByThumbnail(String thumbnail) {
    return Restriction.of(THUMBNAIL, thumbnail);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * gadget with the given title.
   *
   * @param title the title to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByTitle(String title) {
    return Restriction.of(TITLE, title);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * gadget with the given URL.
   *
   * @param url the URL to filter.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByUrl(String url) {
    return Restriction.of(URL, url);
  }

  /**
   * Creates an instance of {@link Restriction} that can be used to search for
   * gadget with the given property.
   *
   * @param key the property key.
   * @param value the property value.
   * @return an instance of {@link Restriction}.
   */
  public static Restriction restrictByProperty(String key, String value) {
    return Restriction.of(key, value);
  }
}
