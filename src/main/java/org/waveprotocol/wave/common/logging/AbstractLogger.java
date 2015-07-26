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
 * Skeleton debug logger.
 *
 * Implementations must override {@link #shouldLog}
 *
 */
public abstract class AbstractLogger implements LoggerBundle {
  /**
   * Constants for describing log levels
   */
  public enum Level {
    FATAL("fatal", 0),
    ERROR("error", 1),
    TRACE("trace", 2);
    Level(String string, int value) {
      this.string = string;
      this.value = value;
    }
    private final String string;
    private final int value;
    public int value() { return value; }
    @Override public String toString() { return string; }
    /**
     * Return the Level that corresponds to an int value.
     */
    public static Level getLevelByValue(int value) {
      if (value == TRACE.value()) {
        return TRACE;
      } else if (value == ERROR.value()) {
        return ERROR;
      } else {
        return FATAL;
      }
    }
  }

  private final LevelLogger errorLogger;
  private final LevelLogger fatalLogger;
  private final LevelLogger traceLogger;

  protected final LogSink sink;

  public AbstractLogger(LogSink sink) {
    this.sink = sink;
    errorLogger = new LevelLogger(Level.ERROR);
    fatalLogger = new LevelLogger(Level.FATAL);
    traceLogger = new LevelLogger(Level.TRACE);
  }

  @Override
  public final void log(Level level, Object... messages) {
    handleClientErrors(level, null, messages);
    if (shouldLog(level)) {
      doLog(level, messages);
    }
  }

  @Override
  public final Logger trace() {
    return traceLogger;
  }

  @Override
  public final Logger error() {
    return errorLogger;
  }

  @Override
  public final Logger fatal() {
    return fatalLogger;
  }

  // TODO(user): Eliminate this method. Make plain text the default and do the
  // escaping / wrapping in the sinks where appropriate. Add a log(SafeHtml) method
  // for cases where we actually want to output HTML
  protected void logPlainTextInner(Level level, String msg) {
    doLog(level, "<pre style='display:inline'>", LogUtils.xmlEscape(msg), "</pre>");
  }

  /**
   * Logs messages to the sink.
   *
   * @param level level to log at
   * @param msgs Message to log
   */
  protected final void doLog(Level level, Object... msgs) {
    sink.lazyLog(level, msgs);
  }

  /**
   * Determines whether this bundle is interested in logging events at
   * the provided level.
   *
   * @param level the level to check
   * @return true if the provided level is loggable
   */
  protected abstract boolean shouldLog(Level level);

  /**
   * Should only be overridden by client.debug.logger.DomLogger. Performs error
   * handling that is specific to the client.
   *
   * @param level the level of the messages to be logged
   * @param t The exception. If null, a ClientLogException is created.
   * @param messages Messages to be concatenated to create the log message.
   */
  // TODO(user): Eliminate this method. The client should handle its errors independently of
  // the logging infrastructure
  protected void handleClientErrors(Level level, Throwable t, Object... messages) {
    return;
  }

  /**
   * Represents an instance of a Logger, curried with a particular log level.
   * It's required to support the {@code bundle.fine().log("foo");}, pattern
   * in the LoggerBundle API.
   */
  private final class LevelLogger implements Logger, NonNotifyingLogger {
    private final Level level;

    public LevelLogger(Level level) {
      this.level = level;
    }

    @Override
    public void log(String msg) {
      handleClientErrors(level, null, msg);
      if (shouldLog()) {
        doLog(level, msg);
      }
    }

    @Override
    public void log(Object... messages) {
      handleClientErrors(level, null, messages);
      if (shouldLog()) {
        doLog(level, messages);
      }
    }

    @Override
    public void logPlainText(String msg) {
      handleClientErrors(level, null, msg);
      if (shouldLog()) {
        // display:inline is nicer to display things because it does not have
        // extra padding at the top and bottom of the message.
        // This makes editor-io log look nice at least.
        logPlainTextInner(level, msg);
      }
    }

    @Override
    public void logPlainText(String msg, Throwable t) {
      handleClientErrors(level, t, msg);
      if (shouldLog()) {
        logPlainTextInner(level, msg + t.toString());
      }
    }

    @Override
    public void logXml(String xml) {
      handleClientErrors(level, null, xml);
      if (shouldLog()) {
        doLog(level, LogUtils.xmlEscape(xml));
      }
    }

    @Override
    public void log(String label, Object o) {
      handleClientErrors(level, null, label, o);
      if (shouldLog()) {
        doLog(level, (label.length() > 0 ? label + ": " : ""), LogUtils.printObjectAsHtml(o));
      }
    }

    @Override
    public void log(Throwable t) {
      handleClientErrors(level, t, t.getMessage());
      if (shouldLog()) {
        sink.lazyLog(level, t.toString(), t);
      }
    }

    @Override
    public void log(String label, Throwable t) {
      handleClientErrors(level, t, label);
      if (shouldLog()) {
        sink.lazyLog(level, label, t);
      }
    }

    @Override
    public void logLazyObjects(Object... objects) {
      handleClientErrors(level, null, objects);
      if (shouldLog()) {
        doLog(level, objects);
      }
    }

    @Override
    public boolean shouldLog() {
      return AbstractLogger.this.shouldLog(level);
    }

    @Override
    public void logWithoutNotifying(String message, Level level) {
      if (AbstractLogger.this instanceof NonNotifyingLogger) {
        ((NonNotifyingLogger) AbstractLogger.this).logWithoutNotifying(message, level);
      }
    }
  }

  /**
   * A logger which has a logWithoutNotifying method. Note that this does not
   * extend Logger because it's used by the LevelLogger above, as well as
   * by DomLogger (to avoid this package depending on the client).
   */
  public interface NonNotifyingLogger {
    /**
     * Write a log entry without notifying the listeners. This is used internally
     * by the logging framework to write extra information about a log entry that
     * has already been sent to the log listeners. This method is required to
     * prevent looping of returned deobfuscated stack traces from WFE.
     *
     * @param message message to log
     * @param level level to log the message at
     */
    void logWithoutNotifying(String message, Level level);
  }
}
