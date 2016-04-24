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

package org.waveprotocol.wave.client.scheduler;

import com.google.gwt.junit.client.GWTTestCase;

import org.waveprotocol.wave.client.scheduler.Scheduler.Priority;
import org.waveprotocol.wave.client.scheduler.Scheduler.Schedulable;

/**
 * Invasive test for one of the scheduler's data structures.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */

public class DelayedJobRegistryGwtTest extends GWTTestCase {

  public void testDelayedJobRegistry() {
    DelayedJobRegistry r = new DelayedJobRegistry();

    Schedulable[] tasks = new Scheduler.Task[10];
    TaskInfo[] info = new TaskInfo[10];
    for (int i = 0; i < 10; i++) {
      tasks[i] = new Scheduler.Task() {
        public void execute() {
        }
      };
      info[i] = new TaskInfo(Priority.HIGH, tasks[i]);
    }
    info[0] = new TaskInfo(Priority.HIGH, 0.0, -1.0, tasks[0]);
    info[1] = new TaskInfo(Priority.HIGH, 0.0, 10.0, tasks[1]);

    String id1 = info[0].id;
    String id2 = info[1].id;
    String id3 = info[2].id;

    assertTrue(r.debugIsClear());
    assertFalse(r.has(id1));
    assertFalse(r.has(id2));
    assertEquals(-1, r.getNextDueDelayedJobTime(), 0.001);
    assertEquals(null, r.getDueDelayedJob(0));
    assertEquals(null, r.getDueDelayedJob(10000));
    assertEquals(null, r.getDueDelayedJob(Double.MAX_VALUE));
    r.removeDelayedJob(id1); // just check doesn't throw exception when id not
                            // found
    r.removeDelayedJob(id2);

    r.addDelayedJob(info[0]);
    r.addDelayedJob(info[1]);
    assertTrue(r.has(id1));
    assertTrue(r.has(id2));
    Schedulable j0 = r.getDueDelayedJob(0);
    Schedulable j1 = r.getDueDelayedJob(0); // because it will be scheduled
                                            // slightly later than 0
    assertTrue(j0 != j1);
    assertTrue(j0 == tasks[0]);
    assertTrue(j1 == tasks[1]);

    assertFalse(r.has(id1));
    info[1].calculateNextExecuteTime(0);
    r.addDelayedJob(info[1]);
    assertTrue(r.has(id2));
    assertNull(r.getDueDelayedJob(0));

    checkTimeAdvance(r, info[1], 10);
    assertNull(r.getDueDelayedJob(19));
    checkTimeAdvance(r, info[1], 20);
    checkTimeAdvance(r, info[1], 50);
    assertNull(r.getDueDelayedJob(50)); // Won't get scheduled more times if
                                        // time runs out
    assertTrue(r.has(id2));
    r.removeDelayedJob(id2);
    assertTrue(r.debugIsClear());
    assertFalse(r.has(id2));
    assertNull(r.getDueDelayedJob(100));

    // TODO(danilatos): More?
  }

  private void checkTimeAdvance(DelayedJobRegistry r, TaskInfo info, double time) {
    assertEquals(info.job, r.getDueDelayedJob(time));
    info.calculateNextExecuteTime(time);
    r.addDelayedJob(info);
    assertNull(r.getDueDelayedJob(time));
  }

  @Override
  public String getModuleName() {
    return "org.waveprotocol.wave.client.scheduler.tests";
  }
}
