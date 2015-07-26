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

package org.waveprotocol.box.server.robots.util;

import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.util.logging.Log;

/**
 * {@link SubmitRequestListener} that simply logs its calls.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public final class LoggingRequestListener implements SubmitRequestListener {

  private final Log log;

  /**
   * Constructs a new {@link SubmitRequestListener} that logs its calls to the
   * given log.
   *
   * @param log the log to use.
   */
  public LoggingRequestListener(Log log) {
    this.log = log;
  }

  @Override
  public void onFailure(String errorMessage) {
    log.warning("Robot operations failed to be submitted: " + errorMessage);
  }

  @Override
  public void onSuccess(int operationsApplied, HashedVersion hashedVersionAfterApplication,
      long applicationTimestamp) {
    log.fine(operationsApplied + " Robot operations have been succesfully applied "
        + "changing the version to " + hashedVersionAfterApplication);
  }
}
