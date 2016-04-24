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

/**
 * Implements the JsonSerializer using JSON browser object.
 *
 */
public class BrowserAidedJsonSerailizer implements JsonSerializer {

  /**
   * @return true if the browser has support for Json objects so
   *      BrowserAidedJsonSerailizer can be used.
   */
  public static native boolean hasBrowserSupport() /*-{
    return typeof(JSON) == "object" && typeof(JSON.parse) == "function" &&
        typeof(JSON.stringify) == "function";
  }-*/;

  @Override
  public native JavaScriptObject parse(String str) /*-{
    return JSON.parse(str);
  }-*/;

  @Override
  public native String serialize(JavaScriptObject obj) /*-{
    return JSON.stringify(obj);
  }-*/;
}
