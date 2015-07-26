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

/**
 * A command that will be notified if it isn't going to be execute()d.
 *
 */
public interface CancellableCommand extends Command {
  /**
   * A do-nothing instance.
   */
  public static final CancellableCommand NO_OP = new CancellableCommand() {
    @Override
    public void execute() {}

    @Override
    public void onCancelled() {}
  };

  /**
   * Indicate to this command that it will never be execute()d.
   */
  void onCancelled();
}
