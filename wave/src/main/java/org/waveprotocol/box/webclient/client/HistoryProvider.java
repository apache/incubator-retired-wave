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

package org.waveprotocol.box.webclient.client;

import com.google.gwt.event.logical.shared.ValueChangeHandler;

/**
 * The Interface HistoryProvider gives access to the browser history stack in
 * order to access to the wave tokens.
 *
 * Also this give us the opportunity to intercept these wave token petitions.
 * Imagine that you are in fact using #clean-urls in your client you can return
 * the waveref equivalent to the #clean-url when the wave webclient ask for the
 * current token.
 *
 * @author vjrj@ourproject.org
 */
public interface HistoryProvider {

  /**
   * Gets the current wave history token. The handler will not receive a
   * {@link ValueChangeHandler#onValueChange(com.google.gwt.event.logical.shared.ValueChangeEvent)}
   * event for the initial token; requiring that an application request the
   * token explicitly on startup gives it an opportunity to run different
   * initialization code in the presence or absence of an initial token.
   *
   * @return the initial token, or the empty string if none is present.
   */
  String getToken();

}
