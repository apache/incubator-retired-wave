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

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Cookies;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.common.logging.AbstractLogger;
import org.waveprotocol.wave.common.logging.AbstractLogger.NonNotifyingLogger;
import org.waveprotocol.wave.common.logging.InMemoryLogSink;
import org.waveprotocol.wave.common.logging.LogSink;
import org.waveprotocol.wave.common.logging.LogUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * Debug logger
 */
public class DomLogger extends AbstractLogger implements NonNotifyingLogger {
  public static final String WEBDRIVER_GET_FATAL_ERROR_HOOK_NAME = "webdriverGetFatalError";

  public interface Resources extends ClientBundle {
    /** CSS class names used by Logger. These are used in Logger.css */
    interface Css extends CssResource {
      String panel();
      String entry();
      String module();
      String error();
      String fatal();
      String trace();
      String time();
      String msg();
    }

    @Source("Logger.css")
    Css css();
  }

  /** This class is to be injected that decides on how to handle errors. */
  public interface ErrorHandler {
    void handleClientErrors(Level level, Throwable t, Object... messages);
  }

   /**
    * @return content of the latest fatal error message if any.
    */
   @SuppressWarnings("unused")  // NOTE(user): Used by nativeSetupWebDriverTestPins()
   static String webdriverGetFatalError() {
     return DomLogger.latestFatalError;
   }

   private static native void nativeSetupWebDriverTestPins(String hookName) /*-{
   $wnd[hookName] = function() {
     return @org.waveprotocol.wave.client.debug.logger.DomLogger::webdriverGetFatalError()();
    }
   }-*/;

   /**
    * This is exclusively for Webdriver purpose.
    * It stores the content of the latest fatal error message if any.
    */
   private static String latestFatalError = "";

   static {
     // TODO(user): prevent exposing this hook if not in ll=debug mode.
     // Surprisingly doing a LogLevel.showDebug() at this location always returns false.
     if (GWT.isClient()) {
       DomLogger.nativeSetupWebDriverTestPins(WEBDRIVER_GET_FATAL_ERROR_HOOK_NAME);
     }
   }

   /** The singleton instance of handler error messages. */
  private static ErrorHandler errorHandler = null;

   /** Whether we should log to console instead of the dom for efficiency purposes. */
  private static boolean enableConsoleLogging = false;

  /** The singleton instance of our resources. */
  private static Resources RESOURCES = null;

  /**
   * Element the Logger will append log entries to. No logging
   * will take place when outputElm = null;
   */
  private static Element outputElm = null;

  /**
   * All log modules
   */
  private static Set<String> modules = new TreeSet<String>();

  /**
   * Modules to log to buffer.
   */
  private static HashSet<String> enabledModulesBuffer = new HashSet<String>();

  /**
   * In memory log buffer.
   */
  public static InMemoryLogSink logbuffer = new InMemoryLogSink();

  /**
   * Enabled log modules
   */
  private static HashSet<String> enabledModules = new HashSet<String>();

  private static ArrayList<LoggerListener> listeners = new ArrayList<LoggerListener>();

  /**
   * Only Logger's with level below this static will log
   */
  private static int maxLevel = Level.TRACE.value();

  /**
   * Cookie name
   */
  private static final String COOKIE_DEBUGLOG_MODULES = "wdm";

  static {
    if (GWT.isClient()) {
      RESOURCES = GWT.create(Resources.class);
      // Inject the CSS once.
      StyleInjector.inject(RESOURCES.css().getText());
      // NOTE(user): GWT.create fails if called outside GWTTestCase or the actual client.

      // Get enabled log modules from cookie
      String cookie = Cookies.getCookie(COOKIE_DEBUGLOG_MODULES);
      if (cookie != null) {
        String[] cookies = cookie.split("\\|");
        for (int i = 1; i < cookies.length; i++) {
          enabledModules.add(cookies[i]);
        }
      }
    }
  }

  /** Sets the handler for error messages. This must be set before any errors can be handled. */
  public static void setErrorHandler(ErrorHandler handler) {
    errorHandler = handler;
  }

