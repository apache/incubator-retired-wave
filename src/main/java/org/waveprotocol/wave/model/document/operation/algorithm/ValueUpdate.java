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

package org.waveprotocol.wave.model.document.operation.algorithm;

/**
 * An pair of strings representing an update of a string value.
 */
final class ValueUpdate {

  final String oldValue;
  final String newValue;

  ValueUpdate(String oldValue, String newValue) {
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((newValue == null) ? 0 : newValue.hashCode());
    result = prime * result + ((oldValue == null) ? 0 : oldValue.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof ValueUpdate)) return false;
    ValueUpdate other = (ValueUpdate) obj;
    if (newValue == null) {
      if (other.newValue != null) return false;
    } else if (!newValue.equals(other.newValue)) return false;
    if (oldValue == null) {
      if (other.oldValue != null) return false;
    } else if (!oldValue.equals(other.oldValue)) return false;
    return true;
  }

}
