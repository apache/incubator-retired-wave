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

package org.waveprotocol.box.server.authentication;

import org.eclipse.jetty.util.MultiMap;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * A CallbackHandler which configures callbacks based on a set of parameters
 * sent in an HTTP request.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class HttpRequestBasedCallbackHandler implements CallbackHandler {
  public static final String ADDRESS_FIELD = "address";
  public static final String PASSWORD_FIELD = "password";
  
  private final MultiMap<String> parameters;

  public HttpRequestBasedCallbackHandler(MultiMap<String> parameters) {
    this.parameters = parameters;
  }

  @Override
  public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
    for (Callback c : callbacks) {
      if (c instanceof NameCallback) {
        if (parameters.containsKey(ADDRESS_FIELD)) {
          ((NameCallback) c).setName(parameters.getString(ADDRESS_FIELD));
        }
      } else if (c instanceof PasswordCallback) {
        if (parameters.containsKey(PASSWORD_FIELD)) {
          String password = parameters.getString(PASSWORD_FIELD);
          ((PasswordCallback) c).setPassword(password.toCharArray());
        }
      } else {
        throw new UnsupportedCallbackException(c);
      }
    }
  }
}
