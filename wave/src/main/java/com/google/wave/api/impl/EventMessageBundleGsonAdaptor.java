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

import com.google.gson.InstanceCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.wave.api.Blip;
import com.google.wave.api.BlipData;
import com.google.wave.api.OperationQueue;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.BlipThread;
import com.google.wave.api.Wavelet;
import com.google.wave.api.event.Event;
import com.google.wave.api.event.EventSerializationException;
import com.google.wave.api.event.EventSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Gson adaptor to serialize and deserialize {@link EventMessageBundle}.
 *
 * @author douwe@google.com (Douwe Osinga)
 */
public class EventMessageBundleGsonAdaptor implements
    InstanceCreator<EventMessageBundle>,
    JsonSerializer<EventMessageBundle>,
    JsonDeserializer<EventMessageBundle> {

  public static final String EVENTS_TAG = "events";
  public static final String WAVELET_TAG = "wavelet";
  public static final String BLIPS_TAG = "blips";
  public static final String THREADS_TAG = "threads";
  private static final String PROXYING_FOR_TAG = "proxyingFor";
  private static final String ROBOT_ADDRESS_TAG = "robotAddress";
  private static final String RPC_SERVER_URL_TAG = "rpcServerUrl";

  @Override
  public EventMessageBundle createInstance(Type type) {
    return new EventMessageBundle("", "http://opensocial.example.com");
  }

  @Override
  public JsonElement serialize(EventMessageBundle src, Type typeOfSrc,
      JsonSerializationContext context) {
    JsonObject result = new JsonObject();

    JsonArray events = new JsonArray();
    for (Event event : src.getEvents()) {
      try {
        events.add(EventSerializer.serialize(event, context));
      } catch (EventSerializationException e) {
        throw new JsonParseException(e);
      }
    }
    result.add(EVENTS_TAG, events);

    result.add(WAVELET_TAG, context.serialize(src.getWaveletData()));
    result.add(BLIPS_TAG, context.serialize(src.getBlipData()));
    result.add(THREADS_TAG, context.serialize(src.getThreads()));
    result.addProperty(ROBOT_ADDRESS_TAG, src.getRobotAddress());

    String proxyingFor = src.getProxyingFor();
    if (proxyingFor != null && !proxyingFor.isEmpty()) {
      result.addProperty(PROXYING_FOR_TAG, proxyingFor);
    }

    String rpcServerUrl = src.getRpcServerUrl();
    if (rpcServerUrl != null && !rpcServerUrl.isEmpty()) {
      result.addProperty(RPC_SERVER_URL_TAG, rpcServerUrl);
    }
    return result;
  }

  @Override
  public EventMessageBundle deserialize(JsonElement json, Type typeOfT,
      JsonDeserializationContext context) throws JsonParseException {
    JsonObject jsonObj = json.getAsJsonObject();
    String robotAddress = jsonObj.get(ROBOT_ADDRESS_TAG).getAsString();
    String rpcServerUrl = "";
    if (jsonObj.has(RPC_SERVER_URL_TAG)) {
      rpcServerUrl = jsonObj.get(RPC_SERVER_URL_TAG).getAsString();
    }
    EventMessageBundle result = new EventMessageBundle(robotAddress, rpcServerUrl);

    OperationQueue operationQueue;
    if (jsonObj.has(PROXYING_FOR_TAG)) {
      result.setProxyingFor(jsonObj.get(PROXYING_FOR_TAG).getAsString());
      operationQueue = new OperationQueue(new ArrayList<OperationRequest>(),
          result.getProxyingFor());
    } else {
      operationQueue = new OperationQueue();
    }

    // Deserialize wavelet.
    WaveletData waveletData = context.deserialize(jsonObj.get(WAVELET_TAG), WaveletData.class);
    result.setWaveletData(waveletData);
    Map<String, Blip> blips = new HashMap<String, Blip>();
    Map<String, BlipThread> threads = new HashMap<String, BlipThread>();
    Wavelet wavelet = Wavelet.deserialize(operationQueue, blips, threads, waveletData);
    wavelet.setRobotAddress(robotAddress);
    result.setWavelet(wavelet);

    // Deserialize threads.
    Map<String, BlipThread> tempThreads = context.deserialize(jsonObj.get(THREADS_TAG),
        GsonFactory.THREAD_MAP_TYPE);
    for (Entry<String, BlipThread> entry : tempThreads.entrySet()) {
      BlipThread thread = entry.getValue();
      threads.put(entry.getKey(), new BlipThread(thread.getId(), thread.getLocation(),
          thread.getBlipIds(), blips));
    }

    // Deserialize blips.
    Map<String, BlipData> blipDatas = context.deserialize(jsonObj.get(BLIPS_TAG),
        GsonFactory.BLIP_MAP_TYPE);
    result.setBlipData(blipDatas);
    for(Entry<String, BlipData> entry : blipDatas.entrySet()) {
      blips.put(entry.getKey(), Blip.deserialize(operationQueue, wavelet, entry.getValue()));
    }

    // Deserialize events.
    JsonArray eventsArray = jsonObj.get(EVENTS_TAG).getAsJsonArray();
    List<Event> events = new ArrayList<Event>(eventsArray.size());
    for (JsonElement element : eventsArray) {
      JsonObject eventObject = element.getAsJsonObject();
      try {
        events.add(EventSerializer.deserialize(wavelet, result, eventObject, context));
      } catch (EventSerializationException e) {
        throw new JsonParseException(e.getMessage());
      }
    }
    result.setEvents(events);
    return result;
  }
}
