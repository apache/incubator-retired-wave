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

package org.waveprotocol.box.server.rpc;

import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

/**
 * Implements the client end-point of a wave server RPC connection. This class
 * implements {{@link #reset} and is reusable across multiple RPC calls backed
 * onto the same ClientRpcChannel.
 * 
 * TODO: This class is not currently thread-safe and has some
 * concurrency issues.
 * 
 *
 */
class ClientRpcController implements RpcController {

  enum Status {
    PENDING, ACTIVE, COMPLETE
  }
  
  /**
   * Internal status class to manage the state of a specific outgoing RPC.
   */
  static class RpcState {
    private final ClientRpcChannel creator;
    private final boolean isStreamingRpc;
    private final RpcCallback<Message> callback;
    private final Runnable cancelRpc;
    private boolean complete = false;
    private boolean failed = false;
    private String errorText = null;

    RpcState(ClientRpcChannel creator, boolean isStreamingRpc, RpcCallback<Message> callback,
        Runnable cancelRpc) {
      this.creator = creator;
      this.isStreamingRpc = isStreamingRpc;
      this.callback = callback;
      this.cancelRpc = cancelRpc;
    }
  }

  // The ClientRpcChannel instance that owns this class.
  private final ClientRpcChannel owner;

  // Represents the current call, or null if this is a pending controller.
  private RpcState state = null;

  /**
   * Package-public constructor, to be invoked by instances of ClientRpcChannel.
   */
  ClientRpcController(ClientRpcChannel owner) {
    this.owner = owner;
    state = null;
  }

  /**
   * Returns the current status of this class in terms of the {@link Status}
   * enum.
   */
  Status status() {
    return state == null ? Status.PENDING : (state.complete ? Status.COMPLETE : Status.ACTIVE);
  }
  
  /**
   * Assert that this controller is in the given status.
   */
  private void checkStatus(Status statusToAssert) {
    Status currentStatus = status();
    if (!currentStatus.equals(statusToAssert)) {
      throw new IllegalStateException("Controller expected status " + statusToAssert + ", was "
          + currentStatus);
    }
  }
  
  /**
   * Configure this RpcController with a new RpcStatus instance.
   */
  void configure(RpcState state) {
    checkStatus(Status.PENDING);
    if (this.state != null) {
      throw new IllegalStateException("Can't configure this RPC, already configured.");
    } else if (!owner.equals(state.creator)) {
      throw new IllegalArgumentException("Should only be configured by " + owner
          + ", configuration attempted by " + state.creator);
    }
    this.state = state;
  }

  /**
   * Provide a response to this RpcController. Intercepts valid completion
   * conditions in order to mark a RPC as complete. Passes through all messages
   * to the internal callback for the current RPC invocation.
   */
  void response(Message message) {
    checkStatus(Status.ACTIVE);
    // Any message will complete a normal RPC, whereas only a null message will
    // end a streaming RPC.
    if (!state.isStreamingRpc) {
      if (message == null) {
        // The server end-point should not actually allow non-streaming RPCs
        // to call back with null messages - we should never get here.
        throw new IllegalStateException("Normal RPCs should not be completed early.");
      } else {
        // Normal RPCs will complete on any valid incoming message.
        state.complete = true;
      }
    } else if (message == null) {
      // Complete this streaming RPC with this blank message.
      state.complete = true;
    }
    try {
      state.callback.run(message);
    } catch (RuntimeException e) {
      e.printStackTrace();
    }
  }

  /**
   * Indicate that the RPC has failed. This requires that the RPC is currently
   * active, and marks this RPC as complete.
   */
  void failure(String errorText) {
    checkStatus(Status.ACTIVE);
    state.complete = true;
    state.failed = true;
    state.errorText = errorText;

    // Hint to the internal callback that this RPC is finished (Normal RPCs
    // will always understand this as an error case, whereas streaming RPCs
    // will have to check their controller).
    state.callback.run(null);
  }

  @Override
  public String errorText() {
    return failed() ? state.errorText : null;
  }

  @Override
  public boolean failed() {
    checkStatus(Status.COMPLETE);
    return state.failed;
  }

  @Override
  public boolean isCanceled() {
    throw new UnsupportedOperationException("Server-side method of RpcController only.");
  }

  @Override
  public void notifyOnCancel(RpcCallback<Object> callback) {
    throw new UnsupportedOperationException("Server-side method of RpcController only.");
  }

  @Override
  public void reset() {
    checkStatus(Status.COMPLETE);
    state = null;
  }

  @Override
  public void setFailed(String reason) {
    throw new UnsupportedOperationException("Server-side method of RpcController only.");
  }

  @Override
  public void startCancel() {
    Status status = status();
    if (status == Status.PENDING) {
      throw new IllegalStateException("Can't cancel this RPC, not currently active.");
    } else if (status == Status.COMPLETE) {
      // We drop these requests silently - since there is no way for the client
      // to know whether the RPC has finished while they are setting up their
      // cancellation.
    } else {
      state.cancelRpc.run();
    }
  }
}