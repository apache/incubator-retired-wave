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

package com.google.wave.api.event;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.wave.api.Blip;
import com.google.wave.api.BlipThread;
import com.google.wave.api.Wavelet;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.impl.GsonFactory;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test cases for {@link EventSerializer}.
 */
public class EventSerializerRobotTest extends TestCase {

  private static class Context implements JsonSerializationContext, JsonDeserializationContext {

    private final Gson gson = new GsonFactory().create();

    @Override
    public JsonElement serialize(Object src) {
      return gson.toJsonTree(src);
    }

    @Override
    public JsonElement serialize(Object src, Type type) {
      return gson.toJsonTree(src, type);
    }

    @Override
    public <T> T deserialize(JsonElement json, Type type) throws JsonParseException {
      return gson.<T>fromJson(json, type);
    }
  }

  public void refactor_testSerializeAndDeserializeWaveletEvent() throws Exception {
    List<String> participantsAdded = new ArrayList<String>();
    participantsAdded.add("foo@google.com");
    participantsAdded.add("bar@google.com");

    List<String> participantsRemoved = new ArrayList<String>();
    participantsRemoved.add("baz@google.com");

    Blip blip = mock(Blip.class);
    
    when(blip.getBlipId()).thenReturn("blip123");
    Wavelet wavelet = mock(Wavelet.class);
    when(wavelet.getBlip("blip123")).thenReturn(blip);
    Map<String, Blip> blips = Maps.newHashMap();
    blips.put("blip123", blip);
    when(wavelet.getThread(anyString())).thenReturn(new BlipThread("rootThread", -1, 
        Lists.<String>newArrayList("blip123"), blips));
    
    EventMessageBundle bundle = new EventMessageBundle("http://10.1.1.1",
        "http://wave-active-api.example.com");

    WaveletParticipantsChangedEvent expected = new WaveletParticipantsChangedEvent(wavelet,
        bundle, "mprasetya@google.com", 1l, "blip123", participantsAdded, participantsRemoved);

    Context context = new Context();
    Event actualEvent = EventSerializer.deserialize(wavelet, bundle,
        EventSerializer.serialize(expected, context),
        context);
    WaveletParticipantsChangedEvent actual = WaveletParticipantsChangedEvent.as(actualEvent);
    assertEquals(expected, actual);
    assertEquals(expected.getParticipantsAdded(), actual.getParticipantsAdded());
    assertEquals(expected.getParticipantsRemoved(), actual.getParticipantsRemoved());
  }

  public void refactor_testSerializeAndDeserializeBlipEvent() throws Exception {
    List<String> contributorsAdded = new ArrayList<String>();
    contributorsAdded.add("foo@google.com");
    contributorsAdded.add("bar@google.com");

    List<String> contributorsRemoved = new ArrayList<String>();
    contributorsRemoved.add("baz@google.com");

    Blip blip = mock(Blip.class);
    when(blip.getBlipId()).thenReturn("blip123");
    Wavelet wavelet = mock(Wavelet.class);
    when(wavelet.getBlip("blip123")).thenReturn(blip);
    EventMessageBundle bundle = new EventMessageBundle("http://10.1.1.1",
        "http://wave-active-api.example.com");

    BlipContributorsChangedEvent expected = new BlipContributorsChangedEvent(wavelet,
        bundle, "mprasetya@google.com", 1l, "blip123", contributorsAdded, contributorsRemoved);

    Context context = new Context();
    Event actualEvent = EventSerializer.deserialize(wavelet, bundle,
        EventSerializer.serialize(expected, context),
        context);

    BlipContributorsChangedEvent actual = BlipContributorsChangedEvent.as(actualEvent);
    assertEquals(expected, actual);
    assertEquals(expected.getContributorsAdded(), actual.getContributorsAdded());
    assertEquals(expected.getContributorsRemoved(), actual.getContributorsRemoved());
  }

  public void refactor_testSerializeAndDeserializeGadgetStateChanged() throws Exception {
    Map<String, String> oldState = new HashMap<String, String>();
    oldState.put("key1", "value1");
    oldState.put("key2", "value2");

    Blip blip = mock(Blip.class);
    when(blip.getBlipId()).thenReturn("blip123");
    Wavelet wavelet = mock(Wavelet.class);
    when(wavelet.getBlip("blip123")).thenReturn(blip);
    EventMessageBundle bundle = new EventMessageBundle("http://10.1.1.1",
        "http://wave-active-api.example.com");

    GadgetStateChangedEvent expected = new GadgetStateChangedEvent(wavelet, bundle,
        "mprasetya@google.com", 1l, "blip123", 5, oldState);

    Context context = new Context();
    Event actualEvent = EventSerializer.deserialize(wavelet, bundle,
        EventSerializer.serialize(expected, context),
        context);

    GadgetStateChangedEvent actual = GadgetStateChangedEvent.as(actualEvent);
    assertEquals(expected, actual);
    assertEquals(expected.getIndex(), actual.getIndex());
    assertEquals(expected.getOldState(),actual.getOldState());
  }

  public void refactor_testSerializeAndDeserializeOperationErrorEvent() throws Exception {
    Wavelet wavelet = mock(Wavelet.class);
    EventMessageBundle bundle = new EventMessageBundle("http://10.1.1.1",
        "http://wave-active-api.example.com");

    OperationErrorEvent expected = new OperationErrorEvent(wavelet, bundle,
        "foo@google.com", 123l, "op1", "Error!");
    Context context = new Context();
    Event actual = EventSerializer.deserialize(wavelet, bundle,
        EventSerializer.serialize(expected, context),
        context);
    assertEquals(expected, actual);
  }

  public void testAllEventClassesHaveTheAppropriateConstructor() throws Exception {
    for (EventType eventType : EventType.values()) {
      if (eventType == EventType.UNKNOWN) {
        continue;
      }

      try {
        Class<? extends Event> clazz = eventType.getClazz();
        clazz.getDeclaredConstructor();
      } catch (NoSuchMethodException e) {
        fail(eventType.getClazz().getName() + " should implement a no-arg constructor that will " +
            "be used for deserialization.");
      }
    }
  }

  public void testAllEventClassesHaveConversionStaticMethod() throws Exception {
    for (EventType eventType : EventType.values()) {
      if (eventType == EventType.UNKNOWN) {
        continue;
      }

      try {
        Method method = eventType.getClazz().getDeclaredMethod("as", Event.class);
        assertEquals(eventType.getClazz(), method.getReturnType());
      } catch (NoSuchMethodException e) {
        fail(eventType.getClazz().getName() + " should implement a no-arg constructor that will " +
            "be used for deserialization.");
      }
    }
  }

  private static void assertEquals(Event expected, Event actual) {
    assertEquals(expected.getType(), actual.getType());
    assertEquals(expected.getWavelet(), actual.getWavelet());
    assertEquals(expected.getModifiedBy(), actual.getModifiedBy());
    assertEquals(expected.getTimestamp(), actual.getTimestamp());
    assertEquals(expected.getBlip(), actual.getBlip());
    assertEquals(expected.getBundle(), actual.getBundle());
  }
}