  /** Sets whether we should use console logging */
  public static void setEnableConsoleLogging(boolean enable) {
    enableConsoleLogging = enable;
  }


  /**
   * Sets a cookie value with an expiry date a year from now.
   * @param key The name of the cookie to set.
   * @param value The new value for the cookie.
   */
  @SuppressWarnings("deprecation") // Calendar not supported by GWT
  public static void setCookieValue(String key, String value) {
    // Only set the cookie value if it is changing:
    if (value.equals(Cookies.getCookie(key))) {
      return;
    }

    // Set the cookie to expire in one year
    Date d = new Date();
    d.setYear(d.getYear() + 1);
    Cookies.setCookie(key, value, d);
  }

  /**
   * The Logger's module name
   */
  private final String module;

  /**
   * Sets the listener that is interested in logger events.
   */
  public static void addLoggerListener(LoggerListener listener) {
    DomLogger.listeners.add(listener);
  }

  /**
   * Removes the listener that is interested in logger events.
   */
  public static void removeLoggerListener(LoggerListener listener) {
    DomLogger.listeners.remove(listener);
  }

  private void triggerOnNewLogger(String loggerName) {
    for (LoggerListener l : listeners) {
      l.onNewLogger(loggerName);
    }
  }

  /**
   * Enables all logging (although logging is still subject to module
   * and max-level disabling)
   *
   * @param outputElm The Element to which Logger will
   * appendChild log entries
   */
  public static void enable(Element outputElm) {
    DomLogger.outputElm = outputElm;
    if (RESOURCES != null) {
      outputElm.addClassName(RESOURCES.css().panel());
    }
    if (shouldLogToConsole()) {
      appendEntry("Your debug output has been logged to the console.  " +
          "In firefox it is logged to FireBug's console.  " +
          "In Safari it is logged to the error console.", Level.ERROR);
    }
    maybeClearOutputCache();
  }

  /**
   * @return whether we should log to the brower's console
   */
  private static boolean shouldLogToConsole() {
    return enableConsoleLogging && consoleLoggingAvailable();
  }

  @Override
  public boolean isModuleEnabled() {
    return isModuleEnabled(module);
  }

  /**
   * Disables all logging (but remembers module and level disabling)
   */
  public static void disable() {
    DomLogger.outputElm = null;
  }

  /**
   * Set Loggers' maximum logging level
   *
   * @param maxLevel Only Loggers with value below this will log
   */
  public static void setMaxLevel(Level maxLevel) {
    DomLogger.maxLevel = maxLevel.value();
  }

  /**
   * Constructs a logger with the default log sink.
   * @param module
   */
  public DomLogger(String module) {
    this(module, new GWTLogSink(module));
  }

  /**
   * Constructs a Logger
   *
   * @param module Module string. Log entries will be prefixed with this string; and
   * logging can be enabled/disabled per-module. Loggers may share module string.
   */
  public DomLogger(String module, LogSink logSink) {
    super(logSink);
    if (!modules.contains(module)) {
      modules.add(module);
      triggerOnNewLogger(module);
    }
    this.module = module;
    if (GWT.isClient()) {
      setupNativeLogging(module);
    }
  }

  /**
   * A static log method for calling by JSNI methods, see {@link #setupNativeLogging(String)}
   * @param module
   *
   * @param msg
   */
  @SuppressWarnings("unused")
  private static void nativeLog(String module, String msg) {
    new DomLogger(module).trace().log(msg);
  }

  /**
   * A static logXml method for calling by JSNI methods, see {@link #setupNativeLogging(String)}
   *
   * @param module
   * @param xml
   */
  @SuppressWarnings("unused")
  private static void nativeLogXml(String module, String xml) {
    new DomLogger(module).trace().logXml(xml);
  }

  /**
   * A static log method for calling by JSNI methods, see {@link #setupNativeLogging(String)}
   *
   * @param module
   * @param label
   * @param javaObject
   */
  @SuppressWarnings("unused")
  private static void nativeLog(String module, String label, Object javaObject) {
    new DomLogger(module).trace().log(label, javaObject);
  }

