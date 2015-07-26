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
 * A JsonSerializer that is implemented by custom javascript code that
 * doesn't rely on browser support.
 *
 */
public class JavascriptJsonSerializer implements JsonSerializer {
  @Override
  public native JavaScriptObject parse(String json) /*-{
    return eval("(" + json + ")");
  }-*/;

  @Override
  public native String serialize(JavaScriptObject obj) /*-{
    var print = function (input, output) {
      if (input === null || input === undefined) {
        output.push("null");
      } else if (typeof input == "object" && input.constructor === Array) {
        var hasProperties = false;
        output.push("[");
        for (var property = 0; property < input.length; ++property) {
          arguments.callee(input[property], output);
          output.push(",");
          hasProperties = true;
        }
        if (hasProperties) {
          output.pop();
        }
        output.push("]");
      } else if (typeof input == "object") {
        var hasProperties = false;
        output.push("{");
        for (var property in input) {
          if (input.hasOwnProperty(property) && typeof input[property] != "function") {
            arguments.callee(property, output);
            output.push(":");
            arguments.callee(input[property], output);
            output.push(",");
            hasProperties = true;
          }
        }
        if (hasProperties) {
          output.pop();
        }
        output.push("}");
      } else if (typeof input == "string") {
        output.push(@com.google.gwt.core.client.JsonUtils::escapeValue(Ljava/lang/String;)(input));
      } else if (typeof input == "boolean" || typeof input == "number") {
        output.push(String(input));
      }
      // other types (such as function) are ignored.
    };

    var json = [];
    print(obj, json);
    return json.join("");
  }-*/;

}
