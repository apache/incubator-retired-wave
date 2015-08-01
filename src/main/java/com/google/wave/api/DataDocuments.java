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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A class that represents wavelet's data documents. This class supports various
 * data document related operations, such as, getting, setting, or removing
 * data document value from a wavelet.
 */
public class DataDocuments implements Iterable<Entry<String,String>>, Serializable {

  /** A map of data documents values. */
  private final Map<String, String> dataDocuments;

  /** The wavelet that this data document represents. */
  private final Wavelet wavelet;

  /** The operation queue to queue operation to the robot proxy. */
  private final OperationQueue operationQueue;

  /**
   * Constructor.
   *
   * @param dataDocuments a map of initial data documents.
   * @param wavelet the wavelet that this data documents represents.
   * @param operationQueue the operation queue to queue operation to the robot
   *     proxy.
   */
  public DataDocuments(Map<String, String> dataDocuments, Wavelet wavelet,
      OperationQueue operationQueue) {
    this.dataDocuments = new HashMap<String, String>(dataDocuments);
    this.wavelet = wavelet;
    this.operationQueue = operationQueue;
  }

  /**
   * Associates the given name/key with the given value in the data documents.
   *
   * @param name the key with which the specified value is to be associated.
   * @param value the value to be associated with the given name/key.
   * @return the previous value for the given name/key.
   */
  public String set(String name, String value) {
    if (value == null) {
      throw new IllegalArgumentException("Value should not be null.");
    }
    operationQueue.setDatadocOfWavelet(wavelet, name, value);
    return dataDocuments.put(name, value);
  }

  /**
   * Removes the value of the given key from the data documents.
   *
   * @param name the key whose value will be removed.
   * @return the previous value for the given name/key.
   */
  public String remove(String name) {
    operationQueue.setDatadocOfWavelet(wavelet, name, null);
    return dataDocuments.remove(name);
  }

  /**
   * Checks whether the given key exists in the data documents.
   *
   * @param name the key to check.
   * @return {@code true} if the set contains the given name/key.
   *     Otherwise, returns {@code false}.
   */
  public boolean contains(String name) {
    return dataDocuments.containsKey(name);
  }

  /**
   * Returns the number of values in the data documents.
   *
   * @return the size of the data documents.
   */
  public int size() {
    return dataDocuments.size();
  }

  /**
   * Checks whether this data documents is empty or not.
   *
   * @return {@code true} if the data documents is empty. Otherwise, returns
   *     {@code false}.
   */
  public boolean isEmpty() {
    return dataDocuments.isEmpty();
  }

  /**
   * Returns the data document by its name.
   * @param name the name of the data document.
   * 
   * @return The data document, or {@code null} if it does not exist.
   */
  public String get(String name) {
    return dataDocuments.get(name);
  }
  
  @Override
  public Iterator<Entry<String, String>> iterator() {
    return dataDocuments.entrySet().iterator();
  }
}
