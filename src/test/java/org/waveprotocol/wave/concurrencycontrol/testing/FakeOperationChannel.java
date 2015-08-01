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

package org.waveprotocol.wave.concurrencycontrol.testing;

import org.waveprotocol.wave.concurrencycontrol.channel.OperationChannel;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.List;


/**
 * A minimal fake operation channel.
 *
 * @author zdwang@google.com (David Wang)
 */
public class FakeOperationChannel implements OperationChannel {

  @Override
  public String getDebugString() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<HashedVersion> getReconnectVersions() {
    throw new UnsupportedOperationException();
  }

  @Override
  public WaveletOperation peek() {
    // Does nothing
    return null;
  }

  @Override
  public WaveletOperation receive() {
    // Does nothing
    return null;
  }

  @Override
  public void send(WaveletOperation... operations) {
    // Does nothing
  }

  @Override
  public void setListener(Listener listener) {
    // Does nothing
  }
}
