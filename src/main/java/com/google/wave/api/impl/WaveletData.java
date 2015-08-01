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

import com.google.wave.api.BlipThread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The data representation of Wavelet metadata used to serialize and send to
 * the Robot.
 *
 * @author scovitz@google.com (Seth Covitz)
 */
public class WaveletData {

  private long creationTime = -1L;
  private long lastModifiedTime = -1L;
  private long version = -1L;
  private List<String> participants = new ArrayList<String>();
  private Map<String, String> participantRoles = new HashMap<String, String>();
  private Map<String, String> dataDocuments = new HashMap<String, String>();
  private List<String> tags = new ArrayList<String>();
  private String creator;
  private String rootBlipId;
  private String title;
  private String waveId;
  private String waveletId;
  private BlipThread rootThread;

  public WaveletData() {
    // TODO(mprasetya): Please remove this ctor. It is currently being used for
    // deserialization.
  }

  public WaveletData(String waveId, String waveletId, String rootBlipId,
      BlipThread rootThread) {
    this.waveId = waveId;
    this.waveletId = waveletId;
    this.rootBlipId = rootBlipId;
    this.rootThread = rootThread;
  }

  public WaveletData(WaveletData wavelet) {
    this.creationTime = wavelet.getCreationTime();
    this.creator = wavelet.getCreator();
    this.lastModifiedTime = wavelet.getLastModifiedTime();
    this.participants = wavelet.getParticipants();
    this.participantRoles = new HashMap<String, String>(wavelet.getParticipantRoles());
    this.rootBlipId = wavelet.getRootBlipId();
    this.title = wavelet.getTitle();
    this.version = wavelet.getVersion();
    this.waveId = wavelet.getWaveId();
    this.waveletId = wavelet.getWaveletId();
    this.dataDocuments = new HashMap<String, String>(wavelet.getDataDocuments());
    this.tags = new ArrayList<String>(wavelet.getTags());
  }

  /**
   * @returns a map of participantId to role for participants that don't have
   * the default role.
   */
  public Map<String, String> getParticipantRoles() {
    return participantRoles;
  }

  public long getCreationTime() {
    return creationTime;
  }

  public String getCreator() {
    return creator;
  }

  public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  public List<String> getParticipants() {
    return participants;
  }

  public String getRootBlipId() {
    return rootBlipId;
  }

  public String getTitle() {
    return title;
  }

  public long getVersion() {
    return version;
  }

  public String getWaveId() {
    return waveId;
  }

  public String getWaveletId() {
    return waveletId;
  }

  public void setCreationTime(long creationTime) {
    this.creationTime = creationTime;
  }

  public void setCreator(String creator) {
    this.creator = creator;
  }

  public void setLastModifiedTime(long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  public void setParticipants(List<String> participants) {
    this.participants = participants;
  }

  public void setRootBlipId(String rootBlipId) {
    this.rootBlipId = rootBlipId;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setVersion(long version) {
    this.version = version;
  }

  public void setWaveId(String waveId) {
    this.waveId = waveId;
  }

  public void setWaveletId(String waveletId) {
    this.waveletId = waveletId;
  }

  public Map<String, String> getDataDocuments() {
    return dataDocuments;
  }

  public void setDataDocuments(Map<String, String> dataDocuments) {
    this.dataDocuments = new HashMap<String, String>(dataDocuments);
  }

  public void setDataDocument(String name, String data) {
    dataDocuments.put(name, data);
  }

  public String getDataDocument(String name) {
    if (dataDocuments == null) {
      return null;
    } else {
      return dataDocuments.get(name);
    }
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = new ArrayList<String>(tags);
  }

  public void addTag(String tag) {
    tags.add(tag);
  }

  public void removeTag(String tag) {
    tags.remove(tag);
  }

  public void addParticipant(String participant) {
    participants.add(participant);
  }

  public void setParticipantRole(String participant, String role) {
    participantRoles.put(participant, role);
  }

  public BlipThread getRootThread() {
    return rootThread;
  }

  public void setRootThread(BlipThread rootThread) {
    this.rootThread = rootThread;
  }
}
