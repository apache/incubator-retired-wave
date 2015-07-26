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

import com.google.wave.api.ElementType;

import java.util.Map;

/**
 * DocumentModifyQuery is a class specific to how operations of type
 * DOCUMENT_MODIFY are serialized. DocumentModifyQuery specifies which
 * bit of a blip is matched.
 * 
 * The query specifies either a textMatch in which case the blip is
 * searched for that string literal, or an elementMatch is specified in
 * which case the blip is searched of an instance of that element. In the
 * latter case restrictions can also be set to restrict the elements matched
 * by attribute value. In either case maxRes can be specified to restrict
 * how many matches are returned; specify -1 for as many as possible.
 */
public class DocumentModifyQuery {
  
  private ElementType elementMatch;
  private int maxRes;
  private String textMatch;
  private Map<String, String> restrictions;

  public DocumentModifyQuery(
      ElementType elementMatch, Map<String, String> restrictions, int maxRes) {
    this.textMatch = null;
    this.elementMatch = elementMatch;
    this.restrictions = restrictions;
    this.maxRes = maxRes;
  }

  public DocumentModifyQuery(String stringMatch, int maxRes) {
    this.textMatch = stringMatch;
    this.elementMatch = null;
    this.restrictions = null;
    this.maxRes = maxRes;
  }

  // No Parameter constructor for serialization:
  public DocumentModifyQuery() {
    this.textMatch = null;
    this.elementMatch = null;
    this.restrictions = null;
    this.maxRes = -1;
  }

  /**
   * @return textMatch if any is set or null if not.
   */
  public String getTextMatch() {
    return textMatch;
  }

  /**
   * @return elementMatch if any is set or null if not.
   */
  public ElementType getElementMatch() {
    return elementMatch;
  }

  public Map<String, String> getRestrictions() {
    return restrictions;
  }

  public int getMaxRes() {
    return maxRes;
  }

}
