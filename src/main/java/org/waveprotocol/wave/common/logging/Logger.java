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

/**
 * A logger for logging text, escaped XML, or HTML messages.
 *
 */
public interface Logger {

  /**
   * Logs a basic message
   *
   * @param msg Message to log, expressed in HTML
   */
  void log(String msg);

  /**
   * If this logger is enabled, logs the provided objects to the sink.
   * Some sinks will stringify and concatenate these objects synchronously,
   * while others may delay these calls. As such, if an asyncronous sink is
   * used, provided objects should be immutable.
   * <p/>
   * If the logger is not enabled, the objects will not be sent to the sink,
   * and thus will not be stringified.
   *
   * @param messages messages to log
   */
  public void log(Object... messages);

  /**
   * Logs as plain text and not as HTML.
   *
   * @param msg Message to log as plain text
   */
  // TODO(user): eliminate this, its override, and logXml. Make escaping
  // the default and do it in the sinks.
  void logPlainText(String msg);

  /**
   * Logs as plain text and not as HTML.
   *
   * @param msg Message to log as plain text
   * @param t Exception to log
   */
  void logPlainText(String msg, Throwable t);

  /**
   * Logs an XML string (without interpreting the markup as HTML).
   *
   * @param xml
   * @deprecated use {@link #log} instead. XML will be escaped by default
   */
  @Deprecated
  void logXml(String xml);

  /**
   * Logs an Object using the toString method.
   *
   * @param label
   * @param o
   */
  void log(String label, Object o);

  /**
   * Logs a Throwable
   *
   * @param t
   */
  void log(Throwable t);

  /**
   * Logs a Throwable
   *
   * @param label
   * @param t
   */
  void log(String label, Throwable t);

  /**
   * Does not eagerly serialise the objects to strings when using
   * the in memory log sink, until the log is actually dumped.
   *
   * NOTE(danilatos): Temporary hack for charlie's demo. The
   * logging infrastructure needs to be properly refactored to
   * do this sort of thing in general.
   * <p>
   * TODO(michaell): Deprecate this method, and make all log methods log
   * lazily on the client (ie in the LogSink implementations).
   * @param objects
   */
  public void logLazyObjects(Object... objects);

  /**
   * Tests whether this logger is enabled.
   *
   * @return true if this logger is enabled; false if logging to this logger
   *         will have no effect.
   */
  boolean shouldLog();

  /**
   * NOOP implementation
   */
  public static final Logger NOP_IMPL = new Logger() {
    @Override
    public void log(String msg) {
    }

    @Override
    public void log(Object... messages) {
    }

    @Override
    public void log(String label, Object o) {
    }

    @Override
    public void log(Throwable t) {
    }

    @Override
    public void log(String label, Throwable t) {
    }

    @Override
    public void logLazyObjects(Object... objects) {
    }

    @Override
    public void logPlainText(String msg) {
    }

    @Override
    public void logPlainText(String msg, Throwable t) {
    }

    @Override
    public void logXml(String xml) {
    }

    @Override
    public boolean shouldLog() {
      return false;
    }
  };
}
