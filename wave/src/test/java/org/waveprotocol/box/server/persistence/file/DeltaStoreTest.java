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

package org.waveprotocol.box.server.persistence.file;

import com.google.common.collect.ImmutableList;

import com.typesafe.config.ConfigFactory;
import org.waveprotocol.box.server.persistence.DeltaStoreTestBase;
import org.waveprotocol.box.server.waveserver.DeltaStore;
import org.waveprotocol.box.server.waveserver.WaveletDeltaRecord;
import org.waveprotocol.box.server.waveserver.DeltaStore.DeltasAccess;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.util.logging.Log;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Tests for FileDeltaStore.
 *
 * @author Joseph Gentle (josephg@gmail.com)
 */
public class DeltaStoreTest extends DeltaStoreTestBase {

  private static final Log LOG = Log.get(DeltaStoreTest.class);

  private File path;
  private final WaveletName WAVE1_WAVELET1 =
    WaveletName.of(WaveId.of("example.com", "wave1"), WaveletId.of("example.com", "wavelet1"));

  @Override
  protected void setUp() throws Exception {
    path = FileUtils.createTemporaryDirectory();
    super.setUp();
  }

  @Override
  protected DeltaStore newDeltaStore() {
    return new FileDeltaStore(ConfigFactory.parseString("core.delta_store_directory : " + path
      .getAbsolutePath()));
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    org.apache.commons.io.FileUtils.deleteDirectory(path);

    // This assertion may fail if a test hasn't closed all streams.
    assertFalse(path.exists());
  }

  // Test the delta store strips partially written data.
  public void testRecoverFromTruncatedDeltas() throws Exception {
    // Create an entry with one record. Shrink the file byte by byte and ensure
    // we can read it without crashing.
    DeltaStore store = newDeltaStore();
    WaveletDeltaRecord written = createRecord();
    File deltaFile = FileDeltaCollection.deltasFile(path.getAbsolutePath(), WAVE1_WAVELET1);


    long toRemove = 1;
    while (true) {
      // This generates the full file.
      DeltasAccess wavelet = store.open(WAVE1_WAVELET1);
      wavelet.append(ImmutableList.of(written));
      wavelet.close();

      RandomAccessFile file = new RandomAccessFile(deltaFile, "rw");
      if (toRemove > file.length()) {
        file.close();
        break;
      }
      // eat the planned number of bytes
      LOG.info("trying to remove " + toRemove + " bytes");
      file.setLength(file.length() - toRemove);
      file.close();

      wavelet = store.open(WAVE1_WAVELET1);
      WaveletDeltaRecord read = wavelet.getDelta(0);
      assertNull("got an unexpected record " + read, read);
      wavelet.close();
      toRemove++;
    }
  }
}
