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

/**
 * Common type-independent methods for optimised JSO maps
 *
 * Designed to be subclassed, but also useful in its own right for its methods
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public abstract class JsoMapBase extends JavaScriptObject {

  protected JsoMapBase() {}

  /**
   * Removes all entries from this map.
   */
  public final native void clear() /*-{
    for (var key in this) {
      delete this[key];
    }
  }-*/;

  /**
   * Tests whether this map is empty.
   *
   * @return true if this map has no entries.
   */
  public final native boolean isEmpty() /*-{
    for (var k in this) {
      return false;
    }
    return true;
  }-*/;

  /**
   * Counts the number of entries in this map.  This is a time-consuming
   * operation.
   */
  public final native int countEntries() /*-{
    var n = 0;
    for (var k in this) {
      n++;
    }
    return n;
  }-*/;

}
