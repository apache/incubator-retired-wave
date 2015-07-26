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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * The Operation types supported by Robots.
 *
 * @author scovitz@google.com (Seth Covitz)
 * @author mprasetya@google.com (Marcel Prasetya)
 */
public enum OperationType {
  UNKNOWN("unknown"),
  WAVELET_APPEND_BLIP("wavelet.appendBlip"),
  WAVELET_CREATE("wavelet.create"),
  WAVELET_REMOVE_SELF("wavelet.removeSelf"),
  WAVELET_SET_TITLE("wavelet.setTitle"),

  // TODO(mprasetya): Remove the "newsyntax" suffix once renaming is complete.
  WAVELET_ADD_PARTICIPANT_NEWSYNTAX("wavelet.addParticipant"),
  WAVELET_REMOVE_PARTICIPANT_NEWSYNTAX("wavelet.removeParticipant"),

  WAVELET_APPEND_DATADOC("wavelet.appendDatadoc"),
  WAVELET_SET_DATADOC("wavelet.setDatadoc"),
  WAVELET_MODIFY_TAG("wavelet.modifyTag"),
  WAVELET_MODIFY_PARTICIPANT_ROLE("wavelet.modifyParticipantRole"),

  BLIP_CONTINUE_THREAD("blip.continueThread"),
  BLIP_CREATE_CHILD("blip.createChild"),
  BLIP_DELETE("blip.delete"),
  BLIP_SET_AUTHOR("blip.setAuthor"),
  BLIP_SET_CREATION_TIME("blip.setCreationTime"),

  DOCUMENT_DELETE_ANNOTATION("document.deleteAnnotation"),
  DOCUMENT_SET_ANNOTATION("document.setAnnotation"),
  DOCUMENT_SET_ANNOTATION_NORANGE("document.setAnnotationNoRange"),

  DOCUMENT_APPEND("document.append"),
  DOCUMENT_APPEND_MARKUP("document.appendMarkup"),
  DOCUMENT_APPEND_STYLED_TEXT("document.appendStyledText"),
  DOCUMENT_DELETE("document.delete"),
  DOCUMENT_INSERT("document.insert"),
  DOCUMENT_MODIFY("document.modify"),
  DOCUMENT_REPLACE("document.replace"),

  DOCUMENT_APPEND_ELEMENT("document.appendElement"),
  DOCUMENT_DELETE_ELEMENT("document.deleteElement"),
  DOCUMENT_INSERT_ELEMENT("document.insertElement"),
  DOCUMENT_INSERT_ELEMENT_AFTER("document.insertElementAfter"),
  DOCUMENT_INSERT_ELEMENT_BEFORE("document.insertElementBefore"),
  DOCUMENT_MODIFY_ELEMENT_ATTRS("document.modifyElementAttrs"),
  DOCUMENT_REPLACE_ELEMENT("document.replaceElement"),

  DOCUMENT_APPEND_INLINE_BLIP("document.appendInlineBlip"),
  DOCUMENT_INSERT_INLINE_BLIP("document.insertInlineBlip"),
  DOCUMENT_INSERT_INLINE_BLIP_AFTER_ELEMENT("document.insertInlineBlipAfterElement"),

  // Some operations not associated with a context
  ROBOT_FOLDER_ACTION("robot.folderAction"),
  ROBOT_CREATE_WAVELET("robot.createWavelet"),
  ROBOT_FETCH_MY_PROFILE("robot.fetchMyProfile"),
  ROBOT_FETCH_PROFILES("robot.fetchProfiles"),
  ROBOT_FETCH_WAVE("robot.fetchWave"),
  ROBOT_NOTIFY("robot.notify"),
  ROBOT_SEARCH("robot.search"),

  ROBOT_EXPORT_SNAPSHOT("robot.exportSnapshot"),
  ROBOT_EXPORT_DELTAS("robot.exportDeltas"),
  ROBOT_EXPORT_ATTACHMENT("robot.exportAttachment"),
  ROBOT_IMPORT_SNAPSHOT("robot.importSnapshot"),
  ROBOT_IMPORT_DELTAS("robot.importDeltas"),
  ROBOT_IMPORT_ATTACHMENT("robot.importAttachment"),

  // Remove these deprecated operations once all robots are upgraded to v0.21.
  @Deprecated WAVELET_ADD_PARTICIPANT("wavelet.participant.add"),
  @Deprecated WAVELET_REMOVE_PARTICIPANT("wavelet.participant.remove"),

  @Deprecated WAVELET_DATADOC_APPEND("wavelet.datadoc.append"),
  @Deprecated WAVELET_DATADOC_SET("wavelet.datadoc.set"),

  @Deprecated DOCUMENT_ANNOTATION_DELETE("document.annotation.delete"),
  @Deprecated DOCUMENT_ANNOTATION_SET("document.annotation.set"),
  @Deprecated DOCUMENT_ANNOTATION_SET_NORANGE("document.annotation.setNoRange"),

  @Deprecated DOCUMENT_ELEMENT_APPEND("document.element.append"),
  @Deprecated DOCUMENT_ELEMENT_DELETE("document.element.delete"),
  @Deprecated DOCUMENT_ELEMENT_INSERT("document.element.insert"),
  @Deprecated DOCUMENT_ELEMENT_INSERT_AFTER("document.element.insertAfter"),
  @Deprecated DOCUMENT_ELEMENT_INSERT_BEFORE("document.element.insertBefore"),
  @Deprecated DOCUMENT_ELEMENT_MODIFY_ATTRS("document.element.modifyAttrs"),
  @Deprecated DOCUMENT_ELEMENT_REPLACE("document.element.replace"),

  @Deprecated DOCUMENT_INLINE_BLIP_APPEND("document.inlineBlip.append"),
  @Deprecated DOCUMENT_INLINE_BLIP_INSERT("document.inlineBlip.insert"),
  @Deprecated DOCUMENT_INLINE_BLIP_INSERT_AFTER_ELEMENT("document.inlineBlip.insertAfterElement"),

  @Deprecated ROBOT_NOTIFY_CAPABILITIES_HASH("robot.notifyCapabilitiesHash");

  private static final Logger LOG = Logger.getLogger(OperationType.class.getName());

  private static final Map<String, OperationType> reverseLookupMap =
      new HashMap<String, OperationType>();

  static {
    for (OperationType operationType : OperationType.values()) {
      if (reverseLookupMap.containsKey(operationType.method)) {
        LOG.warning("Operation with method name " + operationType.method + " already exist.");
      }
      reverseLookupMap.put(operationType.method, operationType);
    }
  }

  private final String method;

  private OperationType(String method) {
    this.method = method;
  }

  /**
   * Returns the method name of an operation type.
   *
   * @return The method name of an operation type.
   */
  public String method() {
    return method;
  }

  /**
   * Returns an {@link OperationType} enumeration that has the given method
   * name. If no match is found, UNKNOWN is returned.
   *
   * @param methodName The method name of an operation.
   * @return An {@link OperationType} that has the given method name.
   */
  public static OperationType fromMethodName(String methodName) {
    OperationType operationType = reverseLookupMap.get(methodName);
    if (operationType == null) {
      return UNKNOWN;
    }
    return operationType;
  }
}
