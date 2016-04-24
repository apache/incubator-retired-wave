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

/**
 * A class that models a range, that contains a start and end index.
 */
public class Range implements Serializable {

  /** Start of the range. */
  private final int start;

  /** End of the range. */
  private final int end;

  /**
   * Constructs a range object given a start and end index into the document.
   *
   * @param start the start of the range.
   * @param end the end of the range.
   */
  public Range(int start, int end) {
    this.start = start;
    this.end = end;
  }

  /**
   * Returns the starting index of the range.
   *
   * @return the starting index.
   */
  public int getStart() {
    return start;
  }

  /**
   * Returns the ending index of the range.
   *
   * @return the ending index.
   */
  public int getEnd() {
    return end;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + end;
    result = prime * result + start;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null) {
      return false;
    }

    if (getClass() != obj.getClass()) {
      return false;
    }

    Range other = (Range) obj;
    return start == other.start && end == other.end;
  }

  @Override
  public String toString() {
    return "Range(" + start + ',' + end + ')';
  }
}
