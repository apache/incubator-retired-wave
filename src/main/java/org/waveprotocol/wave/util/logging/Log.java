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

package org.waveprotocol.wave.util.logging;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Standard logging class, wraps a java.util.logging.Logger,
 * augmenting it with convenience methods. Also provides a mapped diagnostic
 * context for each thread. This context information is prepended to messages
 * logged from that thread.
 *
 * The functionality to infer the caller of a logging method has been replicated
 * from {@link LogRecord} because there was no easy way to use it correctly from
 * this wrapper class.
 *
 *
 */
public class Log {

  /**
   * Version of LogRecord that can correctly infer its caller.
   */
  @VisibleForTesting
  static class MyLogRecord extends LogRecord {

    /**
     * Finds the caller of this class. Algorithm derived from code in
     * {@link java.util.logging.Logger}. This method necessary because it is not
     * possible to wrap logger in a way that allows the caller to be derived.
     *
     * @return information about the function that called into this class, or
     *         <code>null</code> if not found.
     */
    @VisibleForTesting
    static StackTraceElement findCaller() {

      // Get the stack trace.
      StackTraceElement stack[] = (new Throwable()).getStackTrace();
      String logClassName = Log.class.getName();

      // Scan down to a call to Log
      int i = 0;
      while (i < stack.length // \u2620
          && !logClassName.equals(stack[i].getClassName())) {
        i++;
      }

      // Scan down through further to first one to call Log
      while (i < stack.length // \u2620
          && logClassName.equals(stack[i].getClassName())) {
        i++;
      }
      return (i < stack.length ? stack[i] : null);
    }

    /** Mimics {@link LogRecord}'s needToInferCaller from superclass. */
    private boolean needToInferCaller = true;

    /** Constructor. Mimics signature of superclass. */
    public MyLogRecord(Level level, String msg, Throwable t) {
      super(level, msg);
      setThrown(t);
    }

    /** {@inheritDoc} */
    @Override
    public String getSourceClassName() {
      if (needToInferCaller) {
        inferCaller();
      }
      return super.getSourceClassName();
    }

    /** {@inheritDoc} */
    @Override
    public String getSourceMethodName() {
      if (needToInferCaller) {
        inferCaller();
      }
      return super.getSourceMethodName();
    }

    /** Infers the caller's source class and method name. */
    private void inferCaller() {
      needToInferCaller = false;

      StackTraceElement caller = findCaller();

      if (caller != null) {
        super.setSourceClassName(caller.getClassName());
        super.setSourceMethodName(caller.getMethodName());
      } else {
        super.setSourceClassName(null);
        super.setSourceMethodName(null);
      }
    }

    /** {@inheritDoc} */
    @Override
    public void setSourceClassName(String sourceClassName) {
      needToInferCaller = false;
      super.setSourceClassName(sourceClassName);
    }

    /** {@inheritDoc} */
    @Override
    public void setSourceMethodName(String sourceMethodName) {
      needToInferCaller = false;
      super.setSourceMethodName(sourceMethodName);
    }

  }

  /** Per-thread mapped diagnostic context. */
  private static final ThreadLocal<Map<String, String>> context =
      new ThreadLocal<Map<String, String>>() {
        /** {@inheritDoc} */
        @Override
        protected Map<String, String> initialValue() {
          // predictable iteration order gives nicer log messages
          return Maps.newLinkedHashMap();
        }
      };

  /**
   * Creates or lazily loads a logger. Currently just creates an instance but
   * allows us to do more clever stuff in the future.
   */
  public static Log get(Class<? extends Object> clazz) {
    return new Log(clazz);
  }

  /** The underlying logger to which this object logs messages. */
  private final Logger logger;

  /**
   * Creates a new logging object.
   *
   * @deprecated use {@link #get(Class)} instead.
   */
  @Deprecated
  private Log(Class<? extends Object> clazz) {
    this(Logger.getLogger(clazz.getName()));
  }

  /**
   * Creates a new logging object that will ultimately log records through the
   * given logger.
   */
  public Log(Logger logger) {
    this.logger = logger;
  }

  /**
   * Logs a message at level CONFIG.
   *
   * @param msg The message to log.
   */
  public void config(String msg) {
    log(Level.CONFIG, msg, null);
  }

  /**
   * Logs a message at level CONFIG.
   *
   * @param msg The message to log.
   * @param t The throwable to log with the message.
   */
  public void config(String msg, Throwable t) {
    log(Level.CONFIG, msg, t);
  }

