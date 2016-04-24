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

package org.waveprotocol.wave.model.id;

/**
 * Checked exception indicating that a serialised wave or wavelet id is
 * invalid.
 *
 * @author anorth@google.com (Alex North)
 */
public class InvalidIdException extends Exception {
  private final String id;
  public InvalidIdException(String id, String message) {
    super(message);
    this.id = id;
  }

  public String getId() {
    return id;
  }

  @Override
  public String getMessage() {
    return "Invalid id '" + id + "': " + super.getMessage();
  }
}