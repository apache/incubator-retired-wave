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

import com.google.wave.api.Element;

import java.util.ArrayList;
import java.util.List;


/**
 * DocumentModifyAction is a class specific to how operations of type
 * DOCUMENT_MODIFY are serialized. DocumentModifyAction specifies what should
 * happen to matched bits of the document.
 *
 */

public class DocumentModifyAction {

  public enum ModifyHow {
    DELETE, REPLACE, INSERT, INSERT_AFTER, ANNOTATE, CLEAR_ANNOTATION, UPDATE_ELEMENT;
  }

  /**
   * Initial annotations to be applied to the document modification range.
   */
  static public class BundledAnnotation {
    public String key;
    public String value;
    
    
    /**
     * Convenience method to create a list of bundled annotations with
     * the even values passed being the keys, the uneven ones the values.
     */
    public static List<BundledAnnotation> listOf(String... values) {
      if (values.length % 2 != 0) {
        throw new IllegalArgumentException("listOf takes an even number of parameters");
      }
      List<BundledAnnotation> res = new ArrayList<BundledAnnotation>(values.length / 2);
      for (int i = 0; i < values.length - 1; i += 2) {
        BundledAnnotation next = new BundledAnnotation();
        next.key = values[i];
        next.value = values[i + 1];
        res.add(next);
      }
      return res;
    }
  }

  private ModifyHow modifyHow;
  private List<String> values;
  private String annotationKey;
  private List<Element> elements;
  private List<BundledAnnotation> bundledAnnotations;
  private boolean useMarkup;

  public DocumentModifyAction(ModifyHow modifyHow, List<String> values, String annotationKey,
      List<Element> elements, List<BundledAnnotation> initialAnnotations, boolean useMarkup) {
    this.modifyHow = modifyHow;
    this.values = values;
    this.annotationKey = annotationKey;
    this.elements = elements;
    this.useMarkup = useMarkup;
    this.bundledAnnotations = initialAnnotations;
  }

  // No parameter constructor for serialization
  public DocumentModifyAction() {
    this.modifyHow = null;
    this.values = null;
    this.annotationKey = null;
    this.elements = null;
    this.useMarkup = false;
    this.bundledAnnotations = null;
  }

  public ModifyHow getModifyHow() {
    return modifyHow;
  }

  public List<String> getValues() {
    return values;
  }

  public String getAnnotationKey() {
    return annotationKey;
  }

  public List<Element> getElements() {
    return elements;
  }

  public boolean isUseMarkup() {
    return useMarkup;
  }

  /**
   * @return the value from values (wrapping around) for the given index.
   */
  public String getValue(int valueIndex) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    return values.get(valueIndex % values.size());
  }

  /**
   * @return the element from elements (wrapping around) for the given index.
   */
  public Element getElement(int valueIndex) {
    if (elements == null || elements.isEmpty()) {
      return null;
    }
    return elements.get(valueIndex % elements.size());
  }

  /**
   * @return whether there is text at the specified index
   */
  public boolean hasTextAt(int valueIndex) {
    if (values == null || values.isEmpty()) {
      return false;
    }
    return values.get(valueIndex % values.size()) != null;
  }

  public List<BundledAnnotation> getBundledAnnotations() {
    return bundledAnnotations;
  }
}
