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

package org.waveprotocol.wave.concurrencycontrol.channel;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * A mock ConnectionListener that records method calls made to
 * onOperationReceived() for future inspection.
 *
 * @author zdwang@google.com (David Wang)
 */
class MockOperationChannelListener implements OperationChannel.Listener {

  private int numOperationsReceived = 0;
  private final ArrayList<OperationChannel.Listener> listeners =
      new ArrayList<OperationChannel.Listener>();

  // Allow a test to hook into the callbacks.
  public void addListener(OperationChannel.Listener newListener) {
    listeners.add(newListener);
  }

  @Override
  public void onOperationReceived() {
    numOperationsReceived++;
    for (OperationChannel.Listener l : listeners) {
      l.onOperationReceived();
    }
  }

  /**
   * @return The number of previous calls to {@link #onOperationReceived()}.
   */
  public int getEventCount() {
    return numOperationsReceived;
  }

  public void clear() {
    numOperationsReceived = 0;
  }

  public void checkOpsReceived(int expectedOpCount) {
    Assert.assertEquals(expectedOpCount, numOperationsReceived);
  }
}
