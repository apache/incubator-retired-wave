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
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayBoolean;
import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.JsArrayString;

/**
 * Utility to sort java.com.gwt.core.client.JsArray* classes.
 *
 * NOTE(user):
 * This is done as a static helper class rather than within the JsArray*
 * classes, to avoid passing in functors to a JSNI method (which make knorton
 * and jgw nervous).
 *
 */
public class JsArraySort {
  /**
   * Utility class, so private constructor.
   */
  private JsArraySort() {
  }

  /**
   * Sorts a JsArray of type T.
   *
   * @param sortMe Array to be sorted.
   * @param comparator Comparator to be used, per native JS sort() method.
   */
  public static native <T extends JavaScriptObject> void sort(JsArray<T> sortMe,
                                                              JavaScriptObject comparator) /*-{
    sortMe.sort(comparator);
  }-*/;

  /**
   * Sorts a JsArray of booleans.
   *
   * @param sortMe Array to be sorted.
   * @param comparator Comparator to be used, per native JS sort() method.
   */
  public static native void sort(JsArrayBoolean sortMe, JavaScriptObject comparator) /*-{
    sortMe.sort(comparator);
  }-*/;

  /**
   * Sorts a JsArray of integers.
   *
   * @param sortMe Array to be sorted.
   * @param comparator Comparator to be used, per native JS sort() method.
   */
  public static native void sort(JsArrayInteger sortMe, JavaScriptObject comparator) /*-{
    sortMe.sort(comparator);
  }-*/;

  /**
   * Sorts a JsArray of doubles.
   *
   * @param sortMe Array to be sorted.
   * @param comparator Comparator to be used, per native JS sort() method.
   */
  public static native void sort(JsArrayNumber sortMe, JavaScriptObject comparator) /*-{
    sortMe.sort(comparator);
  }-*/;

  /**
   * Sorts a JsArray of strings.
   *
   * @param sortMe Array to be sorted.
   * @param comparator Comparator to be used, per native JS sort() method.
   */
  public static native void sort(JsArrayString sortMe, JavaScriptObject comparator) /*-{
    sortMe.sort(comparator);
  }-*/;
}
