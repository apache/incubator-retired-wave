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

package org.waveprotocol.wave.model.gadget;

import static org.waveprotocol.wave.model.gadget.GadgetConstants.AUTHOR_ATTRIBUTE;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.CATEGORY_TAGNAME;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.KEY_ATTRIBUTE;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.PREFS_ATTRIBUTE;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.PREF_TAGNAME;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.STATE_ATTRIBUTE;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.STATE_TAGNAME;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.TAGNAME;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.TITLE_ATTRIBUTE;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.TITLE_TAGNAME;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.URL_ATTRIBUTE;
import static org.waveprotocol.wave.model.gadget.GadgetConstants.VALUE_ATTRIBUTE;

import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * Static methods to produce Wave Gadget XML elements.
 *
 */
public final class GadgetXmlUtil {

  private GadgetXmlUtil() {} // Non-instantiable.

  /**
   * Returns initialized XML string builder for the gadget.
   *
   * @param url that points to the XML definition of this gadget.
   * @return content XML string for the gadget.
   */
  public static XmlStringBuilder constructXml(String url, String author) {
    return constructXml(url, "", author);
  }

  /**
   * Returns initialized XML string builder for the gadget with initial prefs.
   *
   * @param url that points to the XML definition of this gadget.
   * @param prefs initial gadget preferences as escaped JSON string.
   * @return content XML string for the gadget.
   */
  public static XmlStringBuilder constructXml(String url, String prefs, String author) {
    return constructXml(url, prefs, author, null, null);
  }

  /**
   * Returns initialized XML string builder for the gadget with initial prefs.
   *
   * @param url that points to the XML definition of this gadget.
   * @param prefs initial gadget preferences as escaped JSON string.
   * @param categories array of category names for the gadget (e.g. ["game",
   *        "chess"]).
   * @param state the initial gadget state.
   * @return content XML string for the gadget.
   */
  public static XmlStringBuilder constructXml(String url, String prefs, String author,
      @Nullable String[] categories, @Nullable Map<String, String> state) {
    final XmlStringBuilder builder = XmlStringBuilder.createEmpty();
    if (categories != null) {
      for (int i = 0; i < categories.length; ++i) {
        builder.append(
            XmlStringBuilder.createText("").wrap(
                CATEGORY_TAGNAME, KEY_ATTRIBUTE, categories[i]));
      }
    }
    if (state != null) {
      for (Map.Entry<String, String> entry : state.entrySet()) {
        builder.append(
            GadgetXmlUtil.constructStateXml(entry.getKey(), entry.getValue()));
      }
    }
    builder.wrap(
        TAGNAME,
        URL_ATTRIBUTE, url,
        TITLE_ATTRIBUTE, "",
        PREFS_ATTRIBUTE, prefs != null ? prefs : "",
        STATE_ATTRIBUTE, "",
        // TODO(user): Implement finer author management for gadgets.
        AUTHOR_ATTRIBUTE, author);
    return builder;
  }

  /**
   * Returns initialized XML string builder for a gadget categories element.
   *
   * @param categories the categories.
   * @return XML builder for the name element.
   */
  public static XmlStringBuilder constructCategoriesXml(String categories) {
    return XmlStringBuilder.createText("")
                           .wrap(CATEGORY_TAGNAME, KEY_ATTRIBUTE, categories);
  }

  /**
   * Returns initialized XML string builder for a gadget title element.
   *
   * @param title the title value
   * @return XML builder for the title element
   */
  public static XmlStringBuilder constructTitleXml(String title) {
    return XmlStringBuilder.createText("").wrap(TITLE_TAGNAME, VALUE_ATTRIBUTE, title);
  }

  /**
   * Returns initialized XML string builder for a gadget element.
   *
   * @param tag element tag
   * @param key the key
   * @param value the value
   * @return XML string builder for the state element
   */
  public static XmlStringBuilder constructElementXml(String tag, String key, String value) {
    return XmlStringBuilder.createText("").wrap(tag, KEY_ATTRIBUTE, key, VALUE_ATTRIBUTE, value);
  }

  /**
   * Returns initialized XML string builder for a gadget state element.
   *
   * @param key the key
   * @param value the value
   * @return XML string builder for the state element
   */
  public static XmlStringBuilder constructStateXml(String key, String value) {
    return XmlStringBuilder.createText("").wrap(
        STATE_TAGNAME, KEY_ATTRIBUTE, key, VALUE_ATTRIBUTE, value);
  }

  /**
   * Returns initialized XML string builder for a gadget user pref element.
   *
   * @param key the key
   * @param value the value
   * @return XML string builder for the state element
   */
  public static XmlStringBuilder constructPrefXml(String key, String value) {
    return XmlStringBuilder.createText("").wrap(
        PREF_TAGNAME, KEY_ATTRIBUTE, key, VALUE_ATTRIBUTE, value);
  }
}
