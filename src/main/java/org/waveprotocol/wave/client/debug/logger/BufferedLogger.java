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

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import org.waveprotocol.wave.common.logging.LogSink;
import org.waveprotocol.wave.common.logging.LogUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Logger that buffers messages and only outputs to DOM on a deferred command.
 *
 * Logging to the DOM can be slow and can also interfere with other browser
 * operations. Use this logger to buffer messages and only output to DOM on a
 * deferred command. Note, when invocations to a buffered logger is interleaved
 * with invocations to a normal logger, output to the buffered logger will
 * always appear last.
 *
 */
public class BufferedLogger extends DomLogger {
  private static final class BufferedLogSink extends LogSink {
    private static final class LogMessage {
      private final Object message;
      private final Level level;
      LogMessage(Object message, Level level) {
        this.message = message;
        this.level = level;
      }

      void logToSink(LogSink logSink) {
        logSink.log(level, LogUtils.stringifyLogObject(message));
      }
    }

    /**
     * Buffer for log messages.
     */
    private final List<LogMessage> buffer = new ArrayList<LogMessage>();
    private final LogSink outputSink;

    BufferedLogSink(LogSink outputSink) {
      this.outputSink = outputSink;
    }

    /**
     * Flush contents of buffer to output.
     */
    private final Command flush = new Command() {
      public void execute() {
        for (LogMessage m : buffer) {
          m.logToSink(outputSink);
        }
        buffer.clear();
      }
    };

    @Override
    public void log(Level level, String msg) {
      lazyLog(level, msg);
    }

    @Override
    public void lazyLog(Level level, Object... messages) {
      // NOTE(user): It is best to assume that DeferredCommand can execute
      // immediately.
      // NOTE(danilatos): We use DeferredCommand rather than
      // org.waveprotocol.wave.Scheduler, so that we can log in
      // the Scheduler implementations.
      boolean doFlush = buffer.isEmpty();
      buffer.add(new LogMessage(messages, level));
      if (doFlush) {
        DeferredCommand.addCommand(flush);
      }
    }
  }

  /**
   * @param module
   */
  public BufferedLogger(String module) {
    super(module, new BufferedLogSink(new GWTLogSink(module)));
  }
}