  private String contextualiseMessage(String message) {
    Map<String, String> contextData = context.get();
    if (contextData.isEmpty()) {
      return message;
    } else {
      StringBuilder result = new StringBuilder();
      result.append(message);
      result.append(" -- ");
      result.append(contextData);
      return result.toString();
    }
  }

  /**
   * Logs a message at level FINE.
   *
   * @param msg The message to log.
   */
  public void fine(String msg) {
    log(Level.FINE, msg, null);
  }

  /**
   * Logs a message at level FINE.
   *
   * @param msg The message to log.
   * @param t The throwable to log with the message.
   */
  public void fine(String msg, Throwable t) {
    log(Level.FINE, msg, t);
  }

  /**
   * Logs a message at level FINER.
   *
   * @param msg The message to log.
   */
  public void finer(String msg) {
    log(Level.FINER, msg, null);
  }

  /**
   * Logs a message at level FINER.
   *
   * @param msg The message to log.
   * @param t The throwable to log with the message.
   */
  public void finer(String msg, Throwable t) {
    log(Level.FINER, msg, t);
  }

  /**
   * Logs a message at level FINEST.
   *
   * @param msg The message to log.
   */
  public void finest(String msg) {
    log(Level.FINEST, msg, null);
  }

  /**
   * Logs a message at level FINEST.
   *
   * @param msg The message to log.
   * @param t The throwable to log with the message.
   */
  public void finest(String msg, Throwable t) {
    log(Level.FINEST, msg, t);
  }

  /**
   * Gets the underlying logger.
   */
  public Logger getLogger() {
    return logger;
  }

  /**
   * Logs a message at level INFO.
   *
   * @param msg The message to log.
   */
  public void info(String msg) {
    log(Level.INFO, msg, null);
  }

  /**
   * Logs a message at level INFO.
   *
   * @param msg The message to log.
   * @param t The throwable to log with the message.
   */
  public void info(String msg, Throwable t) {
    log(Level.INFO, msg, t);
  }

  /**
   * Whether CONFIG messages are being output.
   */
  public boolean isConfigLoggable() {
    return logger.isLoggable(Level.CONFIG);
  }

  /**
   * Whether FINE messages are being output.
   */
  public boolean isFineLoggable() {
    return logger.isLoggable(Level.FINE);
  }

  /**
   * Whether FINER messages are being output.
   */
  public boolean isFinerLoggable() {
    return logger.isLoggable(Level.FINER);
  }

  /**
   * Whether FINEST messages are being output.
   */
  public boolean isFinestLoggable() {
    return logger.isLoggable(Level.FINEST);
  }

  /**
   * Whether INFO messages are being output.
   */
  public boolean isInfoLoggable() {
    return logger.isLoggable(Level.INFO);
  }

  /**
   * Whether SEVERE messages are being output.
   */
  public boolean isSevereLoggable() {
    return logger.isLoggable(Level.SEVERE);
  }

  /**
   * Whether WARNING messages are being output.
   */
  public boolean isWarningLoggable() {
    return logger.isLoggable(Level.WARNING);
  }

  /**
   * Generic log a message, prepended with the current thread's log context, at
   * a given level.
   */
  public void log(Level level, String msg, Throwable t) {
    if (!logger.isLoggable(level)) {
      return;
    }

    LogRecord record = new MyLogRecord(level, contextualiseMessage(msg), t);
    logger.log(record);
  }

  /** Saves a key/value pair into this thread's log context. */
  public void putContext(String key, String value) {
    context.get().put(key, value);
  }

  /**
   * Removes a key from this thread's log context.
   *
   * @param key The key to remove.
   * @return true if there was a value previously associated with the key, false
   *         if this call resulted in no changes being made.
   */
  public boolean removeContext(String key) {
    return context.get().remove(key) != null;
  }

  /**
   * Logs a message at level SEVERE.
   *
   * @param msg The message to log.
   */
  public void severe(String msg) {
    log(Level.SEVERE, msg, null);
  }

  /**
   * Logs a message at level SEVERE along with an exception that can be
   * extracted from the logs and reported.
   *
   * @param msg The message to log.
   * @param t The throwable to log with the message.
   */
  public void severe(String msg, Throwable t) {
    log(Level.SEVERE, msg, t);
  }

  /**
   * Logs a message at level WARNING.
   *
   * @param msg The message to log.
   */
  public void warning(String msg) {
    log(Level.WARNING, msg, null);
  }

  /**
   * Logs a message at level WARNING along with an exception that can be
   * extracted from the logs and reported.
   *
   * @param msg The message to log.
   * @param t The throwable to log with the message.
   */
  public void warning(String msg, Throwable t) {
    log(Level.WARNING, msg, t);
  }
}
