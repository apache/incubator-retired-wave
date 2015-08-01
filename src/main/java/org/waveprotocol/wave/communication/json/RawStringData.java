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

package org.waveprotocol.wave.communication.json;

/**
 * A raw string data structure that can be efficiently serialized/deserialized
 * without requiring the entire string to be processed.
 *
 * TODO(kalman,user): this file needs some work; efficiency, consolidation
 * of StringBuffer vs StringBuilder...
 *
 */
// TODO(kalman/whoever): fix efficiency of string additions, and general code
// clarity.
public class RawStringData {
  private int numEntries = 0;

  /**
   * The position of the baseString
   */
  private int[] baseStringIndex = new int[] { 0, 0};

  /**
   * The concatenated string of all the stored data
   */
  private String serializedData;

  /**
   * Create a raw string data structure
   */
  public RawStringData() {
    serializedData = "";
  }

  /**
   * Create a RawStringData object by parsing the given str.
   *
   * @param str {@see(serialize())} for the detail of the string format.
   */
  public RawStringData(String str) {
    int start = str.charAt(0) == '[' ? 1 : 0;
    int i = start;

    while (i < str.length() && (Character.isDigit(str.charAt(i)) || str.charAt(i) == ',')) {
      i++;
    }

    String[] subString = str.substring(start, i).split(",");
    if (subString.length != 3) {
      throw new IllegalArgumentException("Error parsing input string " + str);
    }

    numEntries = Integer.parseInt(subString[0]);
    baseStringIndex[0] = Integer.parseInt(subString[1]);
    baseStringIndex[1] = Integer.parseInt(subString[2]);

    // the begin index of the index table is 1 after the first comma
    int beginIndex = i + 1;
    serializedData = str.substring(beginIndex);
  }

  /**
   * Serialize the table into a string.
   *
   * The format of the string is as follow:
   *     '[' numEntries ',' baseStringStart ',' baseStringEnd ']' RawString
   *
   * RawString = A concatenated string containing all the stored data in order.
   */
  public String serialize() {
    StringBuffer serializedIndexTable = new StringBuffer();
    boolean firstIndex = true;
    // insert the number of entries
    serializedIndexTable.insert(0, "[" + numEntries + "," + baseStringIndex[0] +
        "," + baseStringIndex[1] + "]");
    serializedIndexTable.append(serializedData);
    return serializedIndexTable.toString();
  }

  /**
   * Append a string to the data store
   *
   * @param str
   * @return a reference that can be used to retrieve the added object
   */
  public String addString(String str) {
    int length = serializedData.length();
    int index[] = new int[] {length, length + str.length()};
    serializedData += str;
    numEntries++;
    return convertToRef(index);
  }

  /**
   * @param ref the String reference to extract
   * @return the string stored at the given index.
   */
  public String getString(String ref) {
    int[] index = convertToIndex(ref);
    return serializedData.substring(index[0], index[1]);
  }

  /**
   * Set the reference for the base string of the stored data.
   *
   * @param ref the reference returned by addString
   */
  public void setBaseStringIndex(String ref) {
    baseStringIndex = convertToIndex(ref);
  }

  /**
   * Return the index of the base string of the stored data.
   *
   * @return a reference that can be used to retrieve the base string by using getString.
   */
  public String getBaseString() {
    return getString(convertToRef(baseStringIndex));
  }

  /**
   * @return the number of entry in the table.
   */
  public int size() {
    return numEntries;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RawStringData)) {
      return false;
    }
    RawStringData other = (RawStringData) obj;

    if (baseStringIndex.length != other.baseStringIndex.length) {
      return false;
    }
    for (int i = 0; i < baseStringIndex.length; i++) {
      if (baseStringIndex[i] != other.baseStringIndex[i]) {
        return false;
      }
    }

    return numEntries == other.numEntries && serializedData.equals(other.serializedData);
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int hash = numEntries;
    hash = hash * prime + baseStringIndex.hashCode();
    hash = hash * prime + serializedData.hashCode();
    return hash;
  }

  /**
   * @param ref The reference to decode
   * @return the begin and end index of the reference in the serializedData
   */
  private int[] convertToIndex(String ref) {
    String[] str = ref.split(",");
    if (str.length != baseStringIndex.length) {
      throw new IllegalArgumentException("Input argument is not a index" + str);
    }
    return new int[] {
      Integer.parseInt(str[0]), Integer.parseInt(str[1])
    };
  }

  /**
   * @param index The reference to decode
   * @return the begin and end index of the reference in the serializedData
   */
  private String convertToRef(int[] index) {
    return index[0] + "," + index[1];
  }
}