  /**
   * Setup a few native methods for logging. Lets JSNI method call <module>Log(msg),
   * <module>LogXMl(xml), <module>LogJava(javaObject), and <module>LogObject(object).
   * NB: this puts limitations on logging module names. E.g., 'drag-drop' is outlawed,
   * as drag-dropLog would not be a legal function name.
   *
   * @param module
   */
  private native void setupNativeLogging(String module) /*-{
    if (typeof window[module + "Log"] == "undefined") {
      window[module + "Log"] = function(msg) {
        @org.waveprotocol.wave.client.debug.logger.DomLogger::nativeLog(Ljava/lang/String;Ljava/lang/String;)
        (module, msg);
      }
      window[module + "LogXml"] = function(xml) {
        @org.waveprotocol.wave.client.debug.logger.DomLogger::nativeLogXml(Ljava/lang/String;Ljava/lang/String;)
        (module, xml);
      }
      window[module + "LogJava"] = function(label, javaObject) {
        @org.waveprotocol.wave.client.debug.logger.DomLogger::nativeLog(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)
        (module, label, javaObject);
      }
      window[module + "LogObject"] = function(object) {
        var msg = '';
        for (a in object) {
          msg += a + ': ' + object[a] + '<br/>';
        }
        @org.waveprotocol.wave.client.debug.logger.DomLogger::nativeLog(Ljava/lang/String;Ljava/lang/String;)
        (module, msg);
      }
    }
  }-*/;

  /**
   * Toggles whether this logger's module is enabled/disabled
   */
  public void toggleModule() {
    toggleModule(module);
  }

  /**
   * Toggles whether a module is enabled/disabled
   *
   * @param module
   */
  public static void toggleModule(String module) {
    enableModule(module, !isModuleEnabled(module));
  }

  /**
   * Enables/disables this logger's module
   *
   * @param enable True if this logger's module should be enabled;
   * false otherwise
   */
  public void enableModule(boolean enable) {
    enableModule(module, enable);
  }

  /**
   * Enables/disables buffer for this log's module
   *
   * @param enable True if module should be enabled; false otherwise
   */
  public void enableModuleBuffer(boolean enable) {
    enableModuleBuffer(module, enable);
  }

  /**
   * Enables/disables log module
   *
   * @param module
   * @param enable True if module should be enabled; false otherwise
   */
  public static void enableModule(String module, boolean enable) {
    if (enable) {
      enabledModules.add(module);
    } else {
      enabledModules.remove(module);
    }
    persistEnabledModules();
  }

  /**
   * Enables/disables buffer for log module
   *
   * @param module
   * @param enable True if module should be enabled; false otherwise
   */
  public static void enableModuleBuffer(String module, boolean enable) {
    if (enable) {
      enabledModulesBuffer.add(module);
    } else {
      enabledModulesBuffer.remove(module);
    }
  }

  /**
   * Enables all (known) modules
   */
  public static void enableAllModules() {
    enabledModules.addAll(modules);
  }

  /**
   * Disables all modules
   */
  public static void disableAllModules() {
    enabledModules.clear();
  }

  /**
   * @param module
   * @return True if module is enabled.
   */
  public static boolean isModuleEnabled(String module) {
    return enabledModules.contains(module);
  }

  /**
   * @return True if the module is enabled and global logging is enabled.
   */
  public boolean isModuleLoggingEnabled() {
    return isModuleEnabled(module);
  }

  /**
   * Persists enabled module names in cookie
   */
  private static void persistEnabledModules() {
    String cookie = "|";
    for (String s : enabledModules) {
      cookie += s + "|";
    }
    try {
      setCookieValue(COOKIE_DEBUGLOG_MODULES, cookie);
    } catch (Error ignoreOutsideGWT) {
      // NOTE(user): this fails with an UnsatisfiedLinkError when
      // running tests outside GWTTestCase, it's not required for the client to
      // function so this makes it more robust, I should catch
      // UnsatisfiedLinkError but that class is not available in GWT
    }
  }

  /**
   * @return All modules
   */
  public static Collection<String> getModules() {
    return modules;
  }

