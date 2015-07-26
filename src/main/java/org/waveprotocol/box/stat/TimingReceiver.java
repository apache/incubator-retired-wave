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
package org.waveprotocol.box.stat;

import org.waveprotocol.box.common.Receiver;

/**
 * Receiver proxy for profiling.
 *
 * @author akaplanov@gmail.com (Andrew Kaplanov)
 */
@SuppressWarnings({"rawtypes"})
public class TimingReceiver<T> implements Receiver<T> {
  private Receiver<T> receiver;
  private String name;

  public TimingReceiver() {
  }

  public Receiver init(Receiver <T> receiver, String name) {
    this.receiver = receiver;
    this.name = name;
    return this;
  }

  @Override
  public boolean put(T obj) {
    Timer timer = Timing.start(name);
    try {
      return receiver.put(obj);
    } finally {
      Timing.stop(timer);
    }
  }
}
