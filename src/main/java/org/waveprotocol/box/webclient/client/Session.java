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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;

import org.waveprotocol.box.common.SessionConstants;
import org.waveprotocol.box.webclient.client.i18n.SessionMessages;

/**
 * Session data for the web client.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public abstract class Session implements SessionConstants {

  private static final Session INSTANCE;

  private static SessionMessages messages = GWT.create(SessionMessages.class);

  static {
    // In the future we could inject other Session instances for testing.
    if (JsSession.isAvailable()) {
      INSTANCE = new JsSession();
    } else {
      Window.alert(messages.sessionDataNotAvailable());
      INSTANCE = new StubSession();
    }
  }

  private Session() {
  }

  /**
   * @return the singleton {@link Session} instance
   */
  public static Session get() {
    return INSTANCE;
  }

  /**
   * @return the domain the wave server serves waves for
   */
  public abstract String getDomain();

  /**
   * @return the user's logged in address, or null if the user is not logged in.
   */
  public abstract String getAddress();

  /**
   * @return true if the user is currently logged in.
   */
  public boolean isLoggedIn() {
    return getAddress() != null;
  }
  
  /**
   * @return the id seed that's going to be used for generating ids for the session
   */
  public abstract String getIdSeed();


  /**
   * A {@link Session} which gets its data from the __session JS variable.
   */
  private static final class JsSession extends Session {
    /**
     * @return whether it is possible to have a JsSession
     */
    public static native boolean isAvailable() /*-{
      return typeof($wnd.__session) !== "undefined";
    }-*/;

    @Override
    public String getDomain() {
      return getFieldAsString(DOMAIN);
    }

    @Override
    public String getAddress() {
      return getFieldAsString(ADDRESS);
    }

    @Override
    public String getIdSeed() {
      return getFieldAsString(ID_SEED);
    }

    private native String getFieldAsString(String s) /*-{
      return $wnd.__session[s];
    }-*/;
  }

  /**
   * A {@link Session} which returns stub values.
   */
  private static final class StubSession extends Session {
    @Override
    public String getDomain() {
      return DOMAIN;
    }

    @Override
    public String getAddress() {
      return ADDRESS;
    }

    @Override
    public String getIdSeed() {
      return ID_SEED;
    }
  }
}
