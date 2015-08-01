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
 * Base class for log entry sinks.
 *
 */
public abstract class LogSink {
  /**
   * Outputs a log entry to this sink.
   *
   * @param level the level to log at
   * @param message the message to log
   */
  public abstract void log(Level level, String message);

  /**
   * All implementations of this method which are intended to run in the
   * browser must buffer output before writing in order to avoid slowing
   * down the UI. For these cases, messages must not be stringified until
   * they are to be written.
   *
   * @param level the level to log at
   * @param messages list of messages to (potentially lazily) stringify and
   * concatenate
   *
   * NOTE: This method should not have a default implementation which does
   * any stringification, because if a LogSink implementation were to
   * neglect to override this method, and were used in the browser, there
   * may be significant performance implications.
   */
  public abstract void lazyLog(Level level, Object... messages);

  /**
   * Lazily logs a message and a throwable. This implementation XML-escapes
   * the label, and the message from the throwable, and prints the stack trace as
   * HTML. Sinks which are not intended to output HTML should override this method.
   *
   * @param level the level to log at
   * @param label a label to log
   * @param t the throwable to log
   *
   * NOTE: This method should not have a default implementation which does
   * any stringification, because if a LogSink implementation were to
   * neglect to override this method, and were used in the browser, there
   * may be significant performance implications.
   */
  public void lazyLog(Level level, String label, final Throwable t) {
    lazyLog(level, label, ": ", new Object() {
      @Override
      public String toString() {
        return LogUtils.xmlEscape(t.toString()) + "<br/>" + LogUtils.printStackTraceAsHtml(t);
      }
    });
  }
}
