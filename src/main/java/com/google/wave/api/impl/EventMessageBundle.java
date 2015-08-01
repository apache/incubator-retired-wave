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

import com.google.wave.api.BlipData;
import com.google.wave.api.Context;
import com.google.wave.api.BlipThread;
import com.google.wave.api.Wavelet;
import com.google.wave.api.event.Event;
import com.google.wave.api.event.EventType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A container for a bundle of messages to be sent to a robot.
 *
 * @author scovitz@google.com (Seth Covitz)
 */
public class EventMessageBundle {

  private List<Event> events;
  private WaveletData waveletData;
  private Wavelet wavelet;
  private Map<String, BlipData> blipData;
  private Map<String, BlipThread> threads;
  private Map<String, Set<Context>> requiredBlips;
  private String proxyingFor;
  private final String robotAddress;
  private final String rpcServerUrl;

  public EventMessageBundle(String robotAddress, String rpcServerUrl) {
    this.robotAddress = robotAddress;
    events = new ArrayList<Event>();
    this.rpcServerUrl = rpcServerUrl;
    blipData = new HashMap<String, BlipData>();
    requiredBlips = new HashMap<String, Set<Context>>();
    threads = new HashMap<String, BlipThread>();
  }

  public Map<String, Set<Context>> getRequiredBlips() {
    return requiredBlips;
  }

  /**
   * Require the availability of the specified blipId for this bundle.
   *
   * @param blipId the id of the blip that is required.
   * @param contexts we need for this blip.
   */
  public void requireBlip(String blipId, List<Context> contexts) {
    Set<Context> contextSet = requiredBlips.get(blipId);
    if (contextSet == null) {
      contextSet = new HashSet<Context>();
      requiredBlips.put(blipId, contextSet);
    }
    for (Context context : contexts) {
      contextSet.add(context);
    }
  }

  /**
   * Add an event to the events that are tracked.
   * @param event to add.
   */
  public void addEvent(Event event) {
    events.add(event);
  }

  public boolean hasMessages() {
    return !events.isEmpty();
  }

  public List<Event> getEvents() {
    return events;
  }

  public WaveletData getWaveletData() {
    return waveletData;
  }

  public Wavelet getWavelet() {
    return wavelet;
  }

  public Map<String, BlipData> getBlipData() {
    return blipData;
  }

  public Map<String, BlipThread> getThreads() {
    return threads;
  }

  public void setEvents(List<Event> events) {
    this.events = events;
  }

  public void setWaveletData(WaveletData waveletData) {
    this.waveletData = waveletData;
  }

  public void setWavelet(Wavelet wavelet) {
    this.wavelet = wavelet;
  }

  public void setBlipData(Map<String, BlipData> blipData) {
    this.blipData = blipData;
  }

  public void setThreads(Map<String, BlipThread> threads) {
    this.threads = threads;
  }

  public void clear() {
    events.clear();
    blipData.clear();
    requiredBlips.clear();
    waveletData = null;
  }

  /**
   * Return whether a blip is already in the blipdata
   *
   * @param id of the blip
   * @return whether it is in blipData
   */
  public boolean hasBlipId(String id) {
    return blipData.containsKey(id);
  }

  /**
   * Add a blip to the blipdata
   *
   * @param id
   * @param blip
   */
  public void addBlip(String id, BlipData blip) {
    blipData.put(id, blip);
  }

  /**
   * Add a thread to the map of threads.
   *
   * @param id the thread id.
   * @param thread the thread to add.
   */
  public void addThread(String id, BlipThread thread) {
    threads.put(id, thread);
  }

  /**
   * Check whether a given thread has been added to the thread map or not.
   *
   * @param id the id of the thread to check.
   * @return {@code true} if the map contains the given thread id.
   */
  public boolean hasThreadId(String id) {
    return threads.containsKey(id);
  }

  /**
   * Return whether the lookingFor event is contained in this bundle.
   */
  public boolean hasEvent(EventType eventType) {
    for (Event event : events) {
      if (event.getType() == eventType) {
        return true;
      }
    }
    return false;
  }

  public String getProxyingFor() {
    return proxyingFor;
  }

  public void setProxyingFor(String proxyingFor) {
    this.proxyingFor = proxyingFor;
  }

  public String getRobotAddress() {
    return robotAddress;
  }

  public String getRpcServerUrl() {
    return rpcServerUrl;
  }
}
