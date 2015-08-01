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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * BlipData is the serializable data representation of a Blip. It contains
 * metadata, a text-only representation of the document content, and a list of
 * annotations.
 *
 * @author scovitz@google.com (Seth Covitz)
 * @author mprasetya@google.com (Marcel Prasetya)
 */
public class BlipData {

  /**
   * The list of annotations for the document content.
   */
  private List<Annotation> annotations;

  /**
   * The list of elements embedded within the document.
   */
  private Map<Integer, Element> elements;

  /**
   * The blip id for this blip.
   */
  private String blipId;

  /**
   * A list of child blip ids for this blip.
   */
  private List<String> childBlipIds;

  /**
   * A list of contributors to this blip.
   */
  private List<String> contributors;

  /**
   * The creator of this blip.
   */
  private String creator;

  /**
   * The text document content for this blip.
   */
  private String content;

  /**
   * The time this blip was last modified.
   */
  private long lastModifiedTime;

  /**
   * The parent blip id for this blip.
   */
  private String parentBlipId;

  /**
   * The latest version number for this blip.
   */
  private long version;

  /**
   * The Wave ID for the wave containing this blip.
   */
  private String waveId;

  /**
   * The Wavelet ID for the wavelet containing this blip.
   */
  private String waveletId;

  /**
   * The inline and non-inline reply thread ids.
   */
  private List<String> replyThreadIds;

  /**
   * Get the thread to which this blip belongs.
   */
  private String threadId;

  /**
   * Constructs an empty BlipData object.
   */
  public BlipData() {
    annotations = new ArrayList<Annotation>();
    elements = new HashMap<Integer, Element>();
    childBlipIds = new ArrayList<String>();
    content = "\n";
    contributors = new ArrayList<String>();
    lastModifiedTime = -1L;
    version = -1L;
    replyThreadIds = new ArrayList<String>();
  }

  /**
   * Constructs a BlipData object.
   *
   * @param waveId the wave id of the blip.
   * @param waveletId the wavelet id of the blip.
   * @param blipId the blip id.
   * @param initialContent the initial content of the blip. If the supplied
   *     content doesn't start with a newline character, this constructor will
   *     auto-prepend that.
   */
  public BlipData(String waveId, String waveletId, String blipId, String initialContent) {
    this.annotations = new ArrayList<Annotation>();
    this.elements = new HashMap<Integer, Element>();
    this.childBlipIds = new ArrayList<String>();
    this.contributors = new ArrayList<String>();
    this.waveId = waveId;
    this.waveletId = waveletId;
    this.blipId = blipId;

    // Make sure that initial content is valid, and starts with newline.
    if (initialContent == null || initialContent.isEmpty()) {
      initialContent = "\n";
    } else if (!initialContent.startsWith("\n")) {
      initialContent = "\n" + initialContent;
    }

    this.content = initialContent;
  }

  /**
   * Creates a deep copy/clone of a blip's data.
   *
   * @param blip The original blip to be copied.
   */
  public BlipData(BlipData blip) {
    // Deep copy annotations.
    annotations = new ArrayList<Annotation>();
    for (Annotation annotation : blip.getAnnotations()) {
      Range range = annotation.getRange();
      annotations.add(new Annotation(annotation.getName(), annotation.getValue(),
          range.getStart(), range.getEnd()));
    }

    // Deep copy form elements.
    elements = new HashMap<Integer, Element>();
    for (Entry<Integer, Element> entry : blip.getElements().entrySet()) {
      ElementType type = entry.getValue().getType();
      Element result = null;
      if (FormElement.getFormElementTypes().contains(type)) {
        result = new FormElement(type, entry.getValue().getProperties());
      } else if (type == ElementType.GADGET) {
        result = new Gadget(entry.getValue().getProperties());
      } else if (type == ElementType.IMAGE) {
        result = new Image(entry.getValue().getProperties());
      } else if (type == ElementType.LINE) {
        result = new Line(entry.getValue().getProperties());
      } else {
        result = new Element(type, entry.getValue().getProperties());
      }
      elements.put(entry.getKey(), result);
    }

    creator = blip.getCreator();
    childBlipIds = blip.getChildBlipIds();
    content = blip.getContent();
    contributors = blip.getContributors();
    blipId = blip.getBlipId();
    lastModifiedTime = blip.getLastModifiedTime();
    version = blip.getVersion();
    parentBlipId = blip.getParentBlipId();
    waveId = blip.getWaveId();
    waveletId = blip.getWaveletId();
    replyThreadIds = blip.getReplyThreadIds();
    threadId = blip.getThreadId();
  }

  /**
   * Adds an annotation to the end of the list of annotations.
   *
   * @param annotation the annotation to be added.
   */
  public void addAnnotation(Annotation annotation) {
    annotations.add(annotation);
  }

  /**
   * Returns the list of annotations modifying this document's content.
   *
   * @return a list of annotations.
   */
  public List<Annotation> getAnnotations() {
    return annotations == null ? new ArrayList<Annotation>() : annotations;
  }

  /**
   * Adds an element to the blip at a given index into the text document.
   *
   * @param position The character position / index into the document to insert
   *     the form element.
   * @param element The form element to be added.
   */
  public void addElement(int position, Element element) {
    elements.put(position, element);
  }

  /**
   * Returns a map of the elements in the blip and the positions where
   * they have been inserted.
   *
   * @return the map of form elements to document positions.
   */
  public Map<Integer, Element> getElements() {
    return elements;
  }

