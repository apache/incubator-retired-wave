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

package com.google.wave.api;

import com.google.wave.api.JsonRpcConstant.ParamsProperty;

import java.util.Map;

/**
 * Encapsulates the response of a JSON-RPC call.
 *
 * @author douwe@google.com (Douwe Osinga)
 */
public class JsonRpcResponse {

  private final String id;
  private final Map<ParamsProperty, Object> data;
  private final String errorMessage;

  /**
   * Construct a JSON-RPC error response.
   *
   * @param id the id of the operation request.
   * @param errorMessage the error message.
   * @return an instance of {@link JsonRpcResponse} that represents an error.
   */
  public static JsonRpcResponse error(String id, String errorMessage) {
    return new JsonRpcResponse(id, null, errorMessage);
  }

  /**
   * Construct a JSON-RPC response.
   *
   * @param id the id of the operation request.
   * @param data a key value pairs of data that was returned as a result of
   *     operation.
   * @return an instance of {@link JsonRpcResponse} that represents a success
   *     case.
   */
  public static JsonRpcResponse result(String id, Map<ParamsProperty, Object> data) {
    return new JsonRpcResponse(id, data, null);
  }

  private JsonRpcResponse(String id, Map<ParamsProperty, Object> result, String errorMessage) {
    this.id = id;
    this.data = result;
    this.errorMessage = errorMessage;
  }

  public Map<ParamsProperty, Object> getData() {
    return data;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public String getId() {
    return id;
  }

  public boolean isError() {
    return data == null;
  }
}
