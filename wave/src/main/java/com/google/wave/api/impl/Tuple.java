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

import java.util.Arrays;

/**
 * An immutable tuple of values that can be accessed using {@link #get} method.
 * 
 * @author mprasetya@google.com (Marcel Prasetya)
 * @param <A> Type of the tuple element.
 */
public class Tuple<A> {

  /**
   * The elements of the tuple.
   */
  private final A[] elements;
  
  /**
   * Factory method to create a tuple.
   *
   * @param <A> Type of the tuple element.
   * @param elements The elements of the tuple
   * @return A new tuple that contains {@code elements}
   */
  @SuppressWarnings("unchecked")
  public static <A> Tuple<A> of(A ... elements) {
    return new Tuple<A>(elements);
  }
  
  /**
   * Constructor.
   *
   * @param elements The elements of the tuple.
   */
  @SuppressWarnings("unchecked")
  public Tuple(A ... elements) {
    this.elements = elements;
  }
  
  /**
   * Returns the {@code index}th element of the tuple.
   *
   * @param index The index of the element.
   * @return The {@code index}th element of the tuple.
   */
  public A get(int index) {
    return elements[index];
  }
  
  /**
   * Returns the number of elements in the tuple.
   * 
   * @return the number of elements in the tuple.
   */
  public int size() {
    return elements.length;
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    
    if (o == null || o.getClass() != this.getClass()) {
      return false;
    }
    
    Tuple<A> o2 = (Tuple<A>) o;
    return Arrays.equals(elements, o2.elements);
  }
  
  @Override
  public int hashCode() {
    return Arrays.hashCode(elements);
  }
  
  @Override
  public String toString() {
    return Arrays.toString(elements);
  }
}
