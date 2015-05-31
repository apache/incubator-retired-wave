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

package org.waveprotocol.box.server.util.testing;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A log handler which throws a runtime exception at and above a set log level.
 * Useful for catching erroneous conditions during testing, which are only
 * reported to the logs.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public class ExceptionLogHandler extends Handler {
  /** The integer value of the minimum fatal log level. */
  private final int fatalLevel;

  /**
   * Constructs an ExceptionLogHandler that throws a runtime exception at and above the specified
   * log level.
   *
   * @param fatalLevel the minimum log level for which to throw an exception.
   */
  public ExceptionLogHandler(Level fatalLevel) {
    this.fatalLevel = fatalLevel.intValue();
  }

  /**
   * @throws RuntimeException if the log record is at or above the fatal log level.
   */
  @Override
  public void publish(LogRecord record) {
    if (record.getLevel().intValue() >= fatalLevel) {
      throw new RuntimeException(record.getLevel() + ": " + record.getMessage(),
          record.getThrown());
    }
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() {
  }
}
