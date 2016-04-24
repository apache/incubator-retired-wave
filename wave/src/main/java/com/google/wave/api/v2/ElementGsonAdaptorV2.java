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

package com.google.wave.api.v2;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.wave.api.Attachment;
import com.google.wave.api.Element;
import com.google.wave.api.ElementType;
import com.google.wave.api.Image;
import com.google.wave.api.impl.ElementGsonAdaptor;
import com.google.wave.api.impl.GsonFactory;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Gson adaptor to serialize and deserialize {@link Element}. In v0.2, we still
 * use {@link Image} to represents attachment, so we need to convert all
 * attachment objects into Image.
 * 
 * @author jli@google.com (Jimin Li)
 * @author mprasetya@google.com (Marcel Prasetya)
 */
public class ElementGsonAdaptorV2 extends ElementGsonAdaptor {

  private static final Set<String> ATTACHMENT_ONLY_PROPERTIES = new HashSet<String>(
      Arrays.asList(Attachment.MIME_TYPE, Attachment.DATA, Attachment.ATTACHMENT_URL));

  @Override
  public JsonElement serialize(Element src, Type typeOfSrc, JsonSerializationContext context) {
    if (src.getType() == ElementType.ATTACHMENT) {
      src = new Element(ElementType.IMAGE, createImageProperties(src.getProperties()));
    }
    return super.serialize(src, typeOfSrc, context);
  }

  @Override
  public Element deserialize(JsonElement jsonElement, Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    JsonObject json = jsonElement.getAsJsonObject();
    String type = json.get(TYPE_TAG).getAsString();
    
    if (ElementType.IMAGE.name().equals(type)) {
      JsonObject properties = json.getAsJsonObject(PROPERTIES_TAG);
      if (!properties.has(Image.URL)) {
        json.addProperty(TYPE_TAG, ElementType.ATTACHMENT.name());
      }
    }
    return super.deserialize(json, typeOfT, context);
  }

  static Map<String, String> createImageProperties(Map<String, String> props) {
    Map<String, String> imageProps = new HashMap<String, String>();
    Iterator<Entry<String, String>> iter = props.entrySet().iterator();
    while (iter.hasNext()) {
      Entry<String, String> next = iter.next();
      // Removes attachment only properties, and provides backward compatible 
      // image support to python robot with protocol version 0.2
      if (!ATTACHMENT_ONLY_PROPERTIES.contains(next.getKey())) {
        imageProps.put(next.getKey(), next.getValue());
      }
    }
    return imageProps;
  }
  
  /**
   * Registers this {@link ElementGsonAdaptorV2} with the given
   * {@link GsonFactory}.
   * @param factory {@link GsonFactory} to register the type adapters with
   * @return the given {@link GsonFactory} with the registered adapters
   */
  public static GsonFactory registerTypeAdapters(GsonFactory factory) {
    ElementGsonAdaptorV2 elementGsonAdaptorV2 = new ElementGsonAdaptorV2();
    factory.registerTypeAdapter(Element.class, elementGsonAdaptorV2);
    factory.registerTypeAdapter(Attachment.class, elementGsonAdaptorV2);
    return factory;
  }
}
