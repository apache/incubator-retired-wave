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

package org.waveprotocol.wave.client.gadget.renderer;

import static org.waveprotocol.wave.client.gadget.GadgetLog.log;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONValue;
import org.waveprotocol.wave.client.gadget.StateMap;

/**
 * Gadget user preferences container and parser class.
 *
 */
public final class GadgetUserPrefs extends StateMap {
  /**
   * External construction is banned.
   */
  protected GadgetUserPrefs() {
  }

  /**
   * Creates gadget state object.
   */
  public static GadgetUserPrefs create() {
    return StateMap.create().cast();
  }

  /**
   * Extracts default preference values from the gadget metadata JSON object
   * returned from GGS.
   *
   * @param prefs the preference JSON object received from GGS.
   */
  public void parseDefaultValues(JSONObject prefs) {
    if (prefs != null) {
      for (String pref : prefs.keySet()) {
        if (!has(pref)) {
          JSONObject prefJson = prefs.get(pref).isObject();
          if (prefJson != null) {
            JSONValue value = prefJson.get("default");
            if ((value != null) && (value.isString() != null)) {
              put(pref, value.isString().stringValue());
              log("Gadget pref '" + pref + "' = '" + get(pref) + "'");
            }
          } else {
            log("Invalid pref '" + pref + "' value in Gadget metadata.");
          }
        }
      }
    }
  }

  /**
   * Merges data from a state object containing another set of preference values.
   *
   * @param state state object with preference values to merge.
   * @param overwrite true to overwrite values in this object with values from
   *        the merged object; false to ignore values from the merged object if
   *        they are present in this object.
   */
  public void parse(StateMap state, final boolean overwrite) {
    if (state != null) {
      state.each(new StateMap.Each() {
        @Override
        public void apply(String key, String value) {
          setPref(key, value, overwrite);
        }
      });
    }
  }

  /**
   * Assigns an individual preference key.
   *
   * @param pref Preference name.
   * @param value JSON value assigned to this key.
   * @param overwrite If true, will overwrite any value already assigned to this
   *        key.
   */
  public void setPref(String pref, String value, boolean overwrite) {
    if (overwrite || has(pref)) {
      put(pref, value);
    }
  }
}
