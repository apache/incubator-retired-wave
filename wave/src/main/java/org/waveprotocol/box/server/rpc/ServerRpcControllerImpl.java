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

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.Service;

import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Implements the server end-point of a wave server RPC connection. This is a
 * single-use RPC controller.
 *
 *
 */
public class ServerRpcControllerImpl implements ServerRpcController {
  private final Message requestMessage;
  private final Service backingService;
  private final Descriptors.MethodDescriptor serviceMethod;
  private final RpcCallback<Message> callback;
  private final boolean isStreamingRpc;

  // The following variables represent the current status of this instance, and
  // must all only be accessed or modified while synchronised on statusLock.
  private final Object statusLock = new Object();
  private boolean complete = false;
  private RpcCallback<Object> cancelCallback = null;
  private boolean cancelled = false;
  private final ParticipantId loggedInUser;

  /**
   * Instantiate a new ServerRpcController that may later be completely invoked
   * by calling {#link run}.
   *
   * @param requestMessage the request being handled
   * @param backingService the backing service type
   * @param serviceMethod the specific method within the backing service type
   * @param loggedInUser the currently logged in user
   * @param callback the destination where responses may be passed - may be
   *        called once (normal RPC) or 1-n times (streaming RPC), and will pass
   *        instances of RpcFinished as required (error cases, or streaming RPC
   *        shutdown); is also always called under the ServerRpcController's
   *        statusLock to ensure that consecutive calls (in the streaming case)
   *        are called in series
   */
  ServerRpcControllerImpl(Message requestMessage, Service backingService,
      Descriptors.MethodDescriptor serviceMethod, ParticipantId loggedInUser, RpcCallback<Message> callback) {
    this.requestMessage = requestMessage;
    this.backingService = backingService;
    this.serviceMethod = serviceMethod;
    this.loggedInUser = loggedInUser;
    this.isStreamingRpc = serviceMethod.getOptions().getExtension(Rpc.isStreamingRpc);
    this.callback = callback;
  }

  @Override
  public String errorText() {
    throw new UnsupportedOperationException("Client-side method of RpcController only.");
  }

  @Override
  public boolean failed() {
    throw new UnsupportedOperationException("Client-side method of RpcController only.");
  }

  @Override
  public boolean isCanceled() {
    return cancelled;
  }

  /**
   * Registers a cancellation callback. This will always be called as part of
   * this RPC, and always at most once; either when the client asks to cancel
   * it, or when the RPC finishes (regardless of error case).
   *
   * This callback will be called outside normal locks on ServerRpcController
   * state, i.e., not within a block synchronised on statusLock.
   */
  @Override
  public void notifyOnCancel(final RpcCallback<Object> callback) {
    RpcCallback<Object> runCallback = null;
    synchronized (statusLock) {
      if (cancelCallback != null) {
        throw new IllegalStateException("Must only be called once per request.");
      } else {
        cancelCallback = callback;
        if (cancelled || complete) {
          runCallback = cancelCallback;
        }
      }
    }
    if (runCallback != null) {
      runCallback.run(null);
    }
  }

  @Override
  public void reset() {
    throw new UnsupportedOperationException("Client-side method of RpcController only.");
  }

  @Override
  public void setFailed(String reason) {
    RpcCallback<Object> runCallback = null;
    synchronized (statusLock) {
      if (complete) {
        throw new IllegalStateException("Can't fail this RPC, as it is already complete.");
      } else {
        complete = true;
        callback.run(Rpc.RpcFinished.newBuilder().setFailed(true).setErrorText(reason).build());
        if (cancelCallback != null && !cancelled) {
          runCallback = cancelCallback;
        }
      }
    }
    if (runCallback != null) {
      runCallback.run(null);
    }
  }

  @Override
  public void startCancel() {
    throw new UnsupportedOperationException("Client-side method of RpcController only.");
  }

  @Override
  public void cancel() {
    RpcCallback<Object> runCallback = null;
    synchronized (statusLock) {
      if (cancelled) {
        throw new IllegalStateException("Can't cancel RPC, already cancelled.");
      }
      cancelled = true;
      if (cancelCallback != null && !complete) {
        runCallback = cancelCallback;
      }
    }
    if (runCallback != null) {
      runCallback.run(null);
    }
  }

  /**
   * Run this ServerRpcController in the current thread. This must only be
   * invoked ONCE, and will throw an IllegalStateException otherwise.
   */
  @Override
  public void run() {
    RpcCallback<Message> messageCallback = new RpcCallback<Message>() {
      @Override
      public void run(Message result) {
        RpcCallback<Object> runCallback = null;
        synchronized (statusLock) {
          if (complete) {
            throw new IllegalStateException("Can't send responses over this RPC, as it is"
                + " already complete: " + result);
          }
          if (!isStreamingRpc || result == null) {
            // This either completes the streaming RPC (by passing an instance
            // of RpcFinished in place of null) or completes a normal RPC (by
            // passing any other message).
            if (result == null) {
              result = Rpc.RpcFinished.newBuilder().setFailed(false).build();
            }
            callback.run(result);

            // Now complete, mark as such and invoke the cancellation callback.
            complete = true;
            if (cancelCallback != null && !cancelled) {
              runCallback = cancelCallback;
            }
          } else {
            // Streaming RPC update.
            callback.run(result);
          }
        }
        if (runCallback != null) {
          runCallback.run(null);
        }
      }
    };
    try {
      backingService.callMethod(serviceMethod, this, requestMessage, messageCallback);
    } catch (RuntimeException e) {
      // Pass the description of any RuntimeException back to the caller.
      e.printStackTrace();
      if (!complete) {
        setFailed(e.toString());
      }
    }
  }

  @Override
  public ParticipantId getLoggedInUser() {
    return loggedInUser;
  }
}
