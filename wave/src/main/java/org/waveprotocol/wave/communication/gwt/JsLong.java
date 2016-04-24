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

package org.waveprotocol.wave.communication.gwt;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.wave.communication.json.JsonLongHelper;


/**
 * This code wraps around a long value so that it can be transmitted efficiently
 * over the wire.  long value is sent as a pair of integers beause JSON doesn't
 * support 64bit long values.
 *
 */
public final class JsLong extends JavaScriptObject {
  /** hidden constructor */
  protected JsLong() {
  }

  /**
   * @param value
   * @return a new JsLong representation of the long
   */
  public static JsLong create(long value) {
    return toJavaScriptObject(JsonLongHelper.getHighWord(value),
        JsonLongHelper.getLowWord(value));
  }

  /**
   * @return the long value represented by this JavaScriptObject
   */
  public long toLong() {
    return JsonLongHelper.toLong(getHighInt(this), getLowInt(this));
  }

  private static native int getLowInt(JavaScriptObject obj) /*-{
    return obj[
        @org.waveprotocol.wave.communication.json.JsonLongHelper::LOW_WORD_INDEX
        ];
  }-*/;

  private static native int getHighInt(JavaScriptObject obj) /*-{
    return obj[
        @org.waveprotocol.wave.communication.json.JsonLongHelper::HIGH_WORD_INDEX
        ];
  }-*/;

  private static native JsLong toJavaScriptObject(int highInt, int lowInt) /*-{
    return [lowInt, highInt];
  }-*/; 
}
