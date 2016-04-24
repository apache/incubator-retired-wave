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

package com.google.wave.api.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;

import junit.framework.TestCase;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Test cases for {@link OperationRequestGsonAdaptor}.
 */
public class OperationRequestGsonAdaptorRobotTest extends TestCase {

  public void testDeserialize() throws Exception {
    String operation = "{'id':'op1','method':'wavelet.setTitle','params':{" +
        "'waveId':'1','waveletId':'2','waveletTitle':'Title','unknown':'value'}}";
    JsonElement jsonElement = new JsonParser().parse(operation);

    JsonDeserializationContext mockContext = mock(JsonDeserializationContext.class);
    when(mockContext.deserialize(any(JsonElement.class), eq(String.class))).thenAnswer(
        new Answer<String>() {
          public String answer(InvocationOnMock invocation) {
            return ((JsonPrimitive) (invocation.getArguments()[0])).getAsString();
          }
        });

    OperationRequestGsonAdaptor adaptor = new OperationRequestGsonAdaptor();
    OperationRequest result = adaptor.deserialize(jsonElement, null, mockContext);
    assertEquals("op1", result.getId());
    assertEquals("wavelet.setTitle", result.getMethod());
    assertEquals("1", result.getWaveId());
    assertEquals("2", result.getWaveletId());
    assertNull(result.getBlipId());
    assertEquals(3, result.getParams().size());
    assertEquals("Title", result.getParameter(ParamsProperty.WAVELET_TITLE));
  }
}
