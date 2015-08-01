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

package org.waveprotocol.wave.client.editor.selection.html;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Represents the IE selection object
 *
 * All the interface methods directly map to those of the IE native object:
 *
 * http://msdn.microsoft.com/en-us/library/ms535869(VS.85).aspx#
 *
 * Add more methods as needed.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class JsSelectionIE extends JavaScriptObject {

  /**
   * Types reported by getType
   *
   * Do not change the spelling or capitalisation of these, because
   * their name() method depends on it.
   */
  public enum Type {
    /***/
    None,
    /***/
    Text,
    /***/
    Control,
    ;
  }

  /**
   * @param name
   * @return the Type matching name
   */
  public static Type getType(String name) {
    return java.lang.Enum.valueOf(Type.class, name);
  }

  protected JsSelectionIE() {
  }

  /**
   * @return the current selection
   */
  public static native JsSelectionIE get() /*-{
    return $doc.selection;
  }-*/;

  /**
   * @return 'type' property of the selection
   */
  public final native Type getType() /*-{
    return (
        @org.waveprotocol.wave.client.editor.selection.html.JsSelectionIE::getType(Ljava/lang/String;)
            (this.type));
  }-*/;

  /**
   * @return A JsTextRangeIE representing the selection
   */
  public final native JsTextRangeIE createRange() /*-{
    return this.createRange();
  }-*/;

  /**
   * Clears the selection (without changes to the document)
   */
  public final native void empty() /*-{
    this.empty();
  }-*/;

}
