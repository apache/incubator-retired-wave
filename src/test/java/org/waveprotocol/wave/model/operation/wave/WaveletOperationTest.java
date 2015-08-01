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

package org.waveprotocol.wave.model.operation.wave;


import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Constants;

/**
 * Tests for the abstract wavelet operation.
 *
 * @author anorth@google.com (Alex North)
 */

public class WaveletOperationTest extends OperationTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testOpUpdatesTimestamp() throws OperationException {
    WaveletOperationContext context = new WaveletOperationContext(creator, CONTEXT_TIMESTAMP, 0);
    WaveletOperation op = new NoOp(context);
    op.apply(waveletData);
    assertEquals(CONTEXT_TIMESTAMP, waveletData.getLastModifiedTime());
  }

  public void testOpWithoutTimestampDoesntUpdateTimestamp() throws OperationException {
    WaveletOperationContext context = new WaveletOperationContext(creator,
        Constants.NO_TIMESTAMP, 0);
    WaveletOperation op = new NoOp(context);
    long oldTimestamp = waveletData.getLastModifiedTime();
    op.apply(waveletData);
    assertEquals(oldTimestamp, waveletData.getLastModifiedTime());
  }

  public void testOpUpdatesVersion() throws OperationException {
    WaveletOperationContext context = new WaveletOperationContext(creator, Constants.NO_TIMESTAMP,
        1);
    WaveletOperation op = new NoOp(context);
    long oldVersion = waveletData.getVersion();
    op.apply(waveletData);
    assertEquals(oldVersion + 1, waveletData.getVersion());
  }

  public void testOpUpdatesSignature() throws OperationException {
    WaveletOperationContext context = new WaveletOperationContext(creator, Constants.NO_TIMESTAMP,
        0, CONTEXT_HASHED_VERSION);
    WaveletOperation op = new NoOp(context);
    op.apply(waveletData);
    assertEquals(CONTEXT_HASHED_VERSION, waveletData.getHashedVersion());
  }

  public void testOpWithoutSignatureDoesntUpdateSignature() throws OperationException {
    HashedVersion oldHashedVersion = waveletData.getHashedVersion();
    WaveletOperationContext context = new WaveletOperationContext(creator, Constants.NO_TIMESTAMP,
        0);
    WaveletOperation op = new NoOp(context);
    op.apply(waveletData);
    assertEquals(oldHashedVersion, waveletData.getHashedVersion());
  }

  public void testCreateReverseContextReversesContext() {
    long oldTimestamp = waveletData.getLastModifiedTime();
    long oldVersion = waveletData.getVersion();
    WaveletOperationContext context = new WaveletOperationContext(creator, CONTEXT_TIMESTAMP,
        1, CONTEXT_HASHED_VERSION);
    WaveletOperation op = new NoOp(context);
    WaveletOperationContext reverse = op.createReverseContext(waveletData);

    assertEquals(context.getCreator(), reverse.getCreator());
    assertEquals(-context.getVersionIncrement(), reverse.getVersionIncrement());
    assertEquals(waveletData.getLastModifiedTime(), reverse.getTimestamp());
    assertEquals(waveletData.getHashedVersion(), reverse.getHashedVersion());
  }
}
