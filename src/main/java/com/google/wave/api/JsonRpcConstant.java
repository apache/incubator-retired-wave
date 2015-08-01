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

package com.google.wave.api;

import com.google.wave.api.impl.RawAttachmentData;
import com.google.wave.api.impl.DocumentModifyAction;
import com.google.wave.api.impl.DocumentModifyQuery;
import com.google.wave.api.impl.WaveletData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Enumeration for Google Wave JSON-RPC request properties.
 *
 * @author mprasetya@google.com (Marcel Prasetya)
 */
public class JsonRpcConstant {

  /**
   * Enumeration for JSON-RPC request properties.
   *
   * @author mprasetya@google.com (Marcel Prasetya)
   */
  public enum RequestProperty {
    METHOD("method"),
    ID("id"),
    PARAMS("params");

    private final String key;

    private RequestProperty(String key) {
      this.key = key;
    }

    /**
     * Returns the string key to access the property.
     *
     * @return A string key to access the property.
     */
    public String key() {
      return key;
    }
  }

  /**
   * Enumeration for JSON-RPC response properties.
   *
   * @author mprasetya@google.com (Marcel Prasetya)
   */
  public enum ResponseProperty {
    ID("id"),
    DATA("data"),
    ERROR("error");

    private final String key;

    private ResponseProperty(String key) {
      this.key = key;
    }

    /**
     * Returns the string key to access the property.
     *
     * @return A string key to access the property.
     */
    public String key() {
      return key;
    }
  }

  /**
   * Enumeration for Google Wave specific JSON-RPC request parameters.
   *
   * @author mprasetya@google.com (Marcel Prasetya)
   */
  public enum ParamsProperty {
    // TODO(mprasetya): Consider combining this with OperationType, or at least
    // each OperationType should have a list of ParamsProperty.

    // Commonly used parameters.
    WAVE_ID("waveId", String.class),
    WAVELET_ID("waveletId", String.class),
    BLIP_ID("blipId", String.class),
    ATTACHMENT_ID("attachmentId", String.class),

    // Operation specific parameters.
    ANNOTATION("annotation", Annotation.class),
    BLIP_AUTHOR("blipAuthor", String.class),
    BLIP_CREATION_TIME("blipCreationTime", Long.class),
    BLIP_DATA("blipData", BlipData.class),
    BLIPS("blips", Map.class),
    CAPABILITIES_HASH("capabilitiesHash", String.class),
    CHILD_BLIP_ID("childBlipId", String.class),
    CONTENT("content", String.class),
    DATADOC_NAME("datadocName", String.class),
    DATADOC_VALUE("datadocValue", String.class),
    DATADOC_WRITEBACK("datadocWriteback", String.class),
    ELEMENT("element", Element.class),
    FETCH_PROFILES_REQUEST("fetchProfilesRequest", FetchProfilesRequest.class),
    FETCH_PROFILES_RESULT("fetchProfilesResult", FetchProfilesResult.class),
    INDEX("index", Integer.class),
    LANGUAGE("language", String.class),
    MESSAGE("message", String.class),
    MODIFY_ACTION("modifyAction", DocumentModifyAction.class),
    MODIFY_HOW("modifyHow", String.class),
    MODIFY_QUERY("modifyQuery", DocumentModifyQuery.class),
    NAME("name", String.class),
    NEW_BLIP_ID("newBlipId", String.class),
    NUM_RESULTS("numResults", Integer.class),
    PARTICIPANT_ID("participantId", String.class),
    PARTICIPANT_PROFILE("participantProfile", ParticipantProfile.class),
    PARTICIPANT_ROLE("participantRole", String.class),
    PARTICIPANTS_ADDED("participantsAdded", List.class),
    PARTICIPANTS_REMOVED("participantsRemoved", List.class),
    PROTOCOL_VERSION("protocolVersion", String.class),
    PROXYING_FOR("proxyingFor", String.class),
    QUERY("query", String.class),
    RANGE("range", Range.class),
    SEARCH_RESULTS("searchResults", SearchResult.class),
    STYLE_TYPE("styleType", String.class),
    THREADS("threads", Map.class),
    WAVELET_DATA("waveletData", WaveletData.class),
    WAVELET_TITLE("waveletTitle", String.class),
    RETURN_WAVELET_IDS("returnWaveletIds", Boolean.class),
    WAVELET_IDS("waveletIds", List.class),
    RAW_SNAPSHOT("rawSnapshot", String.class),
    FROM_VERSION("fromVersion", byte[].class),
    TO_VERSION("toVersion", byte[].class),
    RAW_DELTAS("rawDeltas", List.class),
    TARGET_VERSION("targetVersion", byte[].class),
    ATTACHMENT_DATA("attachmentData", RawAttachmentData.class),
    IMPORTED_FROM_VERSION("importedFromVersion", Long.class);

    private static final Logger LOG = Logger.getLogger(ParamsProperty.class.getName());

    private static final Map<String, ParamsProperty> reverseLookupMap =
        new HashMap<String, ParamsProperty>();

    static {
      for (ParamsProperty property : ParamsProperty.values()) {
        if (reverseLookupMap.containsKey(property.key)) {
          LOG.warning("Parameter with key " + property.key + " already exist.");
        }
        reverseLookupMap.put(property.key, property);
      }
    }

    private final String key;
    private final Class<? extends Object> clazz;

    private ParamsProperty(String key, Class<? extends Object> clazz) {
      this.key = key;
      this.clazz = clazz;
    }

    /**
     * Returns the string key to access the property.
     *
     * @return A string key to access the property.
     */
    public String key() {
      return key;
    }

    /**
     * Returns the {@link Class} object that represents the type of this
     * property.
     *
     * @return A {@link Class} object that represents the type of this property.
     */
    public Class<? extends Object> clazz() {
      return clazz;
    }

    /**
     * Returns a {@link ParamsProperty} enumeration that has the given key.
     *
     * @param key The method name of a property.
     * @return An {@link ParamsProperty} that has the given key.
     */
    public static ParamsProperty fromKey(String key) {
      return reverseLookupMap.get(key);
    }
  }
}
