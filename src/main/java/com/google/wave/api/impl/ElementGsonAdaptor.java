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

package com.google.wave.api.impl;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.wave.api.Attachment;
import com.google.wave.api.Element;
import com.google.wave.api.ElementType;
import com.google.wave.api.FormElement;
import com.google.wave.api.Gadget;
import com.google.wave.api.Image;
import com.google.wave.api.Line;

import org.apache.commons.codec.binary.Base64;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Gson adaptor to serialize and deserialize {@link Element}.
 *
 * @author mprasetya@google.com (Marcel Prasetya)
 */
public class ElementGsonAdaptor implements JsonDeserializer<Element>,
    JsonSerializer<Element> {

  protected static final String TYPE_TAG = "type";
  protected static final String PROPERTIES_TAG = "properties";

  @Override
  public Element deserialize(JsonElement json, Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    Element result = null;
    ElementType type = ElementType.valueOfIgnoreCase(
        json.getAsJsonObject().get(TYPE_TAG).getAsString());

    Map<String, String> properties = context.deserialize(
        json.getAsJsonObject().get(PROPERTIES_TAG), GsonFactory.STRING_MAP_TYPE);

    if (FormElement.getFormElementTypes().contains(type)) {
      result = new FormElement(type, properties);
    } else if (type == ElementType.GADGET) {
      result = new Gadget(properties);
    } else if (type == ElementType.IMAGE) {
      result = new Image(properties);
    } else if (type == ElementType.ATTACHMENT) {
      byte[] data = null;
      String encodedData = properties.get(Attachment.DATA);
      if (encodedData != null) {
        try {
          data = Base64.decodeBase64(encodedData.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
          throw new JsonParseException("Couldn't convert to utf-8", e);
        }
      }
      result = new Attachment(properties, data);
    } else if (type == ElementType.LINE) {
      result = new Line(properties);
    } else {
      result = new Element(type, properties);
    }
    return result;
  }

  @Override
  public JsonElement serialize(Element src, Type typeOfSrc, JsonSerializationContext context) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(TYPE_TAG, src.getType().toString());
    JsonObject properties = new JsonObject();
    if (src.isAttachment()) {
      Attachment attachment = (Attachment) src;
      if (attachment.hasData()) {
        byte[] encodedData = Base64.encodeBase64(attachment.getData());
        try {
          properties.add(Attachment.DATA, new JsonPrimitive(
            new String(encodedData, "UTF-8")));
        } catch (UnsupportedEncodingException e) {
          // this shouldn't happen, so let it slide.
        }
      }
    }
    for (Entry<String, String> entry : src.getProperties().entrySet()) {
      // Note: Gson's JsonObject and MapTypeAdapter don't escape the key
      // automatically, so we have to manually escape it here by calling
      // JsonSerializationContext.serialize(). Also, unfortunately, calling
      // JsonPrimitive.toString() wraps the text inside double quotes, that we
      // need to strip out.
      String quotedKey = context.serialize(entry.getKey()).toString();
      String key = quotedKey.substring(1, quotedKey.length() - 1);

      JsonElement value = context.serialize(entry.getValue());
      properties.add(key, value);
    }

    jsonObject.add(PROPERTIES_TAG, properties);
    return jsonObject;
  }
}
