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

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Simple, GWT-independent logger for unit tests and such.
 * Accumulates all log lines and returns them in toString().
 * Also echoes the lines to the specified streams, e.g., System.out or System.err.
 *
 * @author jochen@google.com (Jochen Bekmann)
 * @author soren@google.com (Soren Lassen)
 */
// TODO(user): Combine this and WallyLogger into a single ServerLogger class
public class PrintLogger extends AbstractLogger {
  private static class NoOutputStream extends OutputStream {
    // NOTE(user):
    //   Eclipse complains that this should have an @Override.  However, GWT does not support
    //   @Override except for implementation overriding (i.e., neither interface implementation nor
    //   abstract-method implementation).  So ideally, we'd use SuppressWarnings("over-ann").
    //   However, either Eclipse or javac seems not to support that either.
    @SuppressWarnings("all")
    public void write(int b) { /* Discard the byte in b. */  }
  }

  private static final class PrintLoggerSink extends LogSink {
    private final StringBuilder log = new StringBuilder();
    private final PrintStream echoPrinter;
    private Level allowedMinLogLevel = Level.FATAL;

    public PrintLoggerSink(PrintStream echoPrinter) {
      this.echoPrinter = echoPrinter;
    }

    @Override
    public void log(Level level, String msg) {
      log.append(msg).append("\n");
      echoPrinter.println(msg);

      if (level.value() < allowedMinLogLevel.value()) {
        throw new IllegalArgumentException("Cannot log below the [min log level:" +
            allowedMinLogLevel + "] with [log level:" + level + "] and [msg:" + msg + "]");
      }
    }

    public void setAllowedMinLogLevel(Level allowedMinLogLevel) {
      this.allowedMinLogLevel = allowedMinLogLevel;
    }

    @Override
    public String toString() {
      return log.toString();
    }

    @Override
    public void lazyLog(Level level, Object... messages) {
      log(level, LogUtils.stringifyLogObject(messages));
    }
  }

  private final PrintLoggerSink loggerSink;

  /** Echoes log lines to echoOutput in addition to accumulating log lines for toString(). */
  private PrintLogger(PrintLoggerSink loggerSink) {
    super(loggerSink);
    this.loggerSink = loggerSink;
  }

  /** Echoes nothing, only accumulates log lines for toString(). */
  public PrintLogger() {
    this(new PrintLoggerSink(new PrintStream(new NoOutputStream())));
  }

  /**
   * @return All log lines, regardless of whether the PrintLogger is enabled.
   */
  @Override
  public String toString() {
    return loggerSink.toString();
  }

  @Override
  public boolean isModuleEnabled() {
    return true;
  }

  @Override
  public boolean shouldLog(Level level) {
    return true;
  }

  /**
   * @param allowedMinLogLevel If logging happens below this level an IllegalArgumentException
   *    will be thrown. This is useful in tests to check we don't log fatals, as the client
   *    takes that as an unrecoverable error and stops working.
   */
  public void setAllowedMinLogLevel(Level allowedMinLogLevel) {
    loggerSink.setAllowedMinLogLevel(allowedMinLogLevel);
  }
}
