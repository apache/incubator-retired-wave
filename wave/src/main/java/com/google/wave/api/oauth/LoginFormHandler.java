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

package com.google.wave.api.oauth;

import com.google.wave.api.Wavelet;

/**
 * Interface that handles the rendering in the wave of a login form in the wave 
 * to direct the user to authorize access to the service provider's resources.
 * 
 * @author elizabethford@google.com (Elizabeth Ford)
 */
public interface LoginFormHandler {

  /**
   * Renders a link to the service provider's login page and a confirmation
   * button to press when login is complete.
   * 
   * @param userRecordKey The user id.
   * @param wavelet The wavelet to which the robot is added.
   */
  void renderLogin(String userRecordKey, Wavelet wavelet);
}
