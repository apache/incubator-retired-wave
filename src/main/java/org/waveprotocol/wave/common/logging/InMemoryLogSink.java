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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * An in-memory log sink.
 *
 * This buffers log entries to a circular buffer to be retrieved later.
 *
 */
// TODO(user): Combine this, DomLogger.GWTLogSink and BufferedLogger.BufferedLogSink
// into a single class
public final class InMemoryLogSink extends LogSink {
  private static final int MAX_ENTRIES = 2000;
  private final CircularBuffer<Object> buffer = new CircularBuffer<Object>(MAX_ENTRIES);

  private final Set<LogSink> sinks = new HashSet<LogSink>();

  @Override
  public void log(Level level, String message) {
    buffer.add(message);
    for (LogSink s : sinks) {
      s.log(level, message);
    }
  }

  @Override
  public void lazyLog(Level level, Object... messages) {
    buffer.add(messages);
    if (!sinks.isEmpty()) {
      for (LogSink s : sinks) {
        s.lazyLog(level, messages);
      }
    }
  }

  /**
   * Returns all log entries.
   */
  public List<String> showAll() {
    List<String> ret = new ArrayList<String>(buffer.size());
    for (Object o : buffer) {
      ret.add(LogUtils.stringifyLogObject(o));
    }
    return ret;
  }

  /**
   * Clears the log buffer.
   */
  public void clear() {
    buffer.clear();
  }

  /**
   * NOTE(danilatos): Do not chain a sink in any way that could cause
   * slowness!!! The logging stuff should all be overhauled.
   *
   * Adds a log sink to allow forwarding.
   * @param sink
   */
  public void addLogSink_DO_NOT_USE(LogSink sink) {
    sinks.add(sink);
  }

  /**
   * Remove log sink from listening to this.
   * @param sink
   */
  public void removeLogSink(LogSink sink) {
    sinks.remove(sink);
  }
}
