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



package org.waveprotocol.wave.client.util;

import com.google.common.annotations.VisibleForTesting;


/**
 * This class extends the generated ClientFlagsBase to provide user access to
 * flags from WFE.
 *
 * The reason we extend ClientFlagsBase is to extract constants and methods from
 * the generated file.
 *
 *
 */
public final class ClientFlags extends ClientFlagsBase {

  private static ClientFlags instance = null;

  private static native ExtendedJSObject getJSObj() /*-{
    if ($wnd.__client_flags) {
      return $wnd.__client_flags;
    }
    return null;
  }-*/;

  private ClientFlags(ClientFlagsBaseHelper helper) {
    super(helper);
  }

  /**
   * Inject a TypedSource object for the purpose of testing.
   */
  @VisibleForTesting
  public static void resetWithSourceForTesting(TypedSource source) {
    ClientFlags.instance = new ClientFlags(new ClientFlagsBaseHelper(source));
  }

  /**
   * Returns an instance of a ClientFlagsBase object, which allows users to
   * access flags passed from WFE.
   *
   * If we are running in hosted mode, fall back to using default flags.
   */
  public static ClientFlags get() {
    if (instance == null) {
      // HACK(user): Use the existence of the client_flags var to determine
      // whether we are running in WFE or hosted mode. If we're in WFE,
      // initialize client flags with the js var, else initialize using query
      // string.
      ExtendedJSObject jsObj = getJSObj();
      TypedSource source = (jsObj != null ? new WrappedJSObject(jsObj) : UrlParameters.get());
      instance = new ClientFlags(new ClientFlagsBaseHelper(source));
    }
    return instance;
  }
}
