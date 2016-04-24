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

import static org.waveprotocol.wave.client.uibuilder.BuilderHelper.KIND_ATTRIBUTE;

import org.waveprotocol.wave.client.common.safehtml.EscapeUtils;
import org.waveprotocol.wave.client.common.safehtml.SafeHtml;
import org.waveprotocol.wave.client.common.safehtml.SafeHtmlBuilder;


/**
 * Helper for UiBuilders that produce HTML strings. This is a temporary utility
 * class for use only while HTML is manually specified, rather than generated
 * though an XML template.
 *
 */
public final class OutputHelper {

  // Utility class
  private OutputHelper() {
  }

  /**
   * Opens a div.
   *
   * @param builder output builder
   * @param id value for the HTML id attribute (or {@code null} for none)
   * @param clazz value for the CSS class attribute (or {@code null} for none)
   * @param kind value for the kind attribute (or {@code null} for none)
   */
  public static void open(SafeHtmlBuilder builder, String id, String clazz, String kind) {
    openWith(builder, "div", id, clazz, kind, null);
  }

  /**
   * Opens a span.
   *
   * @param builder output builder
   * @param id value for the HTML id attribute (or {@code null} for none)
   * @param clazz value for the CSS class attribute (or {@code null} for none)
   * @param kind value for the kind attribute (or {@code null} for none)
   */
  public static void openSpan(SafeHtmlBuilder builder, String id, String clazz, String kind) {
    openWith(builder, "span", id, clazz, kind, null);
  }

  /**
   * Opens a div, with some extra attributes.
   *
   * @param builder output builder
   * @param id value for the HTML id attribute (or {@code null} for none)
   * @param clazz value for the CSS class attribute (or {@code null} for none)
   * @param kind value for the kind attribute (or {@code null} for none)
   * @param extra extra HTML, which must be a compile-time safe string
   */
  public static void openWith(
      SafeHtmlBuilder builder, String id, String clazz, String kind, String extra) {
    openWith(builder, "div", id, clazz, kind, extra);
  }

  /**
   * Opens a span, with some extra attributes.
   *
   * @param builder output builder
   * @param id value for the HTML id attribute (or {@code null} for none)
   * @param clazz value for the CSS class attribute (or {@code null} for none)
   * @param kind value for the kind attribute (or {@code null} for none)
   * @param extra extra HTML, which must be a compile-time safe string
   */
  public static void openSpanWith(
      SafeHtmlBuilder builder, String id, String clazz, String kind, String extra) {
    openWith(builder, "span", id, clazz, kind, extra);
  }

  /**
   * Opens an element.
   *
   * @param tag tag for the element
   * @param builder output builder
   * @param id value for the HTML id attribute (or {@code null} for none)
   * @param clazz value for the CSS class attribute (or {@code null} for none)
   * @param kind value for the kind attribute (or {@code null} for none)
   * @param extra extra HTML, which must be a compile-time safe string
   */
  private static void openWith(SafeHtmlBuilder builder,
      String tag,
      String id,
      String clazz,
      String kind,
      String extra) {
    builder.appendHtmlConstant("<" + tag //
        + (id != null ? " id='" + id + "'" : "") //
        + (clazz != null ? " class='" + clazz + "'" : "")
        + (kind != null ? " " + BuilderHelper.KIND_ATTRIBUTE + "='" + kind + "'" : "")
        + (extra != null ? " " + extra : "") + ">");
  }

  /**
   * Closes the currently open div.
   */
  public static void close(SafeHtmlBuilder builder) {
    builder.appendHtmlConstant("</div>");
  }

  /**
   * Closes the currently open span.
   */
  public static void closeSpan(SafeHtmlBuilder builder) {
    builder.appendHtmlConstant("</span>");
  }

  /**
   * Opens and closes a div.
   *
   * @see #open(SafeHtmlBuilder, String, String, String)
   */
  public static void append(SafeHtmlBuilder builder, String id, String style, String kind) {
    open(builder, id, style, kind);
    close(builder);
  }

  /**
   * Opens and closes a span.
   *
   * @see #open(SafeHtmlBuilder, String, String, String)
   */
  public static void appendSpan(SafeHtmlBuilder builder, String id, String style, String kind) {
    openSpan(builder, id, style, kind);
    closeSpan(builder);
  }

  /**
   * Opens and closes a div.
   *
   * @see #openWith(SafeHtmlBuilder, String, String, String, String)
   */
  public static void appendWith(
      SafeHtmlBuilder builder, String id, String style, String kind, String extra) {
    openWith(builder, id, style, kind, extra);
    close(builder);
  }

  /**
   * Appends an image.
   *
   * @param url attribute-value safe URL
   * @param info attribute-value safe image information
   * @param style
   */
  public static void image(
      SafeHtmlBuilder builder, String id, String style, SafeHtml url, SafeHtml info, String kind) {
    String safeUrl = url != null ? EscapeUtils.sanitizeUri(url.asString()) : null;
    SafeHtml img = EscapeUtils.fromSafeConstant("<img " //
        + "id='" + id + "' " //
        + "class='" + style + "' " //
        + (safeUrl != null ? "src='" + safeUrl + "' " : "") //
        + (info != null ? " alt='" + info.asString() + "' title='" + info.asString() + "' " : "") //
        + (kind != null ? " " + KIND_ATTRIBUTE + "='" + kind + "'" : "") //
        + "></img>");
    builder.append(img);
  }

  public static void button(SafeHtmlBuilder builder,
      String id,
      String clazz,
      String kind,
      String title,
      String caption) {
    builder.appendHtmlConstant("<button " //
        + (id != null ? " id='" + id + "'" : "") //
        + (title != null ? " title='" + title + "'" : "") //
        + (clazz != null ? " class='" + clazz + "'" : "") //
        + (kind != null ? " " + BuilderHelper.KIND_ATTRIBUTE + "='" + kind + "'" : "") //
        + ">"+ caption + "</button>");
  }
}
