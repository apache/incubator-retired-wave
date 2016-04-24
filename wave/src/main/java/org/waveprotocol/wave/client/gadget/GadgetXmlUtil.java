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

package org.waveprotocol.wave.client.gadget;

import com.google.gwt.http.client.URL;

import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

/**
 * Static methods to produce Wave Gadget XML elements.
 *
 */
public final class GadgetXmlUtil {

  private GadgetXmlUtil() {} // Non-instantiable.

  /**
   * Returns initialized XML string builder for the gadget with initial prefs.
   *
   * @param url that points to the XML definition of this gadget.
   * @param stateMap initial preference state map for this gadget.
   * @return content XML string for the gadget.
   */
  public static XmlStringBuilder constructXml(String url, StateMap stateMap, String loginName) {
    return constructXml(url, stateMap != null ? URL.encodeComponent(stateMap.toJson()) : "",
        loginName);
  }

  /**
   * Returns initialized XML string builder for the gadget with initial prefs.
   *
   * @param url that points to the XML definition of this gadget.
   * @param prefs initial gadget preferences as escaped JSON string.
   * @return content XML string for the gadget.
   */
  public static XmlStringBuilder constructXml(String url, String prefs, String loginName) {
    return constructXml(url, prefs, null, loginName);
  }

  /**
   * Returns initialized XML string builder for the gadget with initial prefs.
   *
   * @param url that points to the XML definition of this gadget.
   * @param prefs initial gadget preferences as escaped JSON string.
   * @param categories array of category names for the gadget (e.g. ["game",
   *        "chess"]).
   * @return content XML string for the gadget.
   */
  public static XmlStringBuilder constructXml(String url, String prefs, String[] categories,
      String loginName) {
    return org.waveprotocol.wave.model.gadget.GadgetXmlUtil.constructXml(url, prefs,
        loginName, categories, null);
  }
}
