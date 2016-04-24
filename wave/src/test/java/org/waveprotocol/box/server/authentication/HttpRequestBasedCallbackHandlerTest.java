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

import junit.framework.TestCase;

import org.eclipse.jetty.util.MultiMap;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 *
 */
public class HttpRequestBasedCallbackHandlerTest extends TestCase {
  public void testBindsUsernameAndPassword() throws IOException, UnsupportedCallbackException {
    MultiMap<String> args = new MultiMap<String>();
    args.add("address", "seph@example.com");
    args.add("password", "internet");

    CallbackHandler handler = new HttpRequestBasedCallbackHandler(args);
    Callback[] callbacks =
        new Callback[] {new NameCallback("ignored"), new PasswordCallback("ignored", false),};

    handler.handle(callbacks);

    assertEquals("seph@example.com", ((NameCallback) callbacks[0]).getName());
    assertEquals("internet", new String(((PasswordCallback) callbacks[1]).getPassword()));
  }

  public void testCallbackThrowsHandlingUnsupportedCallback() throws IOException {
    CallbackHandler handler = new HttpRequestBasedCallbackHandler(new MultiMap<String>());

    try {
      handler.handle(new Callback[] {new Callback() {}});
      fail("Should have thrown due to unsupported callback");
    } catch (UnsupportedCallbackException e) {
      // Pass.
    }
  }
}