  /**
   * Returns the Blip ID for this blip.
   *
   * @return the blip id for this blip.
   */
  public String getBlipId() {
    return blipId;
  }

  /**
   * Returns a list of child Blip IDs for this blip.
   *
   * @return a list of child Blip IDs.
   */
  public List<String> getChildBlipIds() {
    return childBlipIds;
  }

  /**
   * Returns the list of email addresses corresponding to the contributors who
   * have modified this blip's content.
   *
   * @return the list of contributors.
   */
  public List<String> getContributors() {
    return contributors;
  }

  /**
   * Returns the email address corresponding to the creator of this blip.
   *
   * @return the creator of this blip.
   */
  public String getCreator() {
    return creator;
  }

  /**
   * Returns the text document content for this blip.
   *
   * @return the text document content for this blip.
   */
  public String getContent() {
    return content;
  }

  /**
   * Returns the time in milliseconds since the UNIX epoch when this blip was
   * last modified.
   *
   * @return the last modified time for this blip.
   */
  public long getLastModifiedTime() {
    return lastModifiedTime;
  }

  /**
   * Returns the parent Blip ID for this blip.
   *
   * @return the parent Blip ID for this blip.
   */
  public String getParentBlipId() {
    return parentBlipId;
  }

  /**
   * Returns the version number for this blip.
   *
   * @return the version number for this blip.
   */
  public long getVersion() {
    return version;
  }

  /**
   * Returns the Wave ID for the wave containing this blip.
   *
   * @return the Wave ID for the wave containing this blip.
   */
  public String getWaveId() {
    return waveId;
  }

  /**
   * Returns the Wavelet ID for the wavelet containing this blip.
   *
   * @return the Wavelet ID for the wavelet containing this blip.
   */
  public String getWaveletId() {
    return waveletId;
  }

  /**
   * Replaces the blip's list of annotations with a new list of annotations.
   *
   * @param annotations the new list of annotations.
   */
  public void setAnnotations(List<Annotation> annotations) {
    this.annotations = annotations;
  }

  /**
   * Replaces the blip's list of elements with a new list of elements.
   *
   * @param map the new list of elements.
   */
  public void setElements(Map<Integer, Element> map) {
    this.elements = map;
  }

  /**
   * Returns the Blip ID for this blip.
   *
   * @param blipId the Blip ID for this blip.
   */
  public void setBlipId(String blipId) {
    this.blipId = blipId;
  }

  /**
   * Replaces the blip's list of child Blip IDs with a new list.
   *
   * @param childBlipIds the new list of child Blip IDs.
   */
  public void setChildBlipIds(List<String> childBlipIds) {
    this.childBlipIds = childBlipIds;
  }

  /**
   * Adds a new child blip id to this blip's list of child id's.
   *
   * @param blipId the Blip ID to be added.
   */
  public void addChildBlipId(String blipId) {
    this.childBlipIds.add(blipId);
  }

  /**
   * Replaces the blip's list of contributors with a new list.
   *
   * @param contributors the new list of contributors.
   */
  public void setContributors(List<String> contributors) {
    this.contributors = contributors;
  }

  /**
   * Adds a contributor to this blip's list of contributors.
   *
   * @param contributor a new contributor to the blip.
   */
  public void addContributor(String contributor) {
    this.contributors.add(contributor);
  }

  /**
   * Sets the creator of the blip.
   *
   * @param creator the creator of the blip.
   */
  public void setCreator(String creator) {
    this.creator = creator;
  }

  /**
   * Replaces the blip's text document content.
   *
   * @param content the new text content for the blip.
   */
  public void setContent(String content) {
    this.content = content;
  }

  /**
   * Sets the last modified time measured in milliseconds since the UNIX epoch
   * when the blip was last modified.
   *
   * @param lastModifiedTime the last modified time of the blip.
   */
  public void setLastModifiedTime(long lastModifiedTime) {
    this.lastModifiedTime = lastModifiedTime;
  }

  /**
   * Set's the parent Blip ID for the blip.
   *
   * @param parentBlipId the parent blip id.
   */
  public void setParentBlipId(String parentBlipId) {
    this.parentBlipId = parentBlipId;
  }

  /**
   * Sets the version of the blip.
   *
   * @param version the version of the blip.
   */
  public void setVersion(long version) {
    this.version = version;
  }

  /**
   * Sets the Wave ID of the blip.
   *
   * @param waveId the Wave ID of the blip.
   */
  public void setWaveId(String waveId) {
    this.waveId = waveId;
  }

  /**
   * Sets the Wavelet ID of the blip.
   *
   * @param waveletId the Wavelet ID of the blip.
   */
  public void setWaveletId(String waveletId) {
    this.waveletId = waveletId;
  }

  public void removeChildBlipId(String blipId) {
    childBlipIds.remove(blipId);
  }

  /**
   * @return the inline and non-inline reply threads' ids.
   */
  public List<String> getReplyThreadIds() {
    return replyThreadIds;
  }

  /**
   * Sets the list of inline and non-inline reply threads' ids.
   *
   * @param replyThreadIds a list of ids of the reply threads.
   */
  public void setReplyThreadIds(List<String> replyThreadIds) {
    this.replyThreadIds = replyThreadIds;
  }

  /**
   * @return the id of the thread to which this blip belongs to.
   */
  public String getThreadId() {
    return threadId;
  }

  /**
   * Sets the id of the thread to which this blip belongs to.

   * @param threadId the id of the parent thread.
   */
  public void setThreadId(String threadId) {
    this.threadId = threadId;
  }
}
