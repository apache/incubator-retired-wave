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

package org.waveprotocol.wave.client.gadget;

import org.waveprotocol.wave.client.debug.logger.DomLogger;

import org.waveprotocol.wave.common.logging.LoggerBundle;

/**
 * Common gadget log utilities.
 *
 */
public final class GadgetLog {
  private GadgetLog() {} // Non-instantiable.

  /**
   * Debug logger for Gadget and related classes.
   */
  public static final LoggerBundle LOG = new DomLogger("gadgets");

  /**
   * Debug logger for verbose (fine) Gadget logging.
   */
  public static final LoggerBundle FINE_LOG = new DomLogger("gadgets-fine");

  /**
   * Debug logger for developer messages logged using Gadget API wave.log().
   */
  public static final LoggerBundle DEVELOPER_LOG = new DomLogger("gadgets-dev");

  /**
   * @returns whether the trace logging is enabled.
   */
  public static boolean shouldLog() {
    return LOG.trace().shouldLog();
  }

  /**
   * Logs a trace message in the Gadget log.
   *
   * @param logMessage the message to log
   */
  public static void log(String logMessage) {
    LOG.trace().log(logMessage);
  }

  /**
   * Logs a trace message in the Gadget log.
   *
   * @param objects the objects to lazily log
   */
  public static void logLazy(Object ... objects) {
    LOG.trace().logLazyObjects(objects);
  }

  /**
   * Logs an error message in the Gadget log.
   *
   * @param logMessage the message to log
   */
  public static void logError(String logMessage) {
    LOG.error().log(logMessage);
  }

  /**
   * @returns whether the fine logging is enabled.
   */
  public static boolean shouldLogFine() {
    return FINE_LOG.trace().shouldLog();
  }

  /**
   * Logs a message in the fine (verbose) Gadget log.
   *
   * @param logMessage the message to log
   */
  public static void logFine(String logMessage) {
    FINE_LOG.trace().log(logMessage);
  }

  /**
   * Logs a message in the fine (verbose) Gadget log.
   *
   * @param objects the objects to lazily log
   */
  public static void logFineLazy(Object ... objects) {
    FINE_LOG.trace().logLazyObjects(objects);
  }

  /**
   * Logs a trace message in the Gadget developer log.
   *
   * @param logMessage the message to log
   */
  public static void developerLog(String logMessage) {
    DEVELOPER_LOG.trace().log(logMessage);
  }
}