  /**
   * Turns attribute of element into string suitable for human consumption
   *
   * @param e
   * @param attr
   * @return String rendering of attribute
   */
  private static String attr(Element e, String attr) {
    String a = e.getAttribute(attr);
    return a == null ? "" : " " + attr + "='" + a + "'";
  }

  /**
   * Shortens a string to 10 chars by adding '...'
   *
   * @param in
   * @return Shortened version of in
   */
  private static String shortString(String in) {
    return in.length() <= 10 ? in : in.substring(0, 9) + "...";
  }

  /**
   * Outputs debug-string representing a DOM element
   *
   * @param e
   * @return XML rendering of element
   */
  @SuppressWarnings("unused")
  private static String toXml(Element e) {
    String out = "";
    if (DomHelper.isTextNode(e)) {
      out = "'" + shortString(e.getNodeValue()) + "'";
    } else {
      String tagName = e.getTagName();
      out = "<" + tagName;
      if (tagName.equals("INPUT")) {
        out += attr(e, "type");
        out += attr(e, "value");
      }
      out += attr(e, "class");
      if (tagName.equals("A")) {
        out += ">" + e.getInnerText() + "</A>";
      } else {
        out += "/>";
      }
    }
    return out;
  }

  private static boolean shouldLogToBuffer(String module, Level level) {
    // Buffer all error messages.
    return enabledModulesBuffer.contains(module) || level.value() <= Level.ERROR.value();
  }

  private static boolean shouldLogToPanel(String module, Level level) {
    // Always log fatal messages.
    if (level.value() == Level.FATAL.value()) {
      return true;
    }

    return outputElm != null && enabledModules.contains(module) &&
        level.value() <= maxLevel;
  }

  /**
   * {@inheritDoc}
   *
   * @return If Logger should log based on module, level and
   * whether logging system is enabled
   */
  @Override
  protected boolean shouldLog(Level level) {
    // For production and unit-tests, don't log full stack traces:
    // NOTE(user): LogLevel.showErrors() indirectly causes a GWT.create, so
    //     guard by GWT.isClient().
    boolean shouldShowErrorDetail = GWT.isClient() && LogLevel.showErrors();

    // Only log in client/GWTTestCases when logging is not disabled.
    return shouldShowErrorDetail
           && (shouldLogToBuffer(module, level) || shouldLogToPanel(module, level));
  }

  /**
   * Formats a log entry.
   * @param module
   * @param msg
   * @param timestamp
   */
  public static String formatLogEntry(String module, String msg, String timestamp) {
    return "<span class='" + RESOURCES.css().module() + "'>" + module + "</span> "
        + "<span class='" + RESOURCES.css().time() + "'>(" + timeStamp() + "):</span> "
        + "<span class='" + RESOURCES.css().msg() + "'>" + msg + "</span>";
  }

  /**
   * Appends an entry to the log panel.
   * @param formatted
   * @param level
   */
  public static void appendEntry(String formatted, Level level) {
    DivElement entry = Document.get().createDivElement();
    entry.setClassName(RESOURCES.css().entry());
    entry.setInnerHTML(formatted);

    // Add the style name associated with the log level.
    switch (level) {
      case ERROR:
        entry.addClassName(RESOURCES.css().error());
        break;
      case FATAL:
        entry.addClassName(RESOURCES.css().fatal());
        break;
      case TRACE:
        entry.addClassName(RESOURCES.css().trace());
        break;
    }

    // Make fatals detectable by WebDriver, so that tests can early out on
    // failure:
    if (level.equals(Level.FATAL)) {
      latestFatalError = formatted;
    }
    writeOrCacheOutput(entry);
  }

  /** List of output divs waiting to be written to the log panel. */
  private static List<DivElement> outputCache = new ArrayList<DivElement>();

  /** Write output or cache it if the output element is not set. */
  private static void writeOrCacheOutput(DivElement element) {
    outputCache.add(element);
    maybeClearOutputCache();
  }

  /** Clear the output cache if the output element is set. */
  private static void maybeClearOutputCache() {
    if (outputElm != null) {
      for (DivElement e : outputCache) {
        outputElm.appendChild(e);
      }
      outputCache.clear();
      outputElm.setScrollTop(1000000);
    }
  }

