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

package org.waveprotocol.box.server.rpc.testing;

import static org.waveprotocol.box.server.util.testing.TestingConstants.USER;

import com.google.protobuf.RpcCallback;

import org.waveprotocol.box.server.rpc.ServerRpcController;
import org.waveprotocol.wave.model.wave.ParticipantId;


/**
  * An {@code RpcController} that just handles error text and failure condition.
  */
public class FakeServerRpcController implements ServerRpcController {
  private boolean failed = false;
  private String errorText = null;

  @Override
  public String errorText() {
    return errorText;
  }

  @Override
  public boolean failed() {
    return failed;
  }

  @Override
  public boolean isCanceled() {
    return false;
  }

  @Override
  public void notifyOnCancel(RpcCallback<Object> arg) {
  }

  @Override
  public void reset() {
    failed = false;
    errorText = null;
  }

  @Override
  public void setFailed(String error) {
    failed = true;
    errorText = error;
  }

  @Override
  public void startCancel() {
  }

  @Override
  public ParticipantId getLoggedInUser() {
    return ParticipantId.ofUnsafe(USER);
  }

  @Override
  public void cancel() {
  }

  @Override
  public void run() {
  }
}
