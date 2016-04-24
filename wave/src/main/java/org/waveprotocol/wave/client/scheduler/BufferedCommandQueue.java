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
 * Basic command queue that queues up commands and runs them all in
 * {@link #execute()}.
 *
 */
public class BufferedCommandQueue implements Command, CommandQueue {
  /** Queued commands, with a FIFO-suitable implementation. */
  private final List<Command> commands = new LinkedList<Command>();

  /** {@inheritDoc} */
  public void addCommand(Command c) {
    commands.add(c);
  }

  /**
   * Runs all the commands in the queue until it's empty.
   */
  public void execute() {
    while (!commands.isEmpty()) {
      commands.remove(0).execute();
    }
  }

  /**
   * @return whether there are any queued commands.
   */
  public boolean isEmpty() {
    return commands.isEmpty();
  }
}