  @Override
  protected void logPlainTextInner(Level level, String msg) {
    // if we are not logging to console, we want to escape.
    if (!shouldLogToConsole()) {
      super.logPlainTextInner(level, msg);
    } else {
      doLog(level, msg);
    }
  }

  @Override
  public void logWithoutNotifying(String message, Level level) {
    assert this.sink instanceof GWTLogSink :
        "we pass a GWTLogSink in the constructor so this cast should always be safe";
    ((GWTLogSink) this.sink).writeLog(message, level);
  }

  private static void notifyLoggerListenersIfErrorOrFatal(Level level) {
    if (level == Level.ERROR) {
      for (LoggerListener l : listeners) {
        l.onError();
      }
    } else if (level == Level.FATAL) {
      for (LoggerListener l : listeners) {
        l.onFatal();
      }
    }
  }

  /**
   * Log output sink to Debug panel.
   */
  public static final class GWTLogSink extends LogSink {
    private final String module;

    public GWTLogSink(String module) {
      this.module = module;
    }

    @Override
    public void log(Level level, String message) {
      lazyLog(level, message);
    }

    @Override
    public void lazyLog(Level level, Object... messages) {
      String formatted = "";
      boolean shouldLogToBuffer = shouldLogToBuffer(module, level);
      boolean shouldLogToPanel = shouldLogToPanel(module, level);
      if (shouldLogToBuffer) {
        logbuffer.lazyLog(level, messages);
      }
      if (shouldLogToPanel) {
        formatted = formatLogEntry(module, LogUtils.stringifyLogObject(messages), timeStamp());
        if (shouldLogToConsole()) {
          // bring up the panel to notify the user that there is something to show.
          if (level == Level.FATAL) {
            showOutput();
          }
          String t = module + " (" + timeStamp() + "): " + formatted;
          logToConsole(t);
        } else {
          logToPanel(formatted, level);
        }
      }

      notifyLoggerListenersIfErrorOrFatal(level);
    }

    /**
     * Write log without invoking listeners.
     */
    private void writeLog(String msg, Level level) {
      String formatted = "";
      boolean shouldLogToBuffer = shouldLogToBuffer(module, level);
      boolean shouldLogToPanel = shouldLogToPanel(module, level);
      if (shouldLogToBuffer || shouldLogToPanel) {
        formatted = formatLogEntry(module, msg, timeStamp());
      }
      if (shouldLogToBuffer) {
        logbuffer.log(level, formatted);
      }
      if (shouldLogToPanel) {
        if (shouldLogToConsole()) {
          logToConsole(formatted);
        } else {
          logToPanel(formatted, level);
        }
      }
    }

    private void logToPanel(String formatted, Level level) {
      showOutput();
      appendEntry(formatted, level);
    }
  }

  static final NumberFormat TIMESTAMP_FORMAT = (GWT.isClient() ?
      NumberFormat.getFormat("0000000000.000") : null);

  /**
   * @return Timestamp for use in log
   */
  private static String timeStamp() {
    double ts = Duration.currentTimeMillis() / 1000.0;
    // divide the startTime to second from millsecond and seconds is much easier to read
    // for the user.
    if (TIMESTAMP_FORMAT != null) {
      return TIMESTAMP_FORMAT.format(ts);
    } else {
      return Double.toString(ts);
    }
  }

  /**
   * Trigger onNeedOutput on all listeners.
   */
  public static void showOutput() {
    for (LoggerListener l : listeners) {
      l.onNeedOutput();
    }
  }

  @Override
  protected void handleClientErrors(Level level, Throwable t, Object... messages) {
    if (errorHandler != null) {
      errorHandler.handleClientErrors(level, t, messages);
    }
  }

  public static native void logToConsole(String msg) /*-{
    var log = $wnd.console;
    if (log) {
      if (log.markTimeline) {
        log.markTimeline(msg);
      }
      if (log.log) {
        log.log(msg);
      }
    }
  }-*/;

  public static native boolean consoleLoggingAvailable() /*-{
    return !!($wnd.console);
  }-*/;
}
