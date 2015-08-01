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

package org.waveprotocol.wave.model.wave;


/**
 * Checked Exception to indicate that the contained address is not a valid participant address.
 *
 */
public class InvalidParticipantAddress extends Exception {

  private final String address;

  public InvalidParticipantAddress(String address, String message) {
    super(message);
    this.address = address;
  }

  public InvalidParticipantAddress(String address, Throwable cause){
    super(cause);
    this.address = address;
  }

  public InvalidParticipantAddress(String address, String message, Throwable cause){
    super(message, cause);
    this.address = address;
  }

  /**
   * @return the address that is invalid
   */
  public String getAddress() {
    return address;
  }

  @Override
  public String getMessage() {
    return "Invalid address '" + getAddress() + "': " + super.getMessage();
  }
}
