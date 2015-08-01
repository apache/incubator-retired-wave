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

package org.waveprotocol.wave.common.logging;

import org.waveprotocol.wave.common.logging.AbstractLogger.Level;

/**
 * A collection of levelled loggers.
 *
 */
public interface LoggerBundle {

  /**
   * Concatenates the toString() output of each of the provided message
   * Objects and logs the result iff the supplied level is loggable by this
   * logger. That is, if the message is not loggable the messages will not be
   * stringified nor concatenated.
   *
   * @param level the level to log at
   * @param messages messages to log
   */
  void log(Level level, Object... messages);

  /**
   * Gets a TRACE level logger.
   *
   * @return a trace logger.
   */
  Logger trace();

  /**
   * Gets an ERROR level logger.
   *
   * @return an error logger.
   */
  Logger error();

  /**
   * Gets a FATAL level logger.
   *
   * @return a fatal logger.
   */
  Logger fatal();

  /**
   * Tests if this logger bundle is enabled.  If enabled, then at least one
   * logger is potentially enabled (but not necessarily - each logger has its
   * own {@link Logger#shouldLog() enabled} state.
   *
   * @return true if logging is generally enabled.
   */
  boolean isModuleEnabled();

  /**
   * NOOP implementation.
   */
  public static final LoggerBundle NOP_IMPL = new LoggerBundle() {
    @Override
    public Logger error() {
      return Logger.NOP_IMPL;
    }

    @Override
    public Logger fatal() {
      return Logger.NOP_IMPL;
    }

    @Override
    public Logger trace() {
      return Logger.NOP_IMPL;
    }

    @Override
    public boolean isModuleEnabled() {
      return false;
    }

    @Override
    public void log(Level level, Object... messages) {}
  };
}
