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

package org.waveprotocol.wave.client.common.util;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import org.waveprotocol.wave.model.util.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * GWT/JS implementation of the regular expression interface. Uses JSNI
 * to call JS methods.
 *
 */
public class JsRegExp implements RegExp {

  private final JavaScriptObject regExp;

  /**
   * Compiles a javascript regular expression. Returns null if the pattern
   * failed to compile.
   */
  private static native JavaScriptObject compileRegExp(String pattern, String flags) /*-{
    try {
      return new RegExp(pattern, flags);
    } catch (e) {
    }
    return null;
  }-*/;

  private static native boolean test(JavaScriptObject regExp, String test) /*-{
    return regExp.test(test);
  }-*/;

  private static native JsArrayString matches(JavaScriptObject regExp, String test) /*-{
    return test.match(regExp);
  }-*/;

  /**
   * Creates a local matching regular expression. Throws an
   * {@link IllegalArgumentException} if a bad pattern is given.
   *
   * @param pattern the pattern to match against.
   * @return JS regular expression object.
   */
  public static JsRegExp createLocal(String pattern) {
    JavaScriptObject jso = compileRegExp(pattern, "");
    Preconditions.checkArgument(jso != null, "Bad regex pattern");
    return new JsRegExp(jso);
  }

  /**
   * Creates a global matching regular expression. Throws an
   * {@link IllegalArgumentException} if a bad pattern is given.
   *
   * @param pattern the pattern to match against.
   * @return JS regular expression object.
   */
  public static JsRegExp createGlobal(String pattern) {
    JavaScriptObject jso = compileRegExp(pattern, "g");
    Preconditions.checkArgument(jso != null, "Bad regex pattern");
    return new JsRegExp(jso);
  }

  /**
   * Tests whether a given pattern is valid or not.
   *
   * @param pattern the pattern to match against.
   * @return true if the pattern is valid.
   */
  public static boolean isValidPattern(String pattern) {
    JavaScriptObject jso = compileRegExp(pattern, "");
    return jso != null;
  }

  private JsRegExp(JavaScriptObject regExp) {
    this.regExp = regExp;
  }

  @Override
  public boolean test(String test) {
    return test(regExp, test);
  }

  @Override
  public List<String> getMatches(String test) {
    JsArrayString matches = matches(regExp, test);
    if (matches != null && matches.length() > 0) {
      List<String> result = new ArrayList<String>(matches.length());
      for (int i = 0; i < matches.length(); ++i) {
        result.add(matches.get(i));
      }
      return result;
    }
    return new ArrayList<String>();
  }
}
