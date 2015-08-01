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

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProvider.GadgetCategoryType;
import org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget.GadgetInfoProvider.GadgetInfo;

import java.util.ArrayList;
import java.util.List;

public class GwtGadgetInfoParser implements GadgetInfoParser {

  @Override
  public List<GadgetInfo> parseGadgetInfoJson(String json) {
    List<GadgetInfo> gadgetList = new ArrayList<GadgetInfo>();
    JSONValue value = JSONParser.parseStrict(json);
    JSONArray array = value.isArray();
    if (array != null) {
      for (int i = 0; i < array.size(); i++) {
        JSONValue item = array.get(i);
        GadgetInfo info = parseGadgetInfo(item);
        if (info != null) {
          gadgetList.add(info);
        }
      }
    }
    return gadgetList;
  }

  private GadgetInfo parseGadgetInfo(JSONValue item) {
    JSONObject object = item.isObject();
    if (object != null) {
      String name = object.get("name").isString().stringValue();
      String desc = object.get("desc").isString().stringValue();
      GadgetCategoryType primaryCategory =
          GadgetCategoryType.of(object.get("primaryCategory").isString().stringValue());
      GadgetCategoryType secondaryCategory =
          GadgetCategoryType.of(object.get("secondaryCategory").isString().stringValue());
      String gadgetUrl = object.get("gadgetUrl").isString().stringValue();
      String author = object.get("author").isString().stringValue();
      String submittedBy = object.get("submittedBy").isString().stringValue();
      String imageUrl = object.get("imageUrl").isString().stringValue();

      return new GadgetInfo(name, desc, primaryCategory, secondaryCategory, gadgetUrl, author,
          submittedBy, imageUrl);
    }
    return null;
  }
}
