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

/**
 * The logger listener that may want to do something when the logger changes.
 * @author zdwang@google.com (David)
 */
public interface LoggerListener {
  /**
   * Call this to notify a new logger of the given name was created.
   */
  public void onNewLogger(String loggerName);

  /**
   * A logger really wants to log something but the output is not yet given to it.
   */
  public void onNeedOutput();

  /**
   * Notify listeners that an error log message has been triggered.
   */
  public void onError();

  /**
   * Notify listeners that a fatal log message has been triggered.
   */
  public void onFatal();
}
