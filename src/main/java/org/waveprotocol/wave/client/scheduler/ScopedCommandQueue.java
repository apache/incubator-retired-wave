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

import com.google.gwt.user.client.Command;

import java.util.LinkedList;
import java.util.List;

/**
 * Basic extension of a buffered command queue that enforces that commands are
 * {@link #addCommand(Command) added} only between balanced {@link #open()} and
 * {@link #close()} calls.  Commands are run FIFO on the last close.
 *
 */
public class ScopedCommandQueue implements CommandQueue {
  /** Command queue (FIFO). */
  private final List<Command> commands = new LinkedList<Command>();

  /** Current nesting level. */
  private int level = 0;

  /**
   * Whether this queue is currently open (at any depth).
   *
   * @return whether this queue is open.
   */
  private boolean isOpen() {
    return level > 0;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalStateException if called when this queue is not open.
   */
  public void addCommand(Command c) {
    if (isOpen()) {
      commands.add(c);
    } else {
      throw new IllegalStateException("can not add command to unopened queue");
    }
  }

  /**
   * Opens this queue, allowing commands to be
   * {@link #addCommand(Command) queued}.
   */
  public void open() {
    level++;
  }

  /**
   * Closes this queue, running all commands queued since it was opened.
   *
   * @throws IllegalStateException if this queue is not open.
   */
  public void close() {
    if (!isOpen()) {
      throw new IllegalStateException("can not close unopened queue");
    } else {
      if (level == 1) {
        try {
          // Run all comments
          while (!commands.isEmpty()) {
            commands.remove(0).execute();
          }
        } finally {
          level = 0;
        }
      } else {
        level--;
      }
    }
  }
}
