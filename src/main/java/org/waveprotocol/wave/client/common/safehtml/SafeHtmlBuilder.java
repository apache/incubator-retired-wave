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

/** A builder that facilitates the building up of XSS-safe HTML from text
 * snippets.  It is used essentially like a {@link StringBuilder}; unlike a
 * {@link StringBuilder}, it automatically HTML-escapes appended input where
 * necessary.
 *
 * <p>In addition, it supports methods that allow strings with HTML markup to be
 * appended without escaping: One can append other {@link SafeHtml} objects, and
 * one can append constant strings.  The method that appends constant strings
 * ({@link #appendHtmlConstant(String)}) requires a convention of use to be
 * adhered to in order for this class to adhere to the contract required by
 * {@link SafeHtml}.
 *
 * <p>The accumulated XSS-safe HTML can be obtained in the form of a {@link
 * SafeHtml} via the {@link #toSafeHtml()} method.
 *
 * <p>This class is not thread-safe.
 */
public final class SafeHtmlBuilder {

  private final StringBuilder sb = new StringBuilder();

  /**
   * Constructs an empty SafeHtmlBuilder.
   */
  public SafeHtmlBuilder() {
  }

  /**
   * Returns the safe HTML accumulated in the builder as a {@link SafeHtml}.
   */
  public SafeHtml toSafeHtml() {
    return new SafeHtmlString(sb.toString());
  }

  /**
   * Appends a compile-time-constant string, which will <em>not</em> be escaped.
   *
   * <p><b>Important</b>: For this class to be able to honour its contract as required by {@link
   * SafeHtml}, all uses of this method must satisfy the following requirements:
   *
   * <ul>
   *
   * <li>The argument expression must be fully determined and known to be safe at
   * compile time.
   *
   * <li>The value of the argument must not contain incomplete HTML tags. I.e., the following is not
   * a correct use of this method, because the {@code <a>} tag is incomplete:
   * <pre class="code">{@code shb.appendConstantHtml("<a href='").append(url)}</pre>
   *
   * </ul>
   *
   * @param html the HTML snippet to be appended
   * @return a reference to this object
   */
  public SafeHtmlBuilder appendHtmlConstant(String html) {
    // TODO(user): (hosted-mode only) assert that html satisfies the second constraint.
    sb.append(html);
    return this;
  }

  /**
   * Appends the contents of another {@link SafeHtml} object, without applying HTML-escaping to it.
   *
   * @param html the {@link SafeHtml} to append
   * @return a reference to this object
   */
  public SafeHtmlBuilder append(SafeHtml html) {
    sb.append(html.asString());
    return this;
  }

  /**
   * Appends a string after HTML-escaping it.
   *
   * @param text the string to append
   * @return a reference to this object
   */
  public SafeHtmlBuilder appendEscaped(String text) {
    sb.append(EscapeUtils.htmlEscape(text));
    return this;
  }

  /**
   * Appends a string consisting of several newline-separated lines
   * after HTML-escaping it.  Newlines in the original string are
   * converted to {@code <br>}.
   *
   * @param text the string to append
   * @return a reference to this object
   */
  public SafeHtmlBuilder appendEscapedLines(String text) {
    sb.append(EscapeUtils.htmlEscape(text).replaceAll("\n", "<br>"));
    return this;
  }

  /**
   * Appends a plain text string that does not contain any HTML elements.
   *
   * @param text the string to append
   * @return a reference to this object
   */
  public SafeHtmlBuilder appendPlainText(String text) {
    // TODO(user) assert text does not contain any HTML elements
    // TODO(user) verify that this is actually faster than calling htmlEscape()
    sb.append(text);
    return this;
  }

  /*
   * Boolean and numeric types converted to String are always HTML safe -- no escaping necessary.
   */

  /**
   * Appends the string representation of a boolean.
   *
   * @param b the boolean whose string representation to append
   * @return a reference to this object
   */
  public SafeHtmlBuilder append(boolean b) {
    sb.append(b);
    return this;
  }

  /**
   * Appends the string representation of a char.
   *
   * @param num the number whose string representation to append
   * @return a reference to this object
   */
  public SafeHtmlBuilder append(char num) {
    sb.append(num);
    return this;
  }

  /**
   * Appends the string representation of a number.
   *
   * @param num the number whose string representation to append
   * @return a reference to this object
   */
  public SafeHtmlBuilder append(int num) {
    sb.append(num);
    return this;
  }

  /**
   * Appends the string representation of a number.
   *
   * @param num the number whose string representation to append
   * @return a reference to this object
   */
  public SafeHtmlBuilder append(byte num) {
    sb.append(num);
    return this;
  }

  /**
   * Appends the string representation of a number.
   *
   * @param num the number whose string representation to append
   * @return a reference to this object
   */
  public SafeHtmlBuilder append(long num) {
    sb.append(num);
    return this;
  }

  /**
   * Appends the string representation of a number.
   *
   * @param num the number whose string representation to append
   * @return a reference to this object
   */
  public SafeHtmlBuilder append(float num) {
    sb.append(num);
    return this;
  }

  /**
   * Appends the string representation of a number.
   *
   * @param num the number whose string representation to append
   * @return a reference to this object
   */
  public SafeHtmlBuilder append(double num) {
    sb.append(num);
    return this;
  }
}
