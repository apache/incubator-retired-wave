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

import com.google.common.collect.Lists;

import junit.framework.TestCase;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Tests basic Log functions.
 *
 *
 */
public class LogTest extends TestCase {

  /** Handler of LogRecords which just adds them to an internal list. */
  private class LogRecordRecorder extends Handler {
    private final List<LogRecord> records = Lists.newArrayList();

    /** {@inheritDoc} */
    @Override
    public void close() throws SecurityException {
    }

    /** {@inheritDoc} */
    @Override
    public void flush() {
    }

    /** {@inheritDoc} */
    @Override
    public void publish(LogRecord record) {
      records.add(record);
      record.getSourceClassName(); // Force inference
    }
  }

  private static int logNumber = 0;

  /**
   * Asserts that some context appears in a message. A bit dodgy in that it
   * searches independently for the key and value strings.
   */
  private void assertContext(LogRecord record, String detail, String key, String value) {
    assertTrue("actual message missing from log", record.getMessage().contains(detail));
    assertTrue("context key '" + key + "' missing", record.getMessage().contains(key));
    assertTrue("context value '" + value + "' missing", record.getMessage().contains(value));
  }

  /**
   * Asserts that some context does not appear in a message. Allows neither the
   * key nor the value to appear.
   */
  private void assertNoContext(LogRecord record, String detail, String key, String value) {
    assertTrue("actual message missing from log", record.getMessage().contains(detail));
    assertFalse("context key '" + key + "' found", record.getMessage().contains(key));
    assertFalse("context value '" + value + "' found", record.getMessage().contains(value));
  }

  /** Gets a new Log that logs to the given handler. */
  private Log getNewLog(Handler handler) {
    Logger logger = Logger.getLogger(LogTest.class.getName() + ".log" + logNumber++);
    logger.addHandler(handler);
    return new Log(logger);
  }

  /** Test that logging actually gives back a record. */
  public void test01LogSomething() {
    LogRecordRecorder handler = new LogRecordRecorder();
    Log log = getNewLog(handler);

    log.info("hi there");
    Exception e = new RuntimeException("Akk");
    log.warning("Warning", e);

    assertEquals(2, handler.records.size());
    assertEquals(LogTest.class.getName(), handler.records.get(0).getSourceClassName());
    assertEquals("test01LogSomething", handler.records.get(0).getSourceMethodName());
    assertEquals(Level.INFO, handler.records.get(0).getLevel());
    assertEquals("hi there", handler.records.get(0).getMessage());
    assertNull(handler.records.get(0).getThrown());

    assertEquals(LogTest.class.getName(), handler.records.get(1).getSourceClassName());
    assertEquals("test01LogSomething", handler.records.get(1).getSourceMethodName());
    assertEquals(Level.WARNING, handler.records.get(1).getLevel());
    assertEquals("Warning", handler.records.get(1).getMessage());
    assertEquals(e, handler.records.get(1).getThrown());
  }

  /** Tests that context information is prepended to log messages. */
  public void test02IncludesContext() {
    LogRecordRecorder handler = new LogRecordRecorder();
    Log log = getNewLog(handler);

    log.severe("initial");
    log.putContext("something", "interesting");
    try {
      log.severe("added");
    } finally {
      log.removeContext("something");
    }
    log.severe("removed");

    assertEquals(3, handler.records.size());
    assertNoContext(handler.records.get(0), "initial", "something", "interesting");
    assertContext(handler.records.get(1), "added", "something", "interesting");
    assertNoContext(handler.records.get(2), "removed", "something", "interesting");
  }

  /** Tests that independent log context is maintained for each thread. */
  public void test03ContextIsPerThread() throws Exception {
    LogRecordRecorder handler = new LogRecordRecorder();
    final Log log = getNewLog(handler);
    ExecutorService service = Executors.newSingleThreadExecutor();
    try {
      log.putContext("something", "interesting");
      log.severe("initial");
      assertEquals(1, handler.records.size());
      assertContext(handler.records.get(0), "initial", "something", "interesting");

      service.submit(new Runnable() {
        /** {@inheritDoc} */
        @Override
        public void run() {
          log.severe("other");
        }
      }).get();

      assertEquals(2, handler.records.size());
      assertNoContext(handler.records.get(1), "other", "something", "interesting");

      service.submit(new Runnable() {
        /** {@inheritDoc} */
        @Override
        public void run() {
          log.putContext("something", "else");
          log.severe("context");
        }
      }).get();

      assertEquals(3, handler.records.size());
      assertContext(handler.records.get(2), "context", "something", "else");

      log.severe("final");
      assertEquals(4, handler.records.size());
      assertContext(handler.records.get(3), "final", "something", "interesting");
    } finally {
      log.removeContext("something");
      service.shutdown();
    }
  }
}
