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

package org.waveprotocol.wave.model.document.operation.impl;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A simple, immutable implementation of {@link AnnotationBoundaryMap} that
 * does almost all necessary validation checks.
 */
// TODO: Validate key characters better.
// Should probably only allow whatever is allowed in an xml attribute, + "/"
// TODO: Keep these in sorted order?
public class AnnotationBoundaryMapImpl implements AnnotationBoundaryMap {

  private static final String[] EMPTY_ARRAY = new String[0];

  public static final AnnotationBoundaryMapImpl EMPTY_MAP = builder().build();

  private final String[] changeKeys;
  private final String[] changeOldValues;
  private final String[] changeNewValues;
  private final String[] endKeys;

  public static final class Builder {
    private String[] changeKeys = EMPTY_ARRAY;
    private String[] changeOldValues = EMPTY_ARRAY;
    private String[] changeNewValues = EMPTY_ARRAY;
    private String[] endKeys = EMPTY_ARRAY;

    private Builder() {}

    public AnnotationBoundaryMapImpl build() {
      return new AnnotationBoundaryMapImpl(endKeys, changeKeys, changeOldValues, changeNewValues);
    }

    public Builder initializationEnd(String ... keys) {
      endKeys = keys;
      return this;
    }

    public Builder initializationValues(String ... pairs) {
      if (pairs.length % 2 != 0) {
        throw new IllegalArgumentException("pairs must be even in size");
      }

      String[] keys = new String[pairs.length / 2];
      String[] values = new String[pairs.length / 2];
      for (int i = 0; i < keys.length; i++) {
        keys[i] = pairs[i * 2];
        values[i] = pairs[i * 2 + 1];
      }
      changeKeys = keys;
      changeOldValues = new String[keys.length];
      changeNewValues = values;
      return this;
    }

    public Builder updateValues(String[] keys, String[] oldValues, String[] newValues) {
      Preconditions.checkArgument(keys.length == oldValues.length
          && keys.length == oldValues.length, "Parallel arrays must have same length");
      changeKeys = keys;
      changeOldValues = oldValues;
      changeNewValues = newValues;
      return this;
    }

    public Builder updateValues(String ... triplets) {
      if (triplets.length % 3 != 0) {
        throw new IllegalArgumentException("Triplets must be a multiple of 3 in size");
      }

      String[] keys = new String[triplets.length / 3];
      String[] oldValues = new String[triplets.length / 3];
      String[] newValues = new String[triplets.length / 3];
      for (int i = 0; i < keys.length; i++) {
        keys[i] = triplets[i * 3];
        oldValues[i] = triplets[i * 3 + 1];
        newValues[i] = triplets[i * 3 + 2];
      }
      return updateValues(keys, oldValues, newValues);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public AnnotationBoundaryMapImpl(String[] endKeys, String[] changeKeys,
      String[] changeOldValues, String[] changeNewValues) {
    if (changeKeys.length != changeOldValues.length ||
        changeKeys.length != changeNewValues.length) {
      throw new IllegalArgumentException(
          "Change keys, new values, and old values sizes don't match");
    }
    Set<String> changeKeySet = new HashSet<String>(Arrays.asList(changeKeys));
    Set<String> endKeySet = new HashSet<String>(Arrays.asList(endKeys));
    if (changeKeySet.size() != changeKeys.length || endKeySet.size() != endKeys.length) {
      throw new IllegalArgumentException("Keys must all be unique");
    }

    if (changeKeySet.contains(null) || endKeySet.contains(null)) {
      throw new NullPointerException("Null keys are not permitted");
    }

    if (changeKeySet.contains("") || endKeySet.contains("")) {
      throw new IllegalArgumentException("Empty-string keys are not permitted");
    }

    for (String changeKey : changeKeys) {
      validateAnnotationKey(changeKey);
    }

    for (String endKey : endKeys) {
      validateAnnotationKey(endKey);
      if (changeKeySet.contains(endKey)) {
        throw new IllegalArgumentException("Change keys and end keys must be disjoint sets");
      }
    }

    this.changeKeys = copy(changeKeys);
    this.changeOldValues = copy(changeOldValues);
    this.changeNewValues = copy(changeNewValues);
    this.endKeys = copy(endKeys);
  }

  /**
   * Copies an array. GWT does not seem to support Arrays.copyOf()
   */
  private String[] copy(String[] input) {
    String[] ret = new String[input.length];
    for (int i = 0; i < input.length; i++) {
      ret[i] = input[i];
    }
    return ret;
  }

  public static void validateAnnotationKey(String key) throws IllegalArgumentException {
    if (key.contains("?") || key.contains("@")) {
      throw new IllegalArgumentException(
          "Annotation keys must not contain the '?' or '@' characters");
    }
  }

  @Override
  public int changeSize() {
    return changeKeys.length;
  }

  @Override
  public int endSize() {
    return endKeys.length;
  }

  @Override
  public String getChangeKey(int changeIndex) {
    return changeKeys[changeIndex];
  }

  @Override
  public String getNewValue(int changeIndex) {
    return changeNewValues[changeIndex];
  }

  @Override
  public String getOldValue(int changeIndex) {
    return changeOldValues[changeIndex];
  }

  @Override
  public String getEndKey(int endIndex) {
    return endKeys[endIndex];
  }

  @Override
  public String toString() {
    return DocOpUtil.toConciseString(this);
  }
}
