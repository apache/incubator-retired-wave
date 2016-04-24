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

package org.waveprotocol.wave.client.debug.logger;

import com.google.gwt.core.client.GWT;

/**
 * LogLevel defines an ordered list of logging levels.<br/>
 *
 * <pre>NONE < ERROR < DEBUG</pre>
 *
 * Deferred-binding property "loglevel" determines at which level the system
 * is logging, allowing for unused code to be compiled out in production.
 *
 * e.g. anything logged at level DEBUG will only be logged when the
 *      "loglevel" deferred-binding property is set to "debug".
 *      If "loglevel" is "none", nothing should be logged.
 *
 */
// TODO(user): Consider rather using the GWT logging API throughout:
//     http://code.google.com/p/google-web-toolkit-incubator/wiki/Logging
public abstract class LogLevel {
  private static final LogLevel INSTANCE = GWT.isClient()
      ? GWT.<LogLevel>create(LogLevel.class) : new ErrorImpl();

  // NOTE(user): LogLevel must *not* have a clinit method (i.e. any static
  //               initialisation code), otherwise each inlining of shouldX()
  //               will call the clinit.

  // NOTE(user): This class is not an enum, as GWT (as of 21 Jan 2009) does not
  //               do object identity tracking, even for enums.
  //               Hence the comparisons against the enum values do not get
  //               inlined (which is an essential goal of the "#ifdef" style
  //               methods below).

  /**
   * Should an entry with level ERROR be logged?
   */
  public static boolean showErrors() {
    return INSTANCE.showErrorsInstance();
  }

  /**
   * Should an entry with level DEBUG be logged?
   */
  public static boolean showDebug() {
    return INSTANCE.showDebugInstance();
  }

  // Intended for overriding per-implementation:
  protected abstract boolean showErrorsInstance();
  protected abstract boolean showDebugInstance();

  /**
   * Deferred-binding replacement for LogLevel used for production.
   */
  @SuppressWarnings("unused")  // NOTE(user): Created via deferred binding
  private static class NoneImpl extends LogLevel {
    @Override protected boolean showErrorsInstance() { return false; }
    @Override protected boolean showDebugInstance()  { return false; }
  }

  /**
   * Deferred-binding replacement for LogLevel used for logging errors.
   */
  @SuppressWarnings("unused")  // NOTE(user): Created via deferred binding
  private static class ErrorImpl extends LogLevel {
    @Override protected boolean showErrorsInstance() { return true; }
    @Override protected boolean showDebugInstance()  { return false; }
  }

  /**
   * Deferred-binding replacement for LogLevel used for debugging errors.
   */
  @SuppressWarnings("unused")  // NOTE(user): Created via deferred binding
  private static class DebugImpl extends LogLevel {
    @Override protected boolean showErrorsInstance() { return true; }
    @Override protected boolean showDebugInstance()  { return true; }
  }
}
